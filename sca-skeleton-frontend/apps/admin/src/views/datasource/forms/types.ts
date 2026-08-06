import type { Datasource } from "../../../apis/datasource";

/**
 * 数据源类型表单契约。
 *
 * 架构约定：
 * - 每种数据源类型对应一个「完整、自包含」的表单组件（不做共用字段组件抽取，
 *   允许不同类型自由排版、自由演进；待类型足够多、模式清晰后再回头提炼）。
 * - 新增数据源类型 = 新建一个表单组件文件 + 在 DatasourceFormRegistry 注册一行。
 * - 表单组件通过 defineExpose 暴露 DatasourceTypeFormExpose 契约，
 *   由抽屉编排器（DatasourceDrawerContent）统一调用校验、收集载荷并发起保存请求。
 */

/** 表单模式 */
export type DatasourceFormMode = "add" | "edit" | "view";

/** 类型表单组件统一 props */
export interface DatasourceTypeFormProps {
  mode: DatasourceFormMode;
  /** 编辑/查看模式下的数据源详情 */
  datasource?: Datasource;
}

/**
 * 表单提交的请求载荷。
 * - add：createDatasourceApi 的请求体（含 password）
 * - edit：updateDatasourceApi 的请求体（含 id/version；password 为空表示不修改）
 * 由各表单组件按自身 mode 组装，编排器直接透传给 API。
 */
export type DatasourceFormPayload = Record<string, unknown>;

/** 每个类型表单组件必须通过 defineExpose 暴露的方法 */
export interface DatasourceTypeFormExpose {
  /** 执行表单校验，返回是否通过 */
  validate(): Promise<boolean>;
  /** 收集请求载荷（调用前应先通过 validate） */
  getPayload(): DatasourceFormPayload;
  /** 用户是否修改过表单内容（用于「重新选择类型」时的离开确认） */
  isDirty(): boolean;
}

/** 类型选择面板使用的数据库类型选项 */
export interface DbTypeCardOption {
  label: string;
  value: string;
  defaultPort: number;
}
