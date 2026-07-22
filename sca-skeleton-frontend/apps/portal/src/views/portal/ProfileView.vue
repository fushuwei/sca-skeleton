<script setup lang="ts">
import { ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useQuasar } from "quasar";

const { t } = useI18n({ useScope: "global" });
const $q = useQuasar();
const route = useRoute();
const router = useRouter();

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

// ═══════════════════════════════════════════════════════════════
// 左侧导航
// ═══════════════════════════════════════════════════════════════
interface NavItem {
  path: string;
  icon: string;
  label: string;
}

const navItems = computed<NavItem[]>(() => [
  { path: "/portal/profile", icon: "sym_r_person", label: t("profile.pageTitle") },
  { path: "/portal/my/requests", icon: "sym_r_description", label: t("myRequest.pageTitle") },
  { path: "/portal/my/downloads", icon: "sym_r_download", label: t("myDownload.pageTitle") },
  { path: "/portal/my/favorites", icon: "sym_r_star", label: t("myFavorite.pageTitle") },
  { path: "/portal/profile/notifications", icon: "sym_r_notifications", label: t("notification.pageTitle") },
  { path: "/portal/profile/app-integrations", icon: "sym_r_link", label: t("appIntegration.pageTitle") }
]);

function isNavActive(path: string): boolean {
  return route.path === path;
}

function navigateTo(path: string): void {
  if (path !== route.path) {
    router.push(path);
  }
}

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
    <div class="profile-layout">
      <!-- ═══════════════ 左侧导航栏 ═══════════════ -->
      <aside class="profile-sidebar">
        <div class="profile-sidebar-user">
          <div class="profile-sidebar-avatar">{{ avatarInitial }}</div>
          <div class="profile-sidebar-name">{{ profile.realName }}</div>
          <div class="profile-sidebar-username">{{ profile.username }}</div>
          <q-chip
            dense
            square
            :color="statusColor"
            text-color="white"
            class="profile-sidebar-status"
          >
            {{ statusLabel }}
          </q-chip>
        </div>
        <q-separator />
        <nav class="profile-nav">
          <q-list dense padding>
            <q-item
              v-for="item in navItems"
              :key="item.path"
              clickable
              v-ripple
              :active="isNavActive(item.path)"
              active-class="profile-nav-item--active"
              class="profile-nav-item"
              @click="navigateTo(item.path)"
            >
              <q-item-section avatar>
                <q-icon :name="item.icon" size="20px" />
              </q-item-section>
              <q-item-section>{{ item.label }}</q-item-section>
            </q-item>
          </q-list>
        </nav>
      </aside>

      <!-- ═══════════════ 右侧内容 ═══════════════ -->
      <div class="profile-main">
        <header class="page-header">
          <h1 class="page-title">{{ t("profile.pageTitle") }}</h1>
          <p class="page-desc">{{ t("profile.pageDesc") }}</p>
        </header>

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
  </div>
</template>

<style scoped>
.profile-page {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
}

/* ═══════════════ 布局 ═══════════════ */
.profile-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 24px;
  align-items: start;
}

/* ═══════════════ 左侧导航栏 ═══════════════ */
.profile-sidebar {
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 24px;
}

.body--dark .profile-sidebar {
  background: #2a2a2a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.profile-sidebar-user {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28px 16px 20px;
  text-align: center;
}

.profile-sidebar-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #009688 0%, #00796b 100%);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  font-family: "JetBrains Mono", monospace;
}

.profile-sidebar-name {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  margin-bottom: 2px;
}

.body--dark .profile-sidebar-name {
  color: rgba(255, 255, 255, 0.92);
}

.profile-sidebar-username {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-bottom: 8px;
}

.body--dark .profile-sidebar-username {
  color: rgba(255, 255, 255, 0.45);
}

.profile-sidebar-status {
  margin: 0;
}

/* —— 导航菜单 —— */
.profile-nav {
  padding: 4px 0;
}

.profile-nav-item {
  box-sizing: border-box;
  min-height: 42px;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
  border-radius: 0;
}

.body--dark .profile-nav-item {
  color: rgba(255, 255, 255, 0.65);
}

.profile-nav-item :deep(.q-item__section--avatar) {
  min-width: 32px;
}

.profile-nav-item:hover {
  background: rgba(0, 0, 0, 0.04);
  color: rgba(0, 0, 0, 0.87);
}

.body--dark .profile-nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.87);
}

.profile-nav-item--active {
  color: #009688 !important;
  font-weight: 600;
  box-shadow: inset 4px 0 0 #009688;
  background: rgba(0, 150, 136, 0.06);
}

.body--dark .profile-nav-item--active {
  color: #4db6ac !important;
  box-shadow: inset 4px 0 0 #4db6ac;
  background: rgba(77, 182, 172, 0.1);
}

.profile-nav-item--active :deep(.q-icon) {
  color: inherit;
}

/* ═══════════════ 右侧内容 ═══════════════ */
.profile-main {
  min-width: 0;
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
  .profile-sidebar {
    position: static;
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
