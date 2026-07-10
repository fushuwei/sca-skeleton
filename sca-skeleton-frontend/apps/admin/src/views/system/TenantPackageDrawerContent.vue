<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysTenantPackage, SysPermission, PermissionTreeNode } from "../../types/auth";
import { createTenantPackageApi, updateTenantPackageApi } from "../../apis/tenant-package";
import { getTenantPackagePermissionIdsApi } from "../../apis/tenant-package";
import { getPermissionListApi } from "../../apis/permission";

const { t, locale } = useI18n({ useScope: "global" });

const props = defineProps<{
  mode: "add" | "edit" | "view";
  pkg?: SysTenantPackage;
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
  status: "enabled",
  userLimit: -1 as number | null,
  apiLimit: -1 as number | null,
  storageLimit: -1 as number | null,
  expireDays: -1 as number | null,
  sort: 100,
  remark: "",
  permissionIds: [] as string[]
});

// ── 限额无限制开关：true 时对应字段为 -1（不限），false 时可输入具体值 ──
const userLimitUnlimited = ref(true);
const apiLimitUnlimited = ref(true);
const storageLimitUnlimited = ref(true);
const expireDaysUnlimited = ref(true);

function onUserLimitToggle(val: boolean | null) {
  const checked = !!val;
  userLimitUnlimited.value = checked;
  form.userLimit = checked ? -1 : null;
}
function onApiLimitToggle(val: boolean | null) {
  const checked = !!val;
  apiLimitUnlimited.value = checked;
  form.apiLimit = checked ? -1 : null;
}
function onStorageLimitToggle(val: boolean | null) {
  const checked = !!val;
  storageLimitUnlimited.value = checked;
  form.storageLimit = checked ? -1 : null;
}
function onExpireDaysToggle(val: boolean | null) {
  const checked = !!val;
  expireDaysUnlimited.value = checked;
  form.expireDays = checked ? -1 : null;
}

/** 限额校验：勾选无限制时跳过；未勾选时必填且必须为大于 0 的整数 */
function validateLimit(unlimited: boolean, val: number | null): true | string {
  if (unlimited) return true;
  if (val === null) {
    return t("tenantPackageMgmt.limitRequired");
  }
  if (!Number.isInteger(val) || val <= 0) {
    return t("tenantPackageMgmt.limitPositiveInteger");
  }
  return true;
}

const formRules = computed(() => ({
  name: [(v: string) => !!v?.trim() || t("tenantPackageMgmt.nameRequired")],
  code: [(v: string) => !!v?.trim() || t("tenantPackageMgmt.codeRequired")],
  status: [(v: string) => !!v || t("tenantPackageMgmt.statusRequired")],
  userLimit: [(v: number | null) => validateLimit(userLimitUnlimited.value, v)],
  apiLimit: [(v: number | null) => validateLimit(apiLimitUnlimited.value, v)],
  storageLimit: [(v: number | null) => validateLimit(storageLimitUnlimited.value, v)],
  expireDays: [(v: number | null) => validateLimit(expireDaysUnlimited.value, v)]
}));

/** 套餐编码输入处理：自动转小写 */
function onCodeInput(val: string | number | null) {
  form.code = String(val || "").toLowerCase().trim();
}

const statusOptions = computed(() => [
  { label: t("tenantPackageMgmt.statusEnabled"), value: "enabled" },
  { label: t("tenantPackageMgmt.statusDisabled"), value: "disabled" }
]);

// ── 权限树 ──
const permTreeLoading = ref(false);
const allPermissions = ref<SysPermission[]>([]);
const permTreeNodes = computed(() => buildPermTree(allPermissions.value));
const permTreeExpanded = ref<string[]>([]);
const permTreeTicked = ref<string[]>([]);
const permSearchKey = ref("");

/** 权限树未加载时的待处理操作 */
const pendingPermIds = ref<string[] | null>(null);

/** 将扁平权限列表转成树结构 */
function buildPermTree(perms: SysPermission[]): PermissionTreeNode[] {
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

/** 节点图标：模块/目录用 folder/folder_open（随展开状态切换），菜单用 nest_eco_leaf，按钮无图标 */
function permNodeIcon(node: PermissionTreeNode): string {
  if (node.type === "module" || node.type === "folder") {
    return permTreeExpanded.value.includes(node.id) ? "sym_r_folder_open" : "sym_r_folder";
  }
  if (node.type === "menu") return "sym_r_nest_eco_leaf";
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
    const result = await getPermissionListApi();
    if (result.code === 10_000 && result.data) {
      allPermissions.value = result.data;
      permTreeExpanded.value = permTreeNodes.value.map((n) => n.id);
      if (pendingPermIds.value) {
        permTreeTicked.value = pendingPermIds.value;
        form.permissionIds = [...pendingPermIds.value];
        pendingPermIds.value = null;
      }
    }
  } catch {
    // 静默失败
  } finally {
    permTreeLoading.value = false;
  }
}

/** 加载套餐已分配的权限 ID 列表 */
async function loadPackagePermissions(packageId: string) {
  try {
    const result = await getTenantPackagePermissionIdsApi(packageId);
    if (result.code === 10_000 && result.data) {
      if (allPermissions.value.length === 0) {
        pendingPermIds.value = result.data;
      } else {
        permTreeTicked.value = result.data;
        form.permissionIds = [...result.data];
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
  form.status = "enabled";
  form.userLimit = -1;
  form.apiLimit = -1;
  form.storageLimit = -1;
  form.expireDays = -1;
  form.sort = 100;
  form.remark = "";
  form.permissionIds = [];
  userLimitUnlimited.value = true;
  apiLimitUnlimited.value = true;
  storageLimitUnlimited.value = true;
  expireDaysUnlimited.value = true;
  permTreeTicked.value = [];
}

function initForm() {
  resetForm();
  if (props.pkg) {
    form.id = props.pkg.id;
    form.name = props.pkg.name;
    form.code = props.pkg.code ?? "";
    form.status = props.pkg.status || "enabled";
    form.userLimit = props.pkg.userLimit ?? -1;
    form.apiLimit = props.pkg.apiLimit ?? -1;
    form.storageLimit = props.pkg.storageLimit ?? -1;
    form.expireDays = props.pkg.expireDays ?? -1;
    form.sort = props.pkg.sort ?? 100;
    form.remark = props.pkg.remark || "";

    // 同步无限制开关状态
    userLimitUnlimited.value = form.userLimit === -1;
    apiLimitUnlimited.value = form.apiLimit === -1;
    storageLimitUnlimited.value = form.storageLimit === -1;
    expireDaysUnlimited.value = form.expireDays === -1;

    if (props.pkg.id) {
      loadPackagePermissions(props.pkg.id);
    }
  }
}

watch(() => props.pkg, initForm, { immediate: true });

onMounted(() => {
  loadPermTree();
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

  const data: Record<string, unknown> = {
    name: form.name,
    code: form.code,
    status: form.status,
    userLimit: form.userLimit,
    apiLimit: form.apiLimit,
    storageLimit: form.storageLimit,
    expireDays: form.expireDays,
    sort: form.sort,
    remark: form.remark || undefined,
    permissionIds: form.permissionIds.length ? form.permissionIds : undefined
  };

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createTenantPackageApi(data);
    } else {
      data.id = form.id;
      result = await updateTenantPackageApi(data);
    }

    if (result.code === 10_000) {
      showToast(t("tenantPackageMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("tenantPackageMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("tenantPackageMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}
</script>

<template>
  <div class="pkg-drawer-content">
    <q-form class="pkg-drawer-form" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 套餐名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.name"
            :label="t('tenantPackageMgmt.name')"
            filled
            square
            :rules="formRules.name"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 套餐编码 -->
        <div class="col-12 col-md-6">
          <q-input
            :model-value="form.code"
            @update:model-value="onCodeInput"
            :label="t('tenantPackageMgmt.code')"
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
                <q-tooltip>{{ t('tenantPackageMgmt.codeLocked') }}</q-tooltip>
              </q-icon>
            </template>
          </q-input>
        </div>
        <!-- 套餐状态 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.status"
            :label="t('tenantPackageMgmt.status')"
            filled
            square
            :options="statusOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :rules="formRules.status"
            :disable="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 排序 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.number="form.sort"
            :label="t('tenantPackageMgmt.sort')"
            filled
            square
            type="number"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
      </div>

      <!-- ── 限额配置 ── -->
      <div class="limit-section q-mt-md">
        <div class="limit-section-header row items-center no-wrap q-mb-sm">
          <q-icon name="sym_r_tune" size="20px" class="q-mr-xs" color="grey-8" />
          <span class="limit-section-title">{{ t('tenantPackageMgmt.limitConfig') }}</span>
        </div>
        <div class="row q-col-gutter-md">
          <!-- 用户数限制 -->
          <div class="col-12 col-md-6">
            <q-input
              v-model.number="form.userLimit"
              :label="t('tenantPackageMgmt.userLimit')"
              filled
              square
              type="number"
              :disable="drawerReadonly"
              :readonly="drawerReadonly || userLimitUnlimited"
              :rules="formRules.userLimit"
              hide-bottom-space
            >
              <template #append>
                <q-checkbox
                  :model-value="userLimitUnlimited"
                  @update:model-value="onUserLimitToggle"
                  :label="t('tenantPackageMgmt.unlimited')"
                  :disable="drawerReadonly"
                  dense
                />
              </template>
            </q-input>
          </div>
          <!-- API调用限制 -->
          <div class="col-12 col-md-6">
            <q-input
              v-model.number="form.apiLimit"
              :label="t('tenantPackageMgmt.apiLimit')"
              filled
              square
              type="number"
              :disable="drawerReadonly"
              :readonly="drawerReadonly || apiLimitUnlimited"
              :rules="formRules.apiLimit"
              hide-bottom-space
            >
              <template #append>
                <q-checkbox
                  :model-value="apiLimitUnlimited"
                  @update:model-value="onApiLimitToggle"
                  :label="t('tenantPackageMgmt.unlimited')"
                  :disable="drawerReadonly"
                  dense
                />
              </template>
            </q-input>
          </div>
          <!-- 存储限制 -->
          <div class="col-12 col-md-6">
            <q-input
              v-model.number="form.storageLimit"
              :label="t('tenantPackageMgmt.storageLimit')"
              filled
              square
              type="number"
              :disable="drawerReadonly"
              :readonly="drawerReadonly || storageLimitUnlimited"
              :rules="formRules.storageLimit"
              hide-bottom-space
            >
              <template #append>
                <q-checkbox
                  :model-value="storageLimitUnlimited"
                  @update:model-value="onStorageLimitToggle"
                  :label="t('tenantPackageMgmt.unlimited')"
                  :disable="drawerReadonly"
                  dense
                />
              </template>
            </q-input>
          </div>
          <!-- 有效期天数 -->
          <div class="col-12 col-md-6">
            <q-input
              v-model.number="form.expireDays"
              :label="t('tenantPackageMgmt.expireDays')"
              filled
              square
              type="number"
              :disable="drawerReadonly"
              :readonly="drawerReadonly || expireDaysUnlimited"
              :rules="formRules.expireDays"
              hide-bottom-space
            >
              <template #append>
                <q-checkbox
                  :model-value="expireDaysUnlimited"
                  @update:model-value="onExpireDaysToggle"
                  :label="t('tenantPackageMgmt.unlimited')"
                  :disable="drawerReadonly"
                  dense
                />
              </template>
            </q-input>
          </div>
        </div>
      </div>

      <!-- 备注 -->
      <div class="q-mt-md">
        <q-input
          v-model="form.remark"
          :label="t('tenantPackageMgmt.remark')"
          filled
          square
          type="textarea"
          rows="3"
          :disable="drawerReadonly"
          :readonly="drawerReadonly"
          hide-bottom-space
        />
      </div>

      <!-- ── 权限分配 ── -->
      <div class="perm-section q-mt-md">
        <div class="perm-section-header row items-center no-wrap q-mb-sm">
          <q-icon name="sym_r_security" size="20px" class="q-mr-xs" color="grey-8" />
          <span class="perm-section-title">{{ t('tenantPackageMgmt.permissionAssign') }}</span>
          <q-space />
          <span v-if="permTreeTicked.length" class="text-caption text-grey-7">
            {{ t('tenantPackageMgmt.selectedPermissions', { count: permTreeTicked.length }) }}
          </span>
        </div>
        <div class="perm-tree-container">
          <q-input
            v-model="permSearchKey"
            dense
            outlined
            square
            :placeholder="t('tenantPackageMgmt.searchPermission')"
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
      <div v-if="!drawerReadonly" class="pkg-drawer-footer row justify-end q-gutter-sm">
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
.pkg-drawer-content {
  padding: 0;
}

.pkg-drawer-footer {
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

/* 限额配置区域 */
.limit-section {
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 4px;
  padding: 12px;
}

.limit-section-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

/* 限额配置中"无限制"复选框标签字体大小 */
.limit-section :deep(.q-checkbox__label) {
  font-size: 16px;
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

/* 权限树复选框尺寸 */
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

/* 限额输入框 append 区域的 checkbox 不挤压 */
.limit-section :deep(.q-field__append) {
  white-space: nowrap;
}
</style>

<style>
.body--dark .pkg-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .pkg-drawer-form .q-field__native,
.body--dark .pkg-drawer-form .q-field__prefix,
.body--dark .pkg-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .pkg-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .pkg-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .pkg-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .pkg-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

/* 抽屉底部按钮区域分隔线 */
.body--dark .pkg-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}

/* 限额配置区域暗色模式 */
.body--dark .limit-section {
  border-color: rgba(255, 255, 255, 0.08);
}

.body--dark .limit-section-title {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .limit-section-header .q-icon {
  color: rgba(255, 255, 255, 0.72) !important;
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
