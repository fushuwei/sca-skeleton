import { request } from "./http";
import type { ApiEnvelope, SysConfig, ConfigPageRequest, IPage } from "../types/auth";

/** 分页查询当前租户下系统配置列表 */
export async function getConfigPageApi(
  params: ConfigPageRequest
): Promise<ApiEnvelope<IPage<SysConfig>>> {
  return request<IPage<SysConfig>>({
    method: "GET",
    url: "/sys/config/page",
    params
  });
}

/** 根据 ID 查询系统配置详情 */
export async function getConfigByIdApi(id: string): Promise<ApiEnvelope<SysConfig>> {
  return request<SysConfig>({ method: "GET", url: `/sys/config/${id}` });
}

/** 创建系统配置 */
export async function createConfigApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/config/create", data });
}

/** 更新系统配置 */
export async function updateConfigApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/config/update", data });
}

/** 启用/禁用系统配置（独立接口，仅更新状态，无需先查询详情） */
export async function updateConfigStatusApi(data: {
  id: string;
  status: string;
}): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/config/status", data });
}

/** 删除系统配置 */
export async function deleteConfigApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/config/delete", data: { id } });
}

/** 批量删除系统配置 */
export async function batchDeleteConfigApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/config/batch/delete", data: ids });
}
