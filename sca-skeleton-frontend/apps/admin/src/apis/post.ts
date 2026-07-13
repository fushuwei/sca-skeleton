import { request } from "./http";
import type { ApiEnvelope, SysPost, PostPageRequest, IPage } from "../types/auth";

/** 获取当前租户下岗位列表 */
export async function getPostListApi(): Promise<ApiEnvelope<SysPost[]>> {
  return request<SysPost[]>({ method: "GET", url: "/sys/post/list" });
}

/** 分页查询当前租户下岗位列表 */
export async function getPostPageApi(
  params: PostPageRequest
): Promise<ApiEnvelope<IPage<SysPost>>> {
  return request<IPage<SysPost>>({
    method: "GET",
    url: "/sys/post/page",
    params
  });
}

/** 按 ID 查询岗位详情 */
export async function getPostByIdApi(id: string): Promise<ApiEnvelope<SysPost>> {
  return request<SysPost>({ method: "GET", url: `/sys/post/${id}` });
}

/** 创建岗位 */
export async function createPostApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/post/create", data });
}

/** 更新岗位 */
export async function updatePostApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/post/update", data });
}

/** 删除岗位 */
export async function deletePostApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: `/sys/post/${id}/delete` });
}
