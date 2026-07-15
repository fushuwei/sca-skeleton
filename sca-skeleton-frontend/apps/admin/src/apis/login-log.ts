import { request } from "./http";
import type { ApiEnvelope, SysLoginLog, LoginLogPageRequest, IPage } from "../types/auth";

/** 分页查询登录日志 */
export async function getLoginLogPageApi(
  params: LoginLogPageRequest
): Promise<ApiEnvelope<IPage<SysLoginLog>>> {
  return request<IPage<SysLoginLog>>({
    method: "GET",
    url: "/sys/login-log/page",
    params
  });
}

/** 根据 ID 查询登录日志详情 */
export async function getLoginLogByIdApi(id: string): Promise<ApiEnvelope<SysLoginLog>> {
  return request<SysLoginLog>({ method: "GET", url: `/sys/login-log/${id}` });
}

/** 批量删除登录日志 */
export async function batchDeleteLoginLogApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/login-log/batch/delete", data: ids });
}

/** 清空全部登录日志 */
export async function clearAllLoginLogApi(): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/login-log/clear" });
}
