<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch, markRaw } from "vue";
import { useI18n } from "vue-i18n";
import { useQuasar } from "quasar";
import type { QTableColumn } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysUser, SysDept, DeptTreeNode, UserPageRequest } from "../../types/auth";
import {
  getUserPageApi,
  deleteUserApi,
  changeUserStatusApi,
  resetUserPasswordApi
} from "../../apis/user";
import { getDeptListApi } from "../../apis/dept";
import UserDrawerContent from "./UserDrawerContent.vue";

const { t } = useI18n({ useScope: "global" });
const $q = useQuasar();

// ═══════════════════════════════════════════════════════════════
// 部门树
// ═══════════════════════════════════════════════════════════════

const ROOT_ID = "__root__";
const deptTreeLoading = ref(false);
const deptList = ref<SysDept[]>([]);
const deptTreeNodes = ref<DeptTreeNode[]>([]);
const selectedDeptId = ref<string>("");
let lastSelectedDeptId = "";
const deptTreeExpanded = ref<string[]>([ROOT_ID]);
const leftPanelWidth = ref(260);
const leftPanelCollapsed = ref(false);

/** 高校部门组织样例数据（API 无数据时回退展示），每个节点附带 count 模拟用户数 */
const SAMPLE_DEPT_TREE: DeptTreeNode[] = markRaw([
  {
    id: "d-admin",
    label: "党政办公室",
    parentId: ROOT_ID,
    count: 18,
    children: [
      { id: "d-admin-secretary", label: "秘书科", parentId: "d-admin", count: 5 },
      { id: "d-admin-legal", label: "法务与合规科", parentId: "d-admin", count: 3 }
    ]
  },
  {
    id: "d-hr",
    label: "人事处",
    parentId: ROOT_ID,
    count: 12,
    children: [
      { id: "d-hr-recruit", label: "招聘与配置科", parentId: "d-hr", count: 6 },
      { id: "d-hr-salary", label: "薪酬福利科", parentId: "d-hr", count: 4 }
    ]
  },
  {
    id: "d-academic",
    label: "教务处",
    parentId: ROOT_ID,
    count: 24,
    children: [
      { id: "d-academic-ug", label: "本科教学管理科", parentId: "d-academic", count: 8 },
      { id: "d-academic-pg", label: "研究生培养科", parentId: "d-academic", count: 7 },
      { id: "d-academic-quality", label: "教学质量监控科", parentId: "d-academic", count: 5 }
    ]
  },
  {
    id: "d-research",
    label: "科研处",
    parentId: ROOT_ID,
    count: 15,
    children: [
      { id: "d-research-project", label: "项目管理科", parentId: "d-research", count: 8 },
      { id: "d-research-achievement", label: "成果管理科", parentId: "d-research", count: 5 }
    ]
  },
  {
    id: "d-it",
    label: "信息化建设与管理中心",
    parentId: ROOT_ID,
    count: 31,
    children: [
      { id: "d-it-infra", label: "网络与基础设施科", parentId: "d-it", count: 12 },
      { id: "d-it-app", label: "应用系统开发科", parentId: "d-it", count: 16 }
    ]
  },
  { id: "d-finance", label: "财务处", parentId: ROOT_ID, count: 9 },
  { id: "d-student", label: "学生工作处", parentId: ROOT_ID, count: 21 },
  { id: "d-intl", label: "国际交流合作处", parentId: ROOT_ID, count: 7 }
]);

/** 将后端返回的扁平部门列表转成树结构 */
function buildDeptTree(depts: SysDept[]): DeptTreeNode[] {
  if (!depts.length) return [];

  const sorted = [...depts].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));

  const map = new Map<string, DeptTreeNode>();
  for (const d of sorted) {
    map.set(d.id, { id: d.id, label: d.name, parentId: d.parentId, children: [] });
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
        parent.children!.push(node);
      } else {
        roots.push(node);
      }
    }
  }

  const cleanEmpty = (nodes: DeptTreeNode[]) => {
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

/** 递归计算子树总人数 */
function sumCount(nodes: DeptTreeNode[]): number {
  let total = 0;
  for (const n of nodes) {
    total += n.count ?? 0;
    if (n.children?.length) total += sumCount(n.children);
  }
  return total;
}

/** 带"全部"根节点的树（q-tree 渲染用） */
const deptTreeWithRoot = computed(() => [{
  id: ROOT_ID,
  label: t("user.allDepts"),
  parentId: "0",
  count: sumCount(deptTreeNodes.value),
  children: deptTreeNodes.value
}] as DeptTreeNode[]);

async function loadDeptTree() {
  deptTreeLoading.value = true;
  try {
    const result = await getDeptListApi();
    if (result.code === 10_000 && result.data?.length) {
      deptList.value = result.data;
      deptTreeNodes.value = buildDeptTree(result.data);
    } else {
      const copy = JSON.parse(JSON.stringify(SAMPLE_DEPT_TREE)) as DeptTreeNode[];
      deptTreeNodes.value = copy;
    }
  } catch {
    const copy = JSON.parse(JSON.stringify(SAMPLE_DEPT_TREE)) as DeptTreeNode[];
    deptTreeNodes.value = copy;
  } finally {
    deptTreeExpanded.value = [ROOT_ID];
    deptTreeLoading.value = false;
  }
}

function handleDeptNodeClick(node: DeptTreeNode) {
  if (node.id === ROOT_ID) {
    searchForm.deptId = "";
  } else {
    searchForm.deptId = node.id;
  }
  handleSearch();
}

/** 部门树节点选中回调 */
function onDeptTreeSelect(nodeId: string) {
  // 点击已选中节点时，q-tree 会先清空再设新值；若为空说明是取消选中，恢复即可
  if (!nodeId) {
    selectedDeptId.value = lastSelectedDeptId || ROOT_ID;
    return;
  }
  // 同一节点重复点击不触发搜索
  if (nodeId === lastSelectedDeptId) {
    selectedDeptId.value = nodeId;
    return;
  }
  lastSelectedDeptId = nodeId;

  if (nodeId === ROOT_ID) {
    searchForm.deptId = "";
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

const searchForm = reactive<UserPageRequest>({
  pageNum: 1,
  pageSize: 10,
  username: "",
  nickname: "",
  status: "",
  deptId: ""
});

// 额外搜索条件（前端扩展，后续可对接后端）
const extraSearch = reactive({
  phone: "",
  userType: ""
});

// 搜索区域是否展开
const searchExpanded = ref(true);

// ── 状态选项 ──
const statusOptions = [
  { label: "user.statusActive", value: "active" },
  { label: "user.statusInactive", value: "inactive" },
  { label: "user.statusLocked", value: "locked" },
  { label: "user.statusFrozen", value: "frozen" },
  { label: "user.statusExpired", value: "expired" },
  { label: "user.statusDisabled", value: "disabled" },
  { label: "user.statusCancelled", value: "cancelled" }
];

const statusColorOf = (s: string): string =>
  ({
    active: "positive",
    inactive: "grey-7",
    locked: "red",
    frozen: "blue",
    expired: "orange",
    disabled: "deep-orange",
    cancelled: "grey-5"
  }[s] ?? "grey-5");

const userTypeLabelOf = (t: string): string =>
  ({
    superadmin: "超级管理员",
    tenant_admin: "租户管理员",
    dept_admin: "部门管理员",
    normal: "普通用户"
  }[t] ?? t);

const userTypeColorOf = (t: string): string =>
  ({
    superadmin: "red-8",
    tenant_admin: "orange-8",
    dept_admin: "blue-7",
    normal: "grey-7"
  }[t] ?? "grey-6");

const genderLabelOf = (g: string): string => (g === "male" ? "男" : g === "female" ? "女" : "-");

// ═══════════════════════════════════════════════════════════════
// 本地抽屉 — 添加 / 编辑 / 查看
// ═══════════════════════════════════════════════════════════════

type DrawerMode = "add" | "edit" | "view";

const drawerOpen = ref(false);
const drawerMode = ref<DrawerMode>("add");
const drawerUser = ref<SysUser | undefined>(undefined);

const drawerTitle = computed(() => {
  if (drawerMode.value === "add") return t("user.addUser");
  if (drawerMode.value === "edit") return t("user.editUser");
  return t("user.viewUser");
});

function openUserDrawer(mode: DrawerMode, user?: SysUser) {
  drawerMode.value = mode;
  drawerUser.value = user;
  drawerOpen.value = true;
}

function closeUserDrawer() {
  drawerOpen.value = false;
}

function handleDrawerSaved() {
  closeUserDrawer();
  loadTableData();
}

// ═══════════════════════════════════════════════════════════════
// 表格数据
// ═══════════════════════════════════════════════════════════════

const tableRows = ref<SysUser[]>([]);
const tableTotal = ref(0);
const tableLoading = ref(false);
const tablePagination = ref({
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0,
  sortBy: "",
  descending: false
});
const selectedRows = ref<SysUser[]>([]);
const sortState = ref<{ sortBy: string; descending: boolean }>({ sortBy: "", descending: false });

// ── 表格列定义 ──
const columns: QTableColumn<SysUser>[] = [
  {
    name: "username",
    field: "username",
    label: t("user.username"),
    align: "left",
    sortable: true
  },
  {
    name: "nickname",
    field: "nickname",
    label: t("user.nickname"),
    align: "left",
    sortable: true
  },
  {
    name: "realName",
    field: "realName",
    label: t("user.realName"),
    align: "left",
    sortable: true
  },
  {
    name: "phone",
    field: "phone",
    label: t("user.phone"),
    align: "left",
    sortable: false
  },
  {
    name: "userType",
    field: "userType",
    label: t("user.userType"),
    align: "center",
    sortable: true
  },
  {
    name: "status",
    field: "status",
    label: t("user.status"),
    align: "center",
    sortable: true
  },
  {
    name: "createTime",
    field: "createTime",
    label: t("user.createTime"),
    align: "center",
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
];

const visibleColumns = ref(columns.map((c) => c.name));

// ── 前端列名 → 后端排序列名映射 ──
const SORT_FIELD_MAP: Record<string, string> = {
  realName: "real_name",
  userType: "user_type",
  createTime: "create_time"
};

// ── 标记初始加载是否完成（防止 @request 与 onMounted 重复请求） ──
let initialLoadDone = false;

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
  // 防止 Quasar @request 在 onMounted 之前触发导致竞态
  if (props && !initialLoadDone) return;

  tableLoading.value = true;

  const rawPage = props?.pagination?.page ?? tablePagination.value.page;
  const pageSize = props?.pagination?.rowsPerPage ?? tablePagination.value.rowsPerPage;

  // 将 1-based row offset 转为页码（兼容 Quasar 的 @request 行为）
  const pageNum = rawPage > pageSize ? Math.ceil(rawPage / pageSize) : rawPage;

  // Quasar @request 事件中 sortBy/descending 嵌套在 pagination 内部
  if (props?.pagination) {
    sortState.value.sortBy = props.pagination.sortBy ?? "";
    sortState.value.descending = props.pagination.descending ?? false;
    tablePagination.value.sortBy = props.pagination.sortBy ?? "";
    tablePagination.value.descending = props.pagination.descending ?? false;
  }

  // 前端列名 → 后端真实字段名映射（驼峰 → 下划线）
  const sortBy = sortState.value.sortBy || undefined;
  const orderBy = sortBy ? (SORT_FIELD_MAP[sortBy] ?? sortBy) : undefined;

  const params: UserPageRequest = {
    pageNum,
    pageSize,
    username: searchForm.username || undefined,
    nickname: searchForm.nickname || undefined,
    status: searchForm.status || undefined,
    deptId: searchForm.deptId || undefined,
    orderBy,
    orderDirection: sortState.value.sortBy ? (sortState.value.descending ? "desc" : "asc") : undefined
  };

  try {
    const result = await getUserPageApi(params);
    if (result.code === 10_000) {
      tableRows.value = result.data.records ?? [];
      tableTotal.value = result.data.total ?? 0;
      tablePagination.value.page = result.data.current ?? pageNum;
      tablePagination.value.rowsPerPage = result.data.size ?? pageSize;
      tablePagination.value.rowsNumber = result.data.total ?? 0;
    } else {
      showToast(result.message || t("common.loadFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("common.loadFail"), "negative");
    }
  } finally {
    tableLoading.value = false;
  }
}

function handleSearch() {
  tablePagination.value.page = 1;
  loadTableData();
}

function handleReset() {
  searchForm.username = "";
  searchForm.nickname = "";
  searchForm.status = "";
  searchForm.deptId = "";
  extraSearch.phone = "";
  extraSearch.userType = "";
  selectedDeptId.value = "";
  lastSelectedDeptId = "";
  tablePagination.value.page = 1;
  loadTableData();
}

// ── 监听搜索条件变化后的自动联动（部门树选中同步） ──
watch(
  () => searchForm.deptId,
  (v) => {
    if (!v) selectedDeptId.value = "";
  }
);

// ═══════════════════════════════════════════════════════════════
// 操作
// ═══════════════════════════════════════════════════════════════

// 添加用户
function handleCreate() {
  openUserDrawer("add");
}

// 批量修改（占位）
function handleBatchEdit() {
  if (!selectedRows.value.length) {
    showToast(t("common.selectRowsFirst"), "warning");
    return;
  }
  showToast(t("common.comingSoon"), "info");
}

// 批量删除
async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    showToast(t("common.selectRowsFirst"), "warning");
    return;
  }

  const builtinUsers = selectedRows.value.filter((u) => u.isBuiltin === 1);
  if (builtinUsers.length) {
    showToast(
      t("user.cannotDeleteBuiltinBatch", {
        names: builtinUsers.map((u) => u.username).join("、")
      }),
      "warning"
    );
    return;
  }

  try {
    await $q.dialog({
      title: t("common.confirm"),
      message: t("user.batchDeleteConfirm", { count: selectedRows.value.length }),
      cancel: true,
      persistent: true
    });
  } catch {
    return;
  }

  let successCount = 0;
  let failCount = 0;

  for (const user of selectedRows.value) {
    try {
      const result = await deleteUserApi(user.id);
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
    t("user.batchDeleteResult", { success: successCount, fail: failCount }),
    successCount > 0 ? "positive" : "negative"
  );

  selectedRows.value = [];
  loadTableData();
}

// 查看
function handleView(user: SysUser) {
  openUserDrawer("view", user);
}

// 编辑
function handleEdit(user: SysUser) {
  if (user.userType === "superadmin") {
    showToast(t("user.superadminCannotEdit"), "warning");
    return;
  }
  openUserDrawer("edit", user);
}

// 删除
async function handleDelete(user: SysUser) {
  if (user.userType === "superadmin") {
    showToast(t("user.superadminCannotEdit"), "warning");
    return;
  }
  if (user.isBuiltin === 1) {
    showToast(t("user.cannotDeleteBuiltin"), "warning");
    return;
  }

  try {
    await $q.dialog({
      title: t("common.confirm"),
      message: t("user.deleteConfirm", { username: user.username }),
      cancel: true,
      persistent: true
    });
  } catch {
    return;
  }

  try {
    const result = await deleteUserApi(user.id);
    if (result.code === 10_000) {
      showToast(t("common.deleteSuccess"), "positive");
      loadTableData();
    } else {
      showToast(result.message || t("common.deleteFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("common.deleteFail"), "negative");
    }
  }
}

// 切换启用/停用状态
async function handleToggleStatus(user: SysUser) {
  if (user.userType === "superadmin") {
    showToast(t("user.superadminCannotEdit"), "warning");
    return;
  }
  if (user.isBuiltin === 1) {
    showToast(t("user.cannotChangeBuiltinStatus"), "warning");
    return;
  }

  const newStatus = user.status === "active" ? "inactive" : "active";
  try {
    await $q.dialog({
      title: t("common.confirm"),
      message: t("user.statusChangeConfirm", {
        username: user.username,
        action: newStatus === "active" ? t("common.enable") : t("common.disable")
      }),
      cancel: true,
      persistent: true
    });
  } catch {
    return;
  }

  try {
    const result = await changeUserStatusApi(user.id, newStatus);
    if (result.code === 10_000) {
      showToast(t("common.operationSuccess"), "positive");
      loadTableData();
    } else {
      showToast(result.message || t("common.operationFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("common.operationFail"), "negative");
    }
  }
}

// 重置密码
async function handleResetPassword(user: SysUser) {
  if (user.isBuiltin === 1) {
    showToast(t("user.cannotResetBuiltinPassword"), "warning");
    return;
  }

  try {
    await $q.dialog({
      title: t("common.confirm"),
      message: t("user.resetPasswordConfirm", { username: user.username }),
      cancel: true,
      persistent: true
    });
  } catch {
    return;
  }

  try {
    const result = await resetUserPasswordApi(user.id, "123456");
    if (result.code === 10_000) {
      showToast(t("user.resetPasswordSuccess"), "positive");
    } else {
      showToast(result.message || t("common.operationFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("common.operationFail"), "negative");
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
  <div class="user-list-shell">
    <!-- ═══ 左侧：部门树 ═══ -->
    <div class="left-panel" :class="{ 'left-panel--collapsed': leftPanelCollapsed }">
      <!-- 折叠状态仅展示竖向提示条 -->
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
            t("user.expandDeptTree")
          }}</q-tooltip>
        </q-btn>
      </div>

      <template v-else>
        <!-- 头部：标题 + 折叠按钮 -->
        <div class="left-panel-header">
          <div class="left-panel-header-title row items-center no-wrap">
            <q-icon name="sym_r_account_tree" size="20px" class="q-mr-xs" />
            <span>{{ t("user.deptTree") }}</span>
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
            <q-tooltip>{{ t("user.collapseDeptTree") }}</q-tooltip>
          </q-btn>
        </div>

        <!-- 「全部」节点 + 树 -->
        <q-scroll-area class="left-panel-scroll">
          <div class="left-panel-tree">
            <!-- 加载骨架 -->
            <template v-if="deptTreeLoading">
              <div v-for="i in 6" :key="i" class="dept-skeleton-row">
                <q-skeleton type="rect" width="60%" height="16px" class="q-ml-md q-my-sm" />
              </div>
            </template>

            <!-- 部门树（根节点为"全部"） -->
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
                    :name="deptTreeExpanded.includes(scope.node.id) ? 'sym_r_folder_open' : 'sym_r_folder'"
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

      <!-- 拖拽调整宽度手柄 -->
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
            <span class="search-area-title">{{
              t("common.searchCondition")
            }}</span>
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
                v-model="searchForm.username"
                filled
                square
                dense
                :placeholder="t('user.usernamePlaceholder')"
                hide-bottom-space
                clearable
                @keyup.enter="handleSearch"
              />
            </div>
            <div class="col">
              <q-input
                v-model="searchForm.nickname"
                filled
                square
                dense
                :placeholder="t('user.nicknamePlaceholder')"
                hide-bottom-space
                clearable
                @keyup.enter="handleSearch"
              />
            </div>
            <div class="col">
              <q-input
                v-model="extraSearch.phone"
                filled
                square
                dense
                :placeholder="t('user.phonePlaceholder')"
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
                  <span class="status-placeholder">{{ t('user.statusPlaceholder') }}</span>
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
            {{ t('user.createUser') }}
          </q-btn>
          <q-btn
            color="white"
            text-color="grey-8"
            outline
            dense
            no-caps
            class="toolbar-btn"
            :disable="!selectedRows.length"
            @click="handleBatchEdit"
          >
            <q-icon name="sym_r_edit" size="20px" class="q-mr-xs" />
            {{ t('common.batchEdit') }}
          </q-btn>
          <q-btn
            color="white"
            text-color="negative"
            outline
            dense
            no-caps
            class="toolbar-btn"
            :disable="!selectedRows.length"
            @click="handleBatchDelete"
          >
            <q-icon name="sym_r_delete" size="20px" class="q-mr-xs" />
            {{ t('common.batchDelete') }}
          </q-btn>
        </div>
        <q-space />
        <div class="toolbar-right row items-center no-wrap">
          <!-- 导入导出按钮已隐藏 -->
        </div>
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
        :class="['user-table', { 'user-table--empty': !tableRows.length }]"
        @request="loadTableData"
      >
        <!-- 性别列 -->
        <template #body-cell-gender="props">
          <q-td :props="props">
            <span v-if="props.value">{{ genderLabelOf(props.value) }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 用户类型列 -->
        <template #body-cell-userType="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              :color="userTypeColorOf(props.value)"
              :label="userTypeLabelOf(props.value)"
              rounded
              class="user-type-badge"
            />
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 状态列 -->
        <template #body-cell-status="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              :color="statusColorOf(props.value)"
              :label="t(`user.status${props.value.charAt(0).toUpperCase() + props.value.slice(1)}`)"
              rounded
              class="status-badge"
            />
            <span v-else class="text-grey-5">-</span>
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
              :disable="props.row.userType === 'superadmin'"
              @click.stop="handleEdit(props.row)"
            >
              <q-tooltip>{{ t("common.edit") }}</q-tooltip>
            </q-btn>
            <q-btn
              flat
              dense
              round
              size="sm"
              :color="props.row.status === 'active' ? 'orange' : 'positive'"
              :icon="
                props.row.status === 'active'
                  ? 'sym_r_block'
                  : 'sym_r_check_circle'
              "
              :disable="props.row.userType === 'superadmin'"
              @click="handleToggleStatus(props.row)"
            >
              <q-tooltip>{{
                props.row.status === "active" ? t("common.disable") : t("common.enable")
              }}</q-tooltip>
            </q-btn>
            <q-btn
              flat
              dense
              round
              size="sm"
              color="warning"
              icon="sym_r_lock_reset"
              :disable="props.row.userType === 'superadmin' && props.row.isBuiltin === 1"
              @click="handleResetPassword(props.row)"
            >
              <q-tooltip>{{ t("user.resetPassword") }}</q-tooltip>
            </q-btn>
            <q-btn
              flat
              dense
              round
              size="sm"
              color="negative"
              icon="sym_r_delete"
              :disable="props.row.userType === 'superadmin'"
              @click="handleDelete(props.row)"
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
              v-model="props.pagination.page"
              :max="props.pagesNumber"
              size="sm"
              color="primary"
              boundary-links
              direction-links
              icon-first="keyboard_double_arrow_left"
              icon-prev="keyboard_arrow_left"
              icon-next="keyboard_arrow_right"
              icon-last="keyboard_double_arrow_right"
            />
            <span class="text-caption text-grey-7 q-ml-md q-mr-sm">{{ t("common.rowsPerPageLabel") }}</span>
            <q-select
              v-model="tablePagination.rowsPerPage"
              :options="[10, 20, 50, 100]"
              dense
              flat
              borderless
              class="rows-per-page-select"
              @update:model-value="handleSearch"
            >
              <template #append>
                <span class="text-caption">{{ t("common.rowsPerPageUnit") }}</span>
              </template>
            </q-select>
          </div>
        </template>
      </q-table>
    </div>
  </div>

  <!-- ═══ 本地右侧抽屉：添加 / 编辑 / 查看用户 ═══ -->
  <Teleport to="body">
    <Transition name="user-drawer-slide">
      <div v-if="drawerOpen" class="user-local-drawer-mask" @click.self="closeUserDrawer">
        <div class="user-local-drawer">
          <div class="user-drawer-shell">
            <div class="user-drawer-header row items-center no-wrap">
              <q-icon name="sym_r_add" size="20px" class="q-mr-sm" />
              <span class="user-drawer-title">{{ drawerTitle }}</span>
              <q-space />
              <q-btn
                flat
                dense
                round
                icon="sym_r_close"
                class="user-drawer-close-btn"
                @click="closeUserDrawer"
              />
            </div>
            <div class="user-drawer-body">
              <UserDrawerContent
                :mode="drawerMode"
                :user="drawerUser"
                @close="closeUserDrawer"
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
/* ═══ 整体壳层 — 填满 tab 内容区 ═══ */
.user-list-shell {
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

/* ═══ 图标按钮 — 与系统导航面板 .left-toolbar-action-btn 一致 ═══ */
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

/* 部门计数徽章 */
.dept-count-badge {
  font-size: 11px;
  padding: 1px 6px;
}

/* 部门树骨架 */
.dept-skeleton-row {
  padding: 4px 0;
}

/* 部门树 */
.dept-tree {
  padding: 0 8px;
}

/* 覆盖 Quasar dense 模式下叶子节点的默认缩进，确保同级节点对齐 */
:deep(.dept-tree.q-tree--dense .q-tree__node--child) {
  padding-left: 0 !important;
}

/* 统一所有层级子节点的缩进宽度 */
:deep(.dept-tree.q-tree--dense .q-tree__children) {
  padding-left: 16px !important;
}

/* 隐藏默认展开/收起箭头 */
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

/* 拖拽手柄 */
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

.toolbar-left,
.toolbar-right {
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
.user-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.user-table :deep(.q-table__top) {
  display: none;
}

.user-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.user-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

/* 表头始终可见：sticky 定位，滚动时固定在容器顶部 */
.user-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 1;
}

/* 表格只占自然高度 */
.user-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

/* 表格表头样式 */
.user-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.user-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ── 空数据状态 ── */
/* 容器：.q-table__bottom 填满剩余空间，居中内容 */
.user-table--empty :deep(.q-table__container) {
  height: 100%;
}

.user-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.user-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.user-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

/* 行悬停 */
.user-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.user-table :deep(tbody tr.q-tr--selected td) {
  background: rgba(0, 121, 107, 0.06) !important;
}

.user-table :deep(tbody td) {
  font-size: 13px;
}

/* Badge 统一样式 */
.user-type-badge,
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
.user-table :deep(.q-table__bottom) {
  padding: 3px 16px 4px;
  font-size: 13px;
  min-height: 42px;
  background: #fff;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}
.table-bottom {
  min-height: 40px;
}

/* 分页器按钮：模拟 round + dense，与底部状态栏 database 按钮一致 */
.table-bottom :deep(.q-pagination__content .q-btn) {
  width: 32px !important;
  height: 32px !important;
  min-width: 32px !important;
  min-height: 32px !important;
  border-radius: 50% !important;
  padding: 0 !important;
  font-size: 10px !important;
}

/* 分页器按钮 focus-helper 圆形，与 q-focus-helper--round 一致 */
.table-bottom :deep(.q-pagination__content .q-btn .q-focus-helper) {
  border-radius: 50%;
}

/* 分页器图标：尺寸 20px */
.table-bottom :deep(.q-pagination__content .q-btn .q-icon) {
  font-size: 20px;
}

/* 每页条数选择器：与 Quasar 原生底栏一致 */
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

/* 复选框尺寸 */
.user-table :deep(.q-checkbox__inner) {
  font-size: 32px;
}

/* ═══ 本地右侧抽屉 ═══ */
.user-local-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 5000;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: flex-end;
}

.user-local-drawer {
  width: 680px;
  max-width: 100vw;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.user-drawer-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.user-drawer-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 65px;
  padding: 0 16px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.user-drawer-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.user-drawer-close-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  font-size: 20px;
}

.user-drawer-close-btn :deep(.q-btn__wrapper) {
  min-width: 32px;
  min-height: 32px;
  padding: 0;
}

.user-drawer-close-btn :deep(.q-icon) {
  font-size: 20px;
}

.user-drawer-close-btn:hover {
  background: rgba(128, 128, 128, 0.2);
}

.user-drawer-body {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 16px;
}

/* 抽屉滑入/滑出动画 */
.user-drawer-slide-enter-active,
.user-drawer-slide-leave-active {
  transition: opacity 0.25s ease;
}

.user-drawer-slide-enter-active .user-local-drawer,
.user-drawer-slide-leave-active .user-local-drawer {
  transition: transform 0.25s ease;
}

.user-drawer-slide-enter-from,
.user-drawer-slide-leave-to {
  opacity: 0;
}

.user-drawer-slide-enter-from .user-local-drawer,
.user-drawer-slide-leave-to .user-local-drawer {
  transform: translateX(100%);
}
</style>

<!-- 非 scoped：状态选择下拉弹出层（teleport 到 body） -->
<style>
.status-select-popup .q-item {
  min-height: 40px;
  padding: 0 16px;
}
</style>
