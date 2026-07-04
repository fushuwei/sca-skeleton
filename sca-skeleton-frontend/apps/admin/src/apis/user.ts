import { request } from "./http";
import type { ApiEnvelope, UserProfile, SysUser, UserPageRequest, IPage } from "../types/auth";

/** 获取当前登录用户资料 */
export async function getUserProfileApi(): Promise<ApiEnvelope<UserProfile>> {
  return request<UserProfile>({ method: "GET", url: "/sys/user/profile" });
}

/** 分页查询用户列表 */
export async function getUserPageApi(
  params: UserPageRequest
): Promise<ApiEnvelope<IPage<SysUser>>> {
  return request<IPage<SysUser>>({
    method: "GET",
    url: "/sys/user/page",
    params
  });
}

/** 按 ID 查询用户详情 */
export async function getUserByIdApi(id: string): Promise<ApiEnvelope<SysUser>> {
  return request<SysUser>({ method: "GET", url: `/sys/user/${id}` });
}

/** 创建用户 */
export async function createUserApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/user", data });
}

/** 更新用户 */
export async function updateUserApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "PUT", url: "/sys/user", data });
}

/** 删除用户 */
export async function deleteUserApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "DELETE", url: `/sys/user/${id}` });
}

/** 重置用户密码 */
export async function resetUserPasswordApi(
  id: string,
  newPassword: string
): Promise<ApiEnvelope<null>> {
  return request<null>({
    method: "PUT",
    url: `/sys/user/${id}/password/reset`,
    data: { newPassword }
  });
}

/** 变更用户状态 */
export async function changeUserStatusApi(
  id: string,
  status: string,
  reason?: string
): Promise<ApiEnvelope<null>> {
  return request<null>({
    method: "PUT",
    url: `/sys/user/${id}/status`,
    data: { status, ...(reason ? { reason } : {}) }
  });
}
