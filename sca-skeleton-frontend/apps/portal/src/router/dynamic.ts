import type { RouteRecordRaw, Router } from "vue-router";
import type { MenuComponent, MenuItem } from "../types/auth";
import { flattenRoutableMenus, getIconForMenuRouteName } from "../utils/menu-tree";

/**
 * 菜单组件映射表：DB component 字段 → Vue 视图组件懒加载。
 *
 * 与 admin 的差异：portal 视图组件放在 views/portal/<Component>.vue。
 */
const MENU_COMPONENT_MAP: Record<MenuComponent, () => Promise<unknown>> = {
  HomeView: () => import("../views/portal/HomeView.vue"),
  DataMapView: () => import("../views/portal/DataMapView.vue"),
  ResourceCatalogView: () => import("../views/portal/ResourceCatalogView.vue"),
  DataStandardView: () => import("../views/portal/DataStandardView.vue"),
  AiQueryView: () => import("../views/portal/AiQueryView.vue"),
  DataMarketView: () => import("../views/portal/DataMarketView.vue"),
  DataSubmitView: () => import("../views/portal/DataSubmitView.vue"),
  ProfileView: () => import("../views/portal/ProfileView.vue"),
  MyRequestView: () => import("../views/portal/MyRequestView.vue"),
  MyDownloadView: () => import("../views/portal/MyDownloadView.vue"),
  MyFavoriteView: () => import("../views/portal/MyFavoriteView.vue"),
  NotificationView: () => import("../views/portal/NotificationView.vue"),
  AppIntegrationView: () => import("../views/portal/AppIntegrationView.vue")
};

const dynamicRouteNameSet = new Set<string>();

function buildDynamicRoutes(menus: MenuItem[]): RouteRecordRaw[] {
  const leaves = flattenRoutableMenus(menus);
  return leaves.map((menu) => ({
    // 菜单 path 为绝对路径（如 /portal/home），动态路由需用相对路径挂到 Root 下
    path: menu.path.replace(/^\//, ""),
    name: menu.id,
    component: MENU_COMPONENT_MAP[menu.component!],
    meta: {
      requiresAuth: true,
      title: menu.name,
      /** 英文标题（DB name_en），用于页面标题国际化 */
      titleEn: menu.nameEn,
      icon: menu.icon || getIconForMenuRouteName(menus, menu.id)
    }
  }));
}

export function ensureDynamicRoutes(router: Router, menus: MenuItem[]): void {
  const dynamicRoutes = buildDynamicRoutes(menus);
  for (const route of dynamicRoutes) {
    const routeName = String(route.name);
    if (!dynamicRouteNameSet.has(routeName)) {
      router.addRoute("Root", route);
      dynamicRouteNameSet.add(routeName);
    }
  }
}

export function resetDynamicRoutes(router: Router): void {
  for (const routeName of dynamicRouteNameSet) {
    if (router.hasRoute(routeName)) {
      router.removeRoute(routeName);
    }
  }
  dynamicRouteNameSet.clear();
}
