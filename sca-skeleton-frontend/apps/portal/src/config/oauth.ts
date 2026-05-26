import { readOAuthConfigFromEnv } from "@repo/shared";
import type { OAuthAppConfig } from "@repo/shared";

export function getPortalOAuthConfig(): OAuthAppConfig {
  return readOAuthConfigFromEnv(import.meta.env);
}
