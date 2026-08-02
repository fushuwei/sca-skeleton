import { request } from "./http";
import type { ApiEnvelope, IPage } from "../types/auth";

// ============================================================
// 类型定义
// ============================================================

/** 驱动 */
export interface Driver {
  id: string;
  dbType: string;
  driverName: string;
  driverClass: string;
  driverVersion: string;
  jarSha256: string;
  objectKey: string;
  fileSize: number;
  storageType: string;
  urlTemplate: string;
  allowedParams: string;
  status: string;
  isBuiltin: number;
  remark: string;
  createTime: string;
}

/** 驱动选项 */
export interface DriverOption {
  id: string;
  driverName: string;
  driverVersion: string;
}

/** 数据源 */
export interface Datasource {
  id: string;
  tenantId: string;
  name: string;
  dbType: string;
  driverId: string;
  driverName: string;
  host: string;
  port: number;
  databaseName: string;
  username: string;
  password: string | null;
  connectionParams: string;
  poolConfig: string;
  enabled: number;
  connectionState: string;
  errorMsg: string;
  lastConnectTime: string;
  createTime: string;
}

/** 数据库类型 */
export interface DbTypeOption {
  name: string;
  displayName: string;
  urlPrefix: string;
  defaultPort: number;
}

/** 驱动分页请求 */
export interface DriverPageRequest {
  dbType?: string;
  keyword?: string;
  status?: string;
  pageNum: number;
  pageSize: number;
}

/** 数据源分页请求 */
export interface DatasourcePageRequest {
  dbType?: string;
  keyword?: string;
  enabled?: number;
  pageNum: number;
  pageSize: number;
}

// ============================================================
// 驱动管理 API
// ============================================================

/** 分页查询驱动列表 */
export async function getDriverPageApi(
  params: DriverPageRequest
): Promise<ApiEnvelope<IPage<Driver>>> {
  return request<IPage<Driver>>({
    method: "GET",
    url: "/ds/driver/page",
    params
  });
}

/** 根据 ID 查询驱动详情 */
export async function getDriverByIdApi(id: string): Promise<ApiEnvelope<Driver>> {
  return request<Driver>({ method: "GET", url: `/ds/driver/${id}` });
}

/** 新增驱动 */
export async function createDriverApi(
  data: Record<string, unknown>
): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/ds/driver/create", data });
}

/** 编辑驱动 */
export async function updateDriverApi(
  data: Record<string, unknown>
): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/ds/driver/update", data });
}

/** 删除驱动 */
export async function deleteDriverApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/ds/driver/delete", data: { id } });
}

/** 启用驱动 */
export async function enableDriverApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: `/ds/driver/${id}/enable` });
}

/** 禁用驱动 */
export async function disableDriverApi(id: string): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: `/ds/driver/${id}/disable` });
}

/** 查询驱动选项列表 */
export async function getDriverOptionsApi(
  dbType?: string
): Promise<ApiEnvelope<DriverOption[]>> {
  return request<DriverOption[]>({
    method: "GET",
    url: "/ds/driver/options",
    params: { dbType }
  });
}

// ============================================================
// 数据源管理 API
// ============================================================

/** 分页查询数据源列表 */
export async function getDatasourcePageApi(
  params: DatasourcePageRequest
): Promise<ApiEnvelope<IPage<Datasource>>> {
  return request<IPage<Datasource>>({
    method: "GET",
    url: "/ds/datasource/page",
    params
  });
}

/** 根据 ID 查询数据源详情 */
export async function getDatasourceByIdApi(
  id: string
): Promise<ApiEnvelope<Datasource>> {
  return request<Datasource>({ method: "GET", url: `/ds/datasource/${id}` });
}

/** 新增数据源 */
export async function createDatasourceApi(
  data: Record<string, unknown>
): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/ds/datasource/create", data });
}

/** 编辑数据源 */
export async function updateDatasourceApi(
  data: Record<string, unknown>
): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/ds/datasource/update", data });
}

/** 删除数据源 */
export async function deleteDatasourceApi(
  id: string
): Promise<ApiEnvelope<null>> {
  return request<null>({
    method: "POST",
    url: "/ds/datasource/delete",
    data: { id }
  });
}

/** 测试数据源连接 */
export async function testDatasourceApi(
  id: string
): Promise<ApiEnvelope<Datasource>> {
  return request<Datasource>({
    method: "POST",
    url: "/ds/datasource/test",
    params: { id }
  });
}

/** 启用数据源 */
export async function enableDatasourceApi(
  id: string
): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: `/ds/datasource/${id}/enable` });
}

/** 禁用数据源 */
export async function disableDatasourceApi(
  id: string
): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: `/ds/datasource/${id}/disable` });
}

/** 获取数据库类型下拉 */
export async function getDbTypesApi(): Promise<ApiEnvelope<DbTypeOption[]>> {
  return request<DbTypeOption[]>({ method: "GET", url: "/ds/datasource/db-types" });
}

/** 查询数据源下的数据库列表 */
export async function getDatabasesApi(
  datasourceId: string
): Promise<ApiEnvelope<string[]>> {
  return request<string[]>({
    method: "GET",
    url: `/ds/datasource/${datasourceId}/databases`
  });
}

/** 查询数据源下的表列表 */
export async function getTablesApi(
  datasourceId: string,
  database?: string
): Promise<ApiEnvelope<string[]>> {
  return request<string[]>({
    method: "GET",
    url: `/ds/datasource/${datasourceId}/tables`,
    params: { database }
  });
}

/** 查询表字段列表 */
export async function getColumnsApi(
  datasourceId: string,
  table: string
): Promise<ApiEnvelope<string[]>> {
  return request<string[]>({
    method: "GET",
    url: `/ds/datasource/${datasourceId}/columns`,
    params: { table }
  });
}
