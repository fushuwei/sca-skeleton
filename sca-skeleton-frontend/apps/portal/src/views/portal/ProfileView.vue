<script setup lang="ts">
import { ref, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useQuasar } from "quasar";

const { t } = useI18n({ useScope: "global" });
const $q = useQuasar();

interface ProfileForm {
  username: string;
  nickname: string;
  realName: string;
  gender: string;
  phone: string;
  email: string;
  realm: string;
  status: string;
  lastLoginTime: string;
  lastLoginIp: string;
  createTime: string;
}

const profile = ref<ProfileForm>({
  username: "zhangsan",
  nickname: "张教授",
  realName: "张三丰",
  gender: "male",
  phone: "138****8888",
  email: "zhangsf@donghu.edu.cn",
  realm: "portal",
  status: "active",
  lastLoginTime: "2026-07-22 09:15:32",
  lastLoginIp: "10.20.30.40",
  createTime: "2024-03-15"
});

const avatarInitial = computed(() => {
  const name = profile.value.realName || profile.value.username || "U";
  return [...name][0]?.toUpperCase() ?? "U";
});

const genderOptions = computed(() => [
  { label: t("profile.genderMale"), value: "male" },
  { label: t("profile.genderFemale"), value: "female" },
  { label: t("profile.genderUnknown"), value: "unknown" }
]);

const statusColorMap: Record<string, string> = {
  active: "teal",
  locked: "orange",
  disabled: "red"
};

const statusColor = computed(() => statusColorMap[profile.value.status] ?? "grey");

const statusLabel = computed(() => {
  const map: Record<string, string> = {
    active: t("profile.statusActive"),
    locked: t("profile.statusLocked"),
    disabled: t("profile.statusDisabled")
  };
  return map[profile.value.status] ?? profile.value.status;
});

function saveProfile(): void {
  $q.notify({
    type: "positive",
    message: t("profile.save") + " ✓",
    position: "top",
    timeout: 2000
  });
}

const pwdForm = ref({ oldPassword: "", newPassword: "", confirmPassword: "" });

function changePassword(): void {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    return;
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    $q.notify({ type: "negative", message: t("profile.passwordMismatch"), position: "top" });
    return;
  }
  $q.notify({ type: "positive", message: t("profile.passwordChanged"), position: "top" });
  pwdForm.value = { oldPassword: "", newPassword: "", confirmPassword: "" };
}
</script>

<template>
  <div class="profile-page">
    <header class="page-header">
      <h1 class="page-title">{{ t("profile.pageTitle") }}</h1>
      <p class="page-desc">{{ t("profile.pageDesc") }}</p>
    </header>

    <div class="profile-layout">
      <!-- ═══════════════ 左栏：头像卡片 ═══════════════ -->
      <aside class="avatar-card">
        <div class="avatar-circle">{{ avatarInitial }}</div>
        <div class="avatar-username">{{ profile.username }}</div>
        <div class="avatar-realname">{{ profile.realName }}</div>
        <q-chip
          dense
          square
          :color="statusColor"
          text-color="white"
          class="avatar-status"
        >
          {{ statusLabel }}
        </q-chip>
        <div class="avatar-meta">
          <div class="avatar-meta-row">
            <q-icon name="sym_r_alternate_email" size="16px" />
            <span>{{ profile.email }}</span>
          </div>
          <div class="avatar-meta-row">
            <q-icon name="sym_r_call" size="16px" />
            <span>{{ profile.phone }}</span>
          </div>
          <div class="avatar-meta-row">
            <q-icon name="sym_r_shield_person" size="16px" />
            <span>{{ profile.realm }}</span>
          </div>
        </div>
      </aside>

      <!-- ═══════════════ 右栏：信息卡片 ═══════════════ -->
      <div class="info-column">
        <!-- 基本信息卡 -->
        <q-card flat class="info-card">
          <div class="info-card__header">
            <q-icon name="sym_r_contact_page" size="22px" class="info-card__icon" />
            <span class="info-card__title">{{ t("profile.basicInfo") }}</span>
          </div>
          <q-form class="info-form" @submit.prevent="saveProfile">
            <div class="form-grid">
              <div class="form-field">
                <label class="form-label">{{ t("profile.username") }}</label>
                <q-input
                  v-model="profile.username"
                  outlined
                  dense
                  readonly
                  class="form-input"
                />
              </div>
              <div class="form-field">
                <label class="form-label">{{ t("profile.nickname") }}</label>
                <q-input
                  v-model="profile.nickname"
                  outlined
                  dense
                  class="form-input"
                />
              </div>
              <div class="form-field">
                <label class="form-label">{{ t("profile.realName") }}</label>
                <q-input
                  v-model="profile.realName"
                  outlined
                  dense
                  class="form-input"
                />
              </div>
              <div class="form-field">
                <label class="form-label">{{ t("profile.gender") }}</label>
                <q-select
                  v-model="profile.gender"
                  :options="genderOptions"
                  emit-value
                  map-options
                  outlined
                  dense
                  class="form-input"
                />
              </div>
              <div class="form-field">
                <label class="form-label">{{ t("profile.phone") }}</label>
                <q-input
                  v-model="profile.phone"
                  outlined
                  dense
                  class="form-input"
                />
              </div>
              <div class="form-field">
                <label class="form-label">{{ t("profile.email") }}</label>
                <q-input
                  v-model="profile.email"
                  outlined
                  dense
                  class="form-input"
                />
              </div>
              <div class="form-field">
                <label class="form-label">{{ t("profile.realm") }}</label>
                <q-input
                  v-model="profile.realm"
                  outlined
                  dense
                  readonly
                  class="form-input"
                />
              </div>
              <div class="form-field">
                <label class="form-label">{{ t("profile.status") }}</label>
                <div class="form-readonly-chip">
                  <q-chip dense square :color="statusColor" text-color="white">
                    {{ statusLabel }}
                  </q-chip>
                </div>
              </div>
              <div class="form-field">
                <label class="form-label">{{ t("profile.lastLoginTime") }}</label>
                <q-input
                  v-model="profile.lastLoginTime"
                  outlined
                  dense
                  readonly
                  class="form-input"
                />
              </div>
              <div class="form-field">
                <label class="form-label">{{ t("profile.lastLoginIp") }}</label>
                <q-input
                  v-model="profile.lastLoginIp"
                  outlined
                  dense
                  readonly
                  class="form-input"
                />
              </div>
              <div class="form-field">
                <label class="form-label">{{ t("profile.createTime") }}</label>
                <q-input
                  v-model="profile.createTime"
                  outlined
                  dense
                  readonly
                  class="form-input"
                />
              </div>
            </div>
            <div class="form-actions">
              <q-btn
                unelevated
                no-caps
                type="submit"
                :label="t('profile.save')"
                class="btn-primary"
                icon="sym_r_save"
              />
            </div>
          </q-form>
        </q-card>

        <!-- 安全设置卡 -->
        <q-card flat class="info-card">
          <div class="info-card__header">
            <q-icon name="sym_r_lock_person" size="22px" class="info-card__icon" />
            <span class="info-card__title">{{ t("profile.securityInfo") }}</span>
          </div>
          <q-form class="info-form" @submit.prevent="changePassword">
            <div class="form-grid form-grid--single">
              <div class="form-field">
                <label class="form-label">{{ t("profile.oldPassword") }}</label>
                <q-input
                  v-model="pwdForm.oldPassword"
                  outlined
                  dense
                  type="password"
                  class="form-input"
                />
              </div>
              <div class="form-field">
                <label class="form-label">{{ t("profile.newPassword") }}</label>
                <q-input
                  v-model="pwdForm.newPassword"
                  outlined
                  dense
                  type="password"
                  class="form-input"
                />
              </div>
              <div class="form-field">
                <label class="form-label">{{ t("profile.confirmPassword") }}</label>
                <q-input
                  v-model="pwdForm.confirmPassword"
                  outlined
                  dense
                  type="password"
                  class="form-input"
                />
              </div>
            </div>
            <div class="form-actions">
              <q-btn
                unelevated
                no-caps
                type="submit"
                :label="t('profile.changePassword')"
                class="btn-primary"
                icon="sym_r_key"
              />
            </div>
          </q-form>
        </q-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  margin: 0 0 4px 0;
  line-height: 1.4;
}

.body--dark .page-title {
  color: rgba(255, 255, 255, 0.92);
}

.page-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  margin: 0;
}

.body--dark .page-desc {
  color: rgba(255, 255, 255, 0.55);
}

/* ═══════════════ 布局 ═══════════════ */
.profile-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
  align-items: start;
}

/* ═══════════════ 头像卡片 ═══════════════ */
.avatar-card {
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.body--dark .avatar-card {
  background: #2a2a2a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.avatar-circle {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: linear-gradient(135deg, #009688 0%, #00796b 100%);
  color: #fff;
  font-size: 40px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  font-family: "JetBrains Mono", monospace;
}

.avatar-username {
  font-size: 20px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  margin-bottom: 4px;
}

.body--dark .avatar-username {
  color: rgba(255, 255, 255, 0.92);
}

.avatar-realname {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.55);
  margin-bottom: 12px;
}

.body--dark .avatar-realname {
  color: rgba(255, 255, 255, 0.55);
}

.avatar-status {
  margin-bottom: 24px;
}

.avatar-meta {
  width: 100%;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  padding-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.body--dark .avatar-meta {
  border-top-color: rgba(255, 255, 255, 0.08);
}

.avatar-meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.body--dark .avatar-meta-row {
  color: rgba(255, 255, 255, 0.65);
}

.avatar-meta-row .q-icon {
  color: #009688;
}

.body--dark .avatar-meta-row .q-icon {
  color: #4db6ac;
}

/* ═══════════════ 信息卡片 ═══════════════ */
.info-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-card {
  background: #fff !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.body--dark .info-card {
  background: #2a2a2a !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.info-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.body--dark .info-card__header {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.info-card__icon {
  color: #009688;
}

.body--dark .info-card__icon {
  color: #4db6ac;
}

.info-card__title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.body--dark .info-card__title {
  color: rgba(255, 255, 255, 0.92);
}

.info-form {
  padding: 24px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px 24px;
}

.form-grid--single {
  grid-template-columns: 1fr;
  max-width: 480px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
}

.body--dark .form-label {
  color: rgba(255, 255, 255, 0.65);
}

.form-input :deep(.q-field__control) {
  background: rgba(0, 0, 0, 0.02);
}

.body--dark .form-input :deep(.q-field__control) {
  background: rgba(255, 255, 255, 0.04);
}

.form-readonly-chip {
  height: 40px;
  display: flex;
  align-items: center;
}

.form-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

.btn-primary {
  background: #009688 !important;
  color: #fff !important;
  padding: 0 24px;
  height: 38px;
  font-size: 14px;
  font-weight: 600;
}

.btn-primary:hover {
  background: #00796b !important;
}

.body--dark .btn-primary {
  background: #4db6ac !important;
  color: #002b27 !important;
}

.body--dark .btn-primary:hover {
  background: #009688 !important;
  color: #fff !important;
}

/* ═══════════════ 响应式 ═══════════════ */
@media (max-width: 1024px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
  .avatar-card {
    padding: 24px;
  }
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .profile-page {
    padding: 16px;
  }
}
</style>
