import { request } from "./http";
import type { ApiEnvelope, SysRole } from "../types/auth";

/** 获取当前租户下角色列表 */
export async function getRoleListApi(): Promise<ApiEnvelope<SysRole[]>> {
  return request<SysRole[]>({ method: "GET", url: "/sys/role/list" });
}

/** 按 ID 查询角色详情 */
export async function getRoleByIdApi(id: string): Promise<ApiEnvelope<SysRole>> {
  return request<SysRole>({ method: "GET", url: `/sys/role/${id}` });
}
