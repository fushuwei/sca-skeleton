export const PROJECT_NAME = "sca-skeleton-frontend";

export { API_SUCCESS_CODE, API_UNAUTHORIZED_CODE } from "./oauth/constants";
export { createOAuthAxiosInstance, oauthRequest, isNotificationHandled, isTransientNetworkError } from "./oauth/axios-oauth";
export type { OAuthAxiosOptions } from "./oauth/axios-oauth";
export { showToast, setNotifier } from "./oauth/toast";
export type { NotificationType, Notifier } from "./oauth/toast";

export {
  shouldRetryTransient,
  markRetry,
  resetRetry,
  delay,
  MAX_TRANSIENT_RETRIES,
  RETRY_DELAY_MS
} from "./router/guard-utils";
export type { TransientRetryState } from "./router/guard-utils";
export { createAuthGuard } from "./router/auth-guard";
export type { AuthGuardStore, AuthGuardConfig } from "./router/auth-guard";

export {
  loginWithPassword,
  refreshAccessToken,
  revokeOAuthToken,
  readOAuthConfigFromEnv
} from "./oauth/password-grant";

export type {
  OAuthAppConfig,
  OAuthTokenResponse,
  ApiResult
} from "./oauth/password-grant";
