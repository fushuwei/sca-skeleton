import type { RouteRecordRaw } from "vue-router";
import PortalLayout from "../layouts/PortalLayout.vue";
import LoginView from "../views/auth/LoginView.vue";
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
    component: PortalLayout,
    redirect: "/portal/home",
    meta: { requiresAuth: true, title: "数据中台" },
    children: []
  },
  {
    path: "/:pathMatch(.*)*",
    name: "NotFound",
    component: NotFoundView,
    props: { homePath: "/portal/home" },
    meta: { public: true, title: "页面不存在" }
  }
];
