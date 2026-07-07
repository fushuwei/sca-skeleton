import type { RouteRecordRaw, Router } from "vue-router"; // 导入路由类型与路由实例类型。
import type { MenuComponent, MenuItem } from "../types/auth"; // 导入菜单类型定义。
import { flattenRoutableMenus, getIconForMenuRouteName } from "../utils/menu-tree"; // 导入菜单树工具。

const MENU_COMPONENT_MAP: Record<MenuComponent, () => Promise<unknown>> = { // 定义菜单组件映射表。
  DashboardView: () => import("../views/dashboard/DashboardView.vue"), // 将 Dashboard 菜单映射到控制台页面。
  UserCenterView: () => import("../views/system/UserCenterView.vue"), // 将 UserCenter 菜单映射到用户中心页面。
  UserListView: () => import("../views/system/UserListView.vue"), // 将 UserList 菜单映射到用户列表页面。
  MenuListView: () => import("../views/system/MenuListView.vue"), // 将 MenuList 菜单映射到菜单管理页面。
  RoleListView: () => import("../views/system/RoleListView.vue"), // 将 RoleList 菜单映射到角色管理页面。
  PostListView: () => import("../views/system/PostListView.vue"), // 将 PostList 菜单映射到岗位管理页面。
  DeptListView: () => import("../views/system/DeptListView.vue"), // 将 DeptList 菜单映射到部门管理页面。
  PlaceholderView: () => import("../views/common/PlaceholderView.vue") // 通用占位页。
}; // 结束菜单组件映射表定义。

const dynamicRouteNameSet = new Set<string>(); // 定义已注册动态路由名称集合，避免重复注入。

function buildDynamicRoutes(menus: MenuItem[]): RouteRecordRaw[] {
  const leaves = flattenRoutableMenus(menus);
  return leaves.map((menu) => ({
    path: menu.path.replace(/^\//, ""),
    name: menu.component,
    component: MENU_COMPONENT_MAP[menu.component!],
    meta: {
      requiresAuth: true,
      title: menu.name,
      icon: getIconForMenuRouteName(menus, menu.component!)
    }
  }));
}

export function ensureDynamicRoutes(router: Router, menus: MenuItem[]): void { // 定义动态路由注册函数。
  const dynamicRoutes = buildDynamicRoutes(menus); // 基于菜单构建动态路由列表。

  for (const route of dynamicRoutes) { // 遍历每个动态路由进行注册。
    const routeName = String(route.name); // 提取当前路由名称并转为字符串。
    if (!dynamicRouteNameSet.has(routeName)) { // 判断当前路由是否已注册。
      router.addRoute("Root", route); // 将动态路由挂载到 Root 布局下。
      dynamicRouteNameSet.add(routeName); // 记录已注册路由名称。
    } // 结束未注册路由分支。
  } // 结束动态路由遍历注册流程。
} // 结束动态路由注册函数。

export function resetDynamicRoutes(router: Router): void { // 定义动态路由重置函数。
  for (const routeName of dynamicRouteNameSet) { // 遍历所有已注册的动态路由名称。
    if (router.hasRoute(routeName)) { // 判断路由实例中是否仍存在该路由。
      router.removeRoute(routeName); // 从路由实例中移除动态路由。
    } // 结束已存在路由分支。
  } // 结束动态路由移除循环。
  dynamicRouteNameSet.clear(); // 清空已注册动态路由记录集合。
} // 结束动态路由重置函数。
