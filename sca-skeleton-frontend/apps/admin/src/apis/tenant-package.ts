import { request } from "./http";
import type { ApiEnvelope, SysTenantPackage, TenantPackageOption, TenantPackagePageRequest, IPage } from "../types/auth";

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

/** 查询全部套餐列表（套餐管理页面） */
export async function getTenantPackageListApi(): Promise<ApiEnvelope<SysTenantPackage[]>> {
  return request<SysTenantPackage[]>({ method: "GET", url: "/sys/tenant-package/list" });
}

/**
 * 查询套餐选项列表（租户管理表单下拉选择）
 * 返回最小化字段（id/name/code/status/sort），后端已剥离 userLimit/apiLimit/storageLimit/expireDays/审计字段等。
 * 仅返回 status=enabled 的套餐。
 */
export async function getTenantPackageOptionsApi(): Promise<ApiEnvelope<TenantPackageOption[]>> {
  return request<TenantPackageOption[]>({ method: "GET", url: "/sys/tenant-package/options" });
}

/** 根据 ID 查询套餐详情 */
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
  return request<null>({ method: "POST", url: "/sys/tenant-package/delete", data: { id } });
}

/** 批量删除套餐 */
export async function batchDeleteTenantPackageApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/tenant-package/batch/delete", data: ids });
}
