<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useEscCloseDrawer } from "../../composables/useEscCloseDrawer";
import type { QTableColumn } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysPermission, PermissionTreeNode, PermissionPageRequest } from "../../types/auth";
import {
  getPermissionListApi,
  getPermissionPageApi,
  getPermissionByIdApi,
  deletePermissionApi,
  batchDeletePermissionApi
} from "../../apis/permission";
import { useConfirmDialog } from "@repo/ui";
import PermissionDrawerContent from "./PermissionDrawerContent.vue";

const { t, locale } = useI18n({ useScope: "global" });
const { confirmDialog } = useConfirmDialog();

// ═══════════════════════════════════════════════════════════════
// 权限树 —— 按权限域（realm）分组的两棵子树
// ═══════════════════════════════════════════════════════════════

/** 分类节点 id 前缀，点击分类节点 = 按 realm 筛选该域全部权限 */
const REALM_GROUP_PREFIX = "realm:";
/** 权限域分组定义（顺序即左侧展示顺序） */
const REALM_GROUPS: { realm: string; labelKey: string }[] = [
  { realm: "admin", labelKey: "permissionMgmt.adminGroup" },
  { realm: "portal", labelKey: "permissionMgmt.portalGroup" }
];
const realmGroupId = (realm: string) => REALM_GROUP_PREFIX + realm;
const isRealmGroupId = (id: string) => id.startsWith(REALM_GROUP_PREFIX);

const menuTreeLoading = ref(false);
const allPermissions = ref<SysPermission[]>([]);
const selectedMenuId = ref<string>("");
let lastSelectedMenuId = "";
/** 初始展开所有分类节点 */
const menuTreeExpanded = ref<string[]>(REALM_GROUPS.map((g) => realmGroupId(g.realm)));
const leftPanelWidth = ref(260);
const leftPanelCollapsed = ref(false);

/** 将扁平权限列表转成树结构（排除 button 类型），保留 realm 字段 */
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
      realm: p.realm,
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
      } else {
        delete n.children;
      }
    }
  };
  cleanEmpty(roots);
  return roots;
}

/** 构造一个 realm 分类虚拟节点 */
function buildRealmGroupNode(realm: string, labelKey: string, children: PermissionTreeNode[]): PermissionTreeNode {
  return {
    id: realmGroupId(realm),
    label: t(labelKey),
    parentId: "",
    type: "realm-group",
    icon: "",
    realm,
    children
  };
}

/**
 * 按权限域分组的权限树（q-tree 渲染用）。
 * 顶层为「后台权限 / 前台权限」两个分类节点，各自挂载对应域的权限子树。
 */
const menuTreeWithRoot = computed<PermissionTreeNode[]>(() => {
  const allRoots = buildMenuTree(allPermissions.value);
  return REALM_GROUPS.map((g) => {
    const realmChildren = allRoots.filter((n) => n.realm === g.realm);
    return buildRealmGroupNode(g.realm, g.labelKey, realmChildren);
  });
});

/** 在树中按 id 查找节点（用于业务节点点击时取 realm） */
function findTreeNode(id: string): PermissionTreeNode | null {
  const find = (nodes: PermissionTreeNode[]): PermissionTreeNode | null => {
    for (const n of nodes) {
      if (n.id === id) return n;
      if (n.children) {
        const r = find(n.children);
        if (r) return r;
      }
    }
    return null;
  };
  return find(menuTreeWithRoot.value);
}

/**
 * 构建节点 → 子孙权限数量映射（含分类节点）。
 * 借助 treePath 一次性遍历统计，无递归：
 * treePath 格式为 "0,祖先id,...,自己id"，解析后对每个祖先 id 累加 1。
 * 分类节点（realm:admin / realm:portal）单独统计其域下全部权限数。
 */
const descendantCountMap = computed<Map<string, number>>(() => {
  const counts = new Map<string, number>();
  const filtered = allPermissions.value.filter((p) => p.type !== "button");
  // 分类节点初始化
  for (const g of REALM_GROUPS) {
    counts.set(realmGroupId(g.realm), 0);
  }
  for (const p of filtered) {
    // 该权限计入其所属分类节点
    const gid = realmGroupId(p.realm);
    counts.set(gid, (counts.get(gid) || 0) + 1);
    // 通过 treePath 累加到所有祖先（排除 "0" 和自身）
    if (p.treePath) {
      for (const aid of p.treePath.split(",")) {
        if (aid && aid !== "0" && aid !== p.id) {
          counts.set(aid, (counts.get(aid) || 0) + 1);
        }
      }
    }
  }
  return counts;
});

/** 获取节点的子孙权限数量 */
function getDescendantCount(node: PermissionTreeNode): number {
  return descendantCountMap.value.get(node.id) ?? 0;
}

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
    menuTreeExpanded.value = REALM_GROUPS.map((g) => realmGroupId(g.realm));
    menuTreeLoading.value = false;
  }
}

/** 权限树节点选中回调 */
function onMenuTreeSelect(nodeId: string) {
  if (!nodeId) {
    selectedMenuId.value = lastSelectedMenuId;
    return;
  }
  if (nodeId === lastSelectedMenuId) {
    selectedMenuId.value = nodeId;
    return;
  }
  lastSelectedMenuId = nodeId;
  selectedMenuId.value = nodeId;

  // 分类节点：按 realm 筛选该域全部权限
  if (isRealmGroupId(nodeId)) {
    searchForm.realm = nodeId.slice(REALM_GROUP_PREFIX.length);
    searchForm.parentId = undefined;
    handleSearch();
    return;
  }

  // 业务节点：按 parentId 筛选，realm 跟随该节点所属域（子权限必然同域）
  const node = findTreeNode(nodeId);
  searchForm.parentId = nodeId;
  searchForm.realm = node?.realm || "";
  handleSearch();
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

/** 树节点图标：分类节点/module/folder 用 folder/folder_open，menu 用 eco_leaf */
function menuNodeIcon(node: PermissionTreeNode): string {
  if (node.type === "menu") return "sym_r_nest_eco_leaf";
  // realm-group / module / folder 统一使用 folder / folder_open
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
  realm: "",
  status: ""
});

const searchExpanded = ref(true);

// ── 类型选项 ──
const typeOptions = [
  { label: "permissionMgmt.typeModule", value: "module" },
  { label: "permissionMgmt.typeFolder", value: "folder" },
  { label: "permissionMgmt.typeMenu", value: "menu" },
  { label: "permissionMgmt.typeButton", value: "button" }
];

// ── 权限域选项 ──
const realmOptions = [
  { label: "permissionMgmt.realmAdmin", value: "admin" },
  { label: "permissionMgmt.realmPortal", value: "portal" }
];

// ── 状态选项 ──
const statusOptions = [
  { label: "permissionMgmt.statusEnabled", value: "enabled" },
  { label: "permissionMgmt.statusDisabled", value: "disabled" }
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
    module: t("permissionMgmt.typeModule"),
    folder: t("permissionMgmt.typeFolder"),
    menu: t("permissionMgmt.typeMenu"),
    button: t("permissionMgmt.typeButton")
  }[s] ?? s);

const realmColorOf = (s: string): string =>
  ({
    admin: "blue-8",
    portal: "purple"
  }[s] ?? "grey-5");

const realmLabelOf = (s: string): string =>
  ({
    admin: t("permissionMgmt.realmAdmin"),
    portal: t("permissionMgmt.realmPortal")
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
/** 新建权限时预设的权限域（跟随当前选中的分类节点/业务节点） */
const drawerDefaultRealm = ref<string>("");

useEscCloseDrawer(drawerOpen);

const drawerTitle = computed(() => {
  if (drawerMode.value === "add") return t("permissionMgmt.addPermission");
  if (drawerMode.value === "edit") return t("permissionMgmt.editPermission");
  return t("permissionMgmt.viewPermission");
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
  // 新建时 realm 默认跟随当前选中节点所属域（分类节点或业务节点）
  drawerDefaultRealm.value = mode === "add" ? (searchForm.realm || "") : "";
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
    label: t("permissionMgmt.nameOnList"),
    align: "left",
    sortable: true,
    classes: "sticky-col-left",
    headerClasses: "sticky-col-left"
  },
  {
    name: "type",
    field: "type",
    label: t("permissionMgmt.type"),
    align: "left",
    sortable: true
  },
  {
    name: "realm",
    field: "realm",
    label: t("permissionMgmt.realm"),
    align: "left",
    sortable: true
  },
  {
    name: "code",
    field: "code",
    label: t("permissionMgmt.code"),
    align: "left",
    sortable: true
  },
  {
    name: "path",
    field: "path",
    label: t("permissionMgmt.path"),
    align: "left",
    sortable: false
  },
  {
    name: "component",
    field: "component",
    label: t("permissionMgmt.component"),
    align: "left",
    sortable: false
  },
  {
    name: "icon",
    field: "icon",
    label: t("permissionMgmt.icon"),
    align: "left",
    sortable: false
  },
  {
    name: "sort",
    field: "sort",
    label: t("permissionMgmt.sort"),
    align: "left",
    sortable: true
  },
  {
    name: "isVisible",
    field: "isVisible",
    label: t("permissionMgmt.isVisible"),
    align: "left",
    sortable: false
  },
  {
    name: "status",
    field: "status",
    label: t("permissionMgmt.status"),
    align: "left",
    sortable: true
  },
  {
    name: "createTime",
    field: "createTime",
    label: t("permissionMgmt.createTime"),
    align: "left",
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
  type: "type",
  realm: "realm",
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

  const params: PermissionPageRequest = {
    pageNum,
    pageSize,
    parentId: searchForm.parentId || undefined,
    keyword: searchForm.keyword || undefined,
    type: searchForm.type || undefined,
    realm: searchForm.realm || undefined,
    status: searchForm.status || undefined,
    sortField,
    sortOrder: sortState.value.sortBy ? (sortState.value.descending ? "desc" : "asc") : undefined
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
  searchForm.realm = "";
  searchForm.status = "";
  searchForm.parentId = undefined;
  selectedMenuId.value = "";
  lastSelectedMenuId = "";
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
    // parentId 清空时，仅当 realm 也为空（真正的无选中/重置场景）才清除高亮；
    // 点击分类节点会设 parentId=undefined 但保留 realm，此时不应清除分类节点高亮
    if ((!v || v === "0") && !searchForm.realm) selectedMenuId.value = "";
  }
);

// ═══════════════════════════════════════════════════════════════
// 操作
// ═══════════════════════════════════════════════════════════════

function handleCreate() {
  openMenuDrawer("add");
}

async function handleView(permission: SysPermission) {
  const res = await getPermissionByIdApi(permission.id);
  if (res.code === 10_000 && res.data) {
    openMenuDrawer("view", res.data);
  } else {
    showToast(res.message || t("common.loadFail"), "negative");
  }
}

async function handleEdit(permission: SysPermission) {
  const res = await getPermissionByIdApi(permission.id);
  if (res.code === 10_000 && res.data) {
    openMenuDrawer("edit", res.data);
  } else {
    showToast(res.message || t("common.loadFail"), "negative");
  }
}

async function handleDelete(permission: SysPermission) {
  try {
    await confirmDialog(t("permissionMgmt.deleteConfirm", { name: permission.name }));
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
    await confirmDialog(t("permissionMgmt.batchDeleteConfirm", { count: selectedRows.value.length }));
  } catch {
    return;
  }

  try {
    const result = await batchDeletePermissionApi(selectedRows.value.map((r) => r.id));
    if (result.code === 10_000) {
      showToast(t("common.deleteSuccess"), "positive");
      selectedRows.value = [];
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

// ═══════════════════════════════════════════════════════════════
// 生命周期
onMounted(() => {
  loadMenuTree();
  loadTableData();
  initialLoadDone = true;
});
</script>

<template>
  <div class="permission-list-shell">
    <!-- ═══ 左侧：权限树 ═══ -->
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
            t("permissionMgmt.expandTree")
          }}</q-tooltip>
        </q-btn>
      </div>

      <template v-else>
        <div class="left-panel-header">
          <div class="left-panel-header-title row items-center no-wrap">
            <q-icon name="sym_r_menu" size="20px" class="q-mr-xs" />
            <span>{{ t("permissionMgmt.treeTitle") }}</span>
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
            <q-tooltip>{{ t("permissionMgmt.collapseTree") }}</q-tooltip>
          </q-btn>
        </div>

        <q-scroll-area class="left-panel-scroll">
          <div class="left-panel-tree">
            <template v-if="menuTreeLoading">
              <div v-for="i in 6" :key="i" class="permission-skeleton-row">
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
              class="permission-tree"
              no-nodes-label=" "
            >
              <template #default-header="scope">
                <div
                  class="permission-tree-node row items-center no-wrap full-width"
                  :class="{
                    'permission-tree-node--selected': selectedMenuId === scope.node.id,
                    'permission-tree-node--realm-group': scope.node.type === 'realm-group'
                  }"
                  @click.stop="onNodeHeaderClick(scope.node)"
                >
                  <q-icon
                    :name="menuNodeIcon(scope.node)"
                    size="20px"
                    class="q-mr-sm cursor-pointer permission-tree-icon"
                    :color="selectedMenuId === scope.node.id ? 'primary' : 'grey-7'"
                    @click.stop="toggleMenuNode(scope.node)"
                  />
                  <span
                    class="permission-tree-label ellipsis"
                    :class="{ 'text-weight-medium': scope.node.type === 'realm-group' }"
                  >{{ scope.node.label }}</span>
                  <q-space />
                  <!-- 子孙权限数量（含分类节点，借助 treePath 统计） -->
                  <q-badge
                    color="primary"
                    rounded
                    class="permission-count-badge"
                  >
                    {{ getDescendantCount(scope.node) }}
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
                :placeholder="t('permissionMgmt.keywordPlaceholder')"
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
                  <span class="status-placeholder">{{ t('permissionMgmt.typePlaceholder') }}</span>
                </template>
              </q-select>
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.realm"
                filled
                square
                dense
                :options="realmOptions"
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
                <template v-if="!searchForm.realm" v-slot:selected>
                  <span class="status-placeholder">{{ t('permissionMgmt.realmPlaceholder') }}</span>
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
                  <span class="status-placeholder">{{ t('permissionMgmt.statusPlaceholder') }}</span>
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
            {{ t('permissionMgmt.createPermission') }}
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
        :class="['permission-table', { 'permission-table--empty': !tableRows.length }]"
        @request="loadTableData"
      >
        <!-- 名称列：中文展示 name，英文展示 nameEn -->
        <template #body-cell-name="props">
          <q-td :props="props">
            <span>{{ locale.startsWith("en") && props.row.nameEn ? props.row.nameEn : props.row.name }}</span>
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
              class="permission-type-badge"
            />
          </q-td>
        </template>

        <!-- 权限域列 -->
        <template #body-cell-realm="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              :color="realmColorOf(props.value)"
              :label="realmLabelOf(props.value)"
              rounded
              class="permission-type-badge"
            />
          </q-td>
        </template>

        <!-- 权限编码列 -->
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
              class="permission-type-badge"
            />
          </q-td>
        </template>

        <!-- 状态列 -->
        <template #body-cell-status="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              :color="statusColorOf(props.value)"
              :label="props.value === 'enabled' ? t('permissionMgmt.statusEnabled') : t('permissionMgmt.statusDisabled')"
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

  <!-- ═══ 本地右侧抽屉：添加 / 编辑 / 查看权限 ═══ -->
  <Teleport to="body">
    <Transition name="permission-drawer-slide">
      <div
        v-if="drawerOpen"
        v-mask-close="closeMenuDrawer"
        class="permission-local-drawer-mask"
      >
        <div class="permission-local-drawer">
          <div class="permission-drawer-shell">
            <div class="permission-drawer-header row items-center no-wrap">
              <q-icon :name="drawerIcon" size="20px" class="q-mr-sm" />
              <span class="permission-drawer-title">{{ drawerTitle }}</span>
              <q-space />
              <q-btn
                flat
                dense
                round
                icon="sym_r_close"
                class="permission-drawer-close-btn"
                @click="closeMenuDrawer"
              >
                <q-tooltip>{{ t("common.close") }}</q-tooltip>
              </q-btn>
            </div>
            <div class="permission-drawer-body">
              <PermissionDrawerContent
                :mode="drawerMode"
                :permission="drawerPermission"
                :default-parent-id="drawerDefaultParentId"
                :default-realm="drawerDefaultRealm"
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
.permission-list-shell {
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

.permission-skeleton-row {
  padding: 4px 0;
}

.permission-tree {
  padding: 0 8px;
}

:deep(.permission-tree.q-tree--dense .q-tree__node--child) {
  padding-left: 0 !important;
}

:deep(.permission-tree.q-tree--dense .q-tree__children) {
  padding-left: 16px !important;
}

:deep(.permission-tree .q-tree__node-toggle) {
  display: none !important;
}

:deep(.permission-tree .q-tree__arrow) {
  display: none !important;
}

:deep(.permission-tree .q-tree__node) {
  padding-bottom: 0 !important;
}

:deep(.permission-tree .q-tree__node-header) {
  margin: 1px 0;
  padding: 0;
  min-height: 0;
  border-radius: 0;
  box-sizing: border-box;
}

.permission-tree-node {
  min-width: 0;
  padding: 6px 10px;
  min-height: 34px;
  border-radius: 6px;
  box-sizing: border-box;
  transition: background-color 0.12s ease;
  user-select: none;
  -webkit-user-select: none;
}

.permission-tree-node:hover {
  background: rgba(0, 0, 0, 0.04);
}

.permission-tree-node--selected {
  background: rgba(0, 121, 107, 0.08) !important;
}

.permission-tree-node--selected .permission-tree-label {
  color: #00796b;
  font-weight: 600;
}

/* realm 分类节点（顶级分组：后台权限/前台权限），略增内距以突出分组层级 */
.permission-tree-node--realm-group {
  padding: 8px 10px;
}

.permission-tree-icon {
  transition: transform 0.15s ease;
}

.permission-tree-label {
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
.permission-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.permission-table :deep(.q-table__top) {
  display: none;
}

.permission-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.permission-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.permission-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.permission-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.permission-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.permission-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ═══ 固定列（Sticky Columns）═══ */
.permission-table :deep(th:first-child:not([colspan])),
.permission-table :deep(td:first-child:not([colspan])) {
  width: 64px !important;
  min-width: 64px !important;
}

.permission-table :deep(thead tr th:first-child:not([colspan])) {
  position: sticky;
  left: 0;
  z-index: 1;
}

.permission-table :deep(tbody td:first-child:not([colspan])) {
  position: sticky;
  left: 0;
  z-index: 1;
  background: #fff;
}

.permission-table :deep(thead tr th.sticky-col-left) {
  position: sticky;
  left: 64px;
  z-index: 1;
}

.permission-table :deep(tbody td.sticky-col-left) {
  position: sticky;
  left: 64px;
  z-index: 1;
  background: #fff;
}

.permission-table :deep(thead tr th.sticky-col-right) {
  position: sticky;
  right: 0;
  z-index: 1;
}

.permission-table :deep(tbody td.sticky-col-right) {
  position: sticky;
  right: 0;
  z-index: 1;
  background: #fff;
}

.permission-table :deep(thead tr th.sticky-col-left),
.permission-table :deep(tbody td.sticky-col-left) {
  box-shadow: 4px 0 6px -1px rgba(0, 0, 0, 0.12);
}

.permission-table :deep(thead tr th.sticky-col-right),
.permission-table :deep(tbody td.sticky-col-right) {
  box-shadow: -4px 0 6px -1px rgba(0, 0, 0, 0.12);
}

.permission-table :deep(tbody tr:hover td:first-child:not([colspan])),
.permission-table :deep(tbody tr:hover td.sticky-col-left),
.permission-table :deep(tbody tr:hover td.sticky-col-right) {
  background: #f7fbfb !important;
}

.permission-table :deep(tbody tr.q-tr--selected td:first-child:not([colspan])),
.permission-table :deep(tbody tr.q-tr--selected td.sticky-col-left),
.permission-table :deep(tbody tr.q-tr--selected td.sticky-col-right) {
  background: #f0f6f4 !important;
}

.body--dark .permission-table :deep(tbody td:first-child:not([colspan])),
.body--dark .permission-table :deep(tbody td.sticky-col-left),
.body--dark .permission-table :deep(tbody td.sticky-col-right) {
  background: #1e1e1e !important;
}

.body--dark .permission-table :deep(tbody tr:hover td:first-child:not([colspan])),
.body--dark .permission-table :deep(tbody tr:hover td.sticky-col-left),
.body--dark .permission-table :deep(tbody tr:hover td.sticky-col-right) {
  background: #1d2120 !important;
}

.body--dark .permission-table :deep(tbody tr.q-tr--selected td:first-child:not([colspan])),
.body--dark .permission-table :deep(tbody tr.q-tr--selected td.sticky-col-left),
.body--dark .permission-table :deep(tbody tr.q-tr--selected td.sticky-col-right) {
  background: #1c2323 !important;
}

/* ── 空数据状态 ── */
.permission-table--empty :deep(.q-table__container) {
  height: 100%;
}

.permission-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.permission-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.permission-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

/* 行悬停 */
.permission-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.permission-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

/* 权限计数徽章 */
.permission-count-badge {
  font-size: 11px;
  padding: 1px 6px;
}

/* 复选框尺寸 */
.permission-table :deep(.q-checkbox__inner) {
  font-size: 32px;
}

/* Badge 统一样式 */
.permission-type-badge,
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
.permission-table :deep(.q-table__bottom) {
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
.permission-local-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 5000;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: flex-end;
}

.permission-local-drawer {
  width: 680px;
  max-width: 100vw;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.permission-drawer-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.permission-drawer-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 65px;
  padding: 0 16px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.permission-drawer-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.permission-drawer-close-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  font-size: 20px;
}

.permission-drawer-close-btn :deep(.q-btn__wrapper) {
  min-width: 32px;
  min-height: 32px;
  padding: 0;
}

.permission-drawer-close-btn :deep(.q-icon) {
  font-size: 20px;
}

.permission-drawer-close-btn:hover {
  background: rgba(128, 128, 128, 0.2);
}

.permission-drawer-body {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 16px;
}

/* 抽屉滑入/滑出动画 */
.permission-drawer-slide-enter-active,
.permission-drawer-slide-leave-active {
  transition: opacity 0.25s ease;
}

.permission-drawer-slide-enter-active .permission-local-drawer,
.permission-drawer-slide-leave-active .permission-local-drawer {
  transition: transform 0.25s ease;
}

.permission-drawer-slide-enter-from,
.permission-drawer-slide-leave-to {
  opacity: 0;
}

.permission-drawer-slide-enter-from .permission-local-drawer,
.permission-drawer-slide-leave-to .permission-local-drawer {
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
.body--dark .permission-local-drawer {
  background: #1e1e1e !important;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.4);
}

.body--dark .permission-drawer-header {
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .permission-drawer-title {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .permission-drawer-close-btn {
  color: rgba(255, 255, 255, 0.6) !important;
}

.body--dark .permission-drawer-close-btn:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .permission-drawer-body {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .permission-local-drawer-mask {
  background: rgba(0, 0, 0, 0.5);
}
</style>
