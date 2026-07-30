<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useEscCloseDrawer } from "../../composables/useEscCloseDrawer";
import type { QTableColumn } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysDept, DeptTreeNode, DeptPageRequest } from "../../types/auth";
import {
  getDeptListApi,
  getDeptPageApi,
  getDeptByIdApi,
  deleteDeptApi,
  batchDeleteDeptApi
} from "../../apis/dept";
import { useConfirmDialog } from "@repo/ui";
import DeptDrawerContent from "./DeptDrawerContent.vue";

const { t } = useI18n({ useScope: "global" });
const { confirmDialog } = useConfirmDialog();

// ═══════════════════════════════════════════════════════════════
// 部门树
// ═══════════════════════════════════════════════════════════════

const ROOT_ID = "0";
const deptTreeLoading = ref(false);
const allDepts = ref<SysDept[]>([]);
const deptTreeNodes = ref<DeptTreeNode[]>([]);
const selectedDeptId = ref<string>("");
let lastSelectedDeptId = "";
const deptTreeExpanded = ref<string[]>([ROOT_ID]);
const leftPanelWidth = ref(260);
const leftPanelCollapsed = ref(false);

/** 将扁平部门列表转成树结构 */
function buildDeptTree(depts: SysDept[]): DeptTreeNode[] {
  if (!depts.length) return [];
  const sorted = [...depts].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));

  const map = new Map<string, DeptTreeNode>();
  for (const d of sorted) {
    map.set(d.id, {
      id: d.id,
      label: d.name,
      parentId: d.parentId,
      children: []
    });
  }

  const roots: DeptTreeNode[] = [];
  for (const d of sorted) {
    const node = map.get(d.id)!;
    if (!d.parentId || d.parentId === "0") {
      roots.push(node);
    } else {
      const parent = map.get(d.parentId);
      if (parent) {
        parent.children = parent.children ?? [];
        parent.children.push(node);
      } else {
        roots.push(node);
      }
    }
  }

  const cleanEmpty = (nodes: DeptTreeNode[]) => {
    for (const n of nodes) {
      if (n.children?.length) {
        cleanEmpty(n.children);
        n.count = n.children.length;
      } else {
        delete n.children;
        n.count = 0;
      }
    }
  };
  cleanEmpty(roots);
  return roots;
}

/** 带"全部"根节点的树（q-tree 渲染用） */
const deptTreeWithRoot = computed(() => [{
  id: ROOT_ID,
  label: t("deptMgmt.allDepts"),
  parentId: "",
  count: allDepts.value.length,
  children: deptTreeNodes.value
}] as DeptTreeNode[]);

async function loadDeptTree() {
  deptTreeLoading.value = true;
  try {
    const result = await getDeptListApi();
    if (result.code === 10_000 && result.data?.length) {
      allDepts.value = result.data;
      deptTreeNodes.value = buildDeptTree(result.data);
    } else {
      allDepts.value = [];
      deptTreeNodes.value = [];
    }
  } catch {
    allDepts.value = [];
    deptTreeNodes.value = [];
  } finally {
    deptTreeExpanded.value = [ROOT_ID];
    deptTreeLoading.value = false;
  }
}

function handleDeptNodeClick(node: DeptTreeNode) {
  searchForm.parentId = node.id === ROOT_ID ? undefined : node.id;
  handleSearch();
}

/** 部门树节点选中回调 */
function onDeptTreeSelect(nodeId: string) {
  if (!nodeId) {
    selectedDeptId.value = lastSelectedDeptId || ROOT_ID;
    return;
  }
  if (nodeId === lastSelectedDeptId) {
    selectedDeptId.value = nodeId;
    return;
  }
  lastSelectedDeptId = nodeId;

  if (nodeId === ROOT_ID) {
    searchForm.parentId = undefined;
    handleSearch();
    return;
  }
  const findNode = (nodes: DeptTreeNode[]): DeptTreeNode | null => {
    for (const n of nodes) {
      if (n.id === nodeId) return n;
      if (n.children) {
        const r = findNode(n.children);
        if (r) return r;
      }
    }
    return null;
  };
  const node = findNode(deptTreeNodes.value);
  if (node) handleDeptNodeClick(node);
}

/** 双击切换节点展开/收起 */
function toggleDeptNode(node: DeptTreeNode) {
  const idx = deptTreeExpanded.value.indexOf(node.id);
  if (idx >= 0) {
    deptTreeExpanded.value = deptTreeExpanded.value.filter((id) => id !== node.id);
  } else {
    deptTreeExpanded.value = [...deptTreeExpanded.value, node.id];
  }
}

/** 树节点图标 */
function deptNodeIcon(node: DeptTreeNode): string {
  return deptTreeExpanded.value.includes(node.id) ? "sym_r_folder_open" : "sym_r_folder";
}

/** 节点头部点击计数器 —— 解决同坐标双击不触发 dblclick 的浏览器问题 */
let _nodeClickTimer: ReturnType<typeof setTimeout> | null = null;
let _nodeClickCount = 0;
let _nodeClickKey = "";

function onNodeHeaderClick(node: DeptTreeNode) {
  const key = node.id;
  _nodeClickCount++;

  if (_nodeClickTimer) clearTimeout(_nodeClickTimer);

  if (_nodeClickCount === 1) {
    selectedDeptId.value = node.id;
    _nodeClickTimer = setTimeout(() => {
      if (_nodeClickCount === 1 && _nodeClickKey === key) {
        onDeptTreeSelect(node.id);
      }
      _nodeClickCount = 0;
      _nodeClickKey = "";
      _nodeClickTimer = null;
    }, 280);
  } else if (_nodeClickCount >= 2) {
    if (_nodeClickKey === key) {
      toggleDeptNode(node);
    }
    _nodeClickCount = 0;
    _nodeClickKey = "";
    _nodeClickTimer = null;
  }

  _nodeClickKey = key;
}

/** 左侧面板拖拽调整宽度 */
let resizeStartX = 0;
let resizeStartWidth = 0;

function beginResize(e: PointerEvent) {
  e.preventDefault();
  resizeStartX = e.clientX;
  resizeStartWidth = leftPanelWidth.value;
  window.addEventListener("pointermove", onResizeMove, { capture: true });
  window.addEventListener("pointerup", endResize, { capture: true });
  window.addEventListener("pointercancel", endResize, { capture: true });
  document.body.style.cursor = "col-resize";
  document.body.style.userSelect = "none";
}

function onResizeMove(e: PointerEvent) {
  const delta = e.clientX - resizeStartX;
  leftPanelWidth.value = Math.min(420, Math.max(200, resizeStartWidth + delta));
}

function endResize() {
  window.removeEventListener("pointermove", onResizeMove, { capture: true });
  window.removeEventListener("pointerup", endResize, { capture: true });
  window.removeEventListener("pointercancel", endResize, { capture: true });
  document.body.style.cursor = "";
  document.body.style.userSelect = "";
}

// ═══════════════════════════════════════════════════════════════
// 搜索条件
// ═══════════════════════════════════════════════════════════════

const searchForm = reactive<DeptPageRequest>({
  pageNum: 1,
  pageSize: 10,
  parentId: undefined,
  keyword: "",
  status: ""
});

const searchExpanded = ref(true);

// ── 状态选项 ──
const statusOptions = [
  { label: "deptMgmt.statusEnabled", value: "enabled" },
  { label: "deptMgmt.statusDisabled", value: "disabled" }
];

const statusColorOf = (s: string): string =>
  ({
    enabled: "positive",
    disabled: "grey-7"
  }[s] ?? "grey-5");

// ═══════════════════════════════════════════════════════════════
// 本地抽屉 — 添加 / 编辑 / 查看
// ═══════════════════════════════════════════════════════════════

type DrawerMode = "add" | "edit" | "view";

const drawerOpen = ref(false);
useEscCloseDrawer(drawerOpen);
const drawerMode = ref<DrawerMode>("add");
const drawerDept = ref<SysDept | undefined>(undefined);
const drawerDefaultParentId = ref<string>("0");

const drawerTitle = computed(() => {
  if (drawerMode.value === "add") return t("deptMgmt.addDept");
  if (drawerMode.value === "edit") return t("deptMgmt.editDept");
  return t("deptMgmt.viewDept");
});

const drawerIcon = computed(() => {
  if (drawerMode.value === "add") return "sym_r_add";
  if (drawerMode.value === "edit") return "sym_r_edit";
  return "sym_r_visibility";
});

function openDeptDrawer(mode: DrawerMode, dept?: SysDept, defaultParentId?: string) {
  drawerMode.value = mode;
  drawerDept.value = dept;
  drawerDefaultParentId.value = defaultParentId || searchForm.parentId || "0";
  drawerOpen.value = true;
}

function closeDeptDrawer() {
  drawerOpen.value = false;
}

function handleDrawerSaved() {
  closeDeptDrawer();
  loadTableData();
  loadDeptTree();
}

// ═══════════════════════════════════════════════════════════════
// 表格数据
// ═══════════════════════════════════════════════════════════════

const tableRows = ref<SysDept[]>([]);
const tableTotal = ref(0);
const tableLoading = ref(false);
const tablePagination = ref({
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0,
  sortBy: "",
  descending: false
});
const selectedRows = ref<SysDept[]>([]);
const sortState = ref<{ sortBy: string; descending: boolean }>({ sortBy: "", descending: false });
const jumpToPage = ref<number | null>(null);
const curPage = ref(1);

// ── 表格列定义 ──
const columns = computed<QTableColumn<SysDept>[]>(() => [
  {
    name: "name",
    field: "name",
    label: t("deptMgmt.name"),
    align: "left",
    sortable: true
  },
  {
    name: "code",
    field: "code",
    label: t("deptMgmt.code"),
    align: "left",
    sortable: true
  },
  {
    name: "tenantName",
    field: "tenantName",
    label: t("deptMgmt.tenantName"),
    align: "left",
    sortable: false,
    format: (val: string) => (val ? val : "-")
  },
  {
    name: "leader",
    field: "leader",
    label: t("deptMgmt.leader"),
    align: "left",
    sortable: false
  },
  {
    name: "phone",
    field: "phone",
    label: t("deptMgmt.phone"),
    align: "left",
    sortable: false
  },
  {
    name: "sort",
    field: "sort",
    label: t("deptMgmt.sort"),
    align: "left",
    sortable: true
  },
  {
    name: "status",
    field: "status",
    label: t("deptMgmt.status"),
    align: "left",
    sortable: true
  },
  {
    name: "createTime",
    field: "createTime",
    label: t("deptMgmt.createTime"),
    align: "left",
    sortable: true,
    format: (val: string) => (val ? new Date(val).toLocaleString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false }) : "-")
  },
  {
    name: "actions",
    field: "id",
    label: t("common.actions"),
    align: "center",
    sortable: false
  }
]);

const visibleColumns = ref(columns.value.map((c) => c.name));

// ── 前端列名 → 后端排序列名映射 ──
const SORT_FIELD_MAP: Record<string, string> = {
  name: "name",
  code: "code",
  sort: "sort",
  status: "status",
  createTime: "create_time"
};

let initialLoadDone = false;
let loadRequestId = 0;

// ═══════════════════════════════════════════════════════════════
// 数据加载
// ═══════════════════════════════════════════════════════════════

async function loadTableData(
  props?: {
    pagination: {
      page: number;
      rowsPerPage: number;
      rowsNumber?: number;
      sortBy?: string;
      descending?: boolean;
    };
  }
) {
  if (props && !initialLoadDone) return;

  const requestId = ++loadRequestId;
  tableLoading.value = true;

  const pageSize = Number(props?.pagination?.rowsPerPage ?? tablePagination.value.rowsPerPage) || 10;
  const pageNum = curPage.value || 1;

  if (props?.pagination) {
    sortState.value.sortBy = props.pagination.sortBy ?? "";
    sortState.value.descending = props.pagination.descending ?? false;
    tablePagination.value.sortBy = props.pagination.sortBy ?? "";
    tablePagination.value.descending = props.pagination.descending ?? false;
  }

  const sortBy = sortState.value.sortBy || undefined;
  const sortField = sortBy ? (SORT_FIELD_MAP[sortBy] ?? sortBy) : (!searchForm.parentId ? "sort" : undefined);

  const params: DeptPageRequest = {
    pageNum,
    pageSize,
    parentId: searchForm.parentId || undefined,
    keyword: searchForm.keyword || undefined,
    status: searchForm.status || undefined,
    sortField,
    sortOrder: sortState.value.sortBy ? (sortState.value.descending ? "desc" : "asc") : undefined
  };

  try {
    const result = await getDeptPageApi(params);

    if (requestId !== loadRequestId) return;

    if (result.code === 10_000) {
      tableRows.value = result.data.records ?? [];
      tableTotal.value = Number(result.data.total) || 0;
      tablePagination.value.page = Number(result.data.current) || pageNum;
      tablePagination.value.rowsPerPage = Number(result.data.size) || pageSize;
      tablePagination.value.rowsNumber = Number(result.data.total) || 0;
      curPage.value = Number(result.data.current) || pageNum;
    } else {
      showToast(result.message || t("common.loadFail"), "negative");
    }
  } catch (error) {
    if (requestId !== loadRequestId) return;
    if (!isNotificationHandled(error)) {
      showToast(t("common.loadFail"), "negative");
    }
  } finally {
    if (requestId === loadRequestId) {
      tableLoading.value = false;
    }
  }
}

function handleSearch() {
  tablePagination.value.page = 1;
  curPage.value = 1;
  loadTableData();
}

function onPageChange(page: number) {
  curPage.value = Number(page);
  tablePagination.value.page = Number(page);
  loadTableData();
}

function handleJumpToPage() {
  const page = Number(jumpToPage.value);
  const maxPage = Math.ceil(tableTotal.value / tablePagination.value.rowsPerPage);
  if (page && page >= 1 && page <= maxPage) {
    curPage.value = page;
    tablePagination.value.page = page;
    loadTableData();
  }
  jumpToPage.value = null;
}

function handleReset() {
  searchForm.keyword = "";
  searchForm.status = "";
  searchForm.parentId = undefined;
  selectedDeptId.value = "";
  lastSelectedDeptId = "";
  // 清空排序状态（与 q-table 的 pagination.sortBy / descending 保持一致）
  sortState.value.sortBy = "";
  sortState.value.descending = false;
  tablePagination.value.sortBy = "";
  tablePagination.value.descending = false;
  tablePagination.value.page = 1;
  curPage.value = 1;
  loadTableData();
}

// ── 监听搜索条件变化后的自动联动 ──
watch(
  () => searchForm.parentId,
  (v) => {
    if (!v || v === "0") selectedDeptId.value = "";
  }
);

// ═══════════════════════════════════════════════════════════════
// 操作
// ═══════════════════════════════════════════════════════════════

function handleCreate() {
  openDeptDrawer("add");
}

async function handleView(dept: SysDept) {
  const res = await getDeptByIdApi(dept.id);
  if (res.code === 10_000 && res.data) {
    openDeptDrawer("view", res.data);
  } else {
    showToast(res.message || t("common.loadFail"), "negative");
  }
}

async function handleEdit(dept: SysDept) {
  const res = await getDeptByIdApi(dept.id);
  if (res.code === 10_000 && res.data) {
    openDeptDrawer("edit", res.data);
  } else {
    showToast(res.message || t("common.loadFail"), "negative");
  }
}

async function handleDelete(dept: SysDept) {
  try {
    await confirmDialog(t("deptMgmt.deleteConfirm", { name: dept.name }));
  } catch {
    return;
  }

  try {
    const result = await deleteDeptApi(dept.id);
    if (result.code === 10_000) {
      showToast(t("common.deleteSuccess"), "positive");
      loadTableData();
      loadDeptTree();
    } else {
      showToast(result.message || t("common.deleteFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("common.deleteFail"), "negative");
    }
  }
}

// 批量删除
async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    showToast(t("common.selectRowsFirst"), "warning");
    return;
  }

  try {
    await confirmDialog(t("deptMgmt.batchDeleteConfirm", { count: selectedRows.value.length }));
  } catch {
    return;
  }

  try {
    const result = await batchDeleteDeptApi(selectedRows.value.map((r) => r.id));
    if (result.code === 10_000) {
      showToast(t("common.deleteSuccess"), "positive");
      selectedRows.value = [];
      loadTableData();
      loadDeptTree();
    } else {
      showToast(result.message || t("common.deleteFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("common.deleteFail"), "negative");
    }
  }
}

// ═══════════════════════════════════════════════════════════════
// 生命周期
// ═══════════════════════════════════════════════════════════════

onMounted(() => {
  loadDeptTree();
  loadTableData();
  initialLoadDone = true;
});
</script>

<template>
  <div class="dept-list-shell">
    <!-- ═══ 左侧：部门树 ═══ -->
    <div class="left-panel" :class="{ 'left-panel--collapsed': leftPanelCollapsed }">
      <div v-if="leftPanelCollapsed" class="left-panel-collapsed-bar">
        <q-btn
          flat
          dense
          round
          icon="sym_r_chevron_right"
          size="20px"
          class="left-panel-toggle-btn"
          @click="leftPanelCollapsed = false"
        >
          <q-tooltip anchor="center right" self="center left">{{
            t("deptMgmt.expandDeptTree")
          }}</q-tooltip>
        </q-btn>
      </div>

      <template v-else>
        <div class="left-panel-header">
          <div class="left-panel-header-title row items-center no-wrap">
            <q-icon name="sym_r_account_tree" size="20px" class="q-mr-xs" />
            <span>{{ t("deptMgmt.treeTitle") }}</span>
          </div>
          <q-btn
            flat
            dense
            round
            size="20px"
            icon="sym_r_chevron_left"
            @click="leftPanelCollapsed = true"
            class="left-panel-collapse-btn"
          >
            <q-tooltip>{{ t("deptMgmt.collapseDeptTree") }}</q-tooltip>
          </q-btn>
        </div>

        <q-scroll-area class="left-panel-scroll">
          <div class="left-panel-tree">
            <template v-if="deptTreeLoading">
              <div v-for="i in 6" :key="i" class="dept-skeleton-row">
                <q-skeleton type="rect" width="60%" height="16px" class="q-ml-md q-my-sm" />
              </div>
            </template>

            <q-tree
              v-else
              :nodes="deptTreeWithRoot"
              node-key="id"
              label-key="label"
              children-key="children"
              v-model:expanded="deptTreeExpanded"
              no-connectors
              dense
              class="dept-tree"
              no-nodes-label=" "
            >
              <template #default-header="scope">
                <div
                  class="dept-tree-node row items-center no-wrap full-width"
                  :class="{ 'dept-tree-node--selected': selectedDeptId === scope.node.id }"
                  @click.stop="onNodeHeaderClick(scope.node)"
                >
                  <q-icon
                    :name="deptNodeIcon(scope.node)"
                    size="20px"
                    class="q-mr-sm cursor-pointer dept-tree-icon"
                    :color="selectedDeptId === scope.node.id ? 'primary' : 'grey-7'"
                    @click.stop="toggleDeptNode(scope.node)"
                  />
                  <span class="dept-tree-label ellipsis">{{ scope.node.label }}</span>
                  <q-space />
                  <q-badge
                    v-if="scope.node.count != null && scope.node.count > 0"
                    color="primary"
                    rounded
                    class="dept-count-badge"
                  >
                    {{ scope.node.count }}
                  </q-badge>
                </div>
              </template>
            </q-tree>
          </div>
        </q-scroll-area>
      </template>

      <div
        v-if="!leftPanelCollapsed"
        class="left-panel-resize-handle"
        @pointerdown.prevent="beginResize"
      />
    </div>

    <!-- ═══ 右侧：内容区 ═══ -->
    <div class="right-panel">
      <!-- ── 搜索区域 ── -->
      <div class="search-area">
        <div class="search-area-header row items-center no-wrap">
          <div class="row items-center no-wrap cursor-pointer" @click="searchExpanded = !searchExpanded">
            <q-icon name="sym_r_search" size="20px" class="q-mr-xs" color="grey-8" />
            <span class="search-area-title">{{ t("common.searchCondition") }}</span>
          </div>
          <q-space />
          <q-btn
            flat
            dense
            round
            size="20px"
            :icon="searchExpanded ? 'sym_r_expand_less' : 'sym_r_expand_more'"
            class="search-collapse-btn"
            @click="searchExpanded = !searchExpanded"
          >
            <q-tooltip style="white-space: nowrap">{{
              searchExpanded ? t("common.collapseSearch") : t("common.expandSearch")
            }}</q-tooltip>
          </q-btn>
        </div>

        <div v-show="searchExpanded" class="search-area-body">
          <div class="row q-col-gutter-sm items-end">
            <div class="col">
              <q-input
                v-model="searchForm.keyword"
                filled
                square
                dense
                :placeholder="t('deptMgmt.keywordPlaceholder')"
                hide-bottom-space
                clearable
                @keyup.enter="handleSearch"
              />
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.status"
                filled
                square
                dense
                :options="statusOptions"
                :option-label="(o) => (o ? t(o.label) : '')"
                option-value="value"
                emit-value
                map-options
                hide-bottom-space
                clearable
                transition-show="jump-up"
                transition-hide="jump-down"
                class="status-select"
                popup-content-class="status-select-popup"
              >
                <template v-if="!searchForm.status" v-slot:selected>
                  <span class="status-placeholder">{{ t('deptMgmt.statusPlaceholder') }}</span>
                </template>
              </q-select>
            </div>
            <div class="col-auto">
              <div class="row q-gutter-x-sm no-wrap">
                <q-btn
                  color="primary"
                  unelevated
                  no-caps
                  class="search-btn"
                  @click="handleSearch"
                >
                  <q-icon name="sym_r_search" size="20px" class="q-mr-xs" />
                  {{ t("common.search") }}
                </q-btn>
                <q-btn
                  color="grey-7"
                  outline
                  no-caps
                  class="search-btn"
                  @click="handleReset"
                >
                  <q-icon name="sym_r_refresh" size="20px" class="q-mr-xs" />
                  {{ t("common.reset") }}
                </q-btn>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ── 工具栏区域 ── -->
      <div class="toolbar-area row items-center no-wrap">
        <div class="toolbar-left row items-center no-wrap">
          <q-btn
            color="primary"
            unelevated
            dense
            no-caps
            class="toolbar-btn"
            @click.stop="handleCreate"
          >
            <q-icon name="sym_r_add" size="20px" class="q-mr-xs" />
            {{ t('deptMgmt.createDept') }}
          </q-btn>
          <q-btn
            color="white"
            text-color="negative"
            outline
            dense
            no-caps
            class="toolbar-btn"
            :disable="!selectedRows.length"
            @click.stop="handleBatchDelete"
          >
            <q-icon name="sym_r_delete" size="20px" class="q-mr-xs" />
            {{ t('common.batchDelete') }}
          </q-btn>
        </div>
        <q-space />
      </div>

      <!-- ── 表格区域 ── -->
      <q-table
        v-model:selected="selectedRows"
        v-model:pagination="tablePagination"
        :rows="tableRows"
        :columns="columns"
        :visible-columns="visibleColumns"
        row-key="id"
        :loading="tableLoading"
        :rows-per-page-options="[10, 20, 50, 100]"
        selection="multiple"
        flat
        :class="['dept-table', { 'dept-table--empty': !tableRows.length }]"
        @request="loadTableData"
      >
        <!-- 部门名称列 -->
        <template #body-cell-name="props">
          <q-td :props="props">
            <span>{{ props.row.name }}</span>
          </q-td>
        </template>

        <!-- 部门编码列 -->
        <template #body-cell-code="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 负责人列 -->
        <template #body-cell-leader="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 联系电话列 -->
        <template #body-cell-phone="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 状态列 -->
        <template #body-cell-status="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              :color="statusColorOf(props.value)"
              :label="props.value === 'enabled' ? t('deptMgmt.statusEnabled') : t('deptMgmt.statusDisabled')"
              rounded
              class="status-badge"
            />
          </q-td>
        </template>

        <!-- 操作列 -->
        <template #body-cell-actions="props">
          <q-td :props="props" class="q-gutter-x-xs actions-cell">
            <q-btn
              flat
              dense
              round
              size="sm"
              color="info"
              icon="sym_r_visibility"
              @click.stop="handleView(props.row)"
            >
              <q-tooltip>{{ t("common.view") }}</q-tooltip>
            </q-btn>
            <q-btn
              flat
              dense
              round
              size="sm"
              color="primary"
              icon="sym_r_edit"
              @click.stop="handleEdit(props.row)"
            >
              <q-tooltip>{{ t("common.edit") }}</q-tooltip>
            </q-btn>
            <q-btn
              flat
              dense
              round
              size="sm"
              color="negative"
              icon="sym_r_delete"
              @click.stop="handleDelete(props.row)"
            >
              <q-tooltip>{{ t("common.delete") }}</q-tooltip>
            </q-btn>
          </q-td>
        </template>

        <!-- 空数据 -->
        <template #no-data>
          <div class="column items-center justify-center q-py-xl text-grey-7 empty-state-content">
            <q-icon name="sym_r_database_search" size="56px" class="q-mb-sm" />
            <div class="text-body1 text-weight-medium q-mb-xs">
              {{ t("common.noData") }}
            </div>
            <div class="text-caption text-grey-6">
              {{ t("common.noDataHint") }}
            </div>
          </div>
        </template>

        <!-- 自定义底部分页栏 -->
        <template #bottom="props">
          <div class="row items-center full-width table-bottom">
            <span>
              {{ t("common.totalRows", { count: tableTotal }) }}<template v-if="selectedRows.length">，{{ t("common.selectedRows", { count: selectedRows.length }) }}</template>
            </span>
            <q-space />
            <q-pagination
              v-model="curPage"
              :max="props.pagesNumber"
              :max-pages="7"
              size="sm"
              color="primary"
              boundary-links
              direction-links
              icon-first="keyboard_double_arrow_left"
              icon-prev="keyboard_arrow_left"
              icon-next="keyboard_arrow_right"
              icon-last="keyboard_double_arrow_right"
              @update:model-value="onPageChange"
            />
            <span class="text-caption text-grey-7 q-ml-md q-mr-sm">{{ t("common.rowsPerPageLabel") }}</span>
            <q-select
              v-model="tablePagination.rowsPerPage"
              :options="[10, 20, 50, 100]"
              dense
              flat
              borderless
              class="rows-per-page-select"
              popup-content-class="rows-per-page-popup"
              @update:model-value="handleSearch"
            >
              <template #append>
                <span class="text-caption">{{ t("common.rowsPerPageUnit") }}</span>
              </template>
            </q-select>
            <span class="text-caption text-grey-7 q-ml-md">{{ t("common.jumpToLabel") }}</span>
            <q-input
              v-model.number="jumpToPage"
              dense
              flat
              borderless
              class="jump-to-page-input"
              input-class="text-center"
              :placeholder="String((props.pagesNumber || 1) <= 1 ? 1 : (curPage >= (props.pagesNumber || 1) ? 1 : curPage + 1))"
              @keyup.enter="handleJumpToPage"
            />
            <span class="text-caption text-grey-7">{{ t("common.jumpToUnit") }}</span>
          </div>
        </template>
      </q-table>
    </div>
  </div>

  <!-- ═══ 本地右侧抽屉：添加 / 编辑 / 查看部门 ═══ -->
  <Teleport to="body">
    <Transition name="dept-drawer-slide">
      <div v-if="drawerOpen" class="dept-local-drawer-mask" @click.self="closeDeptDrawer">
        <div class="dept-local-drawer">
          <div class="dept-drawer-shell">
            <div class="dept-drawer-header row items-center no-wrap">
              <q-icon :name="drawerIcon" size="20px" class="q-mr-sm" />
              <span class="dept-drawer-title">{{ drawerTitle }}</span>
              <q-space />
              <q-btn
                flat
                dense
                round
                icon="sym_r_close"
                class="dept-drawer-close-btn"
                @click="closeDeptDrawer"
              >
                <q-tooltip>{{ t("common.close") }}</q-tooltip>
              </q-btn>
            </div>
            <div class="dept-drawer-body">
              <DeptDrawerContent
                :mode="drawerMode"
                :dept="drawerDept"
                :default-parent-id="drawerDefaultParentId"
                @close="closeDeptDrawer"
                @saved="handleDrawerSaved"
              />
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ═══ 整体壳层 ═══ */
.dept-list-shell {
  display: flex;
  height: calc(100vh - 64px - 40px - 44px - 16px);
  min-height: 0;
  gap: 8px;
}

/* ═══ 左侧面板 ═══ */
.left-panel {
  position: relative;
  flex-shrink: 0;
  width: v-bind(leftPanelWidth + 'px');
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
  overflow: hidden;
  transition: width 0.22s ease;
}

.left-panel--collapsed {
  width: 40px;
  min-width: 40px;
  background: #fafafa;
}

.left-panel-collapsed-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
}

.left-panel-toggle-btn,
.left-panel-collapse-btn,
.search-collapse-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.87);
  border-radius: 50%;
}

.left-panel-toggle-btn :deep(.q-btn__wrapper),
.left-panel-collapse-btn :deep(.q-btn__wrapper),
.search-collapse-btn :deep(.q-btn__wrapper) {
  min-height: 32px;
  padding: 0;
}

.left-panel-toggle-btn :deep(.q-icon.material-symbols-rounded),
.left-panel-toggle-btn :deep(.material-symbols-rounded),
.left-panel-collapse-btn :deep(.q-icon.material-symbols-rounded),
.left-panel-collapse-btn :deep(.material-symbols-rounded),
.search-collapse-btn :deep(.q-icon.material-symbols-rounded),
.search-collapse-btn :deep(.material-symbols-rounded) {
  font-size: 20px !important;
}

.left-panel-toggle-btn:hover,
.left-panel-collapse-btn:hover,
.search-collapse-btn:hover {
  background: rgba(128, 128, 128, 0.28);
}

.left-panel-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px;
  padding: 0 8px 0 12px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.left-panel-header-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.left-panel-scroll {
  flex: 1 1 auto;
  min-height: 0;
}

.left-panel-tree {
  padding: 4px 0;
}

.dept-skeleton-row {
  padding: 4px 0;
}

.dept-tree {
  padding: 0 8px;
}

:deep(.dept-tree.q-tree--dense .q-tree__node--child) {
  padding-left: 0 !important;
}

:deep(.dept-tree.q-tree--dense .q-tree__children) {
  padding-left: 16px !important;
}

:deep(.dept-tree .q-tree__node-toggle) {
  display: none !important;
}

:deep(.dept-tree .q-tree__arrow) {
  display: none !important;
}

:deep(.dept-tree .q-tree__node) {
  padding-bottom: 0 !important;
}

:deep(.dept-tree .q-tree__node-header) {
  margin: 1px 0;
  padding: 0;
  min-height: 0;
  border-radius: 0;
  box-sizing: border-box;
}

.dept-tree-node {
  min-width: 0;
  padding: 6px 10px;
  min-height: 34px;
  border-radius: 6px;
  box-sizing: border-box;
  transition: background-color 0.12s ease;
  user-select: none;
  -webkit-user-select: none;
}

.dept-tree-node:hover {
  background: rgba(0, 0, 0, 0.04);
}

.dept-tree-node--selected {
  background: rgba(0, 121, 107, 0.08) !important;
}

.dept-tree-node--selected .dept-tree-label {
  color: #00796b;
  font-weight: 600;
}

.dept-tree-icon {
  transition: transform 0.15s ease;
}

.dept-tree-label {
  font-size: 13px;
  line-height: 1.4;
  color: rgba(0, 0, 0, 0.82);
}

.left-panel-resize-handle {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 6px;
  cursor: col-resize;
  z-index: 10;
  touch-action: none;
}

.left-panel-resize-handle:hover,
.left-panel-resize-handle:active {
  background: none;
}

/* ═══ 右侧面板 ═══ */
.right-panel {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ── 搜索区域 ── */
.search-area {
  flex-shrink: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.search-area-header {
  height: 40px;
  padding: 0 8px 0 12px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  user-select: none;
}

.search-area-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.search-area-header .cursor-pointer {
  padding: 4px 0;
}

.search-area-header .cursor-pointer:hover {
  opacity: 0.85;
}

.search-area-body {
  padding: 8px;
}

.search-btn {
  min-width: 72px;
  height: 40px;
  padding: 0 14px;
  font-size: 13px;
}

.status-select :deep(.q-field__native) {
  /* 与搜索文本框（q-input）输入文字颜色保持一致：rgba(0, 0, 0, 0.87) */
  color: rgba(0, 0, 0, 0.87);
}

.status-select :deep(.q-field__control) {
  min-height: 40px;
  min-width: 160px;
}

/* ── 工具栏区域 ── */
.toolbar-area {
  flex-shrink: 0;
  padding: 8px 1px;
}

.toolbar-left {
  gap: 6px;
}

.toolbar-btn {
  height: 32px;
  font-size: 13px;
  padding: 0 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.toolbar-area :deep(.q-btn) {
  height: 32px;
  font-size: 13px;
}

/* ── 表格 ── */
.dept-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.dept-table :deep(.q-table__top) {
  display: none;
}

.dept-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.dept-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.dept-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.dept-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.dept-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.dept-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ── 空数据状态 ── */
.dept-table--empty :deep(.q-table__container) {
  height: 100%;
}

.dept-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.dept-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.dept-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

/* 行悬停 */
.dept-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.dept-table :deep(tbody tr.q-tr--selected td) {
  background: rgba(0, 121, 107, 0.06) !important;
}

.dept-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

/* 部门计数徽章 */
.dept-count-badge {
  font-size: 11px;
  padding: 1px 6px;
}

/* 复选框尺寸 */
.dept-table :deep(.q-checkbox__inner) {
  font-size: 32px;
}

/* Badge 统一样式 */
.status-badge {
  font-size: 11px;
  padding: 3px 10px;
  font-weight: 500;
}

/* 操作按钮列 */
.actions-cell {
  white-space: nowrap;
}

.actions-cell :deep(.q-btn) {
  width: 32px;
  height: 32px;
}

.actions-cell :deep(.q-btn .q-icon) {
  font-size: 20px;
}

/* 空数据内容 */
.empty-state-content {
  text-align: center;
}

/* 分页底栏 */
.dept-table :deep(.q-table__bottom) {
  padding: 3px 16px 4px;
  font-size: 13px;
  min-height: 42px;
  background: #fff;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.table-bottom {
  min-height: 40px;
}

.table-bottom :deep(.q-pagination__content .q-btn) {
  width: 30px !important;
  height: 30px !important;
  min-width: 30px !important;
  min-height: 30px !important;
  border-radius: 50% !important;
  padding: 0 !important;
  font-size: 10px !important;
}

.table-bottom :deep(.q-pagination__content .q-btn .q-focus-helper) {
  border-radius: 50%;
}

.table-bottom :deep(.q-pagination__content .q-btn .q-icon) {
  font-size: 20px;
}

.table-bottom :deep(.q-pagination__content .q-btn.q-btn--standard) {
  font-weight: 700;
}

.table-bottom :deep(.rows-per-page-select .q-field__control) {
  min-height: 24px;
  padding: 0;
  height: 24px;
}

.table-bottom :deep(.rows-per-page-select .q-field__native) {
  min-height: 24px;
  font-size: 12px;
  padding: 0;
}

.table-bottom :deep(.rows-per-page-select .q-field__marginal) {
  height: 24px;
}

.table-bottom :deep(.jump-to-page-input) {
  width: 40px;
  font-size: 12px;
}

.table-bottom :deep(.jump-to-page-input .q-field__control) {
  min-height: 24px;
  padding: 0;
  height: 24px;
}

.table-bottom :deep(.jump-to-page-input .q-field__native) {
  min-height: 24px;
  font-size: 12px;
  padding: 0;
}

/* ═══ 本地右侧抽屉 ═══ */
.dept-local-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 5000;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: flex-end;
}

.dept-local-drawer {
  width: 680px;
  max-width: 100vw;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dept-drawer-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.dept-drawer-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 65px;
  padding: 0 16px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.dept-drawer-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.dept-drawer-close-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  font-size: 20px;
}

.dept-drawer-close-btn :deep(.q-btn__wrapper) {
  min-width: 32px;
  min-height: 32px;
  padding: 0;
}

.dept-drawer-close-btn :deep(.q-icon) {
  font-size: 20px;
}

.dept-drawer-close-btn:hover {
  background: rgba(128, 128, 128, 0.2);
}

.dept-drawer-body {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 16px;
}

/* 抽屉滑入/滑出动画 */
.dept-drawer-slide-enter-active,
.dept-drawer-slide-leave-active {
  transition: opacity 0.25s ease;
}

.dept-drawer-slide-enter-active .dept-local-drawer,
.dept-drawer-slide-leave-active .dept-local-drawer {
  transition: transform 0.25s ease;
}

.dept-drawer-slide-enter-from,
.dept-drawer-slide-leave-to {
  opacity: 0;
}

.dept-drawer-slide-enter-from .dept-local-drawer,
.dept-drawer-slide-leave-to .dept-local-drawer {
  transform: translateX(100%);
}
</style>

<!-- 非 scoped：状态选择下拉弹出层 & 抽屉暗色模式（Teleport to body，无法用 scoped 覆盖） -->
<style>
.status-select-popup .q-item {
  min-height: 40px;
  padding: 0 16px;
}

.rows-per-page-popup .q-item {
  min-height: 36px;
  padding: 0 16px;
}

/* 抽屉暗色模式 */
.body--dark .dept-local-drawer {
  background: #1e1e1e !important;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.4);
}

.body--dark .dept-drawer-header {
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .dept-drawer-title {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .dept-drawer-close-btn {
  color: rgba(255, 255, 255, 0.6) !important;
}

.body--dark .dept-drawer-close-btn:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .dept-drawer-body {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .dept-local-drawer-mask {
  background: rgba(0, 0, 0, 0.5);
}
</style>
