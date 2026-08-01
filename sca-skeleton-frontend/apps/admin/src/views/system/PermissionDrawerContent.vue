<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysPermission, PermissionTreeNode } from "../../types/auth";
import { createPermissionApi, updatePermissionApi, getPermissionListApi } from "../../apis/permission";

const { t, locale } = useI18n({ useScope: "global" });

const props = defineProps<{
  mode: "add" | "edit" | "view";
  permission?: SysPermission;
  /** 新增时预设的父节点 ID */
  defaultParentId?: string;
  /** 新增时预设的权限域（跟随权限树当前选中节点） */
  defaultRealm?: string;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");

const formLoading = ref(false);
const form = reactive({
  id: "",
  parentId: "",
  name: "",
  nameEn: "",
  type: "",
  code: "",
  realm: "",
  path: "",
  component: "",
  icon: "",
  sort: 100,
  isVisible: 1,
  isExternal: 0,
  status: "enabled",
  remark: ""
});

const formRules = computed(() => ({
  parentId: [(v: string) => !!v || t("permissionMgmt.parentIdRequired")],
  name: [(v: string) => !!v?.trim() || t("permissionMgmt.nameRequired")],
  nameEn: [(v: string) => !!v?.trim() || t("permissionMgmt.nameEnRequired")],
  type: [(v: string) => !!v || t("permissionMgmt.typeRequired")],
  realm: [(v: string) => !!v || t("permissionMgmt.realmRequired")],
  // 菜单和按钮类型要求权限编码必填（module/folder 类型可选）
  code: (form.type === "menu" || form.type === "button")
    ? [(v: string) => !!v?.trim() || t("permissionMgmt.codeRequired")]
    : []
}));

// ── 权限类型：根据上级权限类型动态过滤可选项 ──
// 规则（与后端 ALLOWED_CHILD_TYPES 一致）：
//   根节点（parentId="0"）→ module / folder / menu
//   module → folder / menu
//   folder → menu / button
//   menu   → button
//   button → 不能添加子权限
const ALLOWED_CHILD_TYPES: Record<string, string[]> = {
  "0": ["module", "folder", "menu"],
  module: ["folder", "menu"],
  folder: ["menu", "button"],
  menu: ["button"],
  button: []
};

/** 获取上级权限的类型（根节点返回 "0"） */
const parentType = computed(() => {
  if (!form.parentId || form.parentId === "0") return "0";
  return allPermissions.value.find((p) => p.id === form.parentId)?.type ?? "0";
});

/** 根据上级权限类型动态过滤可选的权限类型 */
const typeOptions = computed(() => {
  const allTypes = [
    { label: t("permissionMgmt.typeModule"), value: "module" },
    { label: t("permissionMgmt.typeFolder"), value: "folder" },
    { label: t("permissionMgmt.typeMenu"), value: "menu" },
    { label: t("permissionMgmt.typeButton"), value: "button" }
  ];
  const allowed = ALLOWED_CHILD_TYPES[parentType.value] ?? [];
  return allTypes.filter((opt) => allowed.includes(opt.value));
});

const realmOptions = computed(() => [
  { label: t("permissionMgmt.realmAdmin"), value: "admin" },
  { label: t("permissionMgmt.realmPortal"), value: "portal" }
]);

// ── 类型默认图标 ──
const TYPE_DEFAULT_ICON: Record<string, string> = {
  module: "",
  folder: "sym_r_folder",
  menu: "sym_r_nest_eco_leaf",
  button: ""
};

/** 切换上级权限后，若当前类型不在新允许列表中则清空，避免提交不合法数据 */
watch(parentType, () => {
  if (drawerReadonly.value) return;
  const allowed = ALLOWED_CHILD_TYPES[parentType.value] ?? [];
  if (form.type && !allowed.includes(form.type)) {
    form.type = "";
  }
});

/** 切换类型时自动填充默认图标（仅新增/编辑模式，图标为空或等于上一类型默认值时触发） */
watch(() => form.type, (newType, oldType) => {
  if (drawerReadonly.value || !newType) return;
  const oldDefault = oldType ? TYPE_DEFAULT_ICON[oldType] : "";
  // button 类型清空图标
  if (newType === "button") {
    form.icon = "";
    return;
  }
  if (!form.icon || form.icon === oldDefault) {
    form.icon = TYPE_DEFAULT_ICON[newType] || "";
  }
});

const statusOptions = computed(() => [
  { label: t("permissionMgmt.statusEnabled"), value: "enabled" },
  { label: t("permissionMgmt.statusDisabled"), value: "disabled" }
]);

const yesNoOptions = computed(() => [
  { label: t("common.yes"), value: 1 },
  { label: t("common.no"), value: 0 }
]);

// ── 上级权限树（排除 button 类型） ──
const allPermissions = ref<SysPermission[]>([]);
const menuTreeNodes = computed(() => buildMenuTree(allPermissions.value));
const menuTreeExpanded = ref<string[]>([]);
const menuSearchKey = ref("");
const menuMenuRef = ref();
const menuMenuOpen = ref(false);

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
      } else {
        delete n.children;
      }
    }
  };
  cleanEmpty(roots);

  return roots;
}

/** 获取当前编辑权限的所有子孙节点 ID（含自身），用于编辑时排除 */
function getSelfAndDescendantIds(): Set<string> {
  const ids = new Set<string>();
  if (props.mode !== "edit" || !props.permission?.id) return ids;
  ids.add(props.permission.id);
  const collect = (nodes: PermissionTreeNode[]) => {
    for (const n of nodes) {
      ids.add(n.id);
      if (n.children?.length) collect(n.children);
    }
  };
  // 从完整树中找到当前权限节点并收集其子孙
  const findAndCollect = (nodes: PermissionTreeNode[]): boolean => {
    for (const n of nodes) {
      if (n.id === props.permission!.id) {
        collect(n.children || []);
        return true;
      }
      if (n.children?.length && findAndCollect(n.children)) return true;
    }
    return false;
  };
  findAndCollect(menuTreeNodes.value);
  return ids;
}

/** 过滤掉自身及子孙节点后的树（编辑模式使用） */
function filterExcludedNodes(nodes: PermissionTreeNode[], excludeIds: Set<string>): PermissionTreeNode[] {
  if (excludeIds.size === 0) return nodes;
  return nodes
    .filter(n => !excludeIds.has(n.id))
    .map(n => ({
      ...n,
      children: n.children?.length ? filterExcludedNodes(n.children, excludeIds) : undefined
    }));
}

/** 带根节点的树（q-tree 渲染用），编辑模式下排除自身及子孙 */
const menuTreeWithRoot = computed(() => {
  const excludeIds = getSelfAndDescendantIds();
  const filteredChildren = excludeIds.size > 0
    ? filterExcludedNodes(menuTreeNodes.value, excludeIds)
    : menuTreeNodes.value;
  return [{
    id: "0",
    label: t("permissionMgmt.allItems"),
    parentId: "",
    type: "root",
    icon: "",
    children: filteredChildren
  }] as PermissionTreeNode[];
});

/** 过滤树节点（按关键字） */
function filterMenuTree(nodes: PermissionTreeNode[], keyword: string): PermissionTreeNode[] {
  if (!keyword?.trim()) return nodes;
  const lower = keyword.toLowerCase();
  const result: PermissionTreeNode[] = [];
  for (const n of nodes) {
    const childResult = n.children?.length ? filterMenuTree(n.children, keyword) : [];
    if (n.label.toLowerCase().includes(lower) || childResult.length) {
      result.push({ ...n, children: childResult.length ? childResult : n.children?.length ? [] : undefined });
    }
  }
  return result;
}

const filteredMenuTreeNodes = computed(() => filterMenuTree(menuTreeWithRoot.value, menuSearchKey.value));

/** 搜索时自动展开所有节点 */
watch(menuSearchKey, (val) => {
  if (val?.trim()) {
    const allKeys: string[] = [];
    const collectKeys = (nodes: PermissionTreeNode[]) => {
      for (const n of nodes) {
        allKeys.push(n.id);
        if (n.children?.length) collectKeys(n.children);
      }
    };
    collectKeys(filteredMenuTreeNodes.value);
    menuTreeExpanded.value = allKeys;
  }
});

/** 查找节点标签 */
function findMenuLabel(nodes: PermissionTreeNode[], id: string): string {
  for (const n of nodes) {
    if (n.id === id) return n.label;
    if (n.children?.length) {
      const found = findMenuLabel(n.children, id);
      if (found) return found;
    }
  }
  return "";
}

const menuDisplayLabel = computed(() => {
  if (!form.parentId) return "";
  return findMenuLabel(menuTreeWithRoot.value, form.parentId);
});

/** 节点图标：root/module/folder 用 folder/folder_open（随展开状态切换），menu 用 nest_eco_leaf */
function menuNodeIcon(node: PermissionTreeNode): string {
  if (node.type === "root" || node.type === "module" || node.type === "folder") {
    return menuTreeExpanded.value.includes(node.id) ? "sym_r_folder_open" : "sym_r_folder";
  }
  if (node.type === "menu") return "sym_r_nest_eco_leaf";
  return "sym_r_article";
}

/** 点击树节点选中 */
function onMenuTreeNodeClick(node: PermissionTreeNode) {
  form.parentId = node.id;
  menuSearchKey.value = "";
  menuMenuRef.value?.hide();
}

async function loadMenuTree() {
  try {
    const result = await getPermissionListApi();
    if (result.code === 10_000 && result.data) {
      allPermissions.value = result.data;
      // 默认展开「全部」根节点
      menuTreeExpanded.value = ["0"];
    }
  } catch {
    // 静默失败
  }
}

function resetForm() {
  form.id = "";
  form.parentId = props.defaultParentId || "0";
  form.name = "";
  form.nameEn = "";
  form.type = "";
  form.code = "";
  form.realm = props.defaultRealm || "";
  form.path = "";
  form.component = "";
  form.icon = "";
  form.sort = 100;
  form.isVisible = 1;
  form.isExternal = 0;
  form.status = "enabled";
  form.remark = "";
}

function initForm() {
  resetForm();
  if (props.permission) {
    form.id = props.permission.id;
    form.parentId = props.permission.parentId;
    form.name = props.permission.name;
    form.nameEn = props.permission.nameEn || "";
    form.type = props.permission.type;
    form.code = props.permission.code || "";
    form.realm = props.permission.realm || "";
    form.path = props.permission.path || "";
    form.component = props.permission.component || "";
    form.icon = props.permission.icon || "";
    form.sort = props.permission.sort ?? 100;
    form.isVisible = props.permission.isVisible ?? 1;
    form.isExternal = props.permission.isExternal ?? 0;
    form.status = props.permission.status || "enabled";
    form.remark = props.permission.remark || "";
  }
}

watch(() => props.permission, initForm, { immediate: true });
watch(() => props.defaultParentId, () => {
  if (props.mode === "add" && !props.permission) {
    form.parentId = props.defaultParentId || "0";
  }
});
watch(() => props.defaultRealm, () => {
  if (props.mode === "add" && !props.permission) {
    form.realm = props.defaultRealm || "";
  }
});

onMounted(() => {
  loadMenuTree();
});

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  const data: Record<string, unknown> = {
    parentId: form.parentId,
    name: form.name,
    nameEn: form.nameEn || undefined,
    type: form.type,
    code: form.code || undefined,
    realm: form.realm || undefined,
    path: form.path || undefined,
    component: form.component || undefined,
    icon: form.type === "button" ? undefined : (form.icon || undefined),
    sort: form.sort,
    isVisible: form.isVisible,
    isExternal: form.isExternal,
    status: form.status,
    remark: form.remark || undefined
  };

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createPermissionApi(data);
    } else {
      data.id = form.id;
      data.version = props.permission?.version;
      result = await updatePermissionApi(data);
    }

    if (result.code === 10_000) {
      showToast(t("permissionMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("permissionMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("permissionMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}
</script>

<template>
  <div class="permission-drawer-content">
    <q-form class="permission-drawer-form" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 上级权限 -->
        <div class="col-12">
          <q-select
            v-model="form.parentId"
            :label="t('permissionMgmt.parentId')"
            filled
            square
            emit-value
            :display-value="menuDisplayLabel"
            :rules="formRules.parentId"
            lazy-rules
            :disable="drawerReadonly"
            hide-bottom-space
            dropdown-icon="sym_r_arrow_drop_down"
            :class="{ 'permission-select--menu-open': menuMenuOpen }"
            class="required-field"
          >
            <q-menu
              ref="menuMenuRef"
              anchor="bottom left"
              self="top left"
              :offset="[0, 0]"
              no-focus
              no-route-update
              fit
              @before-show="menuMenuOpen = true"
              @before-hide="menuMenuOpen = false"
            >
              <div class="q-pa-sm">
                <q-input
                  v-model="menuSearchKey"
                  dense
                  outlined
                  square
                  :placeholder="t('permissionMgmt.searchPermission')"
                  clearable
                  class="q-mb-sm"
                >
                  <template #prepend>
                    <q-icon name="sym_r_search" size="18px" />
                  </template>
                </q-input>
                <q-scroll-area style="height: 300px">
                  <q-tree
                    :nodes="filteredMenuTreeNodes"
                    node-key="id"
                    label-key="label"
                    children-key="children"
                    v-model:expanded="menuTreeExpanded"
                    no-connectors
                    dense
                    no-nodes-label=" "
                  >
                    <template #default-header="scope">
                      <div
                        class="permission-tree-option row items-center no-wrap full-width"
                        @click.stop="onMenuTreeNodeClick(scope.node)"
                      >
                        <q-icon
                          :name="menuNodeIcon(scope.node)"
                          size="18px"
                          class="q-mr-sm"
                          :color="form.parentId === scope.node.id ? 'primary' : 'grey-7'"
                        />
                        <span class="ellipsis" :class="{ 'text-primary text-weight-medium': form.parentId === scope.node.id }">
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
        <!-- 权限名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.name"
            :label="t('permissionMgmt.name')"
            filled
            square
            :rules="formRules.name"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 英文权限名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.nameEn"
            :label="t('permissionMgmt.nameEn')"
            filled
            square
            :rules="formRules.nameEn"
            lazy-rules
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 权限类型（创建后不可修改，编辑/查看模式禁用） -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.type"
            :label="t('permissionMgmt.type')"
            filled
            square
            :options="typeOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :rules="formRules.type"
            :disable="drawerReadonly || mode !== 'add'"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 权限域（创建后不可修改，编辑/查看模式禁用） -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.realm"
            :label="t('permissionMgmt.realm')"
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
        <!-- 权限编码（menu 和 button 类型可填且必填，module/folder 类型禁用） -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.code"
            :label="t('permissionMgmt.code')"
            filled
            square
            :disable="drawerReadonly || (form.type !== 'menu' && form.type !== 'button')"
            :readonly="drawerReadonly"
            :rules="formRules.code"
            lazy-rules
            hide-bottom-space
            :class="{ 'required-field': form.type === 'menu' || form.type === 'button' }"
          />
        </div>
        <!-- 图标（button 类型禁用，硬编码） -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.icon"
            :label="t('permissionMgmt.icon')"
            filled
            square
            :disable="drawerReadonly || form.type === 'button'"
            :readonly="drawerReadonly"
            hide-bottom-space
          >
            <template v-if="form.icon" #prepend>
              <q-icon :name="form.icon" size="20px" />
            </template>
          </q-input>
        </div>
        <!-- 路由地址 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.path"
            :label="t('permissionMgmt.path')"
            filled
            square
            :disable="drawerReadonly || form.type !== 'menu'"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 组件路径（仅 menu 类型可用） -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.component"
            :label="t('permissionMgmt.component')"
            filled
            square
            :disable="drawerReadonly || form.type !== 'menu'"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 排序 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.number="form.sort"
            :label="t('permissionMgmt.sort')"
            filled
            square
            type="number"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 状态 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.status"
            :label="t('permissionMgmt.status')"
            filled
            square
            :options="statusOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 是否可见 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.isVisible"
            :label="t('permissionMgmt.isVisible')"
            filled
            square
            :options="yesNoOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :disable="drawerReadonly || form.type === 'button'"
            hide-bottom-space
          />
        </div>
        <!-- 是否外链（仅 menu 类型可用） -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.isExternal"
            :label="t('permissionMgmt.isExternal')"
            filled
            square
            :options="yesNoOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :disable="drawerReadonly || form.type !== 'menu'"
            hide-bottom-space
          />
        </div>
        <!-- 备注 -->
        <div class="col-12">
          <q-input
            v-model="form.remark"
            :label="t('permissionMgmt.remark')"
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

      <!-- 底部操作按钮 -->
      <div v-if="!drawerReadonly" class="permission-drawer-footer row justify-end q-gutter-sm">
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
.permission-drawer-content {
  padding: 0;
}

.permission-drawer-footer {
  flex-shrink: 0;
  padding: 12px 0 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  margin-top: 16px;
}

.drawer-action-btn {
  min-width: 72px;
}

.permission-tree-option {
  min-height: 32px;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.permission-tree-option:hover {
  background: rgba(0, 0, 0, 0.04);
}

.required-field :deep(.q-field__label::after) {
  content: " *";
  color: var(--q-negative);
}

.permission-select--menu-open :deep(.q-field__append > .q-icon:not(.text-negative)) {
  transform: rotate(180deg);
}

:deep(.q-field__append > .q-icon:not(.text-negative)) {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
</style>

<style>
.body--dark .permission-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .permission-drawer-form .q-field__native,
.body--dark .permission-drawer-form .q-field__prefix,
.body--dark .permission-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .permission-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .permission-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .permission-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .permission-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

/* 抽屉底部按钮区域分隔线 */
.body--dark .permission-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}

/* 上级权限树下拉选项 */
.body--dark .permission-tree-option:hover {
  background: rgba(255, 255, 255, 0.06);
}
</style>
