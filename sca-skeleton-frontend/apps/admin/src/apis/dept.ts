import { request } from "./http";
import type { ApiEnvelope, SysDept, DeptPageRequest, IPage } from "../types/auth";

/** 查询当前租户下部门列表 */
export async function getDeptListApi(): Promise<ApiEnvelope<SysDept[]>> {
  return request<SysDept[]>({ method: "GET", url: "/sys/dept/list" });
}

/** 分页查询当前租户下部门列表 */
export async function getDeptPageApi(
  params: DeptPageRequest
): Promise<ApiEnvelope<IPage<SysDept>>> {
  return request<IPage<SysDept>>({
    method: "GET",
    url: "/sys/dept/page",
    params
  });
}

/** 按 ID 查询部门详情 */
export async function getDeptByIdApi(id: string): Promise<ApiEnvelope<SysDept>> {
  return request<SysDept>({ method: "GET", url: `/sys/dept/${id}` });
}

/** 创建部门 */
export async function createDeptApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dept/create", data });
}

/** 更新部门 */
export async function updateDeptApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dept/update", data });
}

/** 删除部门 */
export async function deleteDeptApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dept/delete", data: id });
}

/** 批量删除部门 */
export async function batchDeleteDeptApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dept/batch/delete", data: ids });
}
