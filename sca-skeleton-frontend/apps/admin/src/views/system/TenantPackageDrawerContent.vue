<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysTenantPackage, PermissionAssignOption, PermissionTreeNode } from "../../types/auth";
import { createTenantPackageApi, updateTenantPackageApi } from "../../apis/tenant-package";
import { getTenantPackagePermissionIdsApi } from "../../apis/tenant-package";
import { getTenantPackageAssignOptionsApi } from "../../apis/tenant-package";

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
  userLimit: null as number | null,
  apiLimit: null as number | null,
  storageLimit: null as number | null,
  expireDays: null as number | null,
  sort: 100,
  remark: "",
  permissionIds: [] as string[]
});

// ── 限额无限制开关：true 时对应字段为 -1（不限），false 时可输入具体值 ──
const userLimitUnlimited = ref(false);
const apiLimitUnlimited = ref(false);
const storageLimitUnlimited = ref(false);
const expireDaysUnlimited = ref(false);

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
const allPermissions = ref<PermissionAssignOption[]>([]);

/** 分类节点 id 前缀（避免与真实权限 ID 冲突） */
const REALM_GROUP_PREFIX = "realm:";
/** 权限域分组定义（顺序即树展示顺序） */
const REALM_GROUPS: { realm: string; labelKey: string }[] = [
  { realm: "admin", labelKey: "permissionMgmt.adminGroup" },
  { realm: "portal", labelKey: "permissionMgmt.portalGroup" }
];
const realmGroupId = (realm: string) => REALM_GROUP_PREFIX + realm;

const permTreeNodes = computed(() => buildPermTree(allPermissions.value));
const permTreeExpanded = ref<string[]>([]);
const permTreeTicked = ref<string[]>([]);
const permSearchKey = ref("");

/** 权限树未加载时的待处理操作 */
const pendingPermIds = ref<string[] | null>(null);

/**
 * 将扁平权限列表转成树结构，并按权限域（admin/portal）分组成两棵子树。
 *
 * 后端已做过滤（仅返回启用且可见的权限，且非超管仅返回自身拥有的权限），
 * 前端直接信任后端数据，不再做任何过滤，避免「掩耳盗铃」式掩盖后端问题。
 */
function buildPermTree(perms: PermissionAssignOption[]): PermissionTreeNode[] {
  if (!perms.length) return [];

  const sorted = [...perms].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));

  // 先按 realm 分组，再各自建树
  const isEn = locale.value.startsWith("en");
  const groups: PermissionTreeNode[] = [];

  for (const group of REALM_GROUPS) {
    const groupPerms = sorted.filter((p) => p.realm === group.realm);
    if (!groupPerms.length) continue;

    const map = new Map<string, PermissionTreeNode>();
    for (const p of groupPerms) {
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
    for (const p of groupPerms) {
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

    if (roots.length) {
      groups.push({
        id: realmGroupId(group.realm),
        label: t(group.labelKey),
        parentId: "",
        type: "realm-group",
        icon: "",
        realm: group.realm,
        children: roots
      });
    }
  }

  return groups;
}

/** 收集权限树中所有叶子节点的 ID（leaf-filtered 模式要求 ticked 数组仅含叶子节点） */
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

/** 节点图标：分类节点（realm 组）/模块/目录统一用 folder/folder_open（随展开状态切换），菜单用 nest_eco_leaf，按钮无图标 */
function permNodeIcon(node: PermissionTreeNode): string {
  if (node.type === "realm-group" || node.type === "module" || node.type === "folder") {
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
    const result = await getTenantPackageAssignOptionsApi();
    if (result.code === 10_000 && result.data) {
      allPermissions.value = result.data;
      permTreeExpanded.value = permTreeNodes.value.map((n) => n.id);
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

/** 加载套餐已分配的权限 ID 列表 */
async function loadPackagePermissions(packageId: string) {
  try {
    const result = await getTenantPackagePermissionIdsApi(packageId);
    if (result.code === 10_000 && result.data) {
      if (allPermissions.value.length === 0) {
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
  form.status = "enabled";
  form.userLimit = null;
  form.apiLimit = null;
  form.storageLimit = null;
  form.expireDays = null;
  form.sort = 100;
  form.remark = "";
  form.permissionIds = [];
  userLimitUnlimited.value = false;
  apiLimitUnlimited.value = false;
  storageLimitUnlimited.value = false;
  expireDaysUnlimited.value = false;
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

  // 收集被勾选叶子节点的所有祖先 ID，确保保存完整权限链（module/folder/menu + button），
  // 避免 leaf-filtered 策略导致只保存 button 而菜单树断裂
  const fullPermissionIds = collectWithAncestors(form.permissionIds);

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
    permissionIds: fullPermissionIds.length ? fullPermissionIds : undefined
  };

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createTenantPackageApi(data);
    } else {
      data.id = form.id;
      data.version = props.pkg?.version;
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
    <!-- 中间内容区（唯一滚动区）：表单字段 -->
    <q-form id="pkg-drawer-form" class="pkg-drawer-form pkg-drawer-main" @submit="handleSave">
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
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
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
                  />
                  <span class="ellipsis">{{ scope.node.label }}</span>
                </div>
              </template>
            </q-tree>
          </q-scroll-area>
        </div>
      </div>
    </q-form>

    <!-- 底部操作栏：固定第三段，不随内容滚动；按钮直接挂在页脚容器上，间距由 CSS gap 控制 -->
    <div v-if="!drawerReadonly" class="pkg-drawer-footer row items-center justify-end no-wrap">
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
        form="pkg-drawer-form"
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
/* 三段式抽屉布局：外层 .pkg-drawer-body 不再滚动，
   中间内容区（q-form.pkg-drawer-main）独立滚动，底部操作栏固定为第三段 */
.pkg-drawer-content {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* 中间内容区：唯一的滚动区域 */
.pkg-drawer-main {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 16px 12px;
}

/* 底部操作栏：固定第三段，不参与内容滚动（按钮直接子元素，gap 控制间距） */
.pkg-drawer-footer {
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

/* 限额配置中"无限制"复选框尺寸与列表页保持一致 */
.limit-section :deep(.q-checkbox__inner) {
  font-size: 32px;
}

.limit-section :deep(.q-checkbox__label) {
  font-size: 14px;
}

/* 限额配置中勾选"无限制"后文本框保持实线底边框（覆盖 Quasar readonly 默认虚线） */
.limit-section :deep(.q-field--filled.q-field--readonly .q-field__control:before) {
  border-bottom-style: none;
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
