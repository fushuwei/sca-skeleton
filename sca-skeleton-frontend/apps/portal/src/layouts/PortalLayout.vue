<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useQuasar } from "quasar";
import { useI18n } from "vue-i18n";
import { usePortalAuthStore } from "../stores/auth";
import { flattenRoutableMenus } from "../utils/menu-tree";
import type { MenuItem } from "../types/auth";
import { persistDark, persistLocale, quasarLangForLocale } from "../i18n";

const LOGO_URL = import.meta.env.BASE_URL + "images/logo.png";

const router = useRouter();
const route = useRoute();
const authStore = usePortalAuthStore();
const $q = useQuasar();
const { locale, t } = useI18n({ useScope: "global" });

const username = computed(() => {
  void locale.value;
  return authStore.profile?.nickname ?? t("layout.defaultNickname");
});
const loginName = computed(() => authStore.profile?.username ?? t("layout.defaultLoginName"));
const realName = computed(() => {
  void locale.value;
  return authStore.profile?.realName ?? authStore.profile?.nickname ?? t("layout.defaultRealName");
});
const avatarInitial = computed(() => {
  const normalizedName = String(username.value ?? "").trim();
  if (!normalizedName) {
    return "U";
  }
  return [...normalizedName][0]?.toUpperCase() ?? "U";
});

const themeToggleIcon = computed(() => ($q.dark.isActive ? "sym_r_light_mode" : "sym_r_dark_mode"));

/** 顶部导航栏可见的一级菜单（folder + 无 children 的 menu） */
const topNavMenus = computed(() => {
  void locale.value;
  return authStore.menus;
});

/** 全部可路由叶子路径集合，用于判断当前路由是否落在某一级菜单的子树内 */
const allLeafPaths = computed(() => flattenRoutableMenus(authStore.menus).map((m) => m.path));

function translateMenuItemTitle(item: MenuItem): string {
  void locale.value;
  if (locale.value === "en-US" && item.nameEn) {
    return item.nameEn;
  }
  return item.name;
}

/** 判断某一级菜单树下是否包含当前激活路由（用于高亮顶部导航项） */
function topMenuContainsActiveRoute(item: MenuItem): boolean {
  const currentPath = route.path;
  if (!currentPath) return false;
  // 直接匹配自身 path
  if (item.path && item.path === currentPath) return true;
  // 递归匹配 children 的 path
  if (item.children?.length) {
    for (const child of item.children) {
      if (child.path === currentPath) return true;
      if (child.children?.length && topMenuContainsActiveRoute(child)) return true;
    }
  }
  return false;
}

function handleMenuClick(path = ""): void {
  if (!path) return;
  router.push(path);
}

function handleNavItemClick(item: MenuItem): void {
  // 无 children 的菜单项直接跳转
  if (!item.children?.length && item.path) {
    router.push(item.path);
  }
  // 有 children 的菜单项由 QMenu 处理下拉，不跳转
}

function toggleColorTheme(): void {
  $q.dark.toggle();
  persistDark($q.dark.isActive);
}

function applyHeaderLocale(code: "zh-CN" | "en-US"): void {
  if (locale.value === code) return;
  locale.value = code;
  $q.lang.set(quasarLangForLocale(code));
  persistLocale(code);
}

async function handleLogout(): Promise<void> {
  await authStore.logout();
}

// 模拟未读通知数量（演示用，后续接入真实接口）
const unreadCount = computed(() => 3);
</script>

<template>
  <q-layout view="hHh Lpr fFf" class="portal-shell" :dark="$q.dark.isActive">
    <q-header bordered class="top-header">
      <q-toolbar class="top-toolbar">
        <!-- 品牌 Logo + 标题 -->
        <div class="top-brand row items-center no-wrap" @click="handleMenuClick('/portal/home')">
          <img :src="LOGO_URL" alt="logo" class="top-logo" />
          <div class="top-brand-text-stack">
            <span class="top-brand-text">{{ t('layout.brandTitle') }}</span>
            <span class="top-brand-subtitle">{{ t('layout.brandSubtitle') }}</span>
          </div>
        </div>

        <!-- 顶部横向导航 -->
        <nav class="top-nav row items-center no-wrap">
          <template v-for="item in topNavMenus" :key="item.id">
            <!-- 无子菜单：直接链接 -->
            <q-btn
              v-if="!item.children?.length"
              flat
              no-caps
              :ripple="false"
              class="top-nav-btn"
              :class="{ 'top-nav-btn--active': topMenuContainsActiveRoute(item) }"
              @click="handleNavItemClick(item)"
            >
              <q-icon v-if="item.icon" :name="item.icon" size="20px" class="top-nav-btn__icon" />
              <span class="top-nav-btn__label">{{ translateMenuItemTitle(item) }}</span>
            </q-btn>
            <!-- 有子菜单：下拉 -->
            <q-btn
              v-else
              flat
              no-caps
              :ripple="false"
              class="top-nav-btn"
              :class="{ 'top-nav-btn--active': topMenuContainsActiveRoute(item) }"
              @click="handleNavItemClick(item)"
            >
              <q-icon v-if="item.icon" :name="item.icon" size="20px" class="top-nav-btn__icon" />
              <span class="top-nav-btn__label">{{ translateMenuItemTitle(item) }}</span>
              <q-icon name="sym_r_arrow_drop_down" size="20px" class="top-nav-btn__caret" />
              <q-menu
                class="top-nav-menu"
                anchor="bottom middle"
                self="top middle"
                :offset="[0, 8]"
                transition-show="jump-down"
                transition-hide="jump-up"
                :dark="$q.dark.isActive"
              >
                <q-list dense class="top-nav-menu-list">
                  <q-item
                    v-for="child in item.children"
                    :key="child.id"
                    v-close-popup
                    clickable
                    class="top-nav-menu-item"
                    @click="handleMenuClick(child.path)"
                  >
                    <q-item-section avatar class="top-nav-menu-item__icon">
                      <q-icon :name="child.icon || 'sym_r_nest_eco_leaf'" size="20px" />
                    </q-item-section>
                    <q-item-section class="top-nav-menu-item__label">
                      {{ translateMenuItemTitle(child) }}
                    </q-item-section>
                  </q-item>
                </q-list>
              </q-menu>
            </q-btn>
          </template>
        </nav>

        <q-space />

        <!-- 右侧操作区 -->
        <div class="top-actions">
          <!-- 全局搜索（占位按钮，后续接入搜索页） -->
          <q-btn flat round icon="sym_r_search" class="top-action-btn" :aria-label="t('layout.search')">
            <q-tooltip>{{ t('layout.search') }}</q-tooltip>
          </q-btn>
          <!-- 消息通知（带未读徽标） -->
          <q-btn
            flat
            round
            icon="sym_r_notifications"
            class="top-action-btn"
            :aria-label="t('layout.notifications')"
            @click="handleMenuClick('/portal/profile/notifications')"
          >
            <q-badge v-if="unreadCount > 0" color="red-7" floating rounded class="top-notif-badge">
              {{ unreadCount > 99 ? '99+' : unreadCount }}
            </q-badge>
            <q-tooltip>{{ t('layout.notifications') }}</q-tooltip>
          </q-btn>
          <!-- 语言切换 -->
          <q-btn
            flat
            round
            icon="sym_r_translate"
            class="top-action-btn"
            :aria-label="t('layout.languageMenuAria')"
          >
            <q-tooltip>{{ t('layout.languageMenuAria') }}</q-tooltip>
            <q-menu
              class="top-locale-menu"
              anchor="bottom middle"
              self="top middle"
              :offset="[0, 8]"
              :dark="$q.dark.isActive"
              transition-show="jump-down"
              transition-hide="jump-up"
            >
              <q-list dense class="top-locale-menu-list">
                <q-item
                  v-close-popup
                  clickable
                  class="top-locale-menu-item"
                  @click="applyHeaderLocale('zh-CN')"
                >
                  <q-item-section avatar class="top-locale-flag">CN</q-item-section>
                  <q-item-section>{{ t('layout.localeChinese') }}</q-item-section>
                  <q-item-section v-if="locale === 'zh-CN'" side>
                    <q-icon name="sym_r_check" class="top-locale-check" />
                  </q-item-section>
                </q-item>
                <q-item
                  v-close-popup
                  clickable
                  class="top-locale-menu-item"
                  @click="applyHeaderLocale('en-US')"
                >
                  <q-item-section avatar class="top-locale-flag">EN</q-item-section>
                  <q-item-section>{{ t('layout.localeEnglish') }}</q-item-section>
                  <q-item-section v-if="locale === 'en-US'" side>
                    <q-icon name="sym_r_check" class="top-locale-check" />
                  </q-item-section>
                </q-item>
              </q-list>
            </q-menu>
          </q-btn>
          <!-- 主题切换 -->
          <q-btn
            flat
            round
            :icon="themeToggleIcon"
            class="top-action-btn"
            :aria-label="t('layout.themeToggleAria')"
            @click="toggleColorTheme"
          >
            <q-tooltip>{{ t('layout.themeToggleAria') }}</q-tooltip>
          </q-btn>
        </div>

        <!-- 用户胶囊 -->
        <q-btn-dropdown
          flat
          class="user-pill btn-shape-exempt"
          dropdown-icon="sym_r_arrow_drop_down"
          no-caps
        >
          <template #label>
            <div class="row items-center no-wrap user-pill-label">
              <span class="user-avatar-circle">{{ avatarInitial }}</span>
              <span class="user-pill-name ellipsis">{{ username }}</span>
            </div>
          </template>
          <div class="q-pa-md user-menu-header">
            <span class="user-menu-avatar">{{ avatarInitial }}</span>
            <div class="user-menu-meta column justify-center">
              <div class="user-menu-realname">{{ realName }}</div>
              <div class="user-menu-email">{{ loginName }}</div>
            </div>
          </div>
          <q-separator />
          <q-list dense class="user-menu-action-list">
            <q-item clickable v-close-popup @click="handleMenuClick('/portal/profile')">
              <q-item-section avatar>
                <q-icon name="sym_r_person" size="20px" />
              </q-item-section>
              <q-item-section>{{ t('layout.personalCenter') }}</q-item-section>
            </q-item>
            <q-item clickable v-close-popup @click="handleMenuClick('/portal/profile/notifications')">
              <q-item-section avatar>
                <q-icon name="sym_r_notifications" size="20px" />
              </q-item-section>
              <q-item-section>{{ t('layout.notifications') }}</q-item-section>
            </q-item>
            <q-separator />
            <q-item clickable v-close-popup @click="handleLogout">
              <q-item-section avatar>
                <q-icon name="sym_r_logout" size="20px" />
              </q-item-section>
              <q-item-section class="text-negative">{{ t('layout.logout') }}</q-item-section>
            </q-item>
          </q-list>
        </q-btn-dropdown>
      </q-toolbar>
    </q-header>

    <q-page-container>
      <q-page class="main-page">
        <div class="page-content">
          <router-view v-slot="{ Component, route: routeForKeepAlive }">
            <keep-alive :include="[]">
              <component :is="Component" :key="routeForKeepAlive.path" />
            </keep-alive>
          </router-view>
        </div>
      </q-page>
    </q-page-container>

    <q-footer bordered class="bottom-footer">
      <q-toolbar class="bottom-toolbar">
        <div class="bottom-toolbar-meta">
          <div class="bottom-toolbar-copyright" v-html="$t('layout.footerCopyright')"></div>
        </div>
      </q-toolbar>
    </q-footer>
  </q-layout>
</template>

<style scoped>
/* ═══════════════ 顶部 Header ═══════════════ */
.top-header {
  background: #009688;
  color: #fff;
}

.top-toolbar {
  min-height: 64px;
  padding: 0 20px;
}

/* —— 品牌 Logo + 标题 —— */
.top-brand {
  cursor: pointer;
  flex-shrink: 0;
  margin-right: 24px;
}

.top-logo {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.top-brand-text-stack {
  display: flex;
  flex-direction: column;
  margin-left: 12px;
  line-height: 1.2;
}

.top-brand-text {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
}

.top-brand-subtitle {
  font-size: 11px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.75);
  letter-spacing: 0.5px;
  white-space: nowrap;
}

/* —— 顶部横向导航 —— */
.top-nav {
  gap: 4px;
  flex-shrink: 0;
}

.top-nav-btn {
  height: 64px;
  min-height: 64px;
  padding: 0 14px;
  color: rgba(255, 255, 255, 0.85);
  border-radius: 0 !important;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.top-nav-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.top-nav-btn--active {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-weight: 600;
  box-shadow: inset 0 -3px 0 #fff;
}

.top-nav-btn__icon {
  margin-right: 6px;
}

.top-nav-btn__caret {
  margin-left: 2px;
}

/* —— 右侧操作按钮 —— */
.top-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-right: 8px;
}

.top-action-btn {
  width: 40px;
  height: 40px;
  min-width: 40px;
  min-height: 40px;
  color: #fff;
  border-radius: 50%;
}

.top-action-btn :deep(.q-icon) {
  font-size: 24px;
}

.top-action-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.top-notif-badge {
  font-size: 10px;
  font-weight: 700;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 9px !important;
}

/* —— 用户胶囊 —— */
.user-pill {
  margin-left: 8px;
  height: 40px;
  min-height: 40px;
  padding: 0 8px 0 4px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 999px !important;
  color: #fff;
  overflow: hidden;
}

.user-pill:hover {
  background: rgba(255, 255, 255, 0.15);
}

.user-pill-label {
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
}

.user-pill-name {
  max-width: 120px;
}

.user-avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  text-transform: uppercase;
}

/* —— 用户下拉菜单 —— */
.user-menu-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #009688;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
  text-transform: uppercase;
}

.user-menu-header {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 12px;
  min-width: 240px;
}

.user-menu-meta {
  min-width: 0;
  flex: 1;
}

.user-menu-realname {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.35;
  color: rgba(0, 0, 0, 0.87);
}

.body--dark .user-menu-realname {
  color: rgba(255, 255, 255, 0.92);
}

.user-menu-email {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.35;
  color: rgba(0, 0, 0, 0.55);
  word-break: break-all;
}

.body--dark .user-menu-email {
  color: rgba(255, 255, 255, 0.6);
}

.user-menu-action-list :deep(.q-item) {
  box-sizing: border-box;
  min-height: 40px;
  height: 40px;
}

/* —— 顶部导航下拉菜单 —— */
.top-nav-menu-list {
  min-width: 220px;
  padding: 4px 0;
}

.top-nav-menu-item {
  box-sizing: border-box;
  min-height: 44px;
  height: 44px;
  padding: 0 16px;
}

.top-nav-menu-item__icon {
  min-width: 36px;
  color: #009688;
}

.body--dark .top-nav-menu-item__icon {
  color: #4db6ac;
}

.top-nav-menu-item__label {
  font-size: 14px;
  font-weight: 500;
}

/* —— 语言菜单 —— */
.top-locale-menu-list {
  min-width: 180px;
}

.top-locale-menu-item {
  box-sizing: border-box;
  min-height: 40px;
  height: 40px;
}

.top-locale-flag {
  min-width: 32px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #009688;
}

.body--dark .top-locale-flag {
  color: #4db6ac;
}

.top-locale-check {
  font-size: 18px !important;
  color: #009688;
}

.body--dark .top-locale-check {
  color: #4db6ac;
}

/* ═══════════════ 主内容区 ═══════════════ */
.main-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 36px);
  overflow: hidden;
}

.page-content {
  flex: 1;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior-y: none;
  background: #f5f5f5;
}

.body--dark .page-content {
  background: #1a1a1a;
}

/* ═══════════════ 底部 Footer ═══════════════ */
.bottom-footer {
  height: 40px;
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.87);
  border-top: 1px solid #ececec;
}

.body--dark .bottom-footer {
  background: #1a1a1a;
  color: rgba(255, 255, 255, 0.87);
  border-top: 1px solid #2a2a2a;
}

.bottom-toolbar {
  min-height: 40px;
  padding: 0 20px;
  background: transparent;
  color: inherit;
}

.bottom-toolbar-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.bottom-toolbar-copyright {
  font-size: 11px;
  font-weight: 400;
  color: rgba(0, 0, 0, 0.45);
  line-height: 1.2;
}

.body--dark .bottom-toolbar-copyright {
  color: rgba(255, 255, 255, 0.45);
}

.bottom-toolbar-copyright :deep(a) {
  color: #009688;
  text-decoration: none;
}

.bottom-toolbar-copyright :deep(a:hover) {
  text-decoration: underline;
}
</style>

<!-- 非 scoped：tooltip 不换行 -->
<style>
.q-tooltip {
  white-space: nowrap;
}
</style>
