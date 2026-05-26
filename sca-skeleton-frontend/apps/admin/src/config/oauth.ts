import { readOAuthConfigFromEnv } from "@repo/shared";
import type { OAuthAppConfig } from "@repo/shared";

/** Admin SPA 的 OAuth2 公共客户端配置（来自 Vite 环境变量）。 */
export function getAdminOAuthConfig(): OAuthAppConfig {
  return readOAuthConfigFromEnv(import.meta.env);
}
