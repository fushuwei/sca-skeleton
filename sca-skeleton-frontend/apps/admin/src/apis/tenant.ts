import { request } from "./http";
import type { ApiEnvelope, SysTenant, TenantPageRequest, IPage } from "../types/auth";

/** 分页查询租户列表 */
export async function getTenantPageApi(
  params: TenantPageRequest
): Promise<ApiEnvelope<IPage<SysTenant>>> {
  return request<IPage<SysTenant>>({
    method: "GET",
    url: "/sys/tenant/page",
    params
  });
}

/** 查询全部租户列表 */
export async function getTenantListApi(): Promise<ApiEnvelope<SysTenant[]>> {
  return request<SysTenant[]>({ method: "GET", url: "/sys/tenant/list" });
}

/** 按 ID 查询租户详情 */
export async function getTenantByIdApi(id: string): Promise<ApiEnvelope<SysTenant>> {
  return request<SysTenant>({ method: "GET", url: `/sys/tenant/${id}` });
}

/** 创建租户 */
export async function createTenantApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/tenant/create", data });
}

/** 更新租户 */
export async function updateTenantApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/tenant/update", data });
}

/** 删除租户 */
export async function deleteTenantApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: `/sys/tenant/${id}/delete` });
}

/** 批量删除租户 */
export async function batchDeleteTenantApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/tenant/batch/delete", data: ids });
}
