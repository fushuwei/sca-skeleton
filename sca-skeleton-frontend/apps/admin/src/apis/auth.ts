import { request } from "./http"; // 导入统一请求函数。
import { mockLogin } from "./mock/auth"; // 导入本地模拟登录实现。
import type { ApiEnvelope, LoginPayload, LoginResponse } from "../types/auth"; // 导入登录相关类型。

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true"; // 根据环境变量决定是否启用模拟接口。

export async function loginApi(payload: LoginPayload): Promise<ApiEnvelope<LoginResponse>> { // 定义登录 API 调用函数。
  if (USE_MOCK) { // 判断当前是否启用模拟模式。
    return mockLogin(payload); // 模拟模式下走本地 mock 登录逻辑。
  } // 结束模拟模式分支。
  return request<LoginResponse>({ method: "POST", url: "/auth/login", data: payload }); // 非模拟模式时调用真实登录接口。
} // 结束登录 API 调用函数。
