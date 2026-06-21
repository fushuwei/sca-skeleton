import { request } from "./http";
import type { ApiEnvelope, SysDept } from "../types/auth";

/** 获取当前租户下部门列表 */
export async function getDeptListApi(): Promise<ApiEnvelope<SysDept[]>> {
  return request<SysDept[]>({ method: "GET", url: "/sys/dept/list" });
}

/** 按 ID 查询部门详情 */
export async function getDeptByIdApi(id: string): Promise<ApiEnvelope<SysDept>> {
  return request<SysDept>({ method: "GET", url: `/sys/dept/${id}` });
}
