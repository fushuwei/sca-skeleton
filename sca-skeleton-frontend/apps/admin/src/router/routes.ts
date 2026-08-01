import type { RouteRecordRaw } from "vue-router";
import AdminLayout from "../layouts/AdminLayout.vue";
import LoginView from "../views/auth/LoginView.vue";
import DashboardView from "../views/dashboard/DashboardView.vue";
import { NotFoundView } from "@repo/ui";

/** 无需登录即可访问的路由名称 */
export const WHITE_LIST_ROUTE_NAMES = new Set(["Login"]);

export const staticRoutes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: LoginView,
    meta: { public: true, title: "登录" }
  },
  {
    path: "/",
    name: "Root",
    component: AdminLayout,
    redirect: "/dashboard",
    meta: { requiresAuth: true, title: "后台" },
    children: [
      {
        path: "dashboard",
        name: "ModuleWorkbench",
        component: DashboardView,
        meta: { requiresAuth: true, title: "工作台", icon: "sym_r_dashboard" }
      }
    ]
  },
  {
    path: "/:pathMatch(.*)*",
    name: "NotFound",
    component: NotFoundView,
    props: { homePath: "/dashboard" },
    meta: { public: true, title: "页面不存在" }
  }
];
