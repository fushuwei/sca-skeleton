import { request } from "./http";
import type { ApiEnvelope, SysDict, SysDictData, DictPageRequest, DictDataPageRequest, IPage } from "../types/auth";

/** 分页查询当前租户下字典列表 */
export async function getDictPageApi(
  params: DictPageRequest
): Promise<ApiEnvelope<IPage<SysDict>>> {
  return request<IPage<SysDict>>({
    method: "GET",
    url: "/sys/dict/page",
    params
  });
}

/** 根据 ID 查询字典详情 */
export async function getDictByIdApi(id: string): Promise<ApiEnvelope<SysDict>> {
  return request<SysDict>({ method: "GET", url: `/sys/dict/${id}` });
}

/** 创建字典 */
export async function createDictApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dict/create", data });
}

/** 更新字典 */
export async function updateDictApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dict/update", data });
}

/** 启用/禁用字典（独立接口，仅更新状态，无需先查询详情） */
export async function updateDictStatusApi(data: {
  id: string;
  status: string;
}): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dict/status", data });
}

/** 删除字典 */
export async function deleteDictApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dict/delete", data: { id } });
}

/** 批量删除字典 */
export async function batchDeleteDictApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dict/batch/delete", data: ids });
}

// ═══════════════════════════════════════════════════════════════
// 字典数据 API
// ═══════════════════════════════════════════════════════════════

/** 分页查询字典数据列表 */
export async function getDictDataPageApi(
  params: DictDataPageRequest
): Promise<ApiEnvelope<IPage<SysDictData>>> {
  return request<IPage<SysDictData>>({
    method: "GET",
    url: "/sys/dict/data/page",
    params
  });
}

/** 根据 ID 查询字典数据详情 */
export async function getDictDataByIdApi(id: string): Promise<ApiEnvelope<SysDictData>> {
  return request<SysDictData>({ method: "GET", url: `/sys/dict/data/${id}` });
}

/** 创建字典数据 */
export async function createDictDataApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dict/data/create", data });
}

/** 更新字典数据 */
export async function updateDictDataApi(data: Record<string, unknown>): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dict/data/update", data });
}

/** 启用/禁用字典数据（独立接口，仅更新状态，无需先查询详情） */
export async function updateDictDataStatusApi(data: {
  id: string;
  status: string;
}): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dict/data/status", data });
}

/** 删除字典数据 */
export async function deleteDictDataApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dict/data/delete", data: { id } });
}

/** 批量删除字典数据 */
export async function batchDeleteDictDataApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/sys/dict/data/batch/delete", data: ids });
}
