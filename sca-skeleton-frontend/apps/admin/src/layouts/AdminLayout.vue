<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useQuasar } from "quasar";
import { useI18n } from "vue-i18n";
import { FolderTree } from "@repo/ui";
import { useAuthStore } from "../stores/auth";
import { flattenRoutableMenus, getIconForMenuRouteName } from "../utils/menu-tree";
import { persistDark, persistLocale, quasarLangForLocale } from "../i18n";

const LOGO_URL = import.meta.env.BASE_URL + "images/logo.png";

/** 与 `.top-toolbar` 高度一致，用于用户菜单纵向对齐 */
const TOP_HEADER_HEIGHT_PX = 64;
/** 与 `.user-pill` 高度一致 */
const USER_PILL_HEIGHT_PX = 40;
/** 菜单锚点在胶囊底边，下移半栏差使菜单顶边与 Header 底边平齐 */
const userPillMenuOffset = [0, (TOP_HEADER_HEIGHT_PX - USER_PILL_HEIGHT_PX) / 2];

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const $q = useQuasar();
const { locale, t, te } = useI18n({ useScope: "global" });

const LEFT_DRAWER_WIDTH_MIN = 300;
const LEFT_DRAWER_WIDTH_MAX = 450;
const RIGHT_DRAWER_WIDTH_MIN = 430;
const RIGHT_DRAWER_WIDTH_MAX = 1000;

const leftDrawerOpen = ref(true);
const rightDrawerOpen = ref(false);
const leftDrawerWidth = ref(LEFT_DRAWER_WIDTH_MIN);
const rightDrawerWidth = ref(RIGHT_DRAWER_WIDTH_MIN);
const leftSearchVisible = ref(false);
const leftSearchKeyword = ref("");
const leftSearchInputRef = ref(null);
const activeRightDrawerTitle = ref(t("layout.genericPanel"));
const activeRightDrawerIcon = ref("sym_r_widgets");
/** true：右侧栏占用布局宽度挤压主区；false：overlay 浮动 */
const rightDrawerPinned = ref(false);
const expandedModuleKeys = ref([]);
const visitedTabs = ref([]);

/** 主区 Tab：工作台路由固定首位且不可关闭（实现上永不从列表移除） */
const WORKBENCH_PATH = "/dashboard";
const WORKBENCH_TAB_BASE = {
  path: WORKBENCH_PATH,
  icon: "sym_r_dashboard"
};

/** 侧栏不展示顶级模块（工作台由静态路由挂载，默认入口仍为 /dashboard） */
const SIDEBAR_HIDDEN_MODULE_NAMES = new Set(["ModuleWorkbench"]);
/** 侧栏手风琴顺序：数据资产在数据开发前，系统管理居末；未列出的模块排在约定项之后保持相对稳定 */
const SIDEBAR_MODULE_ORDER = ["ModuleDatasource", "ModuleDataAsset", "ModuleDataDev", "ModuleOps", "ModuleSystem"];

const username = computed(() => {
  void locale.value;
  return authStore.profile?.nickname ?? t("layout.defaultNickname");
});
const loginName = computed(() => authStore.profile?.username ?? t("layout.defaultLoginName"));
const realName = computed(() => {
  void locale.value;
  return authStore.profile?.nickname ?? t("layout.defaultRealName");
});
const email = computed(() => `${loginName.value}@example.com`);
const avatarInitial = computed(() => {
  const normalizedName = String(username.value ?? "").trim();
  if (!normalizedName) {
    return "U";
  }
  return [...normalizedName][0]?.toUpperCase() ?? "U";
});

/** 明亮主题下展示「进入黑暗」图标；黑暗主题下展示「回到明亮」图标。 */
const themeToggleIcon = computed(() => ($q.dark.isActive ? "sym_r_light_mode" : "sym_r_dark_mode"));

function toggleColorTheme() {
  $q.dark.toggle();
  persistDark($q.dark.isActive);
}

/** Header 语言菜单：同步 vue-i18n、`$q.lang` 与 localStorage。 */
function applyHeaderLocale(code) {
  if (locale.value === code) {
    return;
  }
  locale.value = code;
  $q.lang.set(quasarLangForLocale(code));
  persistLocale(code);
}

function translateMenuItemTitle(item) {
  const key = `menu.${item.name}`;
  if (item?.name && te(key)) {
    return t(key);
  }
  return item.title;
}

/** 将后端菜单树节点转为 Quasar QTree 节点（name 作 node-key，与动态路由 name 一致） */
function mapMenuToTreeNode(item) {
  if (item.children?.length) {
    return {
      label: translateMenuItemTitle(item),
      name: item.name,
      selectable: false,
      children: item.children.map(mapMenuToTreeNode)
    };
  }
  return {
    label: translateMenuItemTitle(item),
    name: item.name
  };
}

/** 顶层手风琴对应一棵树：有 children 则展开一层；单页叶子则树内仅一行 */
function menuToTreeNodes(topItem) {
  if (topItem.children?.length) {
    return topItem.children.map(mapMenuToTreeNode);
  }
  return [mapMenuToTreeNode(topItem)];
}

function filterTreeNodesByKeyword(nodes, keyword) {
  if (!keyword) {
    return nodes;
  }
  const result = [];
  for (const n of nodes) {
    const labelMatch = n.label.toLowerCase().includes(keyword);
    if (n.children?.length) {
      if (labelMatch) {
        result.push({ ...n });
        continue;
      }
      const nextChildren = filterTreeNodesByKeyword(n.children, keyword);
      if (nextChildren.length) {
        result.push({ ...n, children: nextChildren });
      }
    } else if (labelMatch) {
      result.push(n);
    }
  }
  return result;
}

function menusOrderedForSidebar(menus) {
  const filtered = menus.filter((item) => !SIDEBAR_HIDDEN_MODULE_NAMES.has(item.name));
  const rank = (name) => {
    const index = SIDEBAR_MODULE_ORDER.indexOf(name);
    return index === -1 ? SIDEBAR_MODULE_ORDER.length : index;
  };
  return [...filtered].sort((a, b) => rank(a.name) - rank(b.name));
}

/** 当前路由是否落在该模块树下（用于手风琴「含当前页」强调） */
function moduleTreeContainsActiveRoute(nodes, activeName) {
  if (!activeName) {
    return false;
  }
  for (const n of nodes) {
    if (n.name === activeName) {
      return true;
    }
    if (n.children?.length && moduleTreeContainsActiveRoute(n.children, activeName)) {
      return true;
    }
  }
  return false;
}

function setModuleExpanded(moduleKey, expanded) {
  const keys = expandedModuleKeys.value;
  if (expanded) {
    if (!keys.includes(moduleKey)) {
      expandedModuleKeys.value = [...keys, moduleKey];
    }
  } else {
    expandedModuleKeys.value = keys.filter((k) => k !== moduleKey);
  }
}

const menuModules = computed(() => {
  void locale.value;
  return menusOrderedForSidebar(authStore.menus).map((item) => ({
    key: item.name,
    title: translateMenuItemTitle(item),
    icon: item.icon ?? "sym_r_folder",
    treeNodes: menuToTreeNodes(item)
  }));
});

const filteredModules = computed(() => {
  const keyword = leftSearchKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return menuModules.value;
  }
  return menuModules.value
    .map((mod) => {
      const titleMatch = mod.title.toLowerCase().includes(keyword);
      const nodes = filterTreeNodesByKeyword(mod.treeNodes, keyword);
      if (!titleMatch && nodes.length === 0) {
        return null;
      }
      return {
        ...mod,
        treeNodes: titleMatch ? mod.treeNodes : nodes
      };
    })
    .filter(Boolean);
});

const activeTabPath = computed(() => route.path);
const activeRouteName = computed(() => (route.name != null ? String(route.name) : ""));

/** keep-alive 缓存白名单：仅当前 Tab 栏中存在的路由组件会被缓存，关闭 Tab 即销毁组件实例 */
const keepAliveInclude = computed(() => {
  const leaves = flattenRoutableMenus(authStore.menus);
  const names = new Set();
  // 工作台始终缓存
  names.add("DashboardView");
  for (const tab of visitedTabs.value) {
    const leaf = leaves.find((l) => l.path === tab.path);
    if (leaf?.component) {
      names.add(leaf.component);
    }
  }
  return [...names];
});

/**
 * 仅当当前路由落在侧栏菜单树内时才显示为选中（工作台等不在树中则为「无选中」）。
 * 注意：Quasar QTree 用 `selected !== undefined` 判断是否启用选择模式；若传 `undefined`，
 * 所有叶子 selectable 为 false，点击不会 emit，路由与 Tab 都不会更新。无选中须传 `null`。
 */
const sidebarTreeSelectedName = computed(() => {
  const name = activeRouteName.value;
  if (!name) {
    return null;
  }
  for (const mod of menuModules.value) {
    if (moduleTreeContainsActiveRoute(mod.treeNodes, name)) {
      return name;
    }
  }
  return null;
});

function normalizeVisitedTabs(tabs) {
  const rest = tabs.filter((tab) => tab.path !== WORKBENCH_PATH);
  const workbench = {
    ...WORKBENCH_TAB_BASE,
    title: t("layout.workbench")
  };
  return [workbench, ...rest];
}

function tabTitleFromRoute(routeLike) {
  const path = routeLike.path ?? "";
  const name = routeLike.name != null ? String(routeLike.name) : "";
  if (path === WORKBENCH_PATH || name === "ModuleWorkbench") {
    return t("layout.workbench");
  }
  const menuKey = `menu.${name}`;
  if (name && te(menuKey)) {
    return t(menuKey);
  }
  const raw = routeLike.meta?.title;
  if (raw != null && String(raw).trim() !== "") {
    return String(raw);
  }
  return t("layout.unnamedPage");
}

/** 优先 `route.meta.icon`（动态路由按菜单**叶子**自身 icon 写入）；否则按 name 仅解析该菜单项 icon，不继承手风琴模块图标 */
function tabIconForRoute(routeLike) {
  const path = routeLike.path ?? "";
  const name = routeLike.name != null ? String(routeLike.name) : "";
  if (path === WORKBENCH_PATH || name === "ModuleWorkbench") {
    const m = routeLike.meta?.icon;
    return typeof m === "string" && m.trim() !== "" ? m : WORKBENCH_TAB_BASE.icon;
  }
  const fromMeta = routeLike.meta?.icon;
  if (typeof fromMeta === "string" && fromMeta.trim() !== "") {
    return fromMeta;
  }
  return getIconForMenuRouteName(authStore.menus, name);
}

const mainTabsRef = ref(null);
const tabScrollOverflow = ref(false);
const tabScrollShowLeft = ref(false);
const tabScrollShowRight = ref(false);

let tabScrollTeardown = null;

function getTabsContentEl() {
  const root = mainTabsRef.value?.$el;
  if (!root || typeof root.querySelector !== "function") {
    return null;
  }
  return root.querySelector(".q-tabs__content");
}

function updateTabScrollArrows() {
  const content = getTabsContentEl();
  if (!content) {
    tabScrollOverflow.value = false;
    tabScrollShowLeft.value = false;
    tabScrollShowRight.value = false;
    return;
  }

  const { scrollLeft, scrollWidth, clientWidth } = content;
  const maxScroll = Math.max(0, scrollWidth - clientWidth);
  const overflow = scrollWidth > clientWidth + 1;

  tabScrollOverflow.value = overflow;
  if (!overflow) {
    tabScrollShowLeft.value = false;
    tabScrollShowRight.value = false;
    return;
  }

  tabScrollShowLeft.value = scrollLeft > 2;
  tabScrollShowRight.value = scrollLeft < maxScroll - 2;
}

function scrollMainTabs(direction) {
  const content = getTabsContentEl();
  if (!content) {
    return;
  }
  const step = Math.min(200, Math.max(120, Math.round(content.clientWidth * 0.45)));
  content.scrollBy({ left: direction * step, behavior: "smooth" });
  window.setTimeout(updateTabScrollArrows, 340);
}

function bindMainTabsScroll() {
  tabScrollTeardown?.();
  tabScrollTeardown = null;

  nextTick(() => {
    const content = getTabsContentEl();
    const root = mainTabsRef.value?.$el;
    if (!content || !root) {
      updateTabScrollArrows();
      return;
    }

    const onScroll = () => updateTabScrollArrows();
    content.addEventListener("scroll", onScroll, { passive: true });

    const ro = new ResizeObserver(() => updateTabScrollArrows());
    ro.observe(root);
    ro.observe(content);

    tabScrollTeardown = () => {
      content.removeEventListener("scroll", onScroll);
      ro.disconnect();
    };

    updateTabScrollArrows();
  });
}

onMounted(() => {
  bindMainTabsScroll();
});

watch(
  () => visitedTabs.value.length,
  () => bindMainTabsScroll()
);

watch(
  () => route.path,
  () => {
    const currentPath = route.path;
    if (currentPath === "/login") {
      return;
    }

    let tabs = [...visitedTabs.value];

    if (!tabs.some((tab) => tab.path === WORKBENCH_PATH)) {
      tabs = [{ ...WORKBENCH_TAB_BASE, title: t("layout.workbench") }, ...tabs];
    }

    const exists = tabs.some((tab) => tab.path === currentPath);
    if (!exists) {
      tabs.push({
        path: currentPath,
        title: tabTitleFromRoute(route),
        icon: tabIconForRoute(route)
      });
    }

    visitedTabs.value = normalizeVisitedTabs(tabs);
    nextTick(updateTabScrollArrows);
  },
  { immediate: true }
);

watch(locale, () => {
  visitedTabs.value = normalizeVisitedTabs(
    visitedTabs.value.map((tab) => ({
      ...tab,
      title: tabTitleFromRoute(router.resolve(tab.path))
    }))
  );
  nextTick(updateTabScrollArrows);
});

function handleMenuClick(path = "") {
  if (!path) {
    return;
  }
  router.push(path);
}

function handleTabChange(path) {
  if (path && path !== route.path) {
    router.push(path);
  }
}

/** 关闭已访问 Tab；工作台固定首位且不可关闭。关闭当前页时切换到左侧相邻 Tab */
function closeVisitedTab(tabPath) {
  if (tabPath === WORKBENCH_PATH) {
    return;
  }

  const oldTabs = visitedTabs.value;
  const closeIdx = oldTabs.findIndex((t) => t.path === tabPath);
  if (closeIdx === -1) {
    return;
  }

  const newTabs = normalizeVisitedTabs(oldTabs.filter((t) => t.path !== tabPath));
  visitedTabs.value = newTabs;

  if (route.path !== tabPath) {
    nextTick(updateTabScrollArrows);
    return;
  }

  const nextIdx = closeIdx > 0 ? closeIdx - 1 : 0;
  const target = newTabs[nextIdx];
  if (target) {
    router.push(target.path);
  }
  nextTick(updateTabScrollArrows);
}

function handleTreeSelect(routeName) {
  if (!routeName) {
    return;
  }
  const name = String(routeName);
  if (name === activeRouteName.value) {
    return;
  }
  router.push({ name });
}

function expandAllModules() {
  expandedModuleKeys.value = menuModules.value.map((module) => module.key);
}

function collapseAllModules() {
  expandedModuleKeys.value = [];
}

function openLeftSearchToolbar() {
  leftSearchVisible.value = true;
}

function closeLeftSearchToolbar() {
  leftSearchVisible.value = false;
  leftSearchKeyword.value = "";
}

let leftSearchEscHandler = null;

/** 在滑入动画结束后再聚焦，避免 Safari 在 transform 过渡期间 focus 输入框导致主线程卡顿 */
function focusLeftSearchAfterEnter() {
  leftSearchInputRef.value?.focus?.();
}

watch(leftSearchVisible, (visible) => {
  if (leftSearchEscHandler) {
    window.removeEventListener("keydown", leftSearchEscHandler);
    leftSearchEscHandler = null;
  }
  if (visible) {
    leftSearchEscHandler = (e) => {
      if (e.key === "Escape") {
        closeLeftSearchToolbar();
      }
    };
    window.addEventListener("keydown", leftSearchEscHandler);
  }
});

onUnmounted(() => {
  tabScrollTeardown?.();
  tabScrollTeardown = null;
  if (leftSearchEscHandler) {
    window.removeEventListener("keydown", leftSearchEscHandler);
  }
  window.removeEventListener("pointermove", onLeftDrawerResizeMove, POINTER_CAPTURE_OPTS);
  window.removeEventListener("pointerup", endLeftDrawerResize, POINTER_CAPTURE_OPTS);
  window.removeEventListener("pointercancel", endLeftDrawerResize, POINTER_CAPTURE_OPTS);
  window.removeEventListener("pointermove", onRightDrawerResizeMove, POINTER_CAPTURE_OPTS);
  window.removeEventListener("pointerup", endRightDrawerResize, POINTER_CAPTURE_OPTS);
  window.removeEventListener("pointercancel", endRightDrawerResize, POINTER_CAPTURE_OPTS);
  document.body.style.cursor = "";
  document.body.style.userSelect = "";
});

function openRightDrawer(title, icon) {
  activeRightDrawerTitle.value = title;
  activeRightDrawerIcon.value = icon;
  rightDrawerOpen.value = true;
}

function closeRightDrawer() {
  rightDrawerOpen.value = false;
}

function rightDrawerWidthToMin() {
  rightDrawerWidth.value = RIGHT_DRAWER_WIDTH_MIN;
}

function rightDrawerWidthToMax() {
  rightDrawerWidth.value = RIGHT_DRAWER_WIDTH_MAX;
}

function toggleRightDrawerPinned() {
  rightDrawerPinned.value = !rightDrawerPinned.value;
}

/** 桌面端 overlay 右侧抽屉无独立 backdrop 可点；点击中间主区（Tab + 内容）时收起 */
function closeRightDrawerOnMainClick() {
  if (rightDrawerPinned.value) {
    return;
  }
  if (performance.now() < suppressRightDrawerCloseUntil) {
    return;
  }
  if (rightDrawerOpen.value) {
    closeRightDrawer();
  }
}

async function handleLogout() {
  await authStore.logout(router);
}

function clampDrawerWidth(n, min, max) {
  return Math.min(max, Math.max(min, Math.round(n)));
}

/** 窄条拖拽时指针易离开元素；用 capture 阶段挂在 window 上跟指点事件 */
const POINTER_CAPTURE_OPTS = { capture: true };

let leftDrawerResizeStartX = 0;
let leftDrawerResizeStartWidth = 0;

function onLeftDrawerResizeMove(e) {
  leftDrawerWidth.value = clampDrawerWidth(
    leftDrawerResizeStartWidth + (e.clientX - leftDrawerResizeStartX),
    LEFT_DRAWER_WIDTH_MIN,
    LEFT_DRAWER_WIDTH_MAX
  );
}

function endLeftDrawerResize() {
  window.removeEventListener("pointermove", onLeftDrawerResizeMove, POINTER_CAPTURE_OPTS);
  window.removeEventListener("pointerup", endLeftDrawerResize, POINTER_CAPTURE_OPTS);
  window.removeEventListener("pointercancel", endLeftDrawerResize, POINTER_CAPTURE_OPTS);
  document.body.style.cursor = "";
  document.body.style.userSelect = "";
}

function beginLeftDrawerResize(e) {
  e.preventDefault();
  leftDrawerResizeStartX = e.clientX;
  leftDrawerResizeStartWidth = leftDrawerWidth.value;
  document.body.style.cursor = "ew-resize";
  document.body.style.userSelect = "none";
  window.addEventListener("pointermove", onLeftDrawerResizeMove, POINTER_CAPTURE_OPTS);
  window.addEventListener("pointerup", endLeftDrawerResize, POINTER_CAPTURE_OPTS);
  window.addEventListener("pointercancel", endLeftDrawerResize, POINTER_CAPTURE_OPTS);
}

let rightDrawerResizeStartX = 0;
let rightDrawerResizeStartWidth = 0;
/** 拖拽结束若在主区释放，紧随其后的 click 会误触关闭抽屉 */
let suppressRightDrawerCloseUntil = 0;

function onRightDrawerResizeMove(e) {
  rightDrawerWidth.value = clampDrawerWidth(
    rightDrawerResizeStartWidth + (rightDrawerResizeStartX - e.clientX),
    RIGHT_DRAWER_WIDTH_MIN,
    RIGHT_DRAWER_WIDTH_MAX
  );
}

function endRightDrawerResize() {
  window.removeEventListener("pointermove", onRightDrawerResizeMove, POINTER_CAPTURE_OPTS);
  window.removeEventListener("pointerup", endRightDrawerResize, POINTER_CAPTURE_OPTS);
  window.removeEventListener("pointercancel", endRightDrawerResize, POINTER_CAPTURE_OPTS);
  document.body.style.cursor = "";
  document.body.style.userSelect = "";
  suppressRightDrawerCloseUntil = performance.now() + 400;
}

function beginRightDrawerResize(e) {
  e.preventDefault();
  rightDrawerResizeStartX = e.clientX;
  rightDrawerResizeStartWidth = rightDrawerWidth.value;
  document.body.style.cursor = "ew-resize";
  document.body.style.userSelect = "none";
  window.addEventListener("pointermove", onRightDrawerResizeMove, POINTER_CAPTURE_OPTS);
  window.addEventListener("pointerup", endRightDrawerResize, POINTER_CAPTURE_OPTS);
  window.addEventListener("pointercancel", endRightDrawerResize, POINTER_CAPTURE_OPTS);
}
</script>

<template>
  <q-layout view="hHh Lpr fFf" class="admin-shell" :dark="$q.dark.isActive">
    <q-header bordered class="top-header">
      <q-toolbar class="top-toolbar">
        <div class="top-brand row items-center no-wrap">
          <img :src="LOGO_URL" alt="logo" class="top-logo">
          <span class="top-brand-text">{{ t('layout.brandTitle') }}</span>
        </div>
        <q-space />
        <div class="top-actions">
          <q-btn
            flat
            round
            icon="sym_r_search"
            class="top-action-btn"
            @click="openRightDrawer(t('layout.globalSearch'), 'sym_r_search')"
          />
          <q-btn
            flat
            round
            icon="sym_r_notifications"
            class="top-action-btn"
            @click="openRightDrawer(t('layout.notifications'), 'sym_r_notifications')"
          />
          <q-btn
            flat
            round
            icon="sym_r_delete"
            class="top-action-btn"
            @click="openRightDrawer(t('layout.recycleBin'), 'sym_r_delete')"
          />
          <q-btn
            flat
            round
            icon="sym_r_translate"
            class="top-action-btn"
            :aria-label="t('layout.languageMenuAria')"
          >
            <q-menu
              class="admin-locale-q-menu"
              anchor="bottom middle"
              self="top middle"
              :offset="userPillMenuOffset"
              :dark="$q.dark.isActive"
              transition-show="jump-down"
              transition-hide="jump-up"
            >
              <q-list dense class="top-locale-menu-list">
                <q-item
                  v-close-popup
                  clickable
                  class="top-locale-menu-item"
                  @click="applyHeaderLocale('en-US')"
                >
                  <q-item-section avatar class="top-locale-flag">🇺🇸</q-item-section>
                  <q-item-section>{{ t('layout.localeEnglish') }}</q-item-section>
                  <q-item-section v-if="locale === 'en-US'" side>
                    <q-icon name="sym_r_check" class="top-locale-check" />
                  </q-item-section>
                </q-item>
                <q-item
                  v-close-popup
                  clickable
                  class="top-locale-menu-item"
                  @click="applyHeaderLocale('zh-CN')"
                >
                  <q-item-section avatar class="top-locale-flag">🇨🇳</q-item-section>
                  <q-item-section>{{ t('layout.localeChinese') }}</q-item-section>
                  <q-item-section v-if="locale === 'zh-CN'" side>
                    <q-icon name="sym_r_check" class="top-locale-check" />
                  </q-item-section>
                </q-item>
              </q-list>
            </q-menu>
          </q-btn>
          <q-btn
            flat
            round
            :icon="themeToggleIcon"
            class="top-action-btn"
            :aria-label="t('layout.themeToggleAria')"
            @click="toggleColorTheme"
          />
          <q-btn
            flat
            round
            icon="sym_r_settings"
            class="top-action-btn"
            @click="openRightDrawer(t('layout.settings'), 'sym_r_settings')"
          />
        </div>
        <q-btn-dropdown
          flat
          class="user-pill btn-shape-exempt"
          dropdown-icon="sym_r_arrow_drop_down"
          content-class="admin-user-q-menu"
          no-caps
          :menu-offset="userPillMenuOffset"
        >
          <template #label>
            <div class="row items-center no-wrap user-pill-label">
              <span class="user-avatar-circle">
                {{ avatarInitial }}
              </span>
              <span>{{ username }}</span>
            </div>
          </template>
          <div class="q-pa-md user-menu-header">
            <span class="user-menu-avatar">{{ avatarInitial }}</span>
            <div class="user-menu-meta column justify-center">
              <div class="user-menu-realname">{{ realName }}</div>
              <div class="user-menu-email">{{ email }}</div>
            </div>
          </div>
          <q-separator />
          <q-list dense class="user-menu-action-list">
            <q-item clickable @click="handleMenuClick('/system/user')">
              <q-item-section>{{ t('layout.personalCenter') }}</q-item-section>
            </q-item>
            <q-item clickable @click="openRightDrawer(t('layout.changePassword'), 'sym_r_key')">
              <q-item-section>{{ t('layout.changePassword') }}</q-item-section>
            </q-item>
            <q-item clickable @click="handleLogout">
              <q-item-section class="text-negative">{{ t('layout.logout') }}</q-item-section>
            </q-item>
          </q-list>
        </q-btn-dropdown>
      </q-toolbar>
    </q-header>

    <q-drawer
      v-model="leftDrawerOpen"
      show-if-above
      side="left"
      bordered
      :width="leftDrawerWidth"
      :mini="false"
      class="left-drawer"
    >
      <div class="left-toolbar">
        <div class="left-toolbar-stage">
          <div v-if="!leftSearchVisible" class="left-toolbar-default">
            <div class="row items-center no-wrap left-toolbar-brand">
              <q-icon name="sym_r_explore" size="20px" class="left-toolbar-brand-icon" />
              <span class="left-toolbar-title">{{ t('layout.navTitle') }}</span>
            </div>
            <div class="row items-center no-wrap left-toolbar-actions">
              <q-btn flat round icon="sym_r_expand" class="left-toolbar-action-btn" @click="expandAllModules" />
              <q-btn flat round icon="sym_r_compress" class="left-toolbar-action-btn" @click="collapseAllModules" />
              <q-btn flat round icon="sym_r_search" class="left-toolbar-action-btn" @click="openLeftSearchToolbar" />
            </div>
          </div>
          <Transition name="left-search-slide" @after-enter="focusLeftSearchAfterEnter">
            <div v-if="leftSearchVisible" class="left-toolbar-search-layer row items-center no-wrap">
              <q-input
                ref="leftSearchInputRef"
                v-model="leftSearchKeyword"
                dense
                outlined
                :placeholder="t('layout.searchMenuPlaceholder')"
                class="left-toolbar-search-input"
                hide-bottom-space
              />
              <q-btn
                flat
                round
                icon="sym_r_close"
                class="left-toolbar-action-btn left-toolbar-search-close"
                :aria-label="t('layout.closeSearchAria')"
                @click="closeLeftSearchToolbar"
                @pointerdown.prevent="closeLeftSearchToolbar"
              />
            </div>
          </Transition>
        </div>
      </div>
      <q-scroll-area class="left-menu-scroll">
        <q-list padding class="left-menu-list">
          <q-expansion-item
            v-for="module in filteredModules"
            :key="module.key"
            dense-toggle
            expand-separator
            class="left-menu-expansion"
            :class="{ 'left-menu-expansion--active-module': moduleTreeContainsActiveRoute(module.treeNodes, sidebarTreeSelectedName || '') }"
            :icon="module.icon"
            :label="module.title"
            :model-value="expandedModuleKeys.includes(module.key)"
            @update:model-value="(expanded) => setModuleExpanded(module.key, expanded)"
          >
            <FolderTree
              :nodes="module.treeNodes"
              node-key="name"
              label-key="label"
              children-key="children"
              :selected="sidebarTreeSelectedName"
              no-connectors
              @update:selected="handleTreeSelect"
            />
          </q-expansion-item>
        </q-list>
      </q-scroll-area>
      <div
        class="left-drawer-resize-handle"
        aria-hidden="true"
        @pointerdown.prevent="beginLeftDrawerResize"
      />
    </q-drawer>

    <q-drawer
      v-model="rightDrawerOpen"
      side="right"
      bordered
      :width="rightDrawerWidth"
      :overlay="!rightDrawerPinned"
      class="right-drawer"
    >
      <div class="right-drawer-stack">
        <div class="right-drawer-toolbar row items-center no-wrap">
          <q-icon :name="activeRightDrawerIcon" size="20px" class="right-drawer-toolbar-icon" />
          <span class="right-drawer-toolbar-title ellipsis">{{ activeRightDrawerTitle }}</span>
          <q-space />
          <div class="right-drawer-toolbar-trailing row items-center no-wrap">
            <q-btn
              flat
              round
              dense
              :icon="rightDrawerPinned ? 'sym_r_keep_off' : 'sym_r_keep'"
              class="right-drawer-toolbar-action-btn"
              :aria-label="rightDrawerPinned ? t('layout.unpinDrawerAria') : t('layout.pinDrawerAria')"
              @click="toggleRightDrawerPinned"
            />
            <q-btn
              flat
              round
              dense
              icon="sym_r_check_indeterminate_small"
              class="right-drawer-toolbar-action-btn"
              :aria-label="t('layout.minimizeDrawerAria')"
              @click="rightDrawerWidthToMin"
            />
            <q-btn
              flat
              round
              dense
              icon="sym_r_check_box_outline_blank"
              class="right-drawer-toolbar-action-btn"
              :aria-label="t('layout.maximizeDrawerAria')"
              @click="rightDrawerWidthToMax"
            />
            <q-btn
              flat
              round
              dense
              icon="sym_r_close"
              class="right-drawer-toolbar-action-btn"
              :aria-label="t('layout.closeDrawerAria')"
              @click="closeRightDrawer"
            />
          </div>
        </div>
        <div class="right-drawer-body q-pa-md">
          <div class="text-body2 right-drawer-placeholder">{{ t('layout.rightDrawerPlaceholder') }}</div>
        </div>
      </div>
      <!-- 与 stack 并列，避免被包在 overflow 内；定位参照见 :deep(.right-drawer .q-drawer__content) -->
      <div
        class="right-drawer-resize-handle"
        aria-hidden="true"
        @pointerdown.prevent="beginRightDrawerResize"
      />
    </q-drawer>

    <q-page-container>
      <q-page class="main-page">
        <div class="main-page-stack" @click="closeRightDrawerOnMainClick">
          <div class="tab-shell">
            <button
              v-if="tabScrollOverflow"
              type="button"
              class="tab-scroll-arrow tab-scroll-arrow--left"
              :class="{ 'tab-scroll-arrow--disabled': !tabScrollShowLeft }"
              :disabled="!tabScrollShowLeft"
              :aria-label="t('layout.tabScrollLeftAria')"
              @click.stop="scrollMainTabs(-1)"
            >
              <q-icon name="sym_r_chevron_left" size="20px" class="tab-scroll-arrow__sym" />
            </button>
            <!-- Quasar 默认 breakpoint=600：中间区变窄且 Tab 无需横向滚动时会强制 justify，单 Tab 会被 flex 拉满 -->
            <q-tabs
              ref="mainTabsRef"
              :model-value="activeTabPath"
              dense
              align="left"
              :breakpoint="0"
              inline-label
              indicator-color="primary"
              class="main-tabs"
              @update:model-value="handleTabChange"
            >
              <q-tab v-for="tab in visitedTabs" :key="tab.path" :name="tab.path" no-caps class="main-tab">
                <div class="main-tab-row row no-wrap items-center">
                  <q-icon :name="tab.icon ?? 'sym_r_nest_eco_leaf'" class="main-tab-row__icon" />
                  <span class="main-tab-row__label">{{ tab.title }}</span>
                  <q-btn
                    v-if="tab.path !== WORKBENCH_PATH"
                    flat
                    round
                    dense
                    icon="sym_r_close"
                    size="sm"
                    class="main-tab-close-btn"
                    :aria-label="t('layout.tabCloseAria')"
                    @click.stop.prevent="closeVisitedTab(tab.path)"
                  />
                </div>
              </q-tab>
            </q-tabs>
            <button
              v-if="tabScrollOverflow"
              type="button"
              class="tab-scroll-arrow tab-scroll-arrow--right"
              :class="{ 'tab-scroll-arrow--disabled': !tabScrollShowRight }"
              :disabled="!tabScrollShowRight"
              :aria-label="t('layout.tabScrollRightAria')"
              @click.stop="scrollMainTabs(1)"
            >
              <q-icon name="sym_r_chevron_right" size="20px" class="tab-scroll-arrow__sym" />
            </button>
          </div>
          <div class="page-content q-pa-sm">
            <router-view v-slot="{ Component, route: routeForKeepAlive }">
              <keep-alive :include="keepAliveInclude">
                <component :is="Component" :key="routeForKeepAlive.path" />
              </keep-alive>
            </router-view>
          </div>
        </div>
      </q-page>
    </q-page-container>

    <q-footer bordered class="bottom-footer">
      <q-toolbar class="bottom-toolbar bottom-toolbar--footer-grid">
        <q-btn
          flat
          round
          dense
          class="bottom-drawer-toggle"
          :icon="leftDrawerOpen ? 'sym_r_left_panel_close' : 'sym_r_left_panel_open'"
          @click="leftDrawerOpen = !leftDrawerOpen"
        />
        <div class="bottom-toolbar-meta">{{ t('layout.versionLabel') }}</div>
        <div class="bottom-toolbar-footer-actions row items-center no-wrap">
          <q-btn
            flat
            round
            dense
            icon="sym_r_database"
            class="bottom-footer-icon-btn"
            aria-label="Database"
            @click="openRightDrawer(t('layout.database'), 'sym_r_database')"
          />
          <q-btn
            flat
            round
            dense
            icon="sym_r_history"
            class="bottom-footer-icon-btn"
            aria-label="History"
            @click="openRightDrawer(t('layout.history'), 'sym_r_history')"
          />
        </div>
      </q-toolbar>
    </q-footer>
  </q-layout>
</template>

<style scoped>
.top-header {
  background: #009688;
  color: #fff;
}

.top-toolbar {
  min-height: 64px;
}

.top-brand {
  width: 360px;
}

.top-logo {
  width: 44px;
  height: 44px;
  object-fit: contain;
}

.top-brand-text {
  margin-left: 12px;
  font-size: 18px;
  font-weight: 600;
}

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
  background: rgba(128, 128, 128, 0.28);
}

.top-locale-menu-list {
  min-width: 220px;
}

.top-locale-menu-item {
  box-sizing: border-box;
  min-height: 40px;
  height: 40px;
  padding-top: 0;
  padding-bottom: 0;
}

.top-locale-flag {
  min-width: 32px;
  font-size: 20px;
  line-height: 1;
  font-style: normal;
}

.top-locale-check {
  font-size: 18px !important;
}

.right-drawer-placeholder {
  color: rgba(0, 0, 0, 0.62);
}

.user-pill {
  margin-left: 8px;
  height: 40px;
  min-height: 40px;
  padding: 0 8px 0 4px;
  border: 1px solid rgba(255, 255, 255, 0.52);
  border-radius: 999px !important;
  color: #fff;
  overflow: hidden;
}

.user-pill:hover {
  background: rgba(128, 128, 128, 0.28);
}

.user-pill-label {
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  line-height: 1;
}

.user-avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.28);
  color: #ffffff;
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  text-transform: uppercase;
}

.user-menu-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #009688;
  color: #ffffff;
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
  min-width: 250px;
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

.user-menu-email {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 400;
  line-height: 1.35;
  color: rgba(0, 0, 0, 0.6);
  word-break: break-all;
}

.user-menu-action-list :deep(.q-item) {
  box-sizing: border-box;
  min-height: 40px;
  height: 40px;
}

.left-toolbar {
  height: 44px;
  flex-shrink: 0;
  background: #f5f5f5;
  border-bottom: 1px solid #ececec;
  padding: 0 8px;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
}

.left-toolbar-stage {
  position: relative;
  height: 100%;
  width: 100%;
}

.left-toolbar-default {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  width: 100%;
  box-sizing: border-box;
}

.left-toolbar-brand {
  flex: 1 1 auto;
  min-width: 0;
  margin-right: 8px;
}

.left-toolbar-brand-icon {
  flex-shrink: 0;
  color: rgba(0, 0, 0, 0.87);
  font-size: 20px !important;
}

/* 栏高 44px：圆形触控区留上下留白；图标 20px，覆盖全局 .material-symbols-rounded 的 24px !important；操作钮之间不加 gap */
.left-toolbar-action-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.87);
  border-radius: 50%;
}

.left-toolbar-action-btn :deep(.q-btn__wrapper) {
  min-height: 32px;
  padding: 0;
}

.left-toolbar-action-btn :deep(.q-icon.material-symbols-rounded),
.left-toolbar-action-btn :deep(.material-symbols-rounded) {
  font-size: 20px !important;
}

.left-toolbar-action-btn:hover {
  background: rgba(128, 128, 128, 0.28);
}

.left-toolbar-title {
  margin-left: 5px;
  font-size: 14px;
  font-weight: 600;
}

.left-toolbar-search-layer {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  gap: 6px;
  padding-left: 0;
  padding-right: 0;
  background: #f5f5f5;
  z-index: 1;
}

.left-toolbar-search-input {
  flex: 1 1 auto;
  min-width: 0;
  width: 0;
}

.left-toolbar-search-close {
  flex-shrink: 0;
}

.left-toolbar-search-input :deep(.q-field__control) {
  min-height: 34px;
  height: 34px;
}

.left-toolbar-search-input :deep(.q-field__marginal) {
  height: 34px;
}

.left-search-slide-enter-active,
.left-search-slide-leave-active {
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.left-search-slide-enter-from,
.left-search-slide-leave-to {
  transform: translate3d(100%, 0, 0);
}

.left-search-slide-enter-to,
.left-search-slide-leave-from {
  transform: translate3d(0, 0, 0);
}

/* 右侧抽屉：顶栏与左侧导航 toolbar 同级视觉（44px / #f5f5f5）；正文区单独滚动 */
:deep(aside.right-drawer) {
  overflow: hidden !important;
  display: flex;
  flex-direction: column;
}

/* Quasar 将默认槽包在单个子 div 内；并列 stack + resize 手柄的定位参照须在此层 */
:deep(aside.right-drawer .q-drawer__content) {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.right-drawer-stack {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  width: 100%;
  box-sizing: border-box;
}

/* 拖拽改宽：热区悬停 east-west；不改变 aside 的 position（保持 Quasar fixed） */
.left-drawer-resize-handle {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 6px;
  z-index: 6000;
  cursor: ew-resize;
  touch-action: none;
}

.right-drawer-resize-handle {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 6px;
  z-index: 10000;
  cursor: ew-resize;
  touch-action: none;
}

.right-drawer-toolbar {
  width: 100%;
  height: 44px;
  min-height: 44px;
  flex-shrink: 0;
  box-sizing: border-box;
  overflow: hidden;
  padding: 0 8px;
  background: #f5f5f5;
  border-bottom: 1px solid #ececec;
}

/* Quasar `q-icon` 用 Webfont 时 `.material-symbols-rounded` 在根节点上，非子节点；须在本类上 `!important` 压全局 24px */
.right-drawer-toolbar-icon {
  flex-shrink: 0;
  color: rgba(0, 0, 0, 0.87);
  font-size: 20px !important;
}

.right-drawer-toolbar-title {
  flex: 1 1 auto;
  min-width: 0;
  margin-left: 5px;
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.87);
}

.right-drawer-toolbar-trailing {
  flex-shrink: 0;
  gap: 4px;
}

.right-drawer-toolbar-action-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.87);
  border-radius: 50%;
}

.right-drawer-toolbar-action-btn :deep(.q-btn__wrapper) {
  min-height: 32px;
  padding: 0;
}

.right-drawer-toolbar-action-btn :deep(.q-icon.material-symbols-rounded),
.right-drawer-toolbar-action-btn :deep(.material-symbols-rounded) {
  font-size: 20px !important;
}

.right-drawer-toolbar-action-btn:hover {
  background: rgba(128, 128, 128, 0.28);
}

.right-drawer-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}

/*
 * 侧栏仅保留「单一滚动层」：QDrawer 的 .q-drawer__content 默认带 .scroll（overflow:auto），
 * 会与内层 q-scroll-area 形成双滚动容器，工具栏也可能被卷走。这里关掉外层滚动、用 flex 把高度交给 q-scroll-area。
 * 视口级橡皮筋由全局 layout-overscroll.scss 处理，此处不再重复写 overscroll-behavior。
 */
:deep(.left-drawer.q-drawer__content) {
  overflow: hidden !important;
  display: flex;
  flex-direction: column;
}

/* 侧栏列表：覆盖 Quasar padding 工具类，与手风琴贴边对齐 */
.left-menu-list.q-list--padding {
  padding: 0 !important;
}

.left-menu-scroll {
  flex: 1 1 auto;
  min-height: 0;
  touch-action: pan-y;
}

.left-menu-scroll :deep(.q-scrollarea__container.scroll) {
  touch-action: pan-y;
  overscroll-behavior-y: none;
}

.main-page {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 64px - 30px);
}

.main-page-stack {
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  min-height: 0;
}

.tab-shell {
  box-sizing: border-box;
  width: 100%;
  height: 44px;
  min-height: 44px;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  background: #f5f5f5;
  border-bottom: 1px solid #ececec;
  overflow: hidden;
}

.tab-scroll-arrow {
  flex: 0 0 36px;
  width: 36px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  align-self: stretch;
  margin: 0;
  padding: 0;
  border: none;
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.65);
  cursor: pointer;
  transition: background-color 0.15s ease, opacity 0.15s ease;
}

.tab-scroll-arrow:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.05);
}

.tab-scroll-arrow--disabled,
.tab-scroll-arrow:disabled {
  opacity: 0.35;
  cursor: default;
}

.tab-scroll-arrow__sym {
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-scroll-arrow__sym.material-symbols-rounded {
  font-size: 20px !important;
}

.main-tabs {
  flex: 1 1 auto;
  min-width: 0;
  height: 100%;
  background: #f5f5f5;
  box-sizing: border-box;
}

/* 禁用 Quasar 内置箭头，避免与自定义滚动钮错位；横向滚动仍由 .q-tabs__content 承担 */
.main-tabs :deep(.q-tabs__arrow) {
  display: none !important;
}

.main-tabs :deep(.q-tabs__content) {
  overscroll-behavior-x: contain;
}

.main-tabs :deep(.q-tab) {
  box-sizing: border-box;
  min-height: 44px;
  padding: 0 10px;
}

.main-tabs :deep(.q-tab__content) {
  padding-top: 0;
  padding-bottom: 0;
  min-width: 0;
}

.main-tab-row {
  gap: 6px;
  min-width: 0;
  max-width: 100%;
}

.main-tabs :deep(.main-tab-row__icon) {
  flex-shrink: 0;
  font-size: 18px !important;
  width: 1em;
  height: 1em;
}

.main-tabs :deep(.main-tab-row__label) {
  flex: 1 1 auto;
  min-width: 0;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.main-tabs :deep(.main-tab-close-btn) {
  flex-shrink: 0;
  margin-right: -4px;
  color: inherit;
  opacity: 0.55;
}

.main-tabs :deep(.main-tab-close-btn:hover) {
  opacity: 1;
}

.main-tabs :deep(.main-tab-close-btn .q-icon) {
  font-size: 16px !important;
}

/*
 * Quasar 的 active-bg-color / active-color 只能传调色板名（生成 bg-* / text-*），不能传 #hex；
 * 选中态背景与字色用样式显式实现，与侧栏树选中、Header #009688 同系。
 */
.main-tabs :deep(.q-tab--inactive) {
  color: rgba(0, 0, 0, 0.82);
}

.main-tabs :deep(.q-tab--active:not(.disabled)) {
  background-color: #e5f2f0 !important;
  color: #00796b !important;
  font-weight: 600;
}

.main-tabs :deep(.q-tab--active:not(.disabled) .main-tab-row__icon),
.main-tabs :deep(.q-tab--active:not(.disabled) .main-tab-row__label),
.main-tabs :deep(.q-tab--active:not(.disabled) .main-tab-close-btn) {
  color: inherit !important;
}

.main-tabs :deep(.q-tab--active:not(.disabled) .main-tab-close-btn) {
  opacity: 0.75;
}

.main-tabs :deep(.q-tab--active:not(.disabled) .main-tab-close-btn:hover) {
  opacity: 1;
}

.main-tabs :deep(.q-tab__indicator) {
  height: 3px;
}

.page-content {
  flex: 1;
  overflow-x: auto;
  overflow-y: auto;
  overscroll-behavior-x: contain;
  overscroll-behavior-y: none;
  background: #fafafa;
}

.bottom-footer {
  height: 40px;
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.87);
}

.bottom-toolbar {
  min-height: 40px;
  padding: 0 8px;
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.87);
}

.bottom-toolbar--footer-grid {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
}

.bottom-toolbar-meta {
  justify-self: center;
  text-align: center;
  font-size: 12px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
  line-height: 1.2;
}

.bottom-toolbar-footer-actions {
  gap: 4px;
  justify-self: end;
}

/* 避免继承布局/主题的浅色字；Material Symbols 图标走 currentColor */
.bottom-drawer-toggle,
.bottom-footer-icon-btn {
  color: rgba(0, 0, 0, 0.87) !important;
}

.bottom-drawer-toggle :deep(.q-icon.material-symbols-rounded),
.bottom-drawer-toggle :deep(.material-symbols-rounded),
.bottom-footer-icon-btn :deep(.q-icon.material-symbols-rounded),
.bottom-footer-icon-btn :deep(.material-symbols-rounded) {
  font-size: 20px !important;
}

/* —— 侧栏手风琴 —— */
.left-menu-expansion {
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.left-menu-expansion :deep(.q-expansion-item__container) {
  border-radius: 0;
}

/* 模块图标列宽度（非右侧展开箭头）；图标距左侧由表头 .q-item 的 padding-left 统一控制 */
.left-menu-expansion :deep(.q-item > .q-item__section--avatar) {
  min-width: 46px;
}

/* 手风琴标题行前置模块图标：`q-icon` 字形类在根节点 */
.left-menu-expansion :deep(.q-item > .q-item__section--avatar .q-icon) {
  font-size: 20px !important;
}

/* 标题行：默认白底；悬停浅青雾面 #E5F2F0（与 Header #009688 同系、更浅）；展开时略加重 */
.left-menu-expansion :deep(.q-item) {
  min-height: 44px;
  padding: 6px 10px 6px 18px;
  background: #fff;
  color: rgba(0, 0, 0, 0.87);
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.18s ease, box-shadow 0.18s ease;
}

.left-menu-expansion :deep(.q-item:hover) {
  background: #e5f2f0;
}

/*
 * Quasar：`q-expansion-item--expanded` 与用户类在同一根节点上，不能使用「祖先 .left-menu-expansion 包住后代 .q-expansion-item--expanded」写法。
 */
.left-menu-expansion.q-expansion-item--expanded :deep(.q-expansion-item__container > .q-item) {
  background: #e5f2f0 !important;
}

.left-menu-expansion.q-expansion-item--expanded :deep(.q-expansion-item__container > .q-item:hover) {
  background: #d8ebe8 !important;
}

/*
 * 模块内含当前路由：仅用左侧竖条区分，背景与「仅展开」及树下选中行同为 #e5f2f0，避免点选菜单后标题忽然变色。
 */
.left-menu-expansion--active-module :deep(> .q-expansion-item__container > .q-item) {
  box-shadow: inset 4px 0 0 #009688;
}

/* 手风琴内容区：无单独背景色 */
.left-menu-expansion :deep(.q-expansion-item__content) {
  background: transparent;
}

/*
 * 菜单行左右 gutter=12px；嵌套 margin-left: calc(12px - I)、width: calc(100% + I - 24px)（24=2×12）。
 * 分支/叶子文案对齐由 @repo/ui 的 FolderTree（文件夹按钮 + 等宽占位）在 header 槽内处理。
 */
.left-menu-expansion :deep(.q-tree) {
  color: rgba(0, 0, 0, 0.55);
  padding: 1px 0 4px;
}

.left-menu-expansion :deep(.q-tree .q-tree__node) {
  padding-bottom: 0 !important;
}

.left-menu-expansion :deep(.q-tree__children) {
  padding-left: 18px;
}

.left-menu-expansion :deep(.q-tree__node-header) {
  margin-top: 2px;
  margin-right: 12px;
  padding: 8px 12px;
  min-height: 40px;
  border-radius: 8px;
  box-sizing: border-box;
}

.left-menu-expansion :deep(.q-tree > .q-tree__node > .q-tree__node-header) {
  margin-left: 12px;
}

.left-menu-expansion :deep(.q-tree__children .q-tree__node > .q-tree__node-header) {
  margin-left: calc(12px - 18px);
  width: calc(100% + 18px - 24px);
}

.left-menu-expansion :deep(.q-tree__children .q-tree__children .q-tree__node > .q-tree__node-header) {
  margin-left: calc(12px - 36px);
  width: calc(100% + 36px - 24px);
}

.left-menu-expansion :deep(.q-tree__children .q-tree__children .q-tree__children .q-tree__node > .q-tree__node-header) {
  margin-left: calc(12px - 54px);
  width: calc(100% + 54px - 24px);
}

.left-menu-expansion :deep(.q-tree__children .q-tree__children .q-tree__children .q-tree__children .q-tree__node > .q-tree__node-header) {
  margin-left: calc(12px - 72px);
  width: calc(100% + 72px - 24px);
}

.left-menu-expansion :deep(.q-tree__node-header-content) {
  font-size: 14px;
  line-height: 1.4;
  color: rgba(0, 0, 0, 0.82);
}

.left-menu-expansion :deep(.q-tree__node-header:hover) {
  background: rgba(0, 0, 0, 0.04);
}

.left-menu-expansion :deep(.q-tree__node-header.q-tree__node--selected) {
  background: #e5f2f0 !important;
}

.left-menu-expansion :deep(.q-tree__node-header.q-tree__node--selected .q-tree__node-header-content) {
  color: #00796b;
  font-weight: 600;
}

.left-menu-expansion :deep(.q-tree__node-header.q-tree__node--selected:hover) {
  background: #d8ebe8 !important;
}
</style>
