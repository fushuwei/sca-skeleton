<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysRole, PermissionAssignOption, PermissionTreeNode, DeptOption } from "../../types/auth";
import { createRoleApi, updateRoleApi } from "../../apis/role";
import { getRolePermissionIdsApi } from "../../apis/role";
import { getRoleAssignOptionsApi } from "../../apis/role";
import { getRoleDeptIdsApi } from "../../apis/role";
import { getDeptOptionsApi } from "../../apis/dept";

const { t, locale } = useI18n({ useScope: "global" });

const props = defineProps<{
  mode: "add" | "edit" | "view";
  role?: SysRole;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");

const formLoading = ref(false);
const form = reactive({
  id: "",
  name: "",
  code: "",
  dataScope: "",
  realm: "",
  sort: 100,
  remark: "",
  permissionIds: [] as string[],
  deptIds: [] as string[]
});

const formRules = computed(() => ({
  name: [(v: string) => !!v?.trim() || t("roleMgmt.nameRequired")],
  code: [(v: string) => !!v?.trim() || t("roleMgmt.codeRequired")],
  dataScope: [(v: string) => !!v || t("roleMgmt.dataScopeRequired")],
  realm: [(v: string) => !!v || t("roleMgmt.realmRequired")],
  // 自定义数据权限必须至少选择一个部门；其余数据权限范围忽略该字段
  deptIds: [(v: string[]) => form.dataScope !== "custom" || (Array.isArray(v) && v.length > 0) || t("roleMgmt.deptRequired")]
}));

/** 角色编码前缀常量 */
const CODE_PREFIX = "ROLE_";

/** 角色编码输入处理：自动转大写 */
function onCodeInput(val: string | number | null) {
  form.code = String(val || "").toUpperCase().trim();
}

const dataScopeOptions = computed(() => [
  { label: t("roleMgmt.scopeAll"), value: "all" },
  { label: t("roleMgmt.scopeTenant"), value: "tenant" },
  { label: t("roleMgmt.scopeDeptAndSub"), value: "dept_and_sub" },
  { label: t("roleMgmt.scopeDept"), value: "dept" },
  { label: t("roleMgmt.scopePersonal"), value: "personal" },
  { label: t("roleMgmt.scopeCustom"), value: "custom" }
]);

const realmOptions = computed(() => [
  { label: t("roleMgmt.realmAdmin"), value: "admin" },
  { label: t("roleMgmt.realmPortal"), value: "portal" }
]);

// ── 权限树 ──
const permTreeLoading = ref(false);
const allPermissions = ref<PermissionAssignOption[]>([]);
const permTreeNodes = computed(() => buildPermTree(allPermissions.value));
const permTreeExpanded = ref<string[]>([]);
const permTreeTicked = ref<string[]>([]);
const permSearchKey = ref("");

/** 权限树未加载时的待处理操作 */
const pendingPermIds = ref<string[] | null>(null);

/**
 * 将扁平权限列表转成树结构。
 *
 * 后端已做过滤（仅返回启用且可见的权限；超管按当前租户套餐过滤，非超管仅返回自身拥有的权限），
 * 前端直接信任后端数据，不再做任何过滤，避免「掩耳盗铃」式掩盖后端问题。
 */
function buildPermTree(perms: PermissionAssignOption[]): PermissionTreeNode[] {
  if (!perms.length) return [];

  const sorted = [...perms].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));

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
      }
      // 父节点不在返回列表中（非超管场景下当前用户未持有该父权限），丢弃该节点
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

/** 收集权限树中所有叶子节点的 ID（leaf-filtered 策略要求 ticked 数组仅含叶子节点） */
function collectLeafIds(nodes: PermissionTreeNode[]): Set<string> {
  const leafIds = new Set<string>();
  const collect = (list: PermissionTreeNode[]) => {
    for (const n of list) {
      if (n.children?.length) {
        collect(n.children);
      } else {
        leafIds.add(n.id);
      }
    }
  };
  collect(nodes);
  return leafIds;
}

/** 统计节点子树内叶子节点总数（含自身为叶子时计 1），用于分支「已选/总数」统计 */
function countLeaves(node: PermissionTreeNode): number {
  if (!node.children?.length) return 1;
  return node.children.reduce((sum, c) => sum + countLeaves(c), 0);
}

/** 读取节点叶子总数 */
function nodeLeafCount(node: PermissionTreeNode): number {
  return countLeaves(node);
}

/** 分支已勾选叶子数：取子树叶子集合与已勾选集合的交集 */
function nodeTickedCount(node: PermissionTreeNode): number {
  if (!permTreeTicked.value.length) return 0;
  const leafIds = collectLeafIds([node]);
  return permTreeTicked.value.filter((id) => leafIds.has(id)).length;
}

/** 收集被勾选节点的所有祖先 ID（含自身），用于保存完整权限链（module/folder/menu + button） */
function collectWithAncestors(tickedIds: string[]): string[] {
  if (!tickedIds.length) return [];
  const result = new Set<string>(tickedIds);
  // 构建 id -> parentId 映射（基于全量权限列表，确保能追溯到被过滤的祖先）
  const parentMap = new Map<string, string>();
  for (const p of allPermissions.value) {
    parentMap.set(p.id, p.parentId);
  }
  // 对每个 ticked ID 沿 parentId 向上追溯，补全所有祖先
  for (const id of tickedIds) {
    let current = parentMap.get(id);
    while (current && current !== "0" && !result.has(current)) {
      result.add(current);
      current = parentMap.get(current);
    }
  }
  return [...result];
}

/** 节点图标：模块/目录用 folder/folder_open（随展开状态切换），菜单用 nest_eco_leaf，按钮无图标 */
function permNodeIcon(node: PermissionTreeNode): string {
  if (node.type === "module" || node.type === "folder") {
    return permTreeExpanded.value.includes(node.id) ? "sym_r_folder_open" : "sym_r_folder";
  }
  if (node.type === "menu") return "sym_r_nest_eco_leaf";
  // button 类型不展示图标
  return "";
}

/** 过滤树节点（按关键字，保留匹配的父节点） */
function filterPermTree(nodes: PermissionTreeNode[], keyword: string | null): PermissionTreeNode[] {
  if (!keyword?.trim()) return nodes;
  const lower = keyword.toLowerCase();
  const result: PermissionTreeNode[] = [];
  for (const n of nodes) {
    const childResult = n.children?.length ? filterPermTree(n.children, keyword) : [];
    if (n.label.toLowerCase().includes(lower) || childResult.length) {
      result.push({ ...n, children: childResult.length ? childResult : n.children?.length ? [] : undefined });
    }
  }
  return result;
}

const filteredPermTreeNodes = computed(() => filterPermTree(permTreeNodes.value, permSearchKey.value));

/** 搜索时自动展开所有节点 */
watch(permSearchKey, (val) => {
  if (val?.trim()) {
    const allKeys: string[] = [];
    const collectKeys = (nodes: PermissionTreeNode[]) => {
      for (const n of nodes) {
        allKeys.push(n.id);
        if (n.children?.length) collectKeys(n.children);
      }
    };
    collectKeys(filteredPermTreeNodes.value);
    permTreeExpanded.value = allKeys;
  }
});

/** 当前过滤结果的全部节点 key（展开/收起与全选均作用于可见范围） */
const filteredTreeKeys = computed(() => {
  const keys: string[] = [];
  const collectKeys = (nodes: PermissionTreeNode[]) => {
    for (const n of nodes) {
      keys.push(n.id);
      if (n.children?.length) collectKeys(n.children);
    }
  };
  collectKeys(filteredPermTreeNodes.value);
  return keys;
});

/** 可见节点是否已全部展开（决定工具栏按钮显示「收起」还是「展开」） */
const allVisibleExpanded = computed(
  () => filteredTreeKeys.value.length > 0 && filteredTreeKeys.value.every((k) => permTreeExpanded.value.includes(k))
);

/** 展开/收起全部（针对当前过滤后的可见树） */
function toggleExpandAll() {
  permTreeExpanded.value = allVisibleExpanded.value ? [] : [...filteredTreeKeys.value];
}

/** 全选：勾选可见树的全部叶子节点（与已有勾选取并集，避免搜索过滤时丢失不可见区域的勾选） */
function selectAllVisible() {
  const visibleLeaves = collectLeafIds(filteredPermTreeNodes.value);
  permTreeTicked.value = [...new Set([...permTreeTicked.value, ...visibleLeaves])];
}

/** 清空全部勾选 */
function clearAllTicks() {
  permTreeTicked.value = [];
}

async function loadPermTree() {
  permTreeLoading.value = true;
  try {
    // 按角色域过滤可分配权限，防止跨域授权；超管按当前租户套餐过滤
    const result = await getRoleAssignOptionsApi(form.realm);
    if (result.code === 10_000 && result.data) {
      allPermissions.value = result.data;
      // 默认展开第一级
      permTreeExpanded.value = permTreeNodes.value.map((n) => n.id);
      // 树加载完成后，执行待处理的权限回显操作
      if (pendingPermIds.value) {
        // 过滤为仅叶子节点 ID，匹配 q-tree leaf-filtered 策略
        const leafIds = collectLeafIds(permTreeNodes.value);
        const filtered = pendingPermIds.value.filter((id) => leafIds.has(id));
        permTreeTicked.value = filtered;
        form.permissionIds = [...filtered];
        pendingPermIds.value = null;
      }
    }
  } catch {
    // 静默失败
  } finally {
    permTreeLoading.value = false;
  }
}

// ── 部门树（自定义数据权限） ──
/** 部门树虚拟根节点 ID（不可选择） */
const ROOT_DEPT_ID = "0";

interface DeptTreeNode {
  id: string;
  label: string;
  parentId: string;
  /** 是否可勾选（QTree 节点属性，false 时该节点 checkbox 以禁用态渲染，不影响子节点） */
  tickable?: boolean;
  children?: DeptTreeNode[];
}
const deptTreeNodes = ref<DeptTreeNode[]>([]);
const deptTreeExpanded = ref<string[]>([]);
const deptSearchKey = ref("");
const deptMenuOpen = ref(false);

/** 将扁平部门列表转换为树结构 */
function buildDeptTree(depts: DeptOption[]): DeptTreeNode[] {
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
        parent.children.push(node);
      } else {
        roots.push(node);
      }
    }
  }

  // 清理空 children 数组，避免 q-tree 渲染多余的展开箭头
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

/** 过滤树节点（按关键字，保留匹配的父节点） */
function filterDeptTree(nodes: DeptTreeNode[], keyword: string): DeptTreeNode[] {
  if (!keyword?.trim()) return nodes;
  const lower = keyword.toLowerCase();
  const result: DeptTreeNode[] = [];
  for (const n of nodes) {
    const childResult = n.children?.length ? filterDeptTree(n.children, keyword) : [];
    if (n.label.toLowerCase().includes(lower) || childResult.length) {
      result.push({ ...n, children: childResult.length ? childResult : n.children?.length ? [] : undefined });
    }
  }
  return result;
}

/** 带虚拟根节点「全部」的树（q-tree 渲染用，根节点不可勾选）。
 *  不能给根节点设置 disabled —— QTree 的 disabled 会级联禁用该节点的所有子节点，导致部门全部无法勾选；
 *  故用 tickable: false 让根节点 checkbox 以禁用态渲染（置灰、不可点），strict 模式下不影响子节点，
 *  @update:ticked 过滤 ROOT_DEPT_ID 作为兜底。 */
const deptTreeWithRoot = computed(() => [{
  id: ROOT_DEPT_ID,
  label: t("roleMgmt.allDepts"),
  parentId: "",
  tickable: false,
  children: deptTreeNodes.value
}] as DeptTreeNode[]);

const filteredDeptTreeNodes = computed(() => filterDeptTree(deptTreeWithRoot.value, deptSearchKey.value));

/** 部门树是否为空态（无部门数据或搜索无结果） */
const deptTreeEmpty = computed(() => {
  if (deptSearchKey.value?.trim()) {
    return filteredDeptTreeNodes.value.length === 0;
  }
  return deptTreeNodes.value.length === 0;
});

/** 部门树空态提示文案 */
const deptTreeEmptyLabel = computed(() => {
  if (deptSearchKey.value?.trim()) {
    return t("roleMgmt.noDeptSearchResult");
  }
  return t("roleMgmt.noDeptData");
});

/** 搜索时自动展开所有节点 */
watch(deptSearchKey, (val) => {
  if (val?.trim()) {
    const allKeys: string[] = [];
    const collectKeys = (nodes: DeptTreeNode[]) => {
      for (const n of nodes) {
        allKeys.push(n.id);
        if (n.children?.length) collectKeys(n.children);
      }
    };
    collectKeys(filteredDeptTreeNodes.value);
    deptTreeExpanded.value = allKeys;
  }
});

/** 部门树加载后默认展开根节点和第一级 */
watch(deptTreeNodes, (nodes) => {
  if (nodes.length) {
    deptTreeExpanded.value = [ROOT_DEPT_ID];
  }
}, { immediate: true });

/** 查找节点标签 */
function findDeptLabel(nodes: DeptTreeNode[], id: string): string {
  for (const n of nodes) {
    if (n.id === id) return n.label;
    if (n.children?.length) {
      const found = findDeptLabel(n.children, id);
      if (found) return found;
    }
  }
  return "";
}

/** 已选部门展示的最大数量，超出部分折叠为 +N */
const MAX_DEPT_DISPLAY = 3;

/** 已选部门的完整名称列表 */
const deptFullNames = computed(() =>
  form.deptIds.map((id) => findDeptLabel(deptTreeWithRoot.value, id) || id)
);

/** 已选部门的展示标签（多选，顿号分隔；数量过多时折叠为前 N 个 +N） */
const deptMultiDisplayLabel = computed(() => {
  if (!deptFullNames.value.length) return "";
  if (deptFullNames.value.length <= MAX_DEPT_DISPLAY) {
    return deptFullNames.value.join("、");
  }
  return (
    deptFullNames.value.slice(0, MAX_DEPT_DISPLAY).join("、") +
    t("roleMgmt.deptMore", { count: deptFullNames.value.length - MAX_DEPT_DISPLAY })
  );
});

/** 节点图标：使用 folder/folder_open 风格，与部门管理一致 */
function deptNodeIcon(node: DeptTreeNode): string {
  return deptTreeExpanded.value.includes(node.id) ? "sym_r_folder_open" : "sym_r_folder";
}

/** 节点是否已勾选 */
function isDeptTicked(node: DeptTreeNode): boolean {
  return form.deptIds.includes(node.id);
}

/** 处理勾选变化：过滤虚拟根节点「全部」，它仅用于分组展示，不可勾选（不能靠 disabled 实现，会级联禁用子节点） */
function onDeptTicked(ticked: readonly string[]) {
  form.deptIds = [...ticked].filter((id) => id !== ROOT_DEPT_ID);
}

/** 部门选择菜单关闭时清理搜索残留与搜索展开态，避免下次打开仍带旧关键字和展开状态 */
function onDeptMenuHide() {
  deptMenuOpen.value = false;
  deptSearchKey.value = "";
  deptTreeExpanded.value = deptTreeNodes.value.length ? [ROOT_DEPT_ID] : [];
}

/** 加载部门选项（当前租户下的部门） */
async function loadDeptOptions() {
  try {
    const result = await getDeptOptionsApi();
    if (result.code === 10_000 && result.data) {
      deptTreeNodes.value = buildDeptTree(result.data);
    }
  } catch {
    // 静默失败，下拉为空
  }
}

/** 加载角色已分配的自定义数据权限部门 ID 列表 */
async function loadRoleDeptIds(roleId: string) {
  try {
    const result = await getRoleDeptIdsApi(roleId);
    if (result.code === 10_000 && result.data) {
      form.deptIds = result.data;
    }
  } catch {
    // 静默失败
  }
}

/** 加载角色已分配的权限 ID 列表 */
async function loadRolePermissions(roleId: string) {
  try {
    const result = await getRolePermissionIdsApi(roleId);
    if (result.code === 10_000 && result.data) {
      if (allPermissions.value.length === 0) {
        // 权限树尚未加载，暂存待处理
        pendingPermIds.value = result.data;
      } else {
        // 过滤为仅叶子节点 ID，匹配 q-tree leaf-filtered 策略
        const leafIds = collectLeafIds(permTreeNodes.value);
        const filtered = result.data.filter((id) => leafIds.has(id));
        permTreeTicked.value = filtered;
        form.permissionIds = [...filtered];
      }
    }
  } catch {
    // 静默失败
  }
}

function resetForm() {
  form.id = "";
  form.name = "";
  form.code = "";
  form.dataScope = "";
  form.realm = "";
  form.sort = 100;
  form.remark = "";
  form.permissionIds = [];
  form.deptIds = [];
  permTreeTicked.value = [];
}

function initForm() {
  resetForm();
  if (props.role) {
    form.id = props.role.id;
    form.name = props.role.name;
    // 编辑/查看时剥离 ROLE_ 前缀，仅展示后缀部分
    form.code = props.role.code?.startsWith(CODE_PREFIX)
      ? props.role.code.slice(CODE_PREFIX.length)
      : (props.role.code ?? "");
    form.dataScope = props.role.dataScope;
    form.realm = props.role.realm;
    form.sort = props.role.sort ?? 100;
    form.remark = props.role.remark || "";
    if (props.role.id) {
      // 编辑/查看模式加载已分配权限
      loadRolePermissions(props.role.id);
      // 编辑/查看模式加载自定义数据权限部门
      loadRoleDeptIds(props.role.id);
    }
  }
}

watch(() => props.role, initForm, { immediate: true });

onMounted(() => {
  // 新增模式：默认不加载授权面板（权限树为空），待选择角色域后动态查询；
  // 编辑/查看模式：realm 已从角色回填，打开时按角色域加载权限树。
  if (props.mode !== "add") {
    loadPermTree();
  }
});

// 同步 q-tree ticked 到 form.permissionIds
watch(permTreeTicked, (val) => {
  form.permissionIds = [...val];
});

/** 新增模式下，是否满足加载权限树的前置条件：需选定角色域 */
function canLoadPermTree(): boolean {
  if (!form.realm) return false;
  return true;
}

// 新增模式下，切换权限域时清空已勾选权限（防止跨域权限残留）并重新加载权限树
watch(() => form.realm, (newRealm, oldRealm) => {
  // 仅新增模式且用户主动切换时触发（编辑/查看模式 realm 禁用，initForm 赋值由 onMounted 负责首次加载）
  if (props.mode !== 'add' || newRealm === oldRealm) return;
  permTreeTicked.value = [];
  form.permissionIds = [];
  if (!canLoadPermTree()) return;
  loadPermTree();
});

// 数据权限切换为「自定义」且部门树未加载时，加载当前租户部门树
watch(() => form.dataScope, (val) => {
  if (val === "custom" && deptTreeNodes.value.length === 0) {
    loadDeptOptions();
  }
});

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  // 收集被勾选叶子节点的所有祖先 ID，确保保存完整权限链（module/folder/menu + button），
  // 避免 leaf-filtered 策略导致只保存 button 而菜单树断裂
  const fullPermissionIds = collectWithAncestors(form.permissionIds);

  const data: Record<string, unknown> = {
    name: form.name,
    code: `${CODE_PREFIX}${form.code}`,
    dataScope: form.dataScope,
    realm: form.realm,
    sort: form.sort,
    remark: form.remark || undefined,
    permissionIds: fullPermissionIds.length ? fullPermissionIds : undefined,
    // 仅自定义数据权限时才提交部门 ID 列表，其余数据权限范围忽略
    deptIds: form.dataScope === "custom" ? (form.deptIds.length ? form.deptIds : undefined) : undefined
  };

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createRoleApi(data);
    } else {
      data.id = form.id;
      data.version = props.role?.version;
      result = await updateRoleApi(data);
    }

    if (result.code === 10_000) {
      showToast(t("roleMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("roleMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("roleMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}
</script>

<template>
  <div class="role-drawer-content">
    <!-- 中间内容区（唯一滚动区）：表单字段 -->
    <q-form id="role-drawer-form" class="role-drawer-form role-drawer-main" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 角色名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.name"
            :label="t('roleMgmt.name')"
            filled
            square
            :rules="formRules.name"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 角色编码 -->
        <div class="col-12 col-md-6">
          <q-input
            :model-value="form.code"
            @update:model-value="onCodeInput"
            :prefix="CODE_PREFIX"
            :label="t('roleMgmt.code')"
            filled
            square
            :rules="formRules.code"
            :disable="drawerReadonly || mode === 'edit'"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          >
            <template v-if="mode === 'edit'" #append>
              <q-icon name="sym_r_lock" size="18px" color="grey-6">
                <q-tooltip>{{ t('roleMgmt.codeLocked') }}</q-tooltip>
              </q-icon>
            </template>
          </q-input>
        </div>
        <!-- 数据权限范围：自定义数据权限时缩为半宽，另一半显示部门多选下拉框 -->
        <div :class="form.dataScope === 'custom' ? 'col-12 col-md-6' : 'col-12'">
          <q-select
            v-model="form.dataScope"
            :label="t('roleMgmt.dataScope')"
            filled
            square
            :options="dataScopeOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :rules="formRules.dataScope"
            :disable="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 自定义数据权限：部门多选。用 v-show 而非 v-if，保证该字段始终挂载、注册顺序位于角色域之前，Quasar 串行校验时先提示部门 -->
        <div v-show="form.dataScope === 'custom'" class="col-12 col-md-6">
          <q-select
            v-model="form.deptIds"
            :label="t('roleMgmt.customDept')"
            filled
            square
            multiple
            emit-value
            :display-value="deptMultiDisplayLabel"
            :rules="formRules.deptIds"
            :disable="drawerReadonly"
            hide-bottom-space
            dropdown-icon="sym_r_arrow_drop_down"
            :class="{ 'dept-select--menu-open': deptMenuOpen, 'required-field': true }"
          >
            <!-- 已选部门过多时折叠为 +N，悬浮提示完整列表 -->
            <template #append>
              <q-icon
                v-if="deptFullNames.length > MAX_DEPT_DISPLAY"
                name="sym_r_info"
                size="24px"
                class="dept-more-tip"
              >
                <q-tooltip anchor="top right" self="bottom right" :offset="[0, 8]" max-width="280px">
                  <!-- 每个已选部门一行展示，超长名称在行内换行 -->
                  <div v-for="(name, idx) in deptFullNames" :key="idx" class="dept-tooltip-line">{{ name }}</div>
                </q-tooltip>
              </q-icon>
            </template>
            <q-menu
              anchor="bottom left"
              self="top left"
              :offset="[0, 0]"
              no-focus
              no-route-update
              fit
              @before-show="deptMenuOpen = true"
              @before-hide="onDeptMenuHide"
            >
              <div class="q-pa-sm">
                <q-input
                  v-model="deptSearchKey"
                  dense
                  outlined
                  square
                  :placeholder="t('roleMgmt.searchDept')"
                  clearable
                  class="q-mb-sm"
                >
                  <template #prepend>
                    <q-icon name="sym_r_search" size="18px" />
                  </template>
                </q-input>
                <q-scroll-area style="height: 280px">
                  <!-- 部门树空态：无部门数据或搜索无结果 -->
                  <div v-if="deptTreeEmpty" class="dept-panel__empty">
                    {{ deptTreeEmptyLabel }}
                  </div>
                  <q-tree
                    v-else
                    :nodes="filteredDeptTreeNodes"
                    node-key="id"
                    label-key="label"
                    children-key="children"
                    v-model:expanded="deptTreeExpanded"
                    :ticked="form.deptIds"
                    @update:ticked="onDeptTicked"
                    tick-strategy="strict"
                    no-connectors
                    dense
                    class="perm-tree"
                    no-nodes-label=" "
                  >
                    <template #default-header="scope">
                      <!-- 行结构复用权限树的 perm-tree-node（40px 行高/占满整行），与套餐权限面板树一致 -->
                      <div
                        class="perm-tree-node dept-tree-option row items-center no-wrap full-width"
                        :class="{ 'dept-tree-option--disabled': scope.node.id === ROOT_DEPT_ID }"
                      >
                        <q-icon
                          :name="deptNodeIcon(scope.node)"
                          size="18px"
                          class="q-mr-sm"
                          :class="{ 'text-primary': isDeptTicked(scope.node) }"
                        />
                        <span class="ellipsis" :class="{ 'text-primary text-weight-medium': isDeptTicked(scope.node) }">
                          {{ scope.node.label }}
                        </span>
                      </div>
                    </template>
                  </q-tree>
                </q-scroll-area>
              </div>
            </q-menu>
          </q-select>
        </div>
        <!-- 角色域 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.realm"
            :label="t('roleMgmt.realm')"
            filled
            square
            :options="realmOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :rules="formRules.realm"
            :disable="drawerReadonly || mode !== 'add'"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 排序 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.number="form.sort"
            :label="t('roleMgmt.sort')"
            filled
            square
            type="number"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
      </div>

      <!-- ── 权限分配：一体化面板（面板头 + 工具行 + 树内容区） ── -->
      <div class="perm-panel q-mt-md" :class="{ 'perm-panel--readonly': drawerReadonly }">
        <!-- 面板头：左侧标题，右侧已选统计胶囊与快捷操作 -->
        <div class="perm-panel__head">
          <q-icon name="sym_r_security" size="18px" class="perm-panel__head-icon" />
          <span class="perm-panel__title">{{ t('roleMgmt.permissionAssign') }}</span>
          <q-space />
          <q-badge v-if="permTreeTicked.length" color="primary" rounded outline class="perm-panel__count">
            {{ t('roleMgmt.selectedPermissions', { count: permTreeTicked.length }) }}
          </q-badge>
          <template v-if="!drawerReadonly">
            <q-btn flat dense no-caps size="12px" color="primary" class="perm-panel__action" @click="selectAllVisible">
              {{ t('roleMgmt.selectAll') }}
            </q-btn>
            <q-btn
              flat dense no-caps size="12px" color="primary"
              class="perm-panel__action"
              :disable="!permTreeTicked.length"
              @click="clearAllTicks"
            >
              {{ t('roleMgmt.clearAll') }}
            </q-btn>
          </template>
          <q-btn flat dense no-caps size="12px" color="primary" class="perm-panel__action" @click="toggleExpandAll">
            {{ allVisibleExpanded ? t('roleMgmt.collapseAll') : t('roleMgmt.expandAll') }}
          </q-btn>
        </div>

        <!-- 工具行：搜索框 -->
        <div class="perm-panel__toolbar">
          <q-input
            v-model="permSearchKey"
            filled
            square
            dense
            :placeholder="t('roleMgmt.searchPermission')"
            clearable
            hide-bottom-space
            class="full-width"
            :disable="drawerReadonly"
          >
            <template #prepend>
              <q-icon name="sym_r_search" size="18px" />
            </template>
          </q-input>
        </div>

        <!-- 树内容区：独立滚动 -->
        <div class="perm-panel__body">
          <q-scroll-area style="height: 320px">
            <template v-if="permTreeLoading">
              <div v-for="i in 6" :key="i" class="perm-skeleton-row">
                <q-skeleton type="rect" width="60%" height="16px" class="q-ml-md q-my-sm" />
              </div>
            </template>
            <div v-else-if="!filteredPermTreeNodes.length && permSearchKey?.trim()" class="perm-panel__empty">
              {{ t('roleMgmt.noSearchResult') }}
            </div>
            <q-tree
              v-else
              :nodes="filteredPermTreeNodes"
              node-key="id"
              label-key="label"
              children-key="children"
              v-model:expanded="permTreeExpanded"
              v-model:ticked="permTreeTicked"
              tick-strategy="leaf-filtered"
              no-connectors
              dense
              :no-tick-children="drawerReadonly"
              :class="['perm-tree', { 'perm-tree--readonly': drawerReadonly }]"
              no-nodes-label=" "
            >
              <template #default-header="scope">
                <div class="perm-tree-node row items-center no-wrap full-width">
                  <!-- 查看模式：以勾选图标展示已选状态，替代禁用的复选框 -->
                  <q-icon
                    v-if="drawerReadonly && permTreeTicked.includes(scope.node.id)"
                    name="sym_r_check_small"
                    size="16px"
                    color="primary"
                    class="q-mr-xs"
                  />
                  <q-icon
                    v-if="permNodeIcon(scope.node)"
                    :name="permNodeIcon(scope.node)"
                    size="18px"
                    class="q-mr-sm"
                  />
                  <span class="ellipsis">{{ scope.node.label }}</span>
                  <q-space />
                  <!-- 分支统计徽章：仅在有勾选时展示，选满时转主色 -->
                  <span
                    v-if="scope.node.children?.length && nodeTickedCount(scope.node) > 0"
                    class="perm-tree-node__stats"
                    :class="{ 'perm-tree-node__stats--full': nodeTickedCount(scope.node) === nodeLeafCount(scope.node) }"
                  >
                    {{ nodeTickedCount(scope.node) }}/{{ nodeLeafCount(scope.node) }}
                  </span>
                </div>
              </template>
            </q-tree>
          </q-scroll-area>
        </div>
      </div>

      <!-- 备注 -->
      <div class="q-mt-md">
        <q-input
          v-model="form.remark"
          :label="t('roleMgmt.remark')"
          filled
          square
          type="textarea"
          rows="3"
          :disable="drawerReadonly"
          :readonly="drawerReadonly"
          hide-bottom-space
        />
      </div>
    </q-form>

    <!-- 底部操作栏：固定第三段，不随内容滚动；按钮直接挂在页脚容器上，间距由 CSS gap 控制 -->
    <div v-if="!drawerReadonly" class="role-drawer-footer row items-center justify-end no-wrap">
      <q-btn
        color="grey-7"
        outline
        no-caps
        class="drawer-action-btn"
        @click="handleClose"
      >
        {{ t('common.cancel') }}
      </q-btn>
      <q-btn
        type="submit"
        form="role-drawer-form"
        color="primary"
        unelevated
        no-caps
        :loading="formLoading"
        class="drawer-action-btn"
      >
        {{ t('common.confirm') }}
      </q-btn>
    </div>
  </div>
</template>

<style scoped>
/* 三段式抽屉布局：外层 .role-drawer-body 不再滚动，
   中间内容区（q-form.role-drawer-main）独立滚动，底部操作栏固定为第三段 */
.role-drawer-content {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* 中间内容区：唯一的滚动区域 */
.role-drawer-main {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 16px 12px;
}

/* 底部操作栏：固定第三段，不参与内容滚动（按钮直接子元素，gap 控制间距） */
.role-drawer-footer {
  flex-shrink: 0;
  gap: 8px;
  padding: 14px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.drawer-action-btn {
  min-width: 72px;
}

/* 必填项星号红色高亮 */
.required-field :deep(.q-field__label::after) {
  content: " *";
  color: var(--q-negative);
}

:deep(.q-field__append > .q-icon:not(.text-negative)) {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 修复 prefix 右侧多余间距 */
:deep(.q-field__prefix) {
  padding-right: 0 !important;
}

/* ── 权限分配一体化面板：面板头 + 工具行 + 树内容区共享同一边框与背景 ── */
.perm-panel {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: #fafbfc;
  overflow: hidden;
}

/* 只读态：白底信息展示卡片 */
.perm-panel--readonly {
  background: #fff;
}

/* 面板头：左侧标题，右侧统计胶囊与快捷操作 */
.perm-panel__head {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.perm-panel__head-icon {
  color: #757575;
  margin-right: 4px;
  flex-shrink: 0;
}

.perm-panel__title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  flex-shrink: 0;
}

.perm-panel__count {
  font-weight: 400;
  padding: 3px 9px;
  margin-right: 4px;
  flex-shrink: 0;
}

.perm-panel__action {
  padding: 0 6px;
  flex-shrink: 0;
}

/* 工具行：搜索框 */
.perm-panel__toolbar {
  padding: 6px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

/* 树内容区 */
.perm-panel__body {
  padding: 6px 2px;
}

/* 滚动区内层内容左右 10px 内边距 */
.perm-panel__body :deep(.q-scrollarea__content) {
  padding: 0 10px;
}

/* 搜索无匹配的空态 */
.perm-panel__empty {
  height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #9aa3af;
}

.perm-skeleton-row {
  padding: 4px 0;
}

/* 树节点行头占满整行 */
.perm-tree :deep(.q-tree__node-child) {
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
}

.perm-tree :deep(.q-tree__node-header) {
  flex: 1 1 auto;
  min-width: 0;
  margin: 0;
  padding: 0;
  min-height: 0;
  border-radius: 6px;
  box-sizing: border-box;
  transition: background 0.15s ease;
}

.perm-tree :deep(.q-tree__node-header:hover) {
  background: rgba(25, 118, 210, 0.04);
}

/* 行高 40px */
.perm-tree-node {
  min-width: 0;
  padding: 4px 4px;
  min-height: 40px;
}

/* 复选框与节点内容零间距 */
.perm-tree :deep(.q-tree__tickbox) {
  margin-right: 0;
}

/* 复选框尺寸 */
.perm-tree :deep(.q-tree__tickbox .q-checkbox__inner) {
  font-size: 32px;
  width: 1em;
  min-width: 1em;
  height: 1em;
}

.perm-tree :deep(.q-tree__tickbox .q-checkbox__bg) {
  top: 25%;
  left: 25%;
  width: 50%;
  height: 50%;
}

/* 悬停圆圈同步表格复选框 */
.perm-tree :deep(.q-tree__tickbox.q-checkbox--dense:not(.disabled):hover .q-checkbox__inner:before),
.perm-tree :deep(.q-tree__tickbox.q-checkbox--dense:not(.disabled):focus .q-checkbox__inner:before) {
  transform: scale3d(1, 1, 1);
}

/* 移除 focus 拘留灰色背景 */
.perm-tree :deep(.q-tree__node-header .q-focus-helper) {
  display: none;
}

/* 分支统计徽章 */
.perm-tree-node__stats {
  margin-left: 8px;
  font-size: 11px;
  color: #9aa3af;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.perm-tree-node__stats--full {
  color: var(--q-primary);
  font-weight: 600;
}

/* 查看模式：隐藏复选框列 */
.perm-tree--readonly :deep(.q-tree__tickbox) {
  display: none;
}

/* 部门树节点行：行高/占满整行/悬停/复选框尺寸均由 perm-tree + perm-tree-node 提供（与套餐权限面板树一致），
   此处仅保留虚拟根节点「全部」的禁用视觉 */
.dept-tree-option--disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.dept-tree-option--disabled:hover {
  background: transparent;
}

.dept-select--menu-open :deep(.q-field__append > .q-icon:not(.text-negative):not(.dept-more-tip)) {
  transform: rotate(180deg);
}

/* 部门树空态：无部门数据或搜索无结果 */
.dept-panel__empty {
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  text-align: center;
  font-size: 13px;
  color: #9aa3af;
}

/* 已选部门折叠提示图标 */
.dept-more-tip {
  color: #9aa3af;
}

/* 悬浮提示：已选部门完整列表多行展示，超长名称行内换行 */
.dept-tooltip-line {
  line-height: 1.6;
  overflow-wrap: anywhere;
}
</style>

<style>
.body--dark .role-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .role-drawer-form .q-field__native,
.body--dark .role-drawer-form .q-field__prefix,
.body--dark .role-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .role-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .role-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .role-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .role-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

/* 抽屉底部按钮区域分隔线 */
.body--dark .role-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}

/* 权限分配面板暗色模式 */
.body--dark .perm-panel {
  border-color: rgba(255, 255, 255, 0.08);
  background: #252525;
}

.body--dark .perm-panel--readonly {
  background: #222;
}

.body--dark .perm-panel__head,
.body--dark .perm-panel__toolbar {
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

.body--dark .perm-panel__head-icon {
  color: rgba(255, 255, 255, 0.72);
}

.body--dark .perm-panel__title {
  color: rgba(255, 255, 255, 0.87);
}

/* 暗色下树节点文字/图标统一 87% 白 */
.body--dark .perm-tree .q-tree__node-header-content {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .perm-tree .q-tree__node-header:hover {
  background: rgba(255, 255, 255, 0.06);
}

.body--dark .perm-tree-node__stats {
  color: rgba(255, 255, 255, 0.45);
}

/* 权限树骨架屏 */
.body--dark .perm-skeleton-row .q-skeleton {
  background: rgba(255, 255, 255, 0.08);
}

/* 部门树空态暗色模式 */
.body--dark .dept-panel__empty {
  color: rgba(255, 255, 255, 0.45);
}
</style>
