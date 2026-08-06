import type { Component } from "vue";
import MysqlDatasourceForm from "./MysqlDatasourceForm.vue";
import OracleDatasourceForm from "./OracleDatasourceForm.vue";
import PostgresqlDatasourceForm from "./PostgresqlDatasourceForm.vue";
import SqlServerDatasourceForm from "./SqlServerDatasourceForm.vue";
import DamengDatasourceForm from "./DamengDatasourceForm.vue";
import KingbaseDatasourceForm from "./KingbaseDatasourceForm.vue";
import OceanbaseDatasourceForm from "./OceanbaseDatasourceForm.vue";
import GaussdbDatasourceForm from "./GaussdbDatasourceForm.vue";
import ClickhouseDatasourceForm from "./ClickhouseDatasourceForm.vue";
import MongodbDatasourceForm from "./MongodbDatasourceForm.vue";

/**
 * 数据源表单注册表：dbType → 表单组件。
 *
 * 新增数据源类型的接入步骤：
 * 1. 在本目录新建 XxxDatasourceForm.vue（完整、自包含的表单，契约见 ./types.ts）；
 * 2. 在下方 FORM_REGISTRY 注册一行映射。
 * 未注册的类型在「类型选择」面板中显示为「即将支持」（置灰不可选）。
 */
const FORM_REGISTRY: Record<string, Component> = {
  MYSQL: MysqlDatasourceForm,
  ORACLE: OracleDatasourceForm,
  POSTGRESQL: PostgresqlDatasourceForm,
  SQLSERVER: SqlServerDatasourceForm,
  DAMENG: DamengDatasourceForm,
  KINGBASE: KingbaseDatasourceForm,
  OCEANBASE: OceanbaseDatasourceForm,
  GAUSSDB: GaussdbDatasourceForm,
  CLICKHOUSE: ClickhouseDatasourceForm,
  MONGODB: MongodbDatasourceForm
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
