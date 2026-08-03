<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    dbType: string;
    size?: number;
  }>(),
  { size: 20 }
);

interface DbIconConfig {
  /** 品牌色 */
  color: string;
  /** 文字颜色（默认白色） */
  textColor?: string;
  /** 缩写文字 */
  text: string;
}

// 各数据库品牌色与缩写
const DB_ICON_MAP: Record<string, DbIconConfig> = {
  MYSQL: { color: "#4479A1", text: "My" },
  ORACLE: { color: "#C74634", text: "Or" },
  POSTGRESQL: { color: "#336791", text: "Pg" },
  SQLSERVER: { color: "#CC2927", text: "SQ" },
  DAMENG: { color: "#0078D7", text: "DM" },
  KINGBASE: { color: "#C41E3A", text: "KB" },
  MONGODB: { color: "#47A248", text: "Mg" },
  CLICKHOUSE: { color: "#FFCC01", textColor: "#1A1A1A", text: "CH" },
  OCEANBASE: { color: "#1677FF", text: "OB" },
  GAUSSDB: { color: "#C7000B", text: "GB" }
};

const config = computed<DbIconConfig>(() =>
  DB_ICON_MAP[props.dbType] ?? { color: "#666666", text: props.dbType.slice(0, 2) }
);

// 根据文字长度和图标尺寸自适应字号
const fontSize = computed(() => {
  const len = config.value.text.length;
  if (len <= 1) return Math.round(props.size * 0.55);
  if (len === 2) return Math.round(props.size * 0.4);
  return Math.round(props.size * 0.3);
});
</script>

<template>
  <span
    class="db-type-icon"
    :style="{
      width: size + 'px',
      height: size + 'px',
      backgroundColor: config.color,
      color: config.textColor ?? '#fff',
      fontSize: fontSize + 'px'
    }"
  >
    {{ config.text }}
  </span>
</template>

<style scoped>
.db-type-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  font-weight: 700;
  flex-shrink: 0;
  line-height: 1;
  font-family: system-ui, -apple-system, sans-serif;
  user-select: none;
}
</style>
