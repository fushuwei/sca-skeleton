import { createRouter, createWebHistory } from "vue-router"; // 导入路由创建函数与历史模式函数。
import { staticRoutes } from "./routes"; // 导入静态路由定义集合。
import { setupRouterGuards } from "./guards"; // 导入路由守卫安装函数。

export const router = createRouter({
  // 创建并导出 admin 路由实例。
  history: createWebHistory("/admin/"), // 配置 HTML5 history 路由模式（/admin/ 子路径下）。
  routes: staticRoutes // 注入静态路由作为初始路由表。
}); // 结束路由实例创建。

setupRouterGuards(router); // 安装全局路由守卫。
