<script setup lang="ts">
import { nextTick, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { showToast } from "@repo/shared";
import {
  getTablesApi,
  getViewsApi,
  getFunctionsApi,
  getProceduresApi,
  getSynonymsApi
} from "../../../apis/datasource";

const { t } = useI18n({ useScope: "global" });

interface Props {
  /** 当前数据源 ID（空/null 表示未选择 — q-select clearable 清空时返回 null） */
  datasourceId: string | null;
  /** 数据库类型（控制示例 SQL 方言） */
  dbType: string;
  /** 数据源配置的数据库（数据中台语义：仅展示配置的库，不枚举连接用户可见的全部库） */
  database: string;
  /** 最大行数（生成示例 SELECT 用） */
  maxRows: number;
  /** 对象筛选关键字（由父级工具栏维护） */
  filter: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  /** 双击表/视图触发的即时查询 */
  run: [sql: string];
  /** 向编辑器光标处插入文本 */
  insert: [text: string];
  /** 已加载的 Schema 变化（表名 → 字段列表，供编辑器补全） */
  "schema-change": [schema: Record<string, string[]>];
}>();

// ── 树节点模型 ──

type NodeKind =
  | "database"
  | "schema"
  | "category"
  | "table"
  | "view"
  | "function"
  | "procedure"
  | "synonym"
  | "empty";

type Category = "tables" | "views" | "functions" | "procedures" | "synonyms";

interface TreeNode {
  key: string;
  label: string;
  kind: NodeKind;
  category?: Category;
  database?: string;
  schema?: string;
  lazy?: boolean;
  children?: TreeNode[];
}

const treeRef = ref();
/** 用于强制重挂载 q-tree，重置展开与懒加载状态 */
const treeKey = ref(0);
const nodes = ref<TreeNode[]>([]);
/** 当前选中节点 key — 与部门树一致：单击选中 */
const selectedKey = ref<string>("");
/** 展开节点 key 列表 — v-model:expanded，由 q-tree 内部维护（setExpanded 会同步更新） */
const expandedKeys = ref<string[]>([]);
/** 各节点子项数量（懒加载后填充，用于计数徽章展示） */
const nodeCounts = ref<Record<string, number>>({});

/** 自动补全 Schema（表名 → 字段列表），随懒加载增量维护 */
const schemaMap = ref<Record<string, string[]>>({});

// ── 节点头部点击计数器 — 与部门树一致：区分单击/双击 ──
// 必须在 watch(immediate: true) 之前声明，否则 rebuild() → resetClickState()
// 访问 let 变量时会触发 TDZ（暂时性死区）ReferenceError
let _nodeClickTimer: ReturnType<typeof setTimeout> | null = null;
let _nodeClickCount = 0;
let _nodeClickKey = "";

function resetClickState() {
  if (_nodeClickTimer) clearTimeout(_nodeClickTimer);
  _nodeClickCount = 0;
  _nodeClickKey = "";
  _nodeClickTimer = null;
}

// ── 节点图标配置 ──

/** 实际对象节点图标 — 容器节点（database/schema/category）统一用文件夹 */
const NODE_ICON: Record<NodeKind, string> = {
  database: "sym_r_folder",
  schema: "sym_r_folder",
  category: "sym_r_folder",
  table: "sym_r_table",
  view: "sym_r_view_list",
  function: "sym_r_function",
  procedure: "sym_r_code_blocks",
  synonym: "sym_r_match_word",
  empty: "sym_r_remove"
};

const CATEGORY_LABEL: Record<Category, string> = {
  tables: "catTables",
  views: "catViews",
  functions: "catFunctions",
  procedures: "catProcedures",
  synonyms: "catSynonyms"
};

const categoryApi: Record<Category, typeof getTablesApi> = {
  tables: getTablesApi,
  views: getViewsApi,
  functions: getFunctionsApi,
  procedures: getProceduresApi,
  synonyms: getSynonymsApi
};

// ── 节点判断与图标 ──

/** 判断节点是否可展开（有子节点或懒加载） — 表/视图不再可展开 */
function isExpandable(node: TreeNode): boolean {
  return node.kind === "database" || node.kind === "schema" || node.kind === "category";
}

/** 节点图标 — 容器节点（database/schema/category）随展开状态切换 folder/folder_open，对象节点固定 */
function nodeIcon(node: TreeNode): string {
  if (node.kind === "database" || node.kind === "schema" || node.kind === "category") {
    return expandedKeys.value.includes(node.key) ? "sym_r_folder_open" : "sym_r_folder";
  }
  return NODE_ICON[node.kind];
}

/** 节点图标颜色 — 通过 CSS color: inherit 与文字颜色保持一致，无需 Quasar 颜色类 */

/** 是否在节点上显示计数徽章 — 分类节点始终显示（含 0），其余不显示 */
function showCount(node: TreeNode): boolean {
  return node.kind === "category" && nodeCounts.value[node.key] != null;
}

// ── 树构建与刷新 ──

function categoryNodes(database?: string): TreeNode[] {
  // 所有数据库类型统一展示 5 个分类节点，顺序：表 → 视图 → 同义词 → 函数 → 存储过程
  const categories: Category[] = ["tables", "views", "synonyms", "functions", "procedures"];
  return categories.map((category) => ({
    key: `cat|${database ?? ""}|${category}`,
    label: t(`sqlQuery.${CATEGORY_LABEL[category]}`),
    kind: "category",
    category,
    database,
    lazy: true
  }));
}

function objectNode(parent: TreeNode, category: Category, name: string): TreeNode {
  const kind = category === "tables" ? "table"
    : category === "views" ? "view"
    : category === "functions" ? "function"
    : category === "procedures" ? "procedure"
    : "synonym";
  return {
    key: `obj|${parent.database}|${category}|${name}`,
    label: name,
    kind,
    database: parent.database
  };
}

function emptyNode(parent: TreeNode): TreeNode {
  return {
    key: `${parent.key}|empty`,
    label: t("sqlQuery.treeEmpty"),
    kind: "empty"
  };
}

/**
 * 按数据源配置的数据库构建树：
 * 配置了库名 → 根节点为该库，下挂分类节点；
 * 未配置库名 → 分类节点直接作为根（元数据 API 使用连接默认库）。
 *
 * 后续 Oracle 等支持 Schema 的数据库，可在 database 与 category 之间
 * 增加 schema 层级（NodeKind = "schema"，图标 sym_r_schema）。
 * MySQL 无 Schema 概念，不显示此层级。
 */
function rebuild() {
  schemaMap.value = {};
  emit("schema-change", {});
  selectedKey.value = "";
  nodeCounts.value = {};
  resetClickState();
  if (!props.datasourceId) {
    nodes.value = [];
    expandedKeys.value = [];
    return;
  }
  const database = props.database.trim();
  nodes.value = database
    ? [{
        key: `db|${database}`,
        label: database,
        kind: "database",
        database,
        children: categoryNodes(database)
      }]
    : categoryNodes();
  treeKey.value++;
  // 默认展开配置的库节点，露出分类
  // 必须在 nextTick 后调用 setExpanded：treeKey 自增触发 q-tree 重新挂载，
  // 需等 DOM 更新完成后 treeRef 才指向新组件实例
  if (database) {
    void nextTick(() => treeRef.value?.setExpanded(`db|${database}`, true));
  }
  // 预加载各分类的对象数量（并行请求，即使为 0 也显示）
  loadCategoryCounts(database);
}

/** 并行拉取 5 个分类的对象数量，填充 nodeCounts */
async function loadCategoryCounts(database?: string) {
  const dsId = props.datasourceId;
  if (!dsId) return;
  const categories: Category[] = ["tables", "views", "synonyms", "functions", "procedures"];
  const results = await Promise.allSettled(
    categories.map((cat) => categoryApi[cat](dsId, database))
  );
  const counts: Record<string, number> = {};
  for (let i = 0; i < categories.length; i++) {
    const cat = categories[i];
    const key = `cat|${database ?? ""}|${cat}`;
    const result = results[i];
    counts[key] = result.status === "fulfilled" && result.value.code === 10_000 && result.value.data
      ? result.value.data.length
      : 0;
  }
  nodeCounts.value = { ...nodeCounts.value, ...counts };
}

defineExpose({ refresh: rebuild });

watch(
  () => [props.datasourceId, props.database],
  rebuild,
  { immediate: true }
);

// ── q-tree 懒加载：分类节点拉取对象列表，表/视图节点拉取字段 ──

async function onLazyLoad({
  node,
  done,
  fail
}: {
  node: TreeNode;
  done: (children: TreeNode[]) => void;
  fail: () => void;
}) {
  try {
    if (node.kind === "category" && node.category) {
      const res = await categoryApi[node.category](props.datasourceId!, node.database);
      if (res.code !== 10_000 || !res.data) {
        fail();
        showToast(res.message || t("sqlQuery.treeLoadFail"), "negative");
        return;
      }
      // 表清单先注入补全 Schema（字段随后懒加载补齐）
      if (node.category === "tables") {
        for (const name of res.data) {
          if (!schemaMap.value[name]) schemaMap.value[name] = [];
        }
        emit("schema-change", { ...schemaMap.value });
      }
      const children = res.data.length
        ? res.data.map((name) => objectNode(node, node.category as Category, name))
        : [emptyNode(node)];
      nodeCounts.value = { ...nodeCounts.value, [node.key]: res.data.length };
      done(children);
    } else {
      done([]);
    }
  } catch {
    fail();
  }
}

// ── 筛选 ──

function filterFn(node: TreeNode, filter: string) {
  return node.label.toLowerCase().includes(filter.toLowerCase());
}

// ── 示例 SQL 生成（按方言） ──

function sampleSql(table: string): string {
  const limit = props.maxRows || 1000;
  if (props.dbType === "ORACLE" || props.dbType === "DAMENG") {
    return `SELECT * FROM ${table} FETCH FIRST ${limit} ROWS ONLY`;
  }
  if (props.dbType === "SQLSERVER") {
    return `SELECT TOP ${limit} * FROM ${table}`;
  }
  return `SELECT * FROM ${table} LIMIT ${limit}`;
}

// ── 节点交互 — 与部门树一致 ──

/**
 * 切换节点展开/收起。
 * 必须使用 treeRef.setExpanded() 走 q-tree 内部 API，
 * 这样才能正确触发 lazy 节点的 @lazy-load 事件。
 * 直接修改 expandedKeys 数组只会更新视觉状态，不会触发懒加载。
 */
function toggleNode(node: TreeNode) {
  if (!isExpandable(node)) return;
  const isExpanded = expandedKeys.value.includes(node.key);
  treeRef.value?.setExpanded(node.key, !isExpanded);
}

/** 节点头部点击 — 可展开节点单击即展开/收缩；对象节点用计数器区分单击/双击 */
function onNodeHeaderClick(node: TreeNode) {
  // 始终更新选中状态
  selectedKey.value = node.key;

  // 可展开节点（database / schema / category）：单击即切换展开/收缩
  if (isExpandable(node)) {
    toggleNode(node);
    return;
  }

  // 对象节点（table / view / function / procedure / synonym）：用计数器区分单击/双击
  const key = node.key;
  _nodeClickCount++;

  if (_nodeClickTimer) clearTimeout(_nodeClickTimer);

  if (_nodeClickCount === 1) {
    _nodeClickTimer = setTimeout(() => {
      // 单击：仅选中，无额外操作
      _nodeClickCount = 0;
      _nodeClickKey = "";
      _nodeClickTimer = null;
    }, 280);
  } else if (_nodeClickCount >= 2) {
    if (_nodeClickKey === key) {
      // 双击：执行对象节点操作
      handleNodeDblClick(node);
    }
    _nodeClickCount = 0;
    _nodeClickKey = "";
    _nodeClickTimer = null;
  }

  _nodeClickKey = key;
}

/** 双击处理：表/视图执行查询，函数/存储过程/同义词插入名称 */
function handleNodeDblClick(node: TreeNode) {
  if (node.kind === "table" || node.kind === "view") {
    emit("run", sampleSql(node.label));
  } else if (node.kind === "function"
    || node.kind === "procedure" || node.kind === "synonym") {
    emit("insert", node.label);
  }
}

// ── 右键菜单 ──

interface CtxItem {
  key: string;
  label: string;
  icon: string;
  action: () => void;
}

function ctxItems(node: TreeNode): CtxItem[] {
  const copyName = () => void copyToClipboard(node.label);
  const insertName = () => emit("insert", node.label);

  if (node.kind === "table" || node.kind === "view") {
    return [
      { key: "query", label: t("sqlQuery.menuQuery"), icon: "sym_r_play_arrow", action: () => handleNodeDblClick(node) },
      { key: "insertSelect", label: t("sqlQuery.menuInsertSelect"), icon: "sym_r_code", action: () => emit("insert", sampleSql(node.label)) },
      { key: "insertName", label: t("sqlQuery.menuInsertName"), icon: "sym_r_input", action: insertName },
      { key: "copy", label: t("sqlQuery.menuCopyName"), icon: "sym_r_content_copy", action: copyName }
    ];
  }
  if (node.kind === "database" || node.kind === "schema") {
    return [
      { key: "copy", label: t("sqlQuery.menuCopyName"), icon: "sym_r_content_copy", action: copyName }
    ];
  }
  if (node.kind === "empty" || node.kind === "category") {
    return [];
  }
  return [
    { key: "insertName", label: t("sqlQuery.menuInsertName"), icon: "sym_r_input", action: insertName },
    { key: "copy", label: t("sqlQuery.menuCopyName"), icon: "sym_r_content_copy", action: copyName }
  ];
}

async function copyToClipboard(text: string) {
  try {
    await navigator.clipboard.writeText(text);
    showToast(t("sqlQuery.copySuccess"), "positive");
  } catch {
    showToast(t("sqlQuery.copyFail"), "negative");
  }
}
</script>

<template>
  <div class="db-tree">
    <div v-if="!datasourceId" class="db-tree-state">
      <q-icon name="sym_r_database_search" size="34px" />
      <span>{{ t("sqlQuery.treeNoDatasource") }}</span>
    </div>

    <q-tree
      v-else
      :key="treeKey"
      ref="treeRef"
      :nodes="nodes"
      node-key="key"
      label-key="label"
      children-key="children"
      v-model:expanded="expandedKeys"
      no-connectors
      dense
      :filter="filter"
      :filter-method="filterFn"
      class="db-tree-tree"
      no-nodes-label=" "
      @lazy-load="onLazyLoad"
    >
      <template #default-header="{ node }">
        <div
          class="db-tree-node row items-center no-wrap full-width"
          :class="{
            'db-tree-node--selected': selectedKey === node.key,
            'db-tree-node--empty': node.kind === 'empty',
            'db-tree-node--root': node.kind === 'database'
          }"
          @click.stop="onNodeHeaderClick(node)"
        >
          <q-icon
            :name="nodeIcon(node)"
            size="20px"
            class="q-mr-sm cursor-pointer db-tree-icon"
            @click.stop="toggleNode(node)"
          />
          <span class="db-tree-label ellipsis" :title="node.label">{{ node.label }}</span>
          <q-space />
          <q-badge
            v-if="showCount(node)"
            :color="nodeCounts[node.key] > 0 ? 'primary' : 'grey-5'"
            rounded
            class="db-count-badge"
          >
            {{ nodeCounts[node.key] }}
          </q-badge>

          <q-menu v-if="ctxItems(node).length" context-menu>
            <q-list dense class="db-tree-ctx-list">
              <q-item
                v-for="item in ctxItems(node)"
                :key="item.key"
                v-close-popup
                clickable
                @click="item.action()"
              >
                <q-item-section avatar class="db-tree-ctx-avatar">
                  <q-icon :name="item.icon" size="16px" />
                </q-item-section>
                <q-item-section>{{ item.label }}</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </div>
      </template>
    </q-tree>
  </div>
</template>

<style scoped>
.db-tree {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
}

.db-tree-state {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px 16px;
  text-align: center;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

/* ═══ 树 — 与部门树 .dept-tree 保持一致 ═══ */
.db-tree-tree {
  padding: 4px 8px;
}

:deep(.db-tree-tree.q-tree--dense .q-tree__node--child) {
  padding-left: 0 !important;
}

:deep(.db-tree-tree.q-tree--dense .q-tree__children) {
  padding-left: 16px !important;
}

:deep(.db-tree-tree .q-tree__node-toggle) {
  display: none !important;
}

:deep(.db-tree-tree .q-tree__arrow) {
  display: none !important;
}

:deep(.db-tree-tree .q-tree__node) {
  padding-bottom: 0 !important;
}

:deep(.db-tree-tree .q-tree__node-header) {
  margin: 1px 0;
  padding: 0;
  min-height: 0;
  border-radius: 0;
  box-sizing: border-box;
}

/* ═══ 树节点 — 与部门树 .dept-tree-node 保持一致 ═══ */
.db-tree-node {
  min-width: 0;
  padding: 6px 10px;
  min-height: 34px;
  border-radius: 6px;
  box-sizing: border-box;
  transition: background-color 0.12s ease;
  user-select: none;
  -webkit-user-select: none;
  color: rgba(0, 0, 0, 0.82);
}

.db-tree-node:hover {
  background: rgba(0, 0, 0, 0.04);
}

/* 根节点（database）字体加粗 */
.db-tree-node--root .db-tree-label {
  font-weight: 700;
}

.db-tree-node--selected {
  background: rgba(0, 121, 107, 0.08) !important;
  color: #00796b;
}

.db-tree-node--selected .db-tree-label {
  font-weight: 600;
}

.db-tree-icon {
  transition: transform 0.15s ease;
  color: inherit;
}

.db-tree-label {
  font-size: 13px;
  line-height: 1.4;
  color: inherit;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.db-tree-node--empty {
  color: rgba(0, 0, 0, 0.35);
}

.db-tree-node--empty .db-tree-label {
  font-style: italic;
}

/* 计数徽章 — 与部门树 .dept-count-badge 保持一致 */
.db-count-badge {
  font-size: 11px;
  padding: 1px 6px;
}

/* 右键菜单 */
.db-tree-ctx-list {
  min-width: 180px;
}

.db-tree-ctx-avatar {
  min-width: 32px !important;
  color: rgba(0, 0, 0, 0.55);
}

/* ═══ 暗色模式 — 与 SqlQueryView 暗色模式设计令牌保持一致 ═══ */
.body--dark .db-tree-node {
  color: rgba(255, 255, 255, 0.82);
}

.body--dark .db-tree-node:hover {
  background: rgba(255, 255, 255, 0.04);
}

.body--dark .db-tree-node--selected {
  background: rgba(0, 150, 136, 0.12) !important;
  color: #4db6ac;
}

.body--dark .db-tree-node--empty {
  color: rgba(255, 255, 255, 0.35);
}

.body--dark .db-tree-state {
  color: rgba(255, 255, 255, 0.45);
}
</style>
