<script setup lang="ts">
import { ref, reactive, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysUser } from "../../types/auth";
import { createUserApi, updateUserApi } from "../../apis/user";

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
  userCategory: "backend",
  userType: "normal",
  status: "active",
  effectiveStartTime: "",
  effectiveEndTime: "",
  remark: ""
});

const formRules = {
  username: [(v: string) => !!v?.trim() || t("user.usernameRequired")],
  nickname: [],
  realName: [],
  gender: [],
  phone: [],
  email: [],
  userCategory: [],
  userType: [],
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

function resetForm() {
  form.id = "";
  form.username = "";
  form.password = "";
  form.nickname = "";
  form.realName = "";
  form.gender = "";
  form.phone = "";
  form.email = "";
  form.userCategory = "backend";
  form.userType = "normal";
  form.status = "active";
  form.effectiveStartTime = "";
  form.effectiveEndTime = "";
  form.remark = "";
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
    form.effectiveStartTime = props.user.effectiveStartTime || "";
    form.effectiveEndTime = props.user.effectiveEndTime || "";
    form.remark = props.user.remark;
  }
}

watch(() => props.user, initForm, { immediate: true });

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
    effectiveStartTime: form.effectiveStartTime || undefined,
    effectiveEndTime: form.effectiveEndTime || undefined,
    remark: form.remark || undefined
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
            dense
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
            dense
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
            dense
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
            dense
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
            dense
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
            dense
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
            dense
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 用户类别 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.userCategory"
            :label="t('user.userCategory')"
            filled
            square
            dense
            :options="userCategoryOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            :disable="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 用户类型 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.userType"
            :label="t('user.userType')"
            filled
            square
            dense
            :options="userTypeOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
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
            dense
            :options="statusOptions"
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
            dense
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
            dense
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
            dense
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

.user-drawer-form :deep(.q-field__control) {
  min-height: 36px;
  padding: 0 12px;
}

.user-drawer-form :deep(.q-field__native) {
  min-height: 36px;
  font-size: 13px;
}

.user-drawer-form :deep(.q-field__label) {
  font-size: 13px;
}

.user-drawer-form :deep(textarea.q-field__native) {
  min-height: 60px;
}

.user-drawer-footer {
  flex-shrink: 0;
  padding: 12px 0 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  margin-top: 16px;
}

.drawer-action-btn {
  min-width: 72px;
  height: 34px;
  font-size: 13px;
}
</style>
