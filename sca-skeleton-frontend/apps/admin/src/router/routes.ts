import type { RouteRecordRaw } from "vue-router"; // 导入路由记录类型定义。
import AdminLayout from "../layouts/AdminLayout.vue"; // 导入后台主布局组件。
import LoginView from "../views/auth/LoginView.vue"; // 导入登录页面组件。
import NotFoundView from "../views/error/NotFoundView.vue"; // 导入 404 页面组件。
import DashboardView from "../views/dashboard/DashboardView.vue"; // 导入控制台页面（侧栏不展示工作台模块时仍静态挂载）。

export const WHITE_LIST_ROUTE_NAMES = new Set(["Login"]); // 定义白名单路由名称集合（仅登录页放行）。

export const staticRoutes: RouteRecordRaw[] = [ // 定义系统静态路由集合。
  { // 定义登录路由。
    path: "/login", // 声明登录页访问路径。
    name: "Login", // 声明登录路由名称。
    component: LoginView, // 绑定登录页面组件。
    meta: { public: true, title: "登录" } // 声明登录路由元信息。
  }, // 结束登录路由定义。
  { // 定义后台根布局路由。
    path: "/", // 声明后台根路径。
    name: "Root", // 声明根路由名称用于挂载动态子路由。
    component: AdminLayout, // 绑定后台布局组件。
    redirect: "/dashboard", // 默认重定向到控制台页面。
    meta: { requiresAuth: true, title: "后台" }, // 声明根路由需要鉴权。
    children: [
      {
        path: "dashboard",
        name: "ModuleWorkbench",
        component: DashboardView,
        meta: { requiresAuth: true, title: "工作台", icon: "sym_r_dashboard" }
      }
    ] // 工作台路由静态挂载；其余业务页由菜单驱动的动态路由注入。
  }, // 结束后台根布局路由定义。
  { // 定义 404 路由。
    path: "/:pathMatch(.*)*", // 声明兜底匹配路径。
    name: "NotFound", // 声明 404 路由名称。
    component: NotFoundView, // 绑定 404 页面组件。
    meta: { public: true, title: "页面不存在" } // 声明 404 路由元信息。
  } // 结束 404 路由定义。
]; // 结束静态路由集合定义。
