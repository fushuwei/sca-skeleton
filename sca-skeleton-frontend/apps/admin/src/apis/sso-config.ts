import { request } from "./http";
import type { ApiEnvelope } from "../types/auth";
import type { CasConfig, Oauth2Config, SsoEnabledStatus, SsoProvider } from "../types/sso";

/**
 * 统一身份认证配置 API 契约。
 *
 * 后端约定：CAS / OAuth2 各为一条单例配置（realm=admin），GET 无数据时返回 data=null，
 * 前端据此回退到默认空表单。互斥启用由 setSsoEnabledApi 在服务端事务内保证。
 */

/** 查询 CAS 配置（不存在时 data 为 null，前端回退默认值） */
export async function getCasConfigApi(): Promise<ApiEnvelope<CasConfig | null>> {
  return request<CasConfig | null>({ method: "GET", url: "/sys/sso-config/cas" });
}

/** 保存 CAS 配置（新增或更新，configJson 载荷整体提交） */
export async function saveCasConfigApi(
  data: CasConfig
): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/sso-config/cas/save", data });
}

/** 查询 OAuth2/OIDC 配置（不存在时 data 为 null） */
export async function getOauth2ConfigApi(): Promise<ApiEnvelope<Oauth2Config | null>> {
  return request<Oauth2Config | null>({ method: "GET", url: "/sys/sso-config/oauth2" });
}

/** 保存 OAuth2/OIDC 配置 */
export async function saveOauth2ConfigApi(
  data: Oauth2Config
): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/sso-config/oauth2/save", data });
}

/** 查询两个提供方当前的启用状态（用于互斥启用前的确认提示） */
export async function getSsoEnabledStatusApi(): Promise<ApiEnvelope<SsoEnabledStatus>> {
  return request<SsoEnabledStatus>({ method: "GET", url: "/sys/sso-config/enabled-status" });
}

/**
 * 设置某提供方的启用状态。
 * 服务端保证互斥：enabled=true 时会禁用另一提供方。
 * confirmed 用于告知服务端「用户已确认覆盖另一启用方」，未确认且存在冲突时后端返回冲突错误码。
 */
export async function setSsoEnabledApi(
  provider: SsoProvider,
  enabled: boolean,
  confirmed: boolean
): Promise<ApiEnvelope<null>> {
  return request<null>({
    method: "POST",
    url: "/sys/sso-config/enabled",
    data: { provider, enabled, confirmed }
  });
}
