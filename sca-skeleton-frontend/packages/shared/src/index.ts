export const PROJECT_NAME = "sca-skeleton-frontend";

export { API_SUCCESS_CODE, API_UNAUTHORIZED_CODE } from "./oauth/constants";
export { createOAuthAxiosInstance, oauthRequest, isNotificationHandled } from "./oauth/axios-oauth";
export type { OAuthAxiosOptions } from "./oauth/axios-oauth";
export { showToast, setNotifier } from "./oauth/toast";
export type { NotificationType, Notifier } from "./oauth/toast";

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
