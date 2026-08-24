import { request } from "./http";
import type { ApiEnvelope, SysNotice, NoticePageRequest, NoticeInboxItem, UserOption, IPage } from "../types/auth";

/** 搜索用户选项（接收范围=指定用户时，输入用户名/昵称/真实姓名模糊搜索，选中后保存用户 ID） */
export async function getNoticeUserOptionsApi(keyword: string): Promise<ApiEnvelope<UserOption[]>> {
  return request<UserOption[]>({
    method: "GET",
    url: "/sys/notice/user-options",
    params: { keyword }
  });
}

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

/** 置顶/取消置顶通知公告 */
export async function updateNoticeTopApi(data: {
  id: string;
  isTop: number;
}): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/notice/top", data });
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
  return request<null>({ method: "POST", url: "/sys/notice/read", data: { id } });
}

/** 查询当前用户未读通知数量 */
export async function getUnreadNoticeCountApi(): Promise<ApiEnvelope<number>> {
  return request<number>({ method: "GET", url: "/sys/notice/unread/count" });
}

/** 查询当前用户消息收件箱（弹窗展示用） */
export async function getNoticeInboxApi(
  pageNum = 1,
  pageSize = 10
): Promise<ApiEnvelope<IPage<NoticeInboxItem>>> {
  return request<IPage<NoticeInboxItem>>({
    method: "GET",
    url: "/sys/notice/inbox",
    params: { pageNum, pageSize }
  });
}

/** 全部标记已读 */
export async function markAllNoticeAsReadApi(): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/notice/read/all" });
}
