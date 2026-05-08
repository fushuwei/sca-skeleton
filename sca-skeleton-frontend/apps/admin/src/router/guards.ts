import type { NavigationGuardNext, RouteLocationNormalized, Router } from "vue-router"; // 导入路由守卫相关类型。
import { WHITE_LIST_ROUTE_NAMES } from "./routes"; // 导入白名单路由名称集合。
import { useAuthStore } from "../stores/auth"; // 导入鉴权仓库。
import { collectLeafMenuPaths } from "../utils/menu-tree"; // 导入叶子菜单路径收集工具。

function handleRedirectWhenNoAuth(to: RouteLocationNormalized, next: NavigationGuardNext): void { // 定义未登录跳转处理函数。
  const redirect = encodeURIComponent(to.fullPath); // 记录目标地址用于登录后回跳。
  next(`/login?redirect=${redirect}`); // 跳转到登录页并携带回跳参数。
} // 结束未登录跳转处理函数。

export function setupRouterGuards(router: Router): void { // 定义路由守卫安装函数。
  router.beforeEach(async (to, _from, next) => { // 注册全局前置路由守卫。
    const authStore = useAuthStore(); // 获取鉴权仓库实例。
    const routeName = String(to.name ?? ""); // 提取目标路由名称字符串。
    const isWhiteRoute = WHITE_LIST_ROUTE_NAMES.has(routeName); // 判断当前路由是否属于白名单。
    const leafPaths = collectLeafMenuPaths(authStore.menus); // 收集树形菜单下全部可访问叶子路径。
    const mayBeDynamicPath = leafPaths.includes(to.path); // 判断目标路径是否属于已授权动态菜单路径。

    if (!authStore.isLoggedIn && !isWhiteRoute) { // 判断未登录访问受保护页面场景。
      handleRedirectWhenNoAuth(to, next); // 执行未登录重定向逻辑。
      return; // 结束当前守卫流程。
    } // 结束未登录受保护页分支。

    if (authStore.isLoggedIn && routeName === "Login") { // 判断已登录用户再次访问登录页场景。
      next("/dashboard"); // 直接重定向到控制台页面。
      return; // 结束当前守卫流程。
    } // 结束已登录访问登录页分支。

    if (authStore.isLoggedIn) { // 判断当前是否已登录。
      authStore.ensureRoutes(router); // 确保动态路由已完成注册。
      if (!authStore.profile) { // 判断用户资料是否已加载。
        await authStore.fetchProfile(); // 未加载时拉取用户资料。
      } // 结束用户资料加载分支。
      if (routeName === "NotFound" && mayBeDynamicPath) { // 判断当前是否为动态菜单路径但尚未重新匹配成功。
        next({ path: to.fullPath, replace: true }); // 触发一次重新匹配以命中新注册的动态路由。
        return; // 结束当前守卫流程。
      } // 结束动态路由刷新修正分支。
    } // 结束已登录通用处理分支。

    next(); // 所有校验通过后放行路由跳转。
  }); // 结束全局前置守卫注册。
} // 结束路由守卫安装函数。
