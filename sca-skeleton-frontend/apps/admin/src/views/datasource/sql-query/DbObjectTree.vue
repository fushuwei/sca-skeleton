<script setup lang="ts">
import { nextTick, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { showToast } from "@repo/shared";
import {
  getTablesApi,
  getViewsApi,
  getFunctionsApi,
  getProceduresApi,
  getSynonymsApi,
  getColumnsApi
} from "../../../apis/datasource";

const { t } = useI18n({ useScope: "global" });

interface Props {
  /** 当前数据源 ID（空表示未选择） */
  datasourceId: string;
  /** 数据库类型（控制同义词节点与示例 SQL 方言） */
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
  | "category"
  | "table"
  | "view"
  | "function"
  | "procedure"
  | "synonym"
  | "column"
  | "empty";

type Category = "tables" | "views" | "functions" | "procedures" | "synonyms";

interface TreeNode {
  key: string;
  label: string;
  kind: NodeKind;
  category?: Category;
  database?: string;
  lazy?: boolean;
  children?: TreeNode[];
}

const treeRef = ref();
/** 用于强制重挂载 q-tree，重置展开与懒加载状态 */
const treeKey = ref(0);
const nodes = ref<TreeNode[]>([]);

/** 自动补全 Schema（表名 → 字段列表），随懒加载增量维护 */
const schemaMap = ref<Record<string, string[]>>({});

// ── 节点展示配置 ──

const NODE_ICON: Record<NodeKind, { icon: string; color: string }> = {
  database: { icon: "sym_r_storage", color: "amber-8" },
  category: { icon: "sym_r_folder", color: "grey-7" },
  table: { icon: "sym_r_table_chart", color: "blue-7" },
  view: { icon: "sym_r_visibility", color: "teal-7" },
  function: { icon: "sym_r_functions", color: "orange-8" },
  procedure: { icon: "sym_r_bolt", color: "purple-7" },
  synonym: { icon: "sym_r_link", color: "cyan-8" },
  column: { icon: "sym_r_tag", color: "grey-6" },
  empty: { icon: "sym_r_remove", color: "grey-5" }
};

const CATEGORY_ICON: Record<Category, { icon: string; color: string }> = {
  tables: { icon: "sym_r_folder_special", color: "blue-7" },
  views: { icon: "sym_r_folder_special", color: "teal-7" },
  functions: { icon: "sym_r_folder_special", color: "orange-8" },
  procedures: { icon: "sym_r_folder_special", color: "purple-7" },
  synonyms: { icon: "sym_r_folder_special", color: "cyan-8" }
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

function categoryNodes(database?: string): TreeNode[] {
  const categories: Category[] = ["tables", "views", "functions", "procedures"];
  if (props.dbType === "ORACLE") {
    categories.push("synonyms");
  }
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
    database: parent.database,
    // 表/视图可继续展开字段
    lazy: kind === "table" || kind === "view"
  };
}

function emptyNode(parent: TreeNode): TreeNode {
  return {
    key: `${parent.key}|empty`,
    label: t("sqlQuery.treeEmpty"),
    kind: "empty"
  };
}

// ── 树构建与刷新 ──

/**
 * 按数据源配置的数据库构建树：
 * 配置了库名 → 根节点为该库，下挂分类节点；
 * 未配置库名 → 分类节点直接作为根（元数据 API 使用连接默认库）。
 */
function rebuild() {
  schemaMap.value = {};
  emit("schema-change", {});
  if (!props.datasourceId) {
    nodes.value = [];
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
  if (database) {
    // 默认展开配置的库节点，露出分类
    void nextTick(() => treeRef.value?.setExpanded(`db|${database}`, true));
  }
}

defineExpose({ refresh: rebuild });

watch(
  () => [props.datasourceId, props.database],
  rebuild,
  { immediate: true }
);

/** q-tree 懒加载：分类节点拉取对象列表，表/视图节点拉取字段 */
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
      const res = await categoryApi[node.category](props.datasourceId, node.database);
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
      done(
        res.data.length
          ? res.data.map((name) => objectNode(node, node.category as Category, name))
          : [emptyNode(node)]
      );
    } else if (node.kind === "table" || node.kind === "view") {
      const res = await getColumnsApi(props.datasourceId, node.label, node.database);
      if (res.code !== 10_000 || !res.data) {
        fail();
        showToast(res.message || t("sqlQuery.treeLoadFail"), "negative");
        return;
      }
      schemaMap.value[node.label] = res.data;
      emit("schema-change", { ...schemaMap.value });
      done(res.data.map<TreeNode>((col) => ({
        key: `col|${node.database}|${node.label}|${col}`,
        label: col,
        kind: "column",
        database: node.database
      })));
    } else {
      done([]);
    }
  } catch {
    fail();
  }
}

function filterFn(node: TreeNode, filter: string) {
  return node.label.toLowerCase().includes(filter.toLowerCase());
}

/** 节点图标（q-tree 插槽的 node 为无类型对象，此处收敛类型） */
function nodeIcon(node: TreeNode): string {
  return node.kind === "category" && node.category
    ? CATEGORY_ICON[node.category].icon
    : NODE_ICON[node.kind].icon;
}

/** 节点图标颜色 */
function nodeColor(node: TreeNode): string {
  return node.kind === "category" && node.category
    ? CATEGORY_ICON[node.category].color
    : NODE_ICON[node.kind].color;
}

// ── 示例 SQL 生成（按方言） ──

function sampleSql(table: string): string {
  const limit = props.maxRows || 1000;
  if (props.dbType === "ORACLE") {
    return `SELECT * FROM ${table} FETCH FIRST ${limit} ROWS ONLY`;
  }
  if (props.dbType === "SQLSERVER") {
    return `SELECT TOP ${limit} * FROM ${table}`;
  }
  return `SELECT * FROM ${table} LIMIT ${limit}`;
}

// ── 节点交互 ──

function onNodeDblClick(node: TreeNode) {
  if (node.kind === "table" || node.kind === "view") {
    emit("run", sampleSql(node.label));
    // 双击的第二次点击会折叠节点，这里恢复展开状态
    treeRef.value?.setExpanded(node.key, true);
  } else if (node.kind === "column" || node.kind === "function"
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
      { key: "query", label: t("sqlQuery.menuQuery"), icon: "sym_r_play_arrow", action: () => onNodeDblClick(node) },
      { key: "insertSelect", label: t("sqlQuery.menuInsertSelect"), icon: "sym_r_code", action: () => emit("insert", sampleSql(node.label)) },
      { key: "insertName", label: t("sqlQuery.menuInsertName"), icon: "sym_r_input", action: insertName },
      { key: "copy", label: t("sqlQuery.menuCopyName"), icon: "sym_r_content_copy", action: copyName }
    ];
  }
  if (node.kind === "database") {
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
      <q-icon name="sym_r_account_tree" size="34px" />
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
      dense
      :filter="filter"
      :filter-method="filterFn"
      class="db-tree-tree"
      @lazy-load="onLazyLoad"
    >
      <template #default-header="{ node }">
        <div
          class="db-tree-node"
          :class="`db-tree-node--${node.kind}`"
          @dblclick="onNodeDblClick(node)"
        >
          <q-icon
            :name="nodeIcon(node)"
            :color="nodeColor(node)"
            size="15px"
            class="db-tree-node-icon"
          />
          <span class="db-tree-node-label" :title="node.label">{{ node.label }}</span>

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
  height: 100%;
  min-height: 0;
}

.db-tree-state {
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

.db-tree-tree {
  padding: 4px;
}

.db-tree-tree :deep(.q-tree__node) {
  padding-bottom: 0;
}

.db-tree-tree :deep(.q-tree__children) {
  padding-left: 14px;
}

.db-tree-node {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 2px 4px;
  border-radius: 3px;
  user-select: none;
}

.db-tree-node-icon {
  flex-shrink: 0;
  margin-right: 6px;
}

.db-tree-node-label {
  font-size: 12.5px;
  line-height: 22px;
  color: rgba(0, 0, 0, 0.82);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.db-tree-node--empty .db-tree-node-label {
  color: rgba(0, 0, 0, 0.35);
  font-style: italic;
}

.db-tree-ctx-list {
  min-width: 180px;
}

.db-tree-ctx-avatar {
  min-width: 32px !important;
  color: rgba(0, 0, 0, 0.55);
}
</style>
