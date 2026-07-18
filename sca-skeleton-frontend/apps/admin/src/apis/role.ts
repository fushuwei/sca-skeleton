import { request } from "./http";
import type { ApiEnvelope, SysRole, RoleOption, RolePageRequest, IPage } from "../types/auth";

/** 获取当前租户下角色列表（角色管理页面） */
export async function getRoleListApi(): Promise<ApiEnvelope<SysRole[]>> {
  return request<SysRole[]>({ method: "GET", url: "/sys/role/list" });
}

/**
 * 查询角色选项列表（用户管理表单下拉选择）
 * 返回最小化字段（id/name/code/sort），后端已剥离 dataScope/tenantId/isBuiltin/审计字段等。
 */
export async function getRoleOptionsApi(): Promise<ApiEnvelope<RoleOption[]>> {
  return request<RoleOption[]>({ method: "GET", url: "/sys/role/options" });
}

/** 分页查询当前租户下角色列表 */
export async function getRolePageApi(
  params: RolePageRequest
): Promise<ApiEnvelope<IPage<SysRole>>> {
  return request<IPage<SysRole>>({
    method: "GET",
    url: "/sys/role/page",
    params
  });
}

/** 根据 ID 查询角色详情 */
export async function getRoleByIdApi(id: string): Promise<ApiEnvelope<SysRole>> {
  return request<SysRole>({ method: "GET", url: `/sys/role/${id}` });
}

/** 查询角色已分配的权限 ID 列表 */
export async function getRolePermissionIdsApi(id: string): Promise<ApiEnvelope<string[]>> {
  return request<string[]>({ method: "GET", url: `/sys/role/${id}/permissions` });
}

/** 创建角色 */
export async function createRoleApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/role/create", data });
}

/** 更新角色 */
export async function updateRoleApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/role/update", data });
}

/** 删除角色 */
export async function deleteRoleApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/role/delete", data: { id } });
}

/** 批量删除角色 */
export async function batchDeleteRoleApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/role/batch/delete", data: ids });
}
