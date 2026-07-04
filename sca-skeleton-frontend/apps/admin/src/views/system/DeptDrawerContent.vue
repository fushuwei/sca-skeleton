<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysDept, DeptTreeNode } from "../../types/auth";
import { createDeptApi, updateDeptApi, getDeptListApi } from "../../apis/dept";

const { t } = useI18n({ useScope: "global" });

const props = defineProps<{
  mode: "add" | "edit" | "view";
  dept?: SysDept;
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
  code: "",
  leader: "",
  phone: "",
  email: "",
  sort: 100,
  status: "enabled"
});

const formRules = computed(() => ({
  parentId: [(v: string) => !!v || t("deptMgmt.parentIdRequired")],
  name: [(v: string) => !!v?.trim() || t("deptMgmt.nameRequired")],
  code: [(v: string) => !!v?.trim() || t("deptMgmt.codeRequired")]
}));

const statusOptions = computed(() => [
  { label: t("deptMgmt.statusEnabled"), value: "enabled" },
  { label: t("deptMgmt.statusDisabled"), value: "disabled" }
]);

// ── 上级部门树 ──
const deptTreeNodes = ref<DeptTreeNode[]>([]);
const deptTreeExpanded = ref<string[]>([]);
const deptSearchKey = ref("");
const deptMenuRef = ref();
const deptMenuOpen = ref(false);

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
      } else {
        delete n.children;
      }
    }
  };
  cleanEmpty(roots);

  return roots;
}

/** 获取当前编辑部门的所有子孙节点 ID（含自身），用于编辑时排除 */
function getSelfAndDescendantIds(): Set<string> {
  const ids = new Set<string>();
  if (props.mode !== "edit" || !props.dept?.id) return ids;
  ids.add(props.dept.id);
  const collect = (nodes: DeptTreeNode[]) => {
    for (const n of nodes) {
      ids.add(n.id);
      if (n.children?.length) collect(n.children);
    }
  };
  // 从完整树中找到当前部门节点并收集其子孙
  const findAndCollect = (nodes: DeptTreeNode[]): boolean => {
    for (const n of nodes) {
      if (n.id === props.dept!.id) {
        collect(n.children || []);
        return true;
      }
      if (n.children?.length && findAndCollect(n.children)) return true;
    }
    return false;
  };
  findAndCollect(deptTreeNodes.value);
  return ids;
}

/** 过滤掉自身及子孙节点后的树（编辑模式使用） */
function filterExcludedNodes(nodes: DeptTreeNode[], excludeIds: Set<string>): DeptTreeNode[] {
  if (excludeIds.size === 0) return nodes;
  return nodes
    .filter(n => !excludeIds.has(n.id))
    .map(n => ({
      ...n,
      children: n.children?.length ? filterExcludedNodes(n.children, excludeIds) : undefined
    }));
}

/** 带根节点的树（q-tree 渲染用），编辑模式下排除自身及子孙 */
const deptTreeWithRoot = computed(() => {
  const excludeIds = getSelfAndDescendantIds();
  const filteredChildren = excludeIds.size > 0
    ? filterExcludedNodes(deptTreeNodes.value, excludeIds)
    : deptTreeNodes.value;
  return [{
    id: "0",
    label: t("deptMgmt.allDepts"),
    parentId: "",
    children: filteredChildren
  }] as DeptTreeNode[];
});

/** 过滤树节点（按关键字） */
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

const filteredDeptTreeNodes = computed(() => filterDeptTree(deptTreeWithRoot.value, deptSearchKey.value));

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

const deptDisplayLabel = computed(() => {
  if (!form.parentId) return "";
  return findDeptLabel(deptTreeWithRoot.value, form.parentId);
});

/** 节点图标 */
function deptNodeIcon(node: DeptTreeNode): string {
  return deptTreeExpanded.value.includes(node.id) ? "sym_r_folder_open" : "sym_r_folder";
}

/** 点击树节点选中 */
function onDeptTreeNodeClick(node: DeptTreeNode) {
  form.parentId = node.id;
  deptSearchKey.value = "";
  deptMenuRef.value?.hide();
}

async function loadDeptTree() {
  try {
    const result = await getDeptListApi();
    if (result.code === 10_000 && result.data) {
      deptTreeNodes.value = buildDeptTree(result.data);
      // 默认展开「全部」根节点
      deptTreeExpanded.value = ["0"];
    }
  } catch {
    // 静默失败
  }
}

function resetForm() {
  form.id = "";
  form.parentId = props.defaultParentId || "0";
  form.name = "";
  form.code = "";
  form.leader = "";
  form.phone = "";
  form.email = "";
  form.sort = 100;
  form.status = "enabled";
}

function initForm() {
  resetForm();
  if (props.dept) {
    form.id = props.dept.id;
    form.parentId = props.dept.parentId;
    form.name = props.dept.name;
    form.code = props.dept.code || "";
    form.leader = props.dept.leader || "";
    form.phone = props.dept.phone || "";
    form.email = props.dept.email || "";
    form.sort = props.dept.sort ?? 100;
    form.status = props.dept.status || "enabled";
  }
}

watch(() => props.dept, initForm, { immediate: true });
watch(() => props.defaultParentId, () => {
  if (props.mode === "add" && !props.dept) {
    form.parentId = props.defaultParentId || "0";
  }
});

// 部门编码自动转大写
watch(() => form.code, (val) => {
  if (val && val !== val.toUpperCase()) {
    form.code = val.toUpperCase();
  }
});

onMounted(() => {
  loadDeptTree();
});

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  const data: Record<string, unknown> = {
    name: form.name,
    code: form.code,
    leader: form.leader || undefined,
    phone: form.phone || undefined,
    email: form.email || undefined,
    sort: form.sort,
    status: form.status
  };

  // 添加模式传 parentId，编辑模式也传 parentId（支持修改上级部门）
  if (props.mode === "add" || (props.mode === "edit" && form.parentId !== props.dept?.parentId)) {
    data.parentId = form.parentId;
  }

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createDeptApi(data);
    } else {
      data.id = form.id;
      result = await updateDeptApi(data);
    }

    if (result.code === 10_000) {
      showToast(t("deptMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("deptMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("deptMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}
</script>

<template>
  <div class="dept-drawer-content">
    <q-form class="dept-drawer-form" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 上级部门 -->
        <div class="col-12">
          <q-select
            v-model="form.parentId"
            :label="t('deptMgmt.parentId')"
            filled
            square
            emit-value
            :display-value="deptDisplayLabel"
            :rules="formRules.parentId"
            lazy-rules
            :disable="drawerReadonly"
            hide-bottom-space
            dropdown-icon="sym_r_arrow_drop_down"
            :class="{ 'dept-select--menu-open': deptMenuOpen }"
            class="required-field"
          >
            <q-menu
              ref="deptMenuRef"
              anchor="bottom left"
              self="top left"
              :offset="[0, 0]"
              no-focus
              no-route-update
              fit
              @before-show="deptMenuOpen = true"
              @before-hide="deptMenuOpen = false"
            >
              <div class="q-pa-sm">
                <q-input
                  v-model="deptSearchKey"
                  dense
                  outlined
                  square
                  :placeholder="t('deptMgmt.searchDept')"
                  clearable
                  class="q-mb-sm"
                >
                  <template #prepend>
                    <q-icon name="sym_r_search" size="18px" />
                  </template>
                </q-input>
                <q-scroll-area style="height: 300px">
                  <q-tree
                    :nodes="filteredDeptTreeNodes"
                    node-key="id"
                    label-key="label"
                    children-key="children"
                    v-model:expanded="deptTreeExpanded"
                    no-connectors
                    dense
                    no-nodes-label=" "
                  >
                    <template #default-header="scope">
                      <div
                        class="dept-tree-option row items-center no-wrap full-width"
                        @click.stop="onDeptTreeNodeClick(scope.node)"
                      >
                        <q-icon
                          :name="deptNodeIcon(scope.node)"
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
        <!-- 部门名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.name"
            :label="t('deptMgmt.name')"
            filled
            square
            :rules="formRules.name"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 部门编码 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.code"
            :label="t('deptMgmt.code')"
            filled
            square
            :rules="formRules.code"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 负责人 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.leader"
            :label="t('deptMgmt.leader')"
            filled
            square
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 联系电话 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.phone"
            :label="t('deptMgmt.phone')"
            filled
            square
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 邮箱 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.email"
            :label="t('deptMgmt.email')"
            filled
            square
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 排序 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.number="form.sort"
            :label="t('deptMgmt.sort')"
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
            :label="t('deptMgmt.status')"
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
      </div>

      <!-- 底部操作按钮 -->
      <div v-if="!drawerReadonly" class="dept-drawer-footer row justify-end q-gutter-sm">
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
.dept-drawer-content {
  padding: 0;
}

.dept-drawer-footer {
  flex-shrink: 0;
  padding: 12px 0 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  margin-top: 16px;
}

.drawer-action-btn {
  min-width: 72px;
}

.dept-tree-option {
  min-height: 32px;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.dept-tree-option:hover {
  background: rgba(0, 0, 0, 0.04);
}

.required-field :deep(.q-field__label::after) {
  content: " *";
  color: var(--q-negative);
}

.dept-select--menu-open :deep(.q-field__append > .q-icon:not(.text-negative)) {
  transform: rotate(180deg);
}

:deep(.q-field__append > .q-icon:not(.text-negative)) {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
</style>

<style>
.body--dark .dept-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .dept-drawer-form .q-field__native,
.body--dark .dept-drawer-form .q-field__prefix,
.body--dark .dept-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .dept-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .dept-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .dept-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .dept-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

/* 抽屉底部按钮区域分隔线 */
.body--dark .dept-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}

/* 上级部门树下拉选项 */
.body--dark .dept-tree-option:hover {
  background: rgba(255, 255, 255, 0.06);
}
</style>
