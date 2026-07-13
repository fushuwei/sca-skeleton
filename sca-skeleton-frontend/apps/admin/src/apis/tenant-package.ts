import { request } from "./http";
import type { ApiEnvelope, SysTenantPackage, TenantPackagePageRequest, IPage } from "../types/auth";

/** 分页查询套餐列表 */
export async function getTenantPackagePageApi(
  params: TenantPackagePageRequest
): Promise<ApiEnvelope<IPage<SysTenantPackage>>> {
  return request<IPage<SysTenantPackage>>({
    method: "GET",
    url: "/sys/tenant-package/page",
    params
  });
}

/** 查询全部套餐列表 */
export async function getTenantPackageListApi(): Promise<ApiEnvelope<SysTenantPackage[]>> {
  return request<SysTenantPackage[]>({ method: "GET", url: "/sys/tenant-package/list" });
}

/** 按 ID 查询套餐详情 */
export async function getTenantPackageByIdApi(id: string): Promise<ApiEnvelope<SysTenantPackage>> {
  return request<SysTenantPackage>({ method: "GET", url: `/sys/tenant-package/${id}` });
}

/** 查询套餐已分配的权限 ID 列表 */
export async function getTenantPackagePermissionIdsApi(id: string): Promise<ApiEnvelope<string[]>> {
  return request<string[]>({ method: "GET", url: `/sys/tenant-package/${id}/permissions` });
}

/** 创建套餐 */
export async function createTenantPackageApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/tenant-package/create", data });
}

/** 更新套餐 */
export async function updateTenantPackageApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/tenant-package/update", data });
}

/** 删除套餐 */
export async function deleteTenantPackageApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: `/sys/tenant-package/${id}/delete` });
}

/** 批量删除套餐 */
export async function batchDeleteTenantPackageApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/tenant-package/batch/delete", data: ids });
}
