import type { Component } from "vue";
import MysqlDatasourceForm from "./MysqlDatasourceForm.vue";

/**
 * 数据源表单注册表：dbType → 表单组件。
 *
 * 目前仅 MySQL 完成开发，其他类型正在开发中。
 * 未注册的类型在「类型选择」面板中可点击，但会提示「正在开发中」。
 */
const FORM_REGISTRY: Record<string, Component> = {
  MYSQL: MysqlDatasourceForm
};

/** 按 dbType 解析对应的表单组件；未注册返回 undefined */
export function resolveDatasourceForm(dbType: string): Component | undefined {
  return FORM_REGISTRY[dbType];
}

/** 类型选择面板的分组定义（数组顺序即展示顺序） */
export interface DbTypeGroup {
  /** i18n 键（datasourceMgmt 命名空间下） */
  labelKey: string;
  dbTypes: string[];
}

export const DB_TYPE_GROUPS: DbTypeGroup[] = [
  {
    labelKey: "datasourceMgmt.groupRelational",
    dbTypes: ["MYSQL", "ORACLE", "POSTGRESQL", "SQLSERVER", "DAMENG", "KINGBASE", "OCEANBASE", "GAUSSDB"]
  },
  {
    labelKey: "datasourceMgmt.groupAnalytics",
    dbTypes: ["CLICKHOUSE"]
  },
  {
    labelKey: "datasourceMgmt.groupNosql",
    dbTypes: ["MONGODB"]
  }
];

/** 「上次选择」记忆的 localStorage 键 */
export const LAST_SELECTED_DB_TYPE_KEY = "datasource:lastSelectedDbType";
