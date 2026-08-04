import { request } from "./http";
import type { ApiEnvelope, IPage } from "../types/auth";

// ============================================================
// 类型定义
// ============================================================

/** 驱动文件 */
export interface DriverFile {
  fileName: string;
  fileSize: number;
  sha256: string;
  sortOrder: number;
}

/** 驱动 */
export interface Driver {
  id: string;
  dbType: string;
  driverName: string;
  driverClass: string;
  objectKey: string;
  storageType: string;
  urlTemplate: string;
  allowedParams: string;
  status: string;
  isBuiltin: number;
  remark: string;
  version: number;
  createTime: string;
  files: DriverFile[];
  totalFileSize: number;
}

/** 驱动选项 */
export interface DriverOption {
  id: string;
  driverName: string;
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
  version: number;
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

/** 新增驱动（表单字段 + 驱动文件随同一次 multipart 请求提交） */
export async function createDriverApi(
  data: Record<string, unknown>,
  files: File[],
  onUploadProgress?: (percent: number) => void
): Promise<ApiEnvelope<null>> {
  const formData = new FormData();
  formData.append("driver", new Blob([JSON.stringify(data)], { type: "application/json" }));
  for (const file of files) {
    formData.append("files", file);
  }
  return request<null>({
    method: "POST",
    url: "/ds/driver/create",
    data: formData,
    // 上传耗时取决于文件大小与带宽，禁用单请求超时（由网关/Nginx 超时兜底）
    timeout: 0,
    onUploadProgress: (e) => {
      if (onUploadProgress && e.total) {
        onUploadProgress(Math.round((e.loaded / e.total) * 100));
      }
    }
  });
}

/** 检查驱动名称是否已存在（驱动名称作为目录名，全局唯一） */
export async function checkDriverNameExistsApi(
  driverName: string
): Promise<ApiEnvelope<boolean>> {
  return request<boolean>({
    method: "GET",
    url: "/ds/driver/name/exists",
    params: { driverName }
  });
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

/** 批量删除驱动 */
export async function batchDeleteDriverApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/ds/driver/batch/delete", data: ids });
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

/** 批量删除数据源 */
export async function batchDeleteDatasourceApi(ids: string[]): Promise<ApiEnvelope<null>> {
  return request<null>({ method: "POST", url: "/ds/datasource/batch/delete", data: ids });
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
  table: string,
  database?: string
): Promise<ApiEnvelope<string[]>> {
  return request<string[]>({
    method: "GET",
    url: `/ds/datasource/${datasourceId}/columns`,
    params: { table, database }
  });
}

// ============================================================
// SQL 查询 API
// ============================================================

/** SQL 查询请求 */
export interface SqlExecuteRequest {
  datasourceId: string;
  sql: string;
  database?: string;
  maxRows?: number;
}

/** SQL 查询响应 */
export interface SqlExecuteResponse {
  columns: string[];
  rows: Record<string, unknown>[];
  rowCount: number;
  costMs: number;
}

/** 执行 SQL 查询 */
export async function executeSqlApi(
  data: SqlExecuteRequest
): Promise<ApiEnvelope<SqlExecuteResponse>> {
  return request<SqlExecuteResponse>({
    method: "POST",
    url: "/ds/query/execute",
    data
  });
}
