import type { RouteRecordRaw } from "vue-router";
import AdminLayout from "../layouts/AdminLayout.vue";
import OAuthCallbackView from "../views/auth/OAuthCallbackView.vue";
import NotFoundView from "../views/error/NotFoundView.vue";
import DashboardView from "../views/dashboard/DashboardView.vue";

/** 无需登录即可访问的路由名称（OAuth 回调页） */
export const WHITE_LIST_ROUTE_NAMES = new Set(["OAuthCallback"]);

export const staticRoutes: RouteRecordRaw[] = [
  {
    path: "/oauth/callback",
    name: "OAuthCallback",
    component: OAuthCallbackView,
    meta: { public: true, title: "登录回调" }
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
    meta: { public: true, title: "页面不存在" }
  }
];
