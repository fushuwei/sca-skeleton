<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysRole, PermissionAssignOption, PermissionTreeNode, SysTenant } from "../../types/auth";
import { createRoleApi, updateRoleApi } from "../../apis/role";
import { getRolePermissionIdsApi } from "../../apis/role";
import { getPermissionAssignOptionsApi } from "../../apis/permission";
import { getTenantListApi } from "../../apis/tenant";
import { useAuthStore } from "../../stores/auth";

const { t, locale } = useI18n({ useScope: "global" });
const authStore = useAuthStore();

const props = defineProps<{
  mode: "add" | "edit" | "view";
  role?: SysRole;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");
/** 当前登录用户是否为超管（超管创建时需选择目标租户） */
const isSuperadmin = computed(() => authStore.isSuperadmin);

const formLoading = ref(false);
const form = reactive({
  id: "",
  tenantId: "",
  name: "",
  code: "",
  dataScope: "",
  sort: 100,
  remark: "",
  permissionIds: [] as string[]
});

// ── 租户下拉数据（仅超管加载） ──
const tenantOptions = ref<SysTenant[]>([]);

async function loadTenantOptions() {
  if (!isSuperadmin.value) return;
  try {
    const result = await getTenantListApi();
    if (result.code === 10_000 && result.data) {
      tenantOptions.value = result.data;
    }
  } catch {
    // 静默失败，下拉为空
  }
}

const tenantOptionsFormatted = computed(() =>
  tenantOptions.value.map(t => ({ label: t.name, value: t.id }))
);

const formRules = computed(() => ({
  name: [(v: string) => !!v?.trim() || t("roleMgmt.nameRequired")],
  code: [(v: string) => !!v?.trim() || t("roleMgmt.codeRequired")],
  dataScope: [(v: string) => !!v || t("roleMgmt.dataScopeRequired")]
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
 * 后端已做过滤（仅返回启用且可见的权限，且非超管仅返回自身拥有的权限），
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
function filterPermTree(nodes: PermissionTreeNode[], keyword: string): PermissionTreeNode[] {
  if (!keyword.trim()) return nodes;
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
  if (val.trim()) {
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

async function loadPermTree() {
  permTreeLoading.value = true;
  try {
    const result = await getPermissionAssignOptionsApi();
    if (result.code === 10_000 && result.data) {
      allPermissions.value = result.data;
      // 默认展开第一级
      permTreeExpanded.value = permTreeNodes.value.map((n) => n.id);
      // 树加载完成后，执行待处理的权限回显操作
      if (pendingPermIds.value) {
        // 过滤为仅叶子节点 ID，匹配 q-tree leaf-filtered 策略
        const leafIds = collectLeafIds(permTreeNodes.value);
        permTreeTicked.value = pendingPermIds.value.filter((id) => leafIds.has(id));
        pendingPermIds.value = null;
      }
    }
  } catch {
    // 静默失败
  } finally {
    permTreeLoading.value = false;
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
        permTreeTicked.value = result.data.filter((id) => leafIds.has(id));
      }
    }
  } catch {
    // 静默失败
  }
}

function resetForm() {
  form.id = "";
  form.tenantId = "";
  form.name = "";
  form.code = "";
  form.dataScope = "";
  form.sort = 100;
  form.remark = "";
  form.permissionIds = [];
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
    form.sort = props.role.sort ?? 100;
    form.remark = props.role.remark || "";
    if (props.role.id) {
      // 编辑/查看模式加载已分配权限
      loadRolePermissions(props.role.id);
    }
  }
}

watch(() => props.role, initForm, { immediate: true });

onMounted(() => {
  loadPermTree();
  loadTenantOptions();
});

// 同步 q-tree ticked 到 form.permissionIds
watch(permTreeTicked, (val) => {
  form.permissionIds = [...val];
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
    name: form.name?.trim(),
    code: `${CODE_PREFIX}${form.code}`,
    dataScope: form.dataScope,
    sort: form.sort,
    remark: form.remark?.trim() || undefined,
    permissionIds: fullPermissionIds.length ? fullPermissionIds : undefined
  };

  // 超管创建时传目标租户 ID
  if (props.mode === "add" && isSuperadmin.value) {
    data.tenantId = form.tenantId;
  }

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
    <q-form class="role-drawer-form" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 目标租户（仅超管创建时显示） -->
        <div v-if="mode === 'add' && isSuperadmin" class="col-12">
          <q-select
            v-model="form.tenantId"
            :label="t('common.targetTenant')"
            filled
            square
            :options="tenantOptionsFormatted"
            emit-value
            map-options
            :rules="[(v: string) => !!v || t('common.targetTenantRequired')]"
            hide-bottom-space
            class="required-field"
          />
        </div>
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
        <!-- 数据权限范围 -->
        <div class="col-12 col-md-6">
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
        <!-- 备注 -->
        <div class="col-12">
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
      </div>

      <!-- ── 权限分配 ── -->
      <div class="perm-section q-mt-md">
        <div class="perm-section-header row items-center no-wrap q-mb-sm">
          <q-icon name="sym_r_security" size="20px" class="q-mr-xs" color="grey-8" />
          <span class="perm-section-title">{{ t('roleMgmt.permissionAssign') }}</span>
          <q-space />
          <span v-if="permTreeTicked.length" class="text-caption text-grey-7">
            {{ t('roleMgmt.selectedPermissions', { count: permTreeTicked.length }) }}
          </span>
        </div>
        <div class="perm-tree-container">
          <q-input
            v-model="permSearchKey"
            dense
            outlined
            square
            :placeholder="t('roleMgmt.searchPermission')"
            clearable
            class="q-mb-sm"
            :disable="drawerReadonly"
          >
            <template #prepend>
              <q-icon name="sym_r_search" size="18px" />
            </template>
          </q-input>
          <q-scroll-area style="height: 320px">
            <template v-if="permTreeLoading">
              <div v-for="i in 6" :key="i" class="perm-skeleton-row">
                <q-skeleton type="rect" width="60%" height="16px" class="q-ml-md q-my-sm" />
              </div>
            </template>
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
                  <q-icon
                    v-if="permNodeIcon(scope.node)"
                    :name="permNodeIcon(scope.node)"
                    size="18px"
                    class="q-mr-sm"
                    color="grey-7"
                  />
                  <span class="ellipsis">{{ scope.node.label }}</span>
                </div>
              </template>
            </q-tree>
          </q-scroll-area>
        </div>
      </div>

      <!-- 底部操作按钮 -->
      <div v-if="!drawerReadonly" class="role-drawer-footer row justify-end q-gutter-sm">
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
          color="primary"
          unelevated
          no-caps
          :loading="formLoading"
          class="drawer-action-btn"
        >
          {{ t('common.confirm') }}
        </q-btn>
      </div>
    </q-form>
  </div>
</template>

<style scoped>
.role-drawer-content {
  padding: 0;
}

.role-drawer-footer {
  flex-shrink: 0;
  padding: 12px 0 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  margin-top: 16px;
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

/* 权限分配区域 */
.perm-section {
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 4px;
  padding: 12px;
}

.perm-section-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.perm-tree-container {
  margin-top: 4px;
}

.perm-skeleton-row {
  padding: 4px 0;
}

.perm-tree {
  padding: 0 4px;
}

:deep(.perm-tree .q-tree__node) {
  padding-bottom: 0 !important;
}

:deep(.perm-tree .q-tree__node-header) {
  margin: 1px 0;
  padding: 0;
  min-height: 0;
  border-radius: 0;
  box-sizing: border-box;
}

.perm-tree-node {
  min-width: 0;
  padding: 4px 8px;
  min-height: 32px;
  border-radius: 4px;
}

/* 修复 prefix 右侧多余间距 */
:deep(.q-field__prefix) {
  padding-right: 0 !important;
}

/* 权限树复选框尺寸与角色列表页保持一致 */
.perm-tree :deep(.q-checkbox__bg) {
  width: 16px !important;
  height: 16px !important;
}

.perm-tree :deep(.q-checkbox__svg) {
  width: 12px !important;
  height: 12px !important;
}

/* 取消权限树复选框悬停背景色 */
.perm-tree :deep(.q-checkbox__inner::before) {
  display: none !important;
}

/* 查看模式：禁止权限树勾选交互 */
.perm-tree--readonly :deep(.q-tree__tickbox) {
  pointer-events: none;
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

/* 权限分配区域暗色模式 */
.body--dark .perm-section {
  border-color: rgba(255, 255, 255, 0.08);
}

.body--dark .perm-section-title {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .perm-section-header .q-icon {
  color: rgba(255, 255, 255, 0.72) !important;
}

/* 权限树骨架屏 */
.body--dark .perm-skeleton-row .q-skeleton {
  background: rgba(255, 255, 255, 0.08);
}
</style>
