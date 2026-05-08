import axios, { AxiosError } from "axios"; // 导入 axios 与错误类型定义。
import type { ApiEnvelope } from "../types/auth"; // 导入通用响应包裹类型。

const REQUEST_TIMEOUT = 10_000; // 定义请求超时时间常量。
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL; // 从环境变量读取当前环境接口基地址。

export const http = axios.create({ // 创建项目统一请求实例。
  baseURL: API_BASE_URL, // 配置接口基地址。
  timeout: REQUEST_TIMEOUT // 配置请求超时时间。
}); // 结束请求实例创建。

http.interceptors.request.use((config) => { // 注册请求拦截器用于注入鉴权信息。
  const token = localStorage.getItem("admin_token"); // 从本地存储读取登录令牌。
  if (token) { // 判断令牌是否存在。
    config.headers.set("Authorization", `Bearer ${token}`); // 存在令牌时注入 Authorization 请求头。
  } // 结束令牌注入分支。
  return config; // 返回处理后的请求配置。
}); // 结束请求拦截器注册。

http.interceptors.response.use( // 注册响应拦截器统一处理网络错误。
  (response) => response, // 成功响应直接透传，业务码在 request 函数中统一处理。
  (error: AxiosError) => { // 处理网络异常或 HTTP 异常。
    if (error.response?.status === 401) { // 判断 HTTP 状态码是否为未授权。
      localStorage.removeItem("admin_token"); // 清理本地令牌。
      window.location.href = "/login"; // 跳转到登录页重新认证。
    } // 结束 HTTP 未授权分支。
    const message = error.message || "网络异常，请稍后重试"; // 组装错误提示文本。
    return Promise.reject(new Error(message)); // 将错误统一抛给调用方处理。
  } // 结束错误响应处理函数。
); // 结束响应拦截器注册。

export async function request<T>(config: Parameters<typeof http.request>[0]): Promise<ApiEnvelope<T>> { // 定义统一请求函数并返回强类型业务包裹结果。
  const response = await http.request<ApiEnvelope<T>>(config); // 发起请求并声明响应体类型。
  const payload = response.data; // 提取响应数据体。
  if (payload.code === 0) { // 判断业务码是否成功。
    return payload; // 成功时返回业务数据包裹。
  } // 结束成功分支。
  if (payload.code === 401) { // 判断业务码是否为未授权。
    localStorage.removeItem("admin_token"); // 清理本地令牌。
    window.location.href = "/login"; // 跳转登录页重新认证。
  } // 结束未授权业务码分支。
  throw new Error(payload.message || "业务请求失败"); // 业务失败时抛出标准错误。
} // 结束统一请求函数定义。
