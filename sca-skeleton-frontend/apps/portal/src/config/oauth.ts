import { readOAuthConfigFromEnv } from "@repo/shared";
import type { OAuthAppConfig } from "@repo/shared";

/** Portal SPA 的 OAuth2 机密客户端配置（来自 Vite 环境变量）。 */
export function getPortalOAuthConfig(): OAuthAppConfig {
  return {
    ...readOAuthConfigFromEnv(import.meta.env),
    basePath: import.meta.env.BASE_URL
  };
}
