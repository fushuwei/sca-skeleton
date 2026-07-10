<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { QTableColumn } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysPermission, PermissionTreeNode, PermissionPageRequest } from "../../types/auth";
import {
  getPermissionListApi,
  getPermissionPageApi,
  deletePermissionApi
} from "../../apis/permission";
import { useConfirmDialog } from "@repo/ui";
import MenuDrawerContent from "./MenuDrawerContent.vue";

const { t, locale } = useI18n({ useScope: "global" });
const { confirmDialog } = useConfirmDialog();

// ═══════════════════════════════════════════════════════════════
// 菜单树
// ═══════════════════════════════════════════════════════════════

const ROOT_ID = "0";
const menuTreeLoading = ref(false);
const allPermissions = ref<SysPermission[]>([]);
const menuTreeNodes = computed(() => buildMenuTree(allPermissions.value));
const selectedMenuId = ref<string>("");
let lastSelectedMenuId = "";
const menuTreeExpanded = ref<string[]>([ROOT_ID]);
const leftPanelWidth = ref(260);
const leftPanelCollapsed = ref(false);

/** 将扁平权限列表转成树结构（排除 button 类型） */
function buildMenuTree(perms: SysPermission[]): PermissionTreeNode[] {
  if (!perms.length) return [];
  const filtered = perms.filter((p) => p.type !== "button");
  const sorted = [...filtered].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));

  const map = new Map<string, PermissionTreeNode>();
  const isEn = locale.value.startsWith("en");
  for (const p of sorted) {
    map.set(p.id, {
      id: p.id,
      label: (isEn && p.nameEn) ? p.nameEn : p.name,
      parentId: p.parentId,
      type: p.type,
      icon: p.icon || "",
      children: []
    });
  }

  const roots: PermissionTreeNode[] = [];
  for (const p of sorted) {
    const node = map.get(p.id)!;
    if (!p.parentId || p.parentId === "0") {
      roots.push(node);
    } else {
      const parent = map.get(p.parentId);
      if (parent) {
        parent.children = parent.children ?? [];
        parent.children.push(node);
      } else {
        roots.push(node);
      }
    }
  }

  const cleanEmpty = (nodes: PermissionTreeNode[]) => {
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
const menuTreeWithRoot = computed(() => [{
  id: ROOT_ID,
  label: t("menuMgmt.allMenus"),
  parentId: "",
  type: "root",
  icon: "",
  count: allPermissions.value.filter((p) => p.type !== "button").length,
  children: menuTreeNodes.value
}] as PermissionTreeNode[]);

async function loadMenuTree() {
  menuTreeLoading.value = true;
  try {
    const result = await getPermissionListApi();
    if (result.code === 10_000 && result.data?.length) {
      allPermissions.value = result.data;
    } else {
      allPermissions.value = [];
    }
  } catch {
    allPermissions.value = [];
  } finally {
    menuTreeExpanded.value = [ROOT_ID];
    menuTreeLoading.value = false;
  }
}

function handleMenuNodeClick(node: PermissionTreeNode) {
  searchForm.parentId = node.id === ROOT_ID ? undefined : node.id;
  handleSearch();
}

/** 菜单树节点选中回调 */
function onMenuTreeSelect(nodeId: string) {
  if (!nodeId) {
    selectedMenuId.value = lastSelectedMenuId || ROOT_ID;
    return;
  }
  if (nodeId === lastSelectedMenuId) {
    selectedMenuId.value = nodeId;
    return;
  }
  lastSelectedMenuId = nodeId;

  if (nodeId === ROOT_ID) {
    searchForm.parentId = undefined;
    handleSearch();
    return;
  }
  const findNode = (nodes: PermissionTreeNode[]): PermissionTreeNode | null => {
    for (const n of nodes) {
      if (n.id === nodeId) return n;
      if (n.children) {
        const r = findNode(n.children);
        if (r) return r;
      }
    }
    return null;
  };
  const node = findNode(menuTreeNodes.value);
  if (node) handleMenuNodeClick(node);
}

/** 双击切换节点展开/收起 */
function toggleMenuNode(node: PermissionTreeNode) {
  const idx = menuTreeExpanded.value.indexOf(node.id);
  if (idx >= 0) {
    menuTreeExpanded.value = menuTreeExpanded.value.filter((id) => id !== node.id);
  } else {
    menuTreeExpanded.value = [...menuTreeExpanded.value, node.id];
  }
}

/** 树节点图标：root/module/folder 统一使用 folder/folder_open，menu 用 eco_leaf */
function menuNodeIcon(node: PermissionTreeNode): string {
  if (node.type === "root") {
    return menuTreeExpanded.value.includes(node.id) ? "sym_r_folder_open" : "sym_r_folder";
  }
  if (node.type === "menu") return "sym_r_nest_eco_leaf";
  // module 和 folder 统一使用 folder / folder_open
  return menuTreeExpanded.value.includes(node.id) ? "sym_r_folder_open" : "sym_r_folder";
}

/** 节点头部点击计数器 —— 解决同坐标双击不触发 dblclick 的浏览器问题 */
let _nodeClickTimer: ReturnType<typeof setTimeout> | null = null;
let _nodeClickCount = 0;
let _nodeClickKey = "";

function onNodeHeaderClick(node: PermissionTreeNode) {
  const key = node.id;
  _nodeClickCount++;

  if (_nodeClickTimer) clearTimeout(_nodeClickTimer);

  if (_nodeClickCount === 1) {
    selectedMenuId.value = node.id;
    _nodeClickTimer = setTimeout(() => {
      if (_nodeClickCount === 1 && _nodeClickKey === key) {
        onMenuTreeSelect(node.id);
      }
      _nodeClickCount = 0;
      _nodeClickKey = "";
      _nodeClickTimer = null;
    }, 280);
  } else if (_nodeClickCount >= 2) {
    if (_nodeClickKey === key) {
      toggleMenuNode(node);
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

const searchForm = reactive<PermissionPageRequest>({
  pageNum: 1,
  pageSize: 10,
  parentId: undefined,
  keyword: "",
  type: "",
  status: ""
});

const searchExpanded = ref(true);

// ── 类型选项 ──
const typeOptions = [
  { label: "menuMgmt.typeModule", value: "module" },
  { label: "menuMgmt.typeFolder", value: "folder" },
  { label: "menuMgmt.typeMenu", value: "menu" },
  { label: "menuMgmt.typeButton", value: "button" }
];

// ── 状态选项 ──
const statusOptions = [
  { label: "menuMgmt.statusEnabled", value: "enabled" },
  { label: "menuMgmt.statusDisabled", value: "disabled" }
];

const typeColorOf = (s: string): string =>
  ({
    module: "deep-purple",
    folder: "teal",
    menu: "primary",
    button: "orange"
  }[s] ?? "grey-5");

const typeLabelOf = (s: string): string =>
  ({
    module: t("menuMgmt.typeModule"),
    folder: t("menuMgmt.typeFolder"),
    menu: t("menuMgmt.typeMenu"),
    button: t("menuMgmt.typeButton")
  }[s] ?? s);

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
const drawerMode = ref<DrawerMode>("add");
const drawerPermission = ref<SysPermission | undefined>(undefined);
const drawerDefaultParentId = ref<string>("0");

const drawerTitle = computed(() => {
  if (drawerMode.value === "add") return t("menuMgmt.addMenu");
  if (drawerMode.value === "edit") return t("menuMgmt.editMenu");
  return t("menuMgmt.viewMenu");
});

const drawerIcon = computed(() => {
  if (drawerMode.value === "add") return "sym_r_add";
  if (drawerMode.value === "edit") return "sym_r_edit";
  return "sym_r_visibility";
});

function openMenuDrawer(mode: DrawerMode, permission?: SysPermission, defaultParentId?: string) {
  drawerMode.value = mode;
  drawerPermission.value = permission;
  drawerDefaultParentId.value = defaultParentId || searchForm.parentId || "0";
  drawerOpen.value = true;
}

function closeMenuDrawer() {
  drawerOpen.value = false;
}

function handleDrawerSaved() {
  closeMenuDrawer();
  loadTableData();
  loadMenuTree();
}

// ═══════════════════════════════════════════════════════════════
// 表格数据
// ═══════════════════════════════════════════════════════════════

const tableRows = ref<SysPermission[]>([]);
const tableTotal = ref(0);
const tableLoading = ref(false);
const tablePagination = ref({
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0,
  sortBy: "",
  descending: false
});
const selectedRows = ref<SysPermission[]>([]);
const sortState = ref<{ sortBy: string; descending: boolean }>({ sortBy: "", descending: false });
const jumpToPage = ref<number | null>(null);
const curPage = ref(1);

// ── 表格列定义 ──
const columns = computed<QTableColumn<SysPermission>[]>(() => [
  {
    name: "name",
    field: "name",
    label: t("menuMgmt.name"),
    align: "left",
    sortable: true,
    classes: "sticky-col-left",
    headerClasses: "sticky-col-left"
  },
  {
    name: "nameEn",
    field: "nameEn",
    label: t("menuMgmt.nameEn"),
    align: "left",
    sortable: true
  },
  {
    name: "type",
    field: "type",
    label: t("menuMgmt.type"),
    align: "center",
    sortable: true
  },
  {
    name: "code",
    field: "code",
    label: t("menuMgmt.code"),
    align: "left",
    sortable: true
  },
  {
    name: "path",
    field: "path",
    label: t("menuMgmt.path"),
    align: "left",
    sortable: false
  },
  {
    name: "component",
    field: "component",
    label: t("menuMgmt.component"),
    align: "left",
    sortable: false
  },
  {
    name: "icon",
    field: "icon",
    label: t("menuMgmt.icon"),
    align: "center",
    sortable: false
  },
  {
    name: "sort",
    field: "sort",
    label: t("menuMgmt.sort"),
    align: "center",
    sortable: true
  },
  {
    name: "isVisible",
    field: "isVisible",
    label: t("menuMgmt.isVisible"),
    align: "center",
    sortable: false
  },
  {
    name: "status",
    field: "status",
    label: t("menuMgmt.status"),
    align: "center",
    sortable: true
  },
  {
    name: "createTime",
    field: "createTime",
    label: t("menuMgmt.createTime"),
    align: "center",
    sortable: true,
    format: (val: string) => (val ? new Date(val).toLocaleString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false }) : "-")
  },
  {
    name: "actions",
    field: "id",
    label: t("common.actions"),
    align: "center",
    sortable: false,
    classes: "sticky-col-right",
    headerClasses: "sticky-col-right"
  }
]);

const visibleColumns = ref(columns.value.map((c) => c.name));

// ── 前端列名 → 后端排序列名映射 ──
const SORT_FIELD_MAP: Record<string, string> = {
  name: "name",
  nameEn: "name_en",
  type: "type",
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
  const orderBy = sortBy ? (SORT_FIELD_MAP[sortBy] ?? sortBy) : (!searchForm.parentId ? "sort" : undefined);

  const params: PermissionPageRequest = {
    pageNum,
    pageSize,
    parentId: searchForm.parentId || undefined,
    keyword: searchForm.keyword || undefined,
    type: searchForm.type || undefined,
    status: searchForm.status || undefined,
    orderBy,
    orderDirection: sortState.value.sortBy ? (sortState.value.descending ? "desc" : "asc") : undefined
  };

  try {
    const result = await getPermissionPageApi(params);

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
  searchForm.type = "";
  searchForm.status = "";
  searchForm.parentId = undefined;
  selectedMenuId.value = "";
  lastSelectedMenuId = "";
  tablePagination.value.page = 1;
  curPage.value = 1;
  loadTableData();
}

// ── 监听搜索条件变化后的自动联动 ──
watch(
  () => searchForm.parentId,
  (v) => {
    if (!v || v === "0") selectedMenuId.value = "";
  }
);

// ═══════════════════════════════════════════════════════════════
// 操作
// ═══════════════════════════════════════════════════════════════

function handleCreate() {
  openMenuDrawer("add");
}

function handleView(permission: SysPermission) {
  openMenuDrawer("view", permission);
}

function handleEdit(permission: SysPermission) {
  openMenuDrawer("edit", permission);
}

async function handleDelete(permission: SysPermission) {
  try {
    await confirmDialog(t("menuMgmt.deleteConfirm", { name: permission.name }));
  } catch {
    return;
  }

  try {
    const result = await deletePermissionApi(permission.id);
    if (result.code === 10_000) {
      showToast(t("common.deleteSuccess"), "positive");
      loadTableData();
      loadMenuTree();
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
    await confirmDialog(t("menuMgmt.batchDeleteConfirm", { count: selectedRows.value.length }));
  } catch {
    return;
  }

  let successCount = 0;
  let failCount = 0;

  for (const perm of selectedRows.value) {
    try {
      const result = await deletePermissionApi(perm.id);
      if (result.code === 10_000) {
        successCount++;
      } else {
        failCount++;
      }
    } catch {
      failCount++;
    }
  }

  showToast(
    t("menuMgmt.batchDeleteResult", { success: successCount, fail: failCount }),
    successCount > 0 ? "positive" : "negative"
  );

  selectedRows.value = [];
  loadTableData();
  loadMenuTree();
}

// ═══════════════════════════════════════════════════════════════
// 生命周期
onMounted(() => {
  loadMenuTree();
  loadTableData();
  initialLoadDone = true;
});
</script>

<template>
  <div class="menu-list-shell">
    <!-- ═══ 左侧：菜单树 ═══ -->
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
            t("menuMgmt.expandMenuTree")
          }}</q-tooltip>
        </q-btn>
      </div>

      <template v-else>
        <div class="left-panel-header">
          <div class="left-panel-header-title row items-center no-wrap">
            <q-icon name="sym_r_menu" size="20px" class="q-mr-xs" />
            <span>{{ t("menuMgmt.treeTitle") }}</span>
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
            <q-tooltip>{{ t("menuMgmt.collapseMenuTree") }}</q-tooltip>
          </q-btn>
        </div>

        <q-scroll-area class="left-panel-scroll">
          <div class="left-panel-tree">
            <template v-if="menuTreeLoading">
              <div v-for="i in 6" :key="i" class="menu-skeleton-row">
                <q-skeleton type="rect" width="60%" height="16px" class="q-ml-md q-my-sm" />
              </div>
            </template>

            <q-tree
              v-else
              :nodes="menuTreeWithRoot"
              node-key="id"
              label-key="label"
              children-key="children"
              v-model:expanded="menuTreeExpanded"
              no-connectors
              dense
              class="menu-tree"
              no-nodes-label=" "
            >
              <template #default-header="scope">
                <div
                  class="menu-tree-node row items-center no-wrap full-width"
                  :class="{ 'menu-tree-node--selected': selectedMenuId === scope.node.id }"
                  @click.stop="onNodeHeaderClick(scope.node)"
                >
                  <q-icon
                    :name="menuNodeIcon(scope.node)"
                    size="20px"
                    class="q-mr-sm cursor-pointer menu-tree-icon"
                    :color="selectedMenuId === scope.node.id ? 'primary' : 'grey-7'"
                    @click.stop="toggleMenuNode(scope.node)"
                  />
                  <span class="menu-tree-label ellipsis">{{ scope.node.label }}</span>
                  <q-space />
                  <q-badge
                    v-if="scope.node.count != null && scope.node.count > 0"
                    color="primary"
                    rounded
                    class="menu-count-badge"
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
                :placeholder="t('menuMgmt.keywordPlaceholder')"
                hide-bottom-space
                clearable
                @keyup.enter="handleSearch"
              />
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.type"
                filled
                square
                dense
                :options="typeOptions"
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
                <template v-if="!searchForm.type" v-slot:selected>
                  <span class="status-placeholder">{{ t('menuMgmt.typePlaceholder') }}</span>
                </template>
              </q-select>
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
                  <span class="status-placeholder">{{ t('menuMgmt.statusPlaceholder') }}</span>
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
            {{ t('menuMgmt.createMenu') }}
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
        :class="['menu-table', { 'menu-table--empty': !tableRows.length }]"
        @request="loadTableData"
      >
        <!-- 名称列 -->
        <template #body-cell-name="props">
          <q-td :props="props">
            <span>{{ props.row.name }}</span>
          </q-td>
        </template>

        <template #body-cell-nameEn="props">
          <q-td :props="props">
            <span>{{ props.row.nameEn || "-" }}</span>
          </q-td>
        </template>

        <!-- 类型列 -->
        <template #body-cell-type="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              :color="typeColorOf(props.value)"
              :label="typeLabelOf(props.value)"
              rounded
              class="menu-type-badge"
            />
          </q-td>
        </template>

        <!-- 权限标识列 -->
        <template #body-cell-code="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 路由地址列 -->
        <template #body-cell-path="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 前端组件列 -->
        <template #body-cell-component="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 图标列 -->
        <template #body-cell-icon="props">
          <q-td :props="props">
            <q-icon v-if="props.value" :name="props.value" size="20px" />
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 是否可见列 -->
        <template #body-cell-isVisible="props">
          <q-td :props="props">
            <q-badge
              :color="props.value === 1 ? 'positive' : 'grey-7'"
              :label="props.value === 1 ? t('common.yes') : t('common.no')"
              rounded
              class="menu-type-badge"
            />
          </q-td>
        </template>

        <!-- 状态列 -->
        <template #body-cell-status="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              :color="statusColorOf(props.value)"
              :label="props.value === 'enabled' ? t('menuMgmt.statusEnabled') : t('menuMgmt.statusDisabled')"
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

  <!-- ═══ 本地右侧抽屉：添加 / 编辑 / 查看菜单 ═══ -->
  <Teleport to="body">
    <Transition name="menu-drawer-slide">
      <div v-if="drawerOpen" class="menu-local-drawer-mask" @click.self="closeMenuDrawer">
        <div class="menu-local-drawer">
          <div class="menu-drawer-shell">
            <div class="menu-drawer-header row items-center no-wrap">
              <q-icon :name="drawerIcon" size="20px" class="q-mr-sm" />
              <span class="menu-drawer-title">{{ drawerTitle }}</span>
              <q-space />
              <q-btn
                flat
                dense
                round
                icon="sym_r_close"
                class="menu-drawer-close-btn"
                @click="closeMenuDrawer"
              />
            </div>
            <div class="menu-drawer-body">
              <MenuDrawerContent
                :mode="drawerMode"
                :permission="drawerPermission"
                :default-parent-id="drawerDefaultParentId"
                @close="closeMenuDrawer"
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
.menu-list-shell {
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

.menu-skeleton-row {
  padding: 4px 0;
}

.menu-tree {
  padding: 0 8px;
}

:deep(.menu-tree.q-tree--dense .q-tree__node--child) {
  padding-left: 0 !important;
}

:deep(.menu-tree.q-tree--dense .q-tree__children) {
  padding-left: 16px !important;
}

:deep(.menu-tree .q-tree__node-toggle) {
  display: none !important;
}

:deep(.menu-tree .q-tree__arrow) {
  display: none !important;
}

:deep(.menu-tree .q-tree__node) {
  padding-bottom: 0 !important;
}

:deep(.menu-tree .q-tree__node-header) {
  margin: 1px 0;
  padding: 0;
  min-height: 0;
  border-radius: 0;
  box-sizing: border-box;
}

.menu-tree-node {
  min-width: 0;
  padding: 6px 10px;
  min-height: 34px;
  border-radius: 6px;
  box-sizing: border-box;
  transition: background-color 0.12s ease;
  user-select: none;
  -webkit-user-select: none;
}

.menu-tree-node:hover {
  background: rgba(0, 0, 0, 0.04);
}

.menu-tree-node--selected {
  background: rgba(0, 121, 107, 0.08) !important;
}

.menu-tree-node--selected .menu-tree-label {
  color: #00796b;
  font-weight: 600;
}

.menu-tree-icon {
  transition: transform 0.15s ease;
}

.menu-tree-label {
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
  color: rgba(0, 0, 0, 0.72);
}

.status-select :deep(.q-field__control) {
  min-height: 40px;
  min-width: 160px;
}

.status-placeholder {
  color: rgba(0, 0, 0, 0.4);
  font-size: 14px;
  pointer-events: none;
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
.menu-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.menu-table :deep(.q-table__top) {
  display: none;
}

.menu-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.menu-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.menu-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.menu-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.menu-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.menu-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ═══ 固定列（Sticky Columns）═══ */
.menu-table :deep(th:first-child:not([colspan])),
.menu-table :deep(td:first-child:not([colspan])) {
  width: 64px !important;
  min-width: 64px !important;
}

.menu-table :deep(thead tr th:first-child:not([colspan])) {
  position: sticky;
  left: 0;
  z-index: 1;
}

.menu-table :deep(tbody td:first-child:not([colspan])) {
  position: sticky;
  left: 0;
  z-index: 1;
  background: #fff;
}

.menu-table :deep(thead tr th.sticky-col-left) {
  position: sticky;
  left: 64px;
  z-index: 1;
}

.menu-table :deep(tbody td.sticky-col-left) {
  position: sticky;
  left: 64px;
  z-index: 1;
  background: #fff;
}

.menu-table :deep(thead tr th.sticky-col-right) {
  position: sticky;
  right: 0;
  z-index: 1;
}

.menu-table :deep(tbody td.sticky-col-right) {
  position: sticky;
  right: 0;
  z-index: 1;
  background: #fff;
}

.menu-table :deep(thead tr th.sticky-col-left),
.menu-table :deep(tbody td.sticky-col-left) {
  box-shadow: 4px 0 6px -1px rgba(0, 0, 0, 0.12);
}

.menu-table :deep(thead tr th.sticky-col-right),
.menu-table :deep(tbody td.sticky-col-right) {
  box-shadow: -4px 0 6px -1px rgba(0, 0, 0, 0.12);
}

.menu-table :deep(tbody tr:hover td:first-child:not([colspan])),
.menu-table :deep(tbody tr:hover td.sticky-col-left),
.menu-table :deep(tbody tr:hover td.sticky-col-right) {
  background: #f7fbfb !important;
}

.menu-table :deep(tbody tr.q-tr--selected td:first-child:not([colspan])),
.menu-table :deep(tbody tr.q-tr--selected td.sticky-col-left),
.menu-table :deep(tbody tr.q-tr--selected td.sticky-col-right) {
  background: #f0f6f4 !important;
}

.body--dark .menu-table :deep(tbody td:first-child:not([colspan])),
.body--dark .menu-table :deep(tbody td.sticky-col-left),
.body--dark .menu-table :deep(tbody td.sticky-col-right) {
  background: #1e1e1e !important;
}

.body--dark .menu-table :deep(tbody tr:hover td:first-child:not([colspan])),
.body--dark .menu-table :deep(tbody tr:hover td.sticky-col-left),
.body--dark .menu-table :deep(tbody tr:hover td.sticky-col-right) {
  background: #1d2120 !important;
}

.body--dark .menu-table :deep(tbody tr.q-tr--selected td:first-child:not([colspan])),
.body--dark .menu-table :deep(tbody tr.q-tr--selected td.sticky-col-left),
.body--dark .menu-table :deep(tbody tr.q-tr--selected td.sticky-col-right) {
  background: #1c2323 !important;
}

/* ── 空数据状态 ── */
.menu-table--empty :deep(.q-table__container) {
  height: 100%;
}

.menu-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.menu-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.menu-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

/* 行悬停 */
.menu-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.menu-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

/* 菜单计数徽章 */
.menu-count-badge {
  font-size: 11px;
  padding: 1px 6px;
}

/* 复选框尺寸 */
.menu-table :deep(.q-checkbox__inner) {
  font-size: 32px;
}

/* Badge 统一样式 */
.menu-type-badge,
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
.menu-table :deep(.q-table__bottom) {
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
.menu-local-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 5000;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: flex-end;
}

.menu-local-drawer {
  width: 680px;
  max-width: 100vw;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.menu-drawer-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.menu-drawer-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 65px;
  padding: 0 16px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.menu-drawer-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.menu-drawer-close-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  font-size: 20px;
}

.menu-drawer-close-btn :deep(.q-btn__wrapper) {
  min-width: 32px;
  min-height: 32px;
  padding: 0;
}

.menu-drawer-close-btn :deep(.q-icon) {
  font-size: 20px;
}

.menu-drawer-close-btn:hover {
  background: rgba(128, 128, 128, 0.2);
}

.menu-drawer-body {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 16px;
}

/* 抽屉滑入/滑出动画 */
.menu-drawer-slide-enter-active,
.menu-drawer-slide-leave-active {
  transition: opacity 0.25s ease;
}

.menu-drawer-slide-enter-active .menu-local-drawer,
.menu-drawer-slide-leave-active .menu-local-drawer {
  transition: transform 0.25s ease;
}

.menu-drawer-slide-enter-from,
.menu-drawer-slide-leave-to {
  opacity: 0;
}

.menu-drawer-slide-enter-from .menu-local-drawer,
.menu-drawer-slide-leave-to .menu-local-drawer {
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
.body--dark .menu-local-drawer {
  background: #1e1e1e !important;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.4);
}

.body--dark .menu-drawer-header {
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .menu-drawer-title {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .menu-drawer-close-btn {
  color: rgba(255, 255, 255, 0.6) !important;
}

.body--dark .menu-drawer-close-btn:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .menu-drawer-body {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .menu-local-drawer-mask {
  background: rgba(0, 0, 0, 0.5);
}
</style>
