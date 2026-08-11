import { request } from "./http";
import type { ApiEnvelope, SysDept, DeptOption, DeptPageRequest, IPage } from "../types/auth";

/** 查询当前租户下部门列表（部门管理页面） */
export async function getDeptListApi(): Promise<ApiEnvelope<SysDept[]>> {
  return request<SysDept[]>({ method: "GET", url: "/sys/dept/list" });
}

/**
 * 查询部门选项列表（用户管理表单下拉选择，当前租户下的部门）
 * 返回最小化字段（id/parentId/name/sort），后端已剥离 leader/phone/email/tenantId/treePath/审计字段等。
 */
export async function getDeptOptionsApi(): Promise<ApiEnvelope<DeptOption[]>> {
  return request<DeptOption[]>({ method: "GET", url: "/sys/dept/options" });
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

/** 根据 ID 查询部门详情 */
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
  return request<null>({ method: "POST", url: "/sys/dept/delete", data: { id } });
}

/** 批量删除部门 */
export async function batchDeleteDeptApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dept/batch/delete", data: ids });
}
