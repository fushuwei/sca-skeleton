<script setup lang="ts">
import { computed } from "vue";

// 数据库品牌图标（来源：DBeaver 开源项目 https://github.com/dbeaver/dbeaver）
import mysqlIcon from "../assets/db-icons/mysql.png";
import oracleIcon from "../assets/db-icons/oracle.png";
import postgresqlIcon from "../assets/db-icons/postgresql.png";
import sqlserverIcon from "../assets/db-icons/sqlserver.png";
import damengIcon from "../assets/db-icons/dameng.png";
import kingbaseIcon from "../assets/db-icons/kingbase.png";
import mongodbIcon from "../assets/db-icons/mongodb.png";
import clickhouseIcon from "../assets/db-icons/clickhouse.png";
import oceanbaseIcon from "../assets/db-icons/oceanbase.png";
import gaussdbIcon from "../assets/db-icons/gaussdb.png";

const props = withDefaults(
  defineProps<{
    dbType: string;
    size?: number;
  }>(),
  { size: 20 }
);

const DB_ICON_MAP: Record<string, string> = {
  MYSQL: mysqlIcon,
  ORACLE: oracleIcon,
  POSTGRESQL: postgresqlIcon,
  SQLSERVER: sqlserverIcon,
  DAMENG: damengIcon,
  KINGBASE: kingbaseIcon,
  MONGODB: mongodbIcon,
  CLICKHOUSE: clickhouseIcon,
  OCEANBASE: oceanbaseIcon,
  GAUSSDB: gaussdbIcon
};

const iconSrc = computed(() => DB_ICON_MAP[props.dbType] ?? "");
</script>

<template>
  <img
    v-if="iconSrc"
    :src="iconSrc"
    :width="size"
    :height="size"
    :alt="dbType"
    class="db-type-icon"
  />
  <span
    v-else
    class="db-type-icon-fallback"
    :style="{ width: size + 'px', height: size + 'px' }"
  >
    {{ dbType.slice(0, 2) }}
  </span>
</template>

<style scoped>
.db-type-icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
  object-fit: contain;
}

.db-type-icon-fallback {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  background: #666;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
  line-height: 1;
  font-family: system-ui, -apple-system, sans-serif;
  user-select: none;
}
</style>
