import { request } from "./http";
import type { ApiEnvelope, SysOperationLog, OperationLogPageRequest, IPage } from "../types/auth";

/** 分页查询操作日志 */
export async function getOperationLogPageApi(
  params: OperationLogPageRequest
): Promise<ApiEnvelope<IPage<SysOperationLog>>> {
  return request<IPage<SysOperationLog>>({
    method: "GET",
    url: "/sys/operation-log/page",
    params
  });
}

/** 按 ID 查询操作日志详情 */
export async function getOperationLogByIdApi(id: string): Promise<ApiEnvelope<SysOperationLog>> {
  return request<SysOperationLog>({ method: "GET", url: `/sys/operation-log/${id}` });
}

/** 批量删除操作日志 */
export async function batchDeleteOperationLogApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/operation-log/batch/delete", data: ids });
}

/** 清空全部操作日志 */
export async function clearAllOperationLogApi(): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/operation-log/clear" });
}
