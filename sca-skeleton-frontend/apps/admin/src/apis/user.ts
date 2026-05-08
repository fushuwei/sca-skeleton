import { request } from "./http"; // 导入统一请求函数。
import { mockGetUserProfile } from "./mock/auth"; // 导入本地模拟用户信息接口。
import type { ApiEnvelope, UserProfile } from "../types/auth"; // 导入用户信息相关类型。

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true"; // 根据环境变量决定是否启用模拟接口。

export async function getUserProfileApi(): Promise<ApiEnvelope<UserProfile>> { // 定义获取当前用户信息 API。
  if (USE_MOCK) { // 判断当前是否启用模拟模式。
    return mockGetUserProfile(); // 模拟模式下返回本地用户资料。
  } // 结束模拟模式分支。
  return request<UserProfile>({ method: "GET", url: "/user/profile" }); // 非模拟模式下请求真实用户资料接口。
} // 结束获取当前用户信息 API。
