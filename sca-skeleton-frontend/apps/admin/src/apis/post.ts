import { request } from "./http";
import type { ApiEnvelope, SysPost } from "../types/auth";

/** 获取当前租户下岗位列表 */
export async function getPostListApi(): Promise<ApiEnvelope<SysPost[]>> {
  return request<SysPost[]>({ method: "GET", url: "/sys/post/list" });
}

/** 按 ID 查询岗位详情 */
export async function getPostByIdApi(id: string): Promise<ApiEnvelope<SysPost>> {
  return request<SysPost>({ method: "GET", url: `/sys/post/${id}` });
}
