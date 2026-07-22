<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { usePortalAuthStore } from "../stores/auth";

const { t } = useI18n({ useScope: "global" });
const route = useRoute();
const router = useRouter();
const authStore = usePortalAuthStore();

const profile = computed(() => authStore.profile);

const avatarInitial = computed(() => {
  const name = profile.value?.realName || profile.value?.nickname || profile.value?.username || "U";
  return [...name][0]?.toUpperCase() ?? "U";
});

const displayName = computed(() => profile.value?.realName || profile.value?.nickname || t("layout.defaultNickname"));
const username = computed(() => profile.value?.username || t("layout.defaultLoginName"));

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
</script>

<template>
  <aside class="profile-sidebar">
    <div class="profile-sidebar-user">
      <div class="profile-sidebar-avatar">{{ avatarInitial }}</div>
      <div class="profile-sidebar-name">{{ displayName }}</div>
      <div class="profile-sidebar-username">{{ username }}</div>
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
</template>

<style scoped>
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

@media (max-width: 1024px) {
  .profile-sidebar {
    position: static;
  }
}
</style>
