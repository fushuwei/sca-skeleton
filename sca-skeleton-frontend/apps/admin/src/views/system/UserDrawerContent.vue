<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysUser, SysDept, SysPost, SysRole } from "../../types/auth";
import { createUserApi, updateUserApi } from "../../apis/user";
import { getDeptListApi } from "../../apis/dept";
import { getPostListApi } from "../../apis/post";
import { getRoleListApi } from "../../apis/role";

const { t } = useI18n({ useScope: "global" });

const props = defineProps<{
  mode: "add" | "edit" | "view";
  user?: SysUser;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");

const formLoading = ref(false);
const form = reactive({
  id: "",
  username: "",
  password: "",
  nickname: "",
  realName: "",
  gender: "",
  phone: "",
  email: "",
  userCategory: "",
  userType: "",
  status: "active",
  mustChangePassword: 1,
  effectiveStartTime: "",
  effectiveEndTime: "",
  remark: "",
  deptId: "",
  postIds: [] as string[],
  roleIds: [] as string[]
});

const formRules = {
  username: [(v: string) => !!v?.trim() || t("user.usernameRequired")],
  userCategory: [(v: string) => !!v || t("user.userCategoryRequired")],
  userType: [(v: string) => !!v || t("user.userTypeRequired")],
  deptId: [(v: string) => !!v || t("user.deptRequired")],
  roleIds: [(v: string[]) => v?.length > 0 || t("user.roleRequired")],
  nickname: [],
  realName: [],
  gender: [],
  phone: [],
  email: [],
  status: [],
  remark: []
};

const genderOptions = computed(() => [
  { label: t("user.pleaseSelect"), value: "" },
  { label: t("user.genderMale"), value: "male" },
  { label: t("user.genderFemale"), value: "female" }
]);

const userCategoryOptions = computed(() => [
  { label: t("user.userCategoryBackend"), value: "backend" },
  { label: t("user.userCategoryFrontend"), value: "frontend" }
]);

const userTypeOptions = computed(() => [
  { label: "超级管理员", value: "superadmin" },
  { label: "租户管理员", value: "tenant_admin" },
  { label: "部门管理员", value: "dept_admin" },
  { label: "普通用户", value: "normal" }
]);

const statusOptions = computed(() => [
  { label: t("user.statusActive"), value: "active" },
  { label: t("user.statusInactive"), value: "inactive" },
  { label: t("user.statusLocked"), value: "locked" },
  { label: t("user.statusFrozen"), value: "frozen" },
  { label: t("user.statusExpired"), value: "expired" },
  { label: t("user.statusDisabled"), value: "disabled" },
  { label: t("user.statusCancelled"), value: "cancelled" }
]);

const mustChangePasswordOptions = computed(() => [
  { label: t("common.yes"), value: 1 },
  { label: t("common.no"), value: 0 }
]);

// ── 下拉数据 ──
const deptOptions = ref<SysDept[]>([]);
const postOptions = ref<SysPost[]>([]);
const roleOptions = ref<SysRole[]>([]);

// ── 部门树 ──
interface DeptTreeNode {
  id: string;
  label: string;
  parentId: string;
  children?: DeptTreeNode[];
}
const deptTreeNodes = ref<DeptTreeNode[]>([]);
const deptTreeExpanded = ref<string[]>([]);
const deptTreeMenuOpen = ref(false);
const deptSearchKey = ref("");

/** 将扁平部门列表转换为树结构 */
function buildDeptTree(depts: SysDept[]): DeptTreeNode[] {
  if (!depts.length) return [];
  const map = new Map<string, DeptTreeNode>();
  for (const d of depts) {
    map.set(d.id, { id: d.id, label: d.name, parentId: d.parentId, children: [] });
  }
  const roots: DeptTreeNode[] = [];
  for (const d of depts) {
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
  return roots;
}

/** 过滤树节点（按关键字） */
function filterDeptTree(nodes: DeptTreeNode[], keyword: string): DeptTreeNode[] {
  if (!keyword.trim()) return nodes;
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

const filteredDeptTreeNodes = computed(() => filterDeptTree(deptTreeNodes.value, deptSearchKey.value));

/** 搜索时自动展开所有节点 */
watch(deptSearchKey, (val) => {
  if (val.trim()) {
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

/** 部门树加载后默认展开第一级 */
watch(deptTreeNodes, (nodes) => {
  if (nodes.length) {
    deptTreeExpanded.value = nodes.map((n) => n.id);
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

const deptDisplayLabel = computed(() => {
  if (!form.deptId) return "";
  return findDeptLabel(deptTreeNodes.value, form.deptId);
});

/** 点击树节点 */
function onDeptTreeNodeClick(node: DeptTreeNode) {
  if (!node.children?.length) {
    form.deptId = node.id;
    deptTreeMenuOpen.value = false;
    deptSearchKey.value = "";
  }
}

/** 清空部门选择 */
function clearDeptSelection() {
  form.deptId = "";
  deptSearchKey.value = "";
}

/** 节点图标 */
function deptNodeIcon(node: DeptTreeNode): string {
  return node.children?.length ? "sym_r_folder" : "sym_r_article";
}

// ── 岗位、角色多选 ──
const postMultiOptions = computed(() =>
  postOptions.value.map((p) => ({ label: p.name, value: p.id }))
);
const roleMultiOptions = computed(() =>
  roleOptions.value.map((r) => ({ label: r.name, value: r.id }))
);

async function loadDropdownData() {
  try {
    const [deptRes, postRes, roleRes] = await Promise.all([
      getDeptListApi(),
      getPostListApi(),
      getRoleListApi()
    ]);
    if (deptRes.code === 10_000 && deptRes.data) {
      deptOptions.value = deptRes.data;
      deptTreeNodes.value = buildDeptTree(deptRes.data);
    }
    if (postRes.code === 10_000 && postRes.data) {
      postOptions.value = postRes.data;
    }
    if (roleRes.code === 10_000 && roleRes.data) {
      roleOptions.value = roleRes.data;
    }
  } catch {
    // 静默失败，下拉为空
  }
}

function resetForm() {
  form.id = "";
  form.username = "";
  form.password = "";
  form.nickname = "";
  form.realName = "";
  form.gender = "";
  form.phone = "";
  form.email = "";
  form.userCategory = "";
  form.userType = "";
  form.status = "active";
  form.mustChangePassword = 1;
  form.effectiveStartTime = "";
  form.effectiveEndTime = "";
  form.remark = "";
  form.deptId = "";
  form.postIds = [];
  form.roleIds = [];
}

function initForm() {
  resetForm();
  if (props.user) {
    form.id = props.user.id;
    form.username = props.user.username;
    form.nickname = props.user.nickname;
    form.realName = props.user.realName;
    form.gender = props.user.gender;
    form.phone = props.user.phone;
    form.email = props.user.email;
    form.userCategory = props.user.userCategory;
    form.userType = props.user.userType;
    form.status = props.user.status;
    form.mustChangePassword = props.user.mustChangePassword ?? 1;
    form.effectiveStartTime = props.user.effectiveStartTime || "";
    form.effectiveEndTime = props.user.effectiveEndTime || "";
    form.remark = props.user.remark;
  }
}

watch(() => props.user, initForm, { immediate: true });

onMounted(() => {
  loadDropdownData();
});

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  const data: Record<string, unknown> = {
    username: form.username,
    nickname: form.nickname,
    realName: form.realName,
    gender: form.gender || undefined,
    phone: form.phone || undefined,
    email: form.email || undefined,
    userCategory: form.userCategory,
    userType: form.userType,
    status: form.status,
    mustChangePassword: form.mustChangePassword,
    effectiveStartTime: form.effectiveStartTime || undefined,
    effectiveEndTime: form.effectiveEndTime || undefined,
    remark: form.remark || undefined,
    deptId: form.deptId || undefined,
    postIds: form.postIds.length ? form.postIds : undefined,
    roleIds: form.roleIds.length ? form.roleIds : undefined
  };

  if (props.mode === "add" && form.password) {
    data.password = form.password;
  }

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createUserApi(data);
    } else {
      data.id = form.id;
      result = await updateUserApi(data);
    }

    if (result.code === 10_000) {
      showToast(t("user.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("user.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("user.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}
</script>

<template>
  <div class="user-drawer-content">
    <q-form class="user-drawer-form" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 用户名 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.username"
            :label="t('user.username') + ' *'"
            filled
            square
            :rules="formRules.username"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 密码（仅添加时显示） -->
        <div v-if="mode === 'add'" class="col-12 col-md-6">
          <q-input
            v-model="form.password"
            :label="t('user.password')"
            :hint="t('user.passwordHint')"
            filled
            square
            type="password"
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 昵称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model="form.nickname"
            :label="t('user.nickname')"
            filled
            square
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 真实姓名 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model="form.realName"
            :label="t('user.realName')"
            filled
            square
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 性别 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.gender"
            :label="t('user.gender')"
            filled
            square
            :options="genderOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 手机号 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model="form.phone"
            :label="t('user.phone')"
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
            v-model="form.email"
            :label="t('user.email')"
            filled
            square
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 用户类别 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.userCategory"
            :label="t('user.userCategory') + ' *'"
            filled
            square
            :options="userCategoryOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :rules="formRules.userCategory"
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 用户类型 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.userType"
            :label="t('user.userType') + ' *'"
            filled
            square
            :options="userTypeOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :rules="formRules.userType"
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 状态 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.status"
            :label="t('user.status')"
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
        <!-- 所属部门 -->
        <div class="col-12 col-md-6">
          <q-input
            :model-value="deptDisplayLabel"
            :label="t('user.dept') + ' *'"
            filled
            square
            readonly
            :disable="drawerReadonly"
            hide-bottom-space
            :rules="formRules.deptId"
            :class="{ 'cursor-pointer': !drawerReadonly, 'dept-field': true, 'q-field--focused': deptTreeMenuOpen }"
            @click.stop="!drawerReadonly && (deptTreeMenuOpen = true)"
          >
            <template v-if="!drawerReadonly" #append>
              <q-icon
                v-if="form.deptId"
                name="sym_r_close"
                class="cursor-pointer"
                size="18px"
                @click.stop="clearDeptSelection"
              />
              <q-icon name="sym_r_account_tree" size="18px" class="cursor-pointer" @click.stop="deptTreeMenuOpen = !deptTreeMenuOpen" />
            </template>
            <q-menu
              v-model="deptTreeMenuOpen"
              anchor="bottom left"
              self="top left"
              :offset="[0, 0]"
              no-route-update
              fit
            >
              <div class="q-pa-sm">
                <q-input
                  v-model="deptSearchKey"
                  dense
                  outlined
                  square
                  :placeholder="t('user.searchDept')"
                  clearable
                  class="q-mb-sm"
                >
                  <template #prepend>
                    <q-icon name="sym_r_search" size="18px" />
                  </template>
                </q-input>
                <q-scroll-area style="height: 280px">
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
                        :class="{ 'dept-tree-option--leaf': !scope.node.children?.length }"
                        @click.stop="onDeptTreeNodeClick(scope.node)"
                      >
                        <q-icon
                          :name="deptNodeIcon(scope.node)"
                          size="18px"
                          class="q-mr-sm"
                          :color="form.deptId === scope.node.id ? 'primary' : 'grey-7'"
                        />
                        <span class="ellipsis" :class="{ 'text-primary text-weight-medium': form.deptId === scope.node.id }">
                          {{ scope.node.label }}
                        </span>
                      </div>
                    </template>
                  </q-tree>
                </q-scroll-area>
              </div>
            </q-menu>
          </q-input>
        </div>
        <!-- 岗位（多选） -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.postIds"
            :label="t('user.post')"
            filled
            square
            :options="postMultiOptions"
            emit-value
            map-options
            multiple
            use-chips
            clearable
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 角色（多选） -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.roleIds"
            :label="t('user.role') + ' *'"
            filled
            square
            :options="roleMultiOptions"
            emit-value
            map-options
            multiple
            use-chips
            clearable
            :rules="formRules.roleIds"
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 是否必须修改密码 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.mustChangePassword"
            :label="t('user.mustChangePassword')"
            filled
            square
            :options="mustChangePasswordOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 生效时间 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model="form.effectiveStartTime"
            :label="t('user.effectiveStartTime')"
            filled
            square
            type="datetime-local"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 失效时间 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model="form.effectiveEndTime"
            :label="t('user.effectiveEndTime')"
            filled
            square
            type="datetime-local"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 备注 -->
        <div class="col-12">
          <q-input
            v-model="form.remark"
            :label="t('user.remark')"
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
      <div v-if="!drawerReadonly" class="user-drawer-footer row justify-end q-gutter-sm">
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
.user-drawer-content {
  padding: 0;
}

.user-drawer-footer {
  flex-shrink: 0;
  padding: 12px 0 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  margin-top: 16px;
}

.drawer-action-btn {
  min-width: 72px;
}

/* 部门树下拉选项 */
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

.dept-tree-option--leaf {
  cursor: pointer;
}
</style>

<style>
.dept-field .q-field__control::before {
  border-style: none none solid !important;
  background: rgba(0, 0, 0, 0.05) !important;
}

.dept-field.q-field--focused .q-field__control::before {
  border-bottom-width: 2px !important;
  border-bottom-color: currentColor !important;
}

.dept-field.q-field--focused .q-field__control::after {
  transform: scaleX(1) !important;
}

.body--dark .user-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .user-drawer-form .q-field__native,
.body--dark .user-drawer-form .q-field__prefix,
.body--dark .user-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .user-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .user-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .user-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .user-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}
</style>
