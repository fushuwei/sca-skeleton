import { request } from "./http";
import type { ApiEnvelope, SysPermission, PermissionPageRequest, IPage } from "../types/auth";

/** 查询全部权限列表 */
export async function getPermissionListApi(): Promise<ApiEnvelope<SysPermission[]>> {
  return request<SysPermission[]>({ method: "GET", url: "/sys/permission/list" });
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
