import { request } from "./http";
import type { ApiEnvelope, SysNotice, NoticePageRequest, IPage } from "../types/auth";

/** 分页查询通知公告列表 */
export async function getNoticePageApi(
  params: NoticePageRequest
): Promise<ApiEnvelope<IPage<SysNotice>>> {
  return request<IPage<SysNotice>>({
    method: "GET",
    url: "/sys/notice/page",
    params
  });
}

/** 根据 ID 查询通知公告详情 */
export async function getNoticeByIdApi(id: string): Promise<ApiEnvelope<SysNotice>> {
  return request<SysNotice>({ method: "GET", url: `/sys/notice/${id}` });
}

/** 创建通知公告 */
export async function createNoticeApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/notice/create", data });
}

/** 更新通知公告 */
export async function updateNoticeApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/notice/update", data });
}

/** 变更通知公告状态（发布、撤回、归档） */
export async function updateNoticeStatusApi(data: {
  id: string;
  status: string;
}): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/notice/status", data });
}

/** 删除通知公告 */
export async function deleteNoticeApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/notice/delete", data: { id } });
}

/** 批量删除通知公告 */
export async function batchDeleteNoticeApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/notice/batch/delete", data: ids });
}

/** 标记通知公告为已读 */
export async function markNoticeAsReadApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: `/sys/notice/read/${id}` });
}
