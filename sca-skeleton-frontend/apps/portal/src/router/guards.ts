import { createAuthGuard } from "@repo/shared";
import { getPortalOAuthConfig } from "../config/oauth";
import { WHITE_LIST_ROUTE_NAMES } from "./routes";
import { usePortalAuthStore } from "../stores/auth";
import { collectLeafMenuPaths } from "../utils/menu-tree";
import type { MenuItem } from "../types/auth";
import {
  REFRESH_TOKEN_STORAGE_KEY,
  TOKEN_STORAGE_KEY
} from "../constants/auth-storage";

const setupRouterGuards = createAuthGuard({
  useStore: usePortalAuthStore,
  getOAuthConfig: getPortalOAuthConfig,
  whiteListRouteNames: WHITE_LIST_ROUTE_NAMES,
  collectLeafMenuPaths: (menus) => collectLeafMenuPaths(menus as MenuItem[]),
  tokenStorageKey: TOKEN_STORAGE_KEY,
  refreshTokenStorageKey: REFRESH_TOKEN_STORAGE_KEY
});

export { setupRouterGuards };
