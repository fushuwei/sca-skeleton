import { defineStore } from "pinia"; // 导入 Pinia 的仓库定义函数。
import type { Router } from "vue-router"; // 导入路由实例类型定义。
import { loginApi } from "../apis/auth"; // 导入登录接口函数。
import { getUserProfileApi } from "../apis/user"; // 导入用户信息接口函数。
import { ensureDynamicRoutes, resetDynamicRoutes } from "../router/dynamic"; // 导入动态路由注册与重置函数。
import type { LoginPayload, MenuItem, UserProfile } from "../types/auth"; // 导入鉴权与用户相关类型。

const TOKEN_STORAGE_KEY = "admin_token"; // 定义本地令牌存储键名。
const MENUS_STORAGE_KEY = "admin_menus"; // 定义本地菜单存储键名。

function readCachedMenus(): MenuItem[] { // 定义读取本地缓存菜单函数。
  const raw = localStorage.getItem(MENUS_STORAGE_KEY); // 从本地存储读取菜单字符串。
  if (!raw) { // 判断本地是否存在缓存菜单。
    return []; // 不存在时返回空数组。
  } // 结束无缓存分支。
  try { // 尝试解析缓存菜单 JSON。
    const parsed = JSON.parse(raw) as MenuItem[]; // 将 JSON 解析为菜单数组类型。
    return Array.isArray(parsed) ? parsed : []; // 仅在结果为数组时返回解析结果。
  } catch { // 捕获无效 JSON 导致的解析异常。
    return []; // 解析失败时返回空数组兜底。
  } // 结束缓存解析流程。
} // 结束读取本地缓存菜单函数。

interface AuthState { // 定义鉴权仓库状态类型。
  token: string; // 定义当前登录令牌状态字段。
  menus: MenuItem[]; // 定义当前菜单列表状态字段。
  profile: UserProfile | null; // 定义当前用户资料状态字段。
  dynamicReady: boolean; // 定义动态路由是否已完成注册的状态字段。
} // 结束鉴权仓库状态类型定义。

export const useAuthStore = defineStore("auth", { // 定义鉴权仓库。
  state: (): AuthState => ({ // 定义仓库状态初始化函数。
    token: localStorage.getItem(TOKEN_STORAGE_KEY) ?? "", // 初始化令牌状态并尝试从本地恢复。
    menus: readCachedMenus(), // 初始化菜单列表并尝试从本地恢复。
    profile: null, // 初始化用户资料为空。
    dynamicReady: false // 初始化动态路由状态为未准备完成。
  }), // 结束状态初始化定义。
  getters: { // 定义仓库派生状态。
    isLoggedIn: (state): boolean => Boolean(state.token) // 基于令牌是否存在计算登录状态。
  }, // 结束派生状态定义。
  actions: { // 定义仓库动作方法集合。
    async login(payload: LoginPayload): Promise<void> { // 定义登录动作函数。
      const response = await loginApi(payload); // 调用登录接口获取登录结果。
      this.token = response.data.token; // 将返回令牌写入状态。
      this.menus = response.data.menus; // 将返回菜单写入状态。
      localStorage.setItem(TOKEN_STORAGE_KEY, this.token); // 将令牌持久化到本地存储。
      localStorage.setItem(MENUS_STORAGE_KEY, JSON.stringify(this.menus)); // 将菜单持久化到本地存储。
      this.dynamicReady = false; // 登录后标记动态路由尚未注册。
    }, // 结束登录动作函数。
    async fetchProfile(): Promise<void> { // 定义获取用户资料动作函数。
      if (!this.token) { // 判断当前是否存在有效令牌。
        this.profile = null; // 无令牌时清空资料状态。
        return; // 直接返回避免无效请求。
      } // 结束无令牌分支。
      const response = await getUserProfileApi(); // 调用用户资料接口。
      this.profile = response.data; // 将用户资料写入状态。
    }, // 结束获取用户资料动作函数。
    ensureRoutes(router: Router): void { // 定义确保动态路由完成注册动作。
      if (!this.token || this.dynamicReady) { // 判断是否无需重复注册动态路由。
        return; // 无需处理时直接返回。
      } // 结束无需注册分支。
      ensureDynamicRoutes(router, this.menus); // 基于菜单注册动态路由。
      this.dynamicReady = true; // 标记动态路由已注册完成。
    }, // 结束动态路由注册动作。
    logout(router: Router): void { // 定义退出登录动作函数。
      this.token = ""; // 清空令牌状态。
      this.menus = []; // 清空菜单状态。
      this.profile = null; // 清空用户资料状态。
      this.dynamicReady = false; // 重置动态路由状态标记。
      localStorage.removeItem(TOKEN_STORAGE_KEY); // 清理本地令牌缓存。
      localStorage.removeItem(MENUS_STORAGE_KEY); // 清理本地菜单缓存。
      resetDynamicRoutes(router); // 移除已注册的动态路由。
    } // 结束退出登录动作函数。
  } // 结束仓库动作定义。
}); // 结束鉴权仓库定义。
