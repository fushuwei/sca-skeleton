<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysUser, DeptOption, PostOption, RoleOption } from "../../types/auth";
import { createUserApi, updateUserApi } from "../../apis/user";
import { getDeptOptionsApi } from "../../apis/dept";
import { getPostOptionsApi } from "../../apis/post";
import { getRoleOptionsApi } from "../../apis/role";
import { checkPasswordStrength } from "../../utils/passwordStrength";
import { useAuthStore } from "../../stores/auth";
import DateTimePicker from "../../components/DateTimePicker.vue";

const { t } = useI18n({ useScope: "global" });
const authStore = useAuthStore();

const props = defineProps<{
  mode: "add" | "edit" | "view";
  user?: SysUser;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");
/** 当前登录用户是否为超管 */
const isSuperadmin = computed(() => authStore.isSuperadmin);

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
  realm: "",
  isSuperadmin: 0,
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
  username: [
    (v: string) => !!v?.trim() || t("user.usernameRequired"),
    (v: string) => v.length >= 2 || t("user.usernameLengthMin"),
    (v: string) => v.length <= 50 || t("user.usernameLengthMax"),
    (v: string) => /^[a-zA-Z0-9_]+$/.test(v) || t("user.usernamePattern")
  ],
  realm: [(v: string) => !!v || t("user.realmRequired")],
  deptId: [(v: string) => !!v || t("user.deptRequired")],
  roleIds: [(v: string[]) => v?.length > 0 || t("user.roleRequired")],
  nickname: [],
  realName: [],
  gender: [],
  phone: [
    (v: string) => {
      if (!v) return true;
      return /^1[3-9]\d{9}$/.test(v) || t("user.phonePattern");
    }
  ],
  email: [
    (v: string) => {
      if (!v) return true;
      return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) || t("user.emailPattern");
    }
  ],
  status: [],
  remark: []
};

const genderOptions = computed(() => [
  { label: t("user.pleaseSelect"), value: "" },
  { label: t("user.genderMale"), value: "male" },
  { label: t("user.genderFemale"), value: "female" }
]);

const realmOptions = computed(() => [
  { label: t("user.categoryAdmin"), value: "admin" },
  { label: t("user.categoryPortal"), value: "portal" }
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

const effectiveEndRules = computed(() => [
  (v: string) => {
    if (!v || !form.effectiveStartTime) return true;
    return v > form.effectiveStartTime || t("user.effectiveEndMustAfterStart");
  }
]);

// ── 密码强度校验 ──
const showPassword = ref(false);
const passwordStrength = computed(() => {
  const pwd = form.password;
  if (!pwd) return { level: 0, label: "", color: "" };

  const result = checkPasswordStrength(pwd);
  return { level: result.score, label: result.label, color: result.color };
});

const passwordRules = computed(() => {
  return [
    (v: string) => {
      if (!v) return true;
      return v.length >= 8 || t("user.passwordMinLength");
    },
    (v: string) => {
      if (!v) return true;
      return v.length <= 20 || t("user.passwordMaxLength");
    }
  ];
});

function getPasswordColor(level: number): string {
  const colors: Record<number, string> = {
    1: "#f44336",
    2: "#ff9800",
    3: "#4caf50",
    4: "#2e7d32"
  };
  return colors[level] || "#f44336";
}

// ── 下拉数据 ──
const deptOptions = ref<DeptOption[]>([]);
const postOptions = ref<PostOption[]>([]);
const roleOptions = ref<RoleOption[]>([]);

// ── 部门树 ──
/** 部门树虚拟根节点 ID（不可选择） */
const ROOT_DEPT_ID = "0";

interface DeptTreeNode {
  id: string;
  label: string;
  parentId: string;
  children?: DeptTreeNode[];
}
const deptTreeNodes = ref<DeptTreeNode[]>([]);
const deptTreeExpanded = ref<string[]>([]);
const deptSearchKey = ref("");
const deptMenuRef = ref();
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

/** 带虚拟根节点「全部」的树（q-tree 渲染用） */
const deptTreeWithRoot = computed(() => [{
  id: ROOT_DEPT_ID,
  label: t("user.allDepts"),
  parentId: "",
  children: deptTreeNodes.value
}] as DeptTreeNode[]);

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

const deptDisplayLabel = computed(() => {
  if (!form.deptId) return "";
  return findDeptLabel(deptTreeWithRoot.value, form.deptId);
});

/** 点击树节点选中部门（根节点「全部」不可选，选中后关闭菜单） */
function onDeptTreeNodeClick(node: DeptTreeNode) {
  if (node.id === ROOT_DEPT_ID) return;
  form.deptId = node.id;
  deptSearchKey.value = "";
  deptMenuRef.value?.hide();
}

/** 节点图标：使用 folder/folder_open 风格，与部门管理一致 */
function deptNodeIcon(node: DeptTreeNode): string {
  return deptTreeExpanded.value.includes(node.id) ? "sym_r_folder_open" : "sym_r_folder";
}

// ── 岗位、角色多选 ──
const postMultiOptions = computed(() =>
  postOptions.value.map((p) => ({ label: p.name, value: p.id }))
);
const roleMultiOptions = computed(() =>
  roleOptions.value.map((r) => ({ label: r.name, value: r.id }))
);

/** 加载角色选项（按目标租户和用户域过滤） */
async function loadRoleOptions(tenantId?: string, realm?: string) {
  try {
    const result = await getRoleOptionsApi(tenantId, realm);
    if (result.code === 10_000 && result.data) {
      roleOptions.value = result.data;
    }
  } catch {
    // 静默失败，下拉为空
  }
}

/** 加载部门选项（按目标租户过滤） */
async function loadDeptOptions(tenantId?: string) {
  try {
    const result = await getDeptOptionsApi(tenantId);
    if (result.code === 10_000 && result.data) {
      deptOptions.value = result.data;
      deptTreeNodes.value = buildDeptTree(result.data);
    }
  } catch {
    // 静默失败，下拉为空
  }
}

/** 加载岗位选项（按目标租户过滤） */
async function loadPostOptions(tenantId?: string) {
  try {
    const result = await getPostOptionsApi(tenantId);
    if (result.code === 10_000 && result.data) {
      postOptions.value = result.data;
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
  form.realm = "";
  form.isSuperadmin = 0;
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
    form.realm = props.user.realm;
    form.isSuperadmin = props.user.isSuperadmin ?? 0;
    form.status = props.user.status;
    form.mustChangePassword = props.user.mustChangePassword ?? 1;
    form.effectiveStartTime = props.user.effectiveStartTime || "";
    form.effectiveEndTime = props.user.effectiveEndTime || "";
    form.remark = props.user.remark;
    // 回填关联数据：部门取第一个（主部门），岗位和角色直接赋列表
    form.deptId = props.user.deptIds?.[0] ?? "";
    form.postIds = props.user.postIds ? [...props.user.postIds] : [];
    form.roleIds = props.user.roleIds ? [...props.user.roleIds] : [];
    // 编辑/查看模式按用户所在租户加载部门、岗位、角色选项
    const tenantId = props.user.tenantId;
    loadDeptOptions(tenantId);
    loadPostOptions(tenantId);
    loadRoleOptions(tenantId, props.user.realm);
  }
}

watch(() => props.user, initForm, { immediate: true });

// 切换用户域时重新加载角色选项（按域过滤），并清空已选角色避免跨域分配
watch(() => form.realm, (val) => {
  if (props.mode === "add") {
    form.roleIds = [];
  }
  loadRoleOptions(undefined, val || undefined);
});

onMounted(() => {
  if (props.mode === "add") {
    // 新增模式：加载部门、岗位（后端按当前用户租户过滤）
    // 编辑/查看模式：initForm 已加载
    loadDeptOptions();
    loadPostOptions();
  }
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
    realm: form.realm,
    isSuperadmin: form.isSuperadmin,
    status: form.status,
    mustChangePassword: form.mustChangePassword,
    effectiveStartTime: form.effectiveStartTime || undefined,
    effectiveEndTime: form.effectiveEndTime || undefined,
    remark: form.remark || undefined,
    deptIds: form.deptId ? [form.deptId] : undefined,
    postIds: form.postIds.length ? form.postIds : undefined,
    roleIds: form.roleIds.length ? form.roleIds : undefined
  };

  if (form.password) {
    data.password = form.password;
  }

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createUserApi(data);
    } else {
      data.id = form.id;
      data.version = props.user?.version;
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
    <!-- 中间内容区（唯一滚动区）：表单字段 -->
    <q-form id="user-drawer-form" class="user-drawer-form user-drawer-main" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 用户名 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.username"
            :label="t('user.username')"
            filled
            square
            :rules="formRules.username"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 密码（可选，留空则使用默认值或不修改） -->
        <div v-if="mode !== 'view'" class="col-12 col-md-6">
          <q-input
            v-model="form.password"
            :label="t('user.password')"
            :hint="!form.password ? (mode === 'add' ? t('user.passwordHint') : t('user.passwordEditHint')) : undefined"
            filled
            square
            :type="showPassword ? 'text' : 'password'"
            :rules="passwordRules"
            :disable="drawerReadonly"
            hide-bottom-space
          >
            <template #append>
              <q-icon
                :name="showPassword ? 'sym_r_visibility_off' : 'sym_r_visibility'"
                class="cursor-pointer"
                size="20px"
                @click="showPassword = !showPassword"
              />
            </template>
          </q-input>
          <!-- 密码强度指示器 -->
          <div v-if="form.password && passwordStrength.level > 0" class="password-strength-container q-mt-xs">
            <div class="password-strength-bar">
              <div
                class="password-strength-segment"
                :class="{ 'active': passwordStrength.level >= 1 }"
                :style="{ backgroundColor: passwordStrength.level >= 1 ? getPasswordColor(1) : undefined }"
              />
              <div
                class="password-strength-segment"
                :class="{ 'active': passwordStrength.level >= 2 }"
                :style="{ backgroundColor: passwordStrength.level >= 2 ? getPasswordColor(2) : undefined }"
              />
              <div
                class="password-strength-segment"
                :class="{ 'active': passwordStrength.level >= 3 }"
                :style="{ backgroundColor: passwordStrength.level >= 3 ? getPasswordColor(3) : undefined }"
              />
              <div
                class="password-strength-segment"
                :class="{ 'active': passwordStrength.level >= 4 }"
                :style="{ backgroundColor: passwordStrength.level >= 4 ? getPasswordColor(4) : undefined }"
              />
            </div>
            <span class="password-strength-label" :class="'text-' + passwordStrength.color">
              {{ t(passwordStrength.label) }}
            </span>
          </div>
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
            :rules="formRules.phone"
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
            :rules="formRules.email"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 用户域（仅新增时可选，编辑时禁止修改用户域） -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.realm"
            :label="t('user.realm')"
            filled
            square
            :options="realmOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :rules="formRules.realm"
            :disable="mode !== 'add'"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 超级管理员（仅超管可见可操作，非超管完全不渲染，配合后端防护避免越权提权） -->
        <div v-if="isSuperadmin" class="col-12 col-md-6">
          <q-toggle
            v-model="form.isSuperadmin"
            :label="t('user.isSuperadmin')"
            :true-value="1"
            :false-value="0"
            :disable="drawerReadonly"
            class="q-mt-sm"
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
          <q-select
            v-model="form.deptId"
            :label="t('user.dept')"
            filled
            square
            emit-value
            :display-value="deptDisplayLabel"
            :rules="formRules.deptId"
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
                  :placeholder="t('user.searchDept')"
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
                        :class="{
                          'dept-tree-option--leaf': !scope.node.children?.length,
                          'dept-tree-option--disabled': scope.node.id === ROOT_DEPT_ID
                        }"
                        @click.stop="onDeptTreeNodeClick(scope.node)"
                      >
                        <q-icon
                          :name="deptNodeIcon(scope.node)"
                          size="18px"
                          class="q-mr-sm"
                          :class="{ 'text-primary': form.deptId === scope.node.id }"
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
          </q-select>
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
            :use-chips="drawerReadonly"
            clearable
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 角色（多选） -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.roleIds"
            :label="t('user.role')"
            filled
            square
            :options="roleMultiOptions"
            emit-value
            map-options
            multiple
            :use-chips="drawerReadonly"
            clearable
            :rules="formRules.roleIds"
            :disable="drawerReadonly"
            hide-bottom-space
            class="required-field"
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
          <DateTimePicker
            v-model="form.effectiveStartTime"
            :label="t('user.effectiveStartTime')"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            :max="form.effectiveEndTime || undefined"
          />
        </div>
        <!-- 失效时间 -->
        <div class="col-12 col-md-6">
          <DateTimePicker
            v-model="form.effectiveEndTime"
            :label="t('user.effectiveEndTime')"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            :min="form.effectiveStartTime || undefined"
            :rules="effectiveEndRules"
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
    </q-form>

    <!-- 底部操作栏：固定第三段，不随内容滚动；按钮直接挂在页脚容器上，间距由 CSS gap 控制 -->
    <div v-if="!drawerReadonly" class="user-drawer-footer row items-center justify-end no-wrap">
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
        form="user-drawer-form"
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
/* 三段式抽屉布局：外层 .user-drawer-body 不再滚动，
   中间内容区（q-form.user-drawer-main）独立滚动，底部操作栏固定为第三段 */
.user-drawer-content {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* 中间内容区：唯一的滚动区域 */
.user-drawer-main {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 16px 12px;
}

/* 底部操作栏：固定第三段，不参与内容滚动（按钮直接子元素，gap 控制间距） */
.user-drawer-footer {
  flex-shrink: 0;
  gap: 8px;
  padding: 14px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
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

.dept-tree-option--disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.dept-tree-option--disabled:hover {
  background: transparent;
}

/* 多选下拉框 display-value 文本过长省略 */
.user-drawer-form :deep(.q-select .q-field__native) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 必填项星号红色高亮 */
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

/* 密码强度指示器 */
.password-strength-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.password-strength-bar {
  display: flex;
  gap: 4px;
  flex: 1;
}

.password-strength-segment {
  height: 4px;
  flex: 1;
  background-color: #e0e0e0;
  border-radius: 2px;
  transition: background-color 0.3s ease;
}

.password-strength-segment.active {
  background-color: inherit;
}

.password-strength-label {
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}
</style>

<style>
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

/* 抽屉底部按钮区域分隔线 */
.body--dark .user-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}

/* 部门树下拉选项 */
.body--dark .dept-tree-option:hover {
  background: rgba(255, 255, 255, 0.06);
}
</style>
