import { defineStore } from "pinia";
import type { Router } from "vue-router";
import { getAdminOAuthConfig } from "../config/oauth";
import { getUserProfileApi } from "../apis/user";
import { getUserMenusApi } from "../apis/permission";
import { ensureDynamicRoutes } from "../router/dynamic";
import {
  MENUS_STORAGE_KEY,
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";
import type { MenuItem, UserProfile, SysPermission } from "../types/auth";

function readCachedMenus(): MenuItem[] {
  const raw = localStorage.getItem(MENUS_STORAGE_KEY);
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw) as MenuItem[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

/** 将后端返回的扁平权限列表转换为前端树形菜单（仅 module/folder/menu） */
function buildMenuTree(permissions: SysPermission[]): MenuItem[] {
  if (!permissions.length) return [];

  // 按 type 过滤：只保留 module、folder、menu（按钮权限不参与菜单树构建）
  const filtered = permissions.filter(p => ["module", "folder", "menu"].includes(p.type));

  // 按 sort 排序
  const sorted = [...filtered].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));

  // 构建 map
  const map = new Map<string, MenuItem>();
  for (const p of sorted) {
    map.set(p.id, {
      id: p.id,
      name: p.name ?? "",
      nameEn: p.nameEn || undefined,
      path: p.path ?? "",
      component: p.component as MenuItem["component"] ?? undefined,
      icon: p.icon ?? undefined,
      children: []
    });
  }

  // 构建树
  const roots: MenuItem[] = [];
  for (const p of sorted) {
    const node = map.get(p.id)!;
    if (!p.parentId || p.parentId === "0") {
      roots.push(node);
    } else {
      const parent = map.get(p.parentId);
      if (parent) {
        parent.children = parent.children ?? [];
        parent.children.push(node);
      } else {
        roots.push(node);
      }
    }
  }

  // 清理空 children
  const cleanEmpty = (nodes: MenuItem[]) => {
    for (const n of nodes) {
      if (n.children?.length) {
        cleanEmpty(n.children);
      } else {
        delete n.children;
      }
    }
  };
  cleanEmpty(roots);

  return roots;
}

interface AuthState {
  token: string;
  refreshToken: string;
  /** 当前用户所有权限（含 button），用于权限校验 */
  permissions: SysPermission[];
  /** 菜单树（仅 module/folder/menu），用于侧栏展示 */
  menus: MenuItem[];
  profile: UserProfile | null;
  dynamicReady: boolean;
}

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    token: localStorage.getItem(TOKEN_STORAGE_KEY) ?? "",
    refreshToken: localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY) ?? "",
    permissions: [],
    menus: readCachedMenus(),
    profile: null,
    dynamicReady: false
  }),
  getters: {
    isLoggedIn: (state): boolean => Boolean(state.token)
  },
  actions: {
    /** OAuth2 PKCE 回调成功后写入令牌并加载菜单。 */
    async applyOAuthTokens(accessToken: string, refreshToken?: string): Promise<void> {
      this.token = accessToken;
      this.refreshToken = refreshToken ?? "";
      localStorage.setItem(TOKEN_STORAGE_KEY, this.token);
      if (refreshToken) {
        localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
      }
      // 从后端获取用户权限
      try {
        const result = await getUserMenusApi();
        if (result.code === 10_000 && result.data) {
          this.permissions = result.data;
          this.menus = buildMenuTree(result.data);
        } else {
          this.permissions = [];
          this.menus = [];
        }
      } catch {
        this.permissions = [];
        this.menus = [];
      }
      localStorage.setItem(MENUS_STORAGE_KEY, JSON.stringify(this.menus));
      this.dynamicReady = false;
    },
    /** Axios 静默 refresh 成功后同步 Pinia 内存态。
     *  refreshToken 为空时也需重置，确保 clearTokens 后 Pinia 与 localStorage 一致。 */
    syncOAuthTokens(accessToken: string, refreshToken?: string): void {
      this.token = accessToken;
      this.refreshToken = refreshToken ?? "";
    },
    async fetchProfile(): Promise<void> {
      if (!this.token) {
        this.profile = null;
        return;
      }
      const response = await getUserProfileApi();
      this.profile = response.data;
    },
    ensureRoutes(router: Router): void {
      if (!this.token || this.dynamicReady) {
        return;
      }
      ensureDynamicRoutes(router, this.menus);
      this.dynamicReady = true;
    },
    /** 退出：前端仅调用统一退出端点，认证服务完成会话与令牌清理。 */
    async logout(router: Router): Promise<void> {
      const oauthConfig = getAdminOAuthConfig();
      const accessToken = this.token;
      const refreshToken = this.refreshToken;

      // 仅清除持久化状态，不清内存（form.submit 会触发浏览器导航，页面销毁后内存自然释放；
      // 提前清空 menus / profile 会导致侧栏和头部在跳转前闪现空白/回退文案）
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
      localStorage.removeItem(MENUS_STORAGE_KEY);

      const logoutUrl = oauthConfig.authorizeUrl.replace("/oauth2/authorize", "/logout");
      const form = document.createElement("form");
      form.method = "POST";
      form.action = logoutUrl;
      form.style.display = "none";
      const appendInput = (name: string, value: string) => {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = name;
        input.value = value;
        form.appendChild(input);
      };
      appendInput("channel", "admin");
      if (accessToken) {
        appendInput("access_token", accessToken);
      }
      if (refreshToken) {
        appendInput("refresh_token", refreshToken);
      }
      document.body.appendChild(form);
      form.submit();
    }
  }
});
