import { request } from "./http";
import type { ApiEnvelope, SysRole, RoleOption, RolePageRequest, PermissionAssignOption, IPage } from "../types/auth";

/** 获取当前租户下角色列表（角色管理页面） */
export async function getRoleListApi(): Promise<ApiEnvelope<SysRole[]>> {
  return request<SysRole[]>({ method: "GET", url: "/sys/role/list" });
}

/**
 * 查询角色授权面板可分配权限列表
 *
 * 后端已做两层防护：
 * 1. 字段最小化：仅返回 id/parentId/name/nameEn/type/realm/icon/sort，不含 path/component/code/treePath 等敏感字段
 * 2. 越权防护：超管按当前租户套餐过滤，非超管仅返回当前用户自身拥有的权限
 *
 * 前端无需再做任何过滤，直接信任后端数据。
 *
 * @param realm 权限域（admin/portal），按角色域过滤可分配的权限，为空则返回所有
 */
export async function getRoleAssignOptionsApi(realm?: string): Promise<ApiEnvelope<PermissionAssignOption[]>> {
  return request<PermissionAssignOption[]>({ method: "GET", url: "/sys/role/assign-options", params: { realm } });
}

/**
 * 查询角色选项列表（用户管理表单下拉选择，当前租户下的角色）
 * 返回最小化字段（id/name/code/sort/realm），后端已剥离 dataScope/tenantId/isBuiltin/审计字段等。
 * @param realm 可选，按角色域过滤（admin/portal）
 */
export async function getRoleOptionsApi(realm?: string): Promise<ApiEnvelope<RoleOption[]>> {
  return request<RoleOption[]>({ method: "GET", url: "/sys/role/options", params: { realm } });
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

/** 查询角色自定义数据权限的部门 ID 列表 */
export async function getRoleDeptIdsApi(id: string): Promise<ApiEnvelope<string[]>> {
  return request<string[]>({ method: "GET", url: `/sys/role/${id}/dept-ids` });
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
