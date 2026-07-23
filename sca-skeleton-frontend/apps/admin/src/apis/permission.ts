import { request } from "./http";
import type { ApiEnvelope, SysPermission, PermissionAssignOption, PermissionPageRequest, IPage } from "../types/auth";

/** 查询全部权限列表（权限管理页面，返回全量数据含 disabled 权限） */
export async function getPermissionListApi(realm?: string): Promise<ApiEnvelope<SysPermission[]>> {
  return request<SysPermission[]>({ method: "GET", url: "/sys/permission/list", params: { realm } });
}

/**
 * 查询可授权权限列表（角色/套餐授权面板）
 *
 * 后端已做两层防护：
 * 1. 字段最小化：仅返回 id/parentId/name/nameEn/type/realm/icon/sort，不含 path/component/code/treePath 等敏感字段
 * 2. 越权防护：非超管仅返回当前用户自身拥有的权限（不能授予自己不具备的权限）
 *
 * 前端无需再做任何过滤，直接信任后端数据。
 *
 * @param realm 权限域（admin/portal），按角色域过滤可分配的权限，为空则返回所有
 */
export async function getPermissionAssignOptionsApi(realm?: string): Promise<ApiEnvelope<PermissionAssignOption[]>> {
  return request<PermissionAssignOption[]>({ method: "GET", url: "/sys/permission/assign-options", params: { realm } });
}

/** 查询当前用户菜单列表（扁平列表，前端负责转树形） */
export async function getUserMenusApi(): Promise<ApiEnvelope<SysPermission[]>> {
  return request<SysPermission[]>({ method: "GET", url: "/sys/permission/menus" });
}

/** 分页查询指定父节点下的子权限列表 */
export async function getPermissionPageApi(
  params: PermissionPageRequest
): Promise<ApiEnvelope<IPage<SysPermission>>> {
  return request<IPage<SysPermission>>({
    method: "GET",
    url: "/sys/permission/page",
    params
  });
}

/** 查询指定父节点下的按钮权限列表 */
export async function getPermissionButtonsApi(
  parentId: string
): Promise<ApiEnvelope<SysPermission[]>> {
  return request<SysPermission[]>({
    method: "GET",
    url: `/sys/permission/buttons/${parentId}`
  });
}

/** 根据 ID 查询权限详情 */
export async function getPermissionByIdApi(id: string): Promise<ApiEnvelope<SysPermission>> {
  return request<SysPermission>({ method: "GET", url: `/sys/permission/${id}` });
}

/** 创建权限 */
export async function createPermissionApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/permission/create", data });
}

/** 更新权限 */
export async function updatePermissionApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/permission/update", data });
}

/** 删除权限 */
export async function deletePermissionApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/permission/delete", data: { id } });
}

/** 批量删除权限 */
export async function batchDeletePermissionApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/permission/batch/delete", data: ids });
}
