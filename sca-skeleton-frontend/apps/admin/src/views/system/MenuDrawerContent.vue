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
  parentId: [(v: string) => !!v || t("menuMgmt.parentIdRequired")],
  name: [(v: string) => !!v?.trim() || t("menuMgmt.nameRequired")],
  nameEn: [(v: string) => !!v?.trim() || t("menuMgmt.nameEnRequired")],
  type: [(v: string) => !!v || t("menuMgmt.typeRequired")],
  // 菜单和按钮类型要求权限标识必填（module/folder 类型可选）
  code: (form.type === "menu" || form.type === "button")
    ? [(v: string) => !!v?.trim() || t("menuMgmt.codeRequired")]
    : []
}));

const typeOptions = computed(() => [
  { label: t("menuMgmt.typeModule"), value: "module" },
  { label: t("menuMgmt.typeFolder"), value: "folder" },
  { label: t("menuMgmt.typeMenu"), value: "menu" },
  { label: t("menuMgmt.typeButton"), value: "button" }
]);

// ── 类型默认图标 ──
const TYPE_DEFAULT_ICON: Record<string, string> = {
  module: "",
  folder: "sym_r_folder",
  menu: "sym_r_nest_eco_leaf",
  button: ""
};

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
  { label: t("menuMgmt.statusEnabled"), value: "enabled" },
  { label: t("menuMgmt.statusDisabled"), value: "disabled" }
]);

const yesNoOptions = computed(() => [
  { label: t("common.yes"), value: 1 },
  { label: t("common.no"), value: 0 }
]);

// ── 上级菜单树（排除 button 类型） ──
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

/** 获取当前编辑菜单的所有子孙节点 ID（含自身），用于编辑时排除 */
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
  // 从完整树中找到当前菜单节点并收集其子孙
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
    label: t("menuMgmt.allMenus"),
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
    name: form.name?.trim(),
    nameEn: form.nameEn?.trim() || undefined,
    type: form.type,
    code: form.code?.trim() || undefined,
    path: form.path?.trim() || undefined,
    component: form.component?.trim() || undefined,
    icon: form.type === "button" ? undefined : (form.icon?.trim() || undefined),
    sort: form.sort,
    isVisible: form.isVisible,
    isExternal: form.isExternal,
    status: form.status,
    remark: form.remark?.trim() || undefined
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
      showToast(t("menuMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("menuMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("menuMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}
</script>

<template>
  <div class="menu-drawer-content">
    <q-form class="menu-drawer-form" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 上级菜单 -->
        <div class="col-12">
          <q-select
            v-model="form.parentId"
            :label="t('menuMgmt.parentId')"
            filled
            square
            emit-value
            :display-value="menuDisplayLabel"
            :rules="formRules.parentId"
            lazy-rules
            :disable="drawerReadonly"
            hide-bottom-space
            dropdown-icon="sym_r_arrow_drop_down"
            :class="{ 'menu-select--menu-open': menuMenuOpen }"
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
                  :placeholder="t('menuMgmt.searchMenu')"
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
                        class="menu-tree-option row items-center no-wrap full-width"
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
        <!-- 菜单名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.name"
            :label="t('menuMgmt.name')"
            filled
            square
            :rules="formRules.name"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 英文菜单名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.nameEn"
            :label="t('menuMgmt.nameEn')"
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
        <!-- 权限类型 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.type"
            :label="t('menuMgmt.type')"
            filled
            square
            :options="typeOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :rules="formRules.type"
            :disable="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 权限标识（menu 和 button 类型可填且必填，module/folder 类型禁用） -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.code"
            :label="t('menuMgmt.code')"
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
            :label="t('menuMgmt.icon')"
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
            :label="t('menuMgmt.path')"
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
            :label="t('menuMgmt.component')"
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
            :label="t('menuMgmt.sort')"
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
            :label="t('menuMgmt.status')"
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
            :label="t('menuMgmt.isVisible')"
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
            :label="t('menuMgmt.isExternal')"
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
            :label="t('menuMgmt.remark')"
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
      <div v-if="!drawerReadonly" class="menu-drawer-footer row justify-end q-gutter-sm">
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
.menu-drawer-content {
  padding: 0;
}

.menu-drawer-footer {
  flex-shrink: 0;
  padding: 12px 0 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  margin-top: 16px;
}

.drawer-action-btn {
  min-width: 72px;
}

.menu-tree-option {
  min-height: 32px;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.menu-tree-option:hover {
  background: rgba(0, 0, 0, 0.04);
}

.required-field :deep(.q-field__label::after) {
  content: " *";
  color: var(--q-negative);
}

.menu-select--menu-open :deep(.q-field__append > .q-icon:not(.text-negative)) {
  transform: rotate(180deg);
}

:deep(.q-field__append > .q-icon:not(.text-negative)) {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
</style>

<style>
.body--dark .menu-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .menu-drawer-form .q-field__native,
.body--dark .menu-drawer-form .q-field__prefix,
.body--dark .menu-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .menu-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .menu-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .menu-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .menu-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

/* 抽屉底部按钮区域分隔线 */
.body--dark .menu-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}

/* 上级菜单树下拉选项 */
.body--dark .menu-tree-option:hover {
  background: rgba(255, 255, 255, 0.06);
}
</style>
