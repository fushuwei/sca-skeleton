<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import type { QTableColumn } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { Datasource, SqlExecuteResponse } from "../../apis/datasource";
import {
  getDatasourcePageApi,
  executeSqlApi
} from "../../apis/datasource";

const { t } = useI18n({ useScope: "global" });

// ── 数据源选项 ──
const datasourceOptions = ref<{ label: string; value: string }[]>([]);
const datasourceLoading = ref(false);

async function loadDatasourceOptions() {
  datasourceLoading.value = true;
  try {
    const res = await getDatasourcePageApi({
      pageNum: 1,
      pageSize: 1000,
      enabled: 1
    });
    if (res.code === 10_000 && res.data) {
      datasourceOptions.value = res.data.records.map((ds: Datasource) => ({
        label: `${ds.name} (${ds.dbType})`,
        value: ds.id
      }));
    }
  } catch {
    // 静默失败
  } finally {
    datasourceLoading.value = false;
  }
}

// ── 查询表单 ──
const selectedDatasource = ref<string>("");
const sqlContent = ref("");
const maxRows = ref(1000);

// ── 执行状态 ──
const executing = ref(false);
const queryResult = ref<SqlExecuteResponse | null>(null);

// ── 结果表格列定义（动态生成） ──
const resultColumns = computed<QTableColumn[]>(() => {
  if (!queryResult.value?.columns) return [];
  return queryResult.value.columns.map((col) => ({
    name: col,
    label: col,
    field: col,
    align: "left",
    sortable: true
  }));
});

// ── 执行查询 ──
async function handleExecute() {
  if (!selectedDatasource.value) {
    showToast(t("sqlQuery.datasourceRequired"), "negative");
    return;
  }
  if (!sqlContent.value.trim()) {
    showToast(t("sqlQuery.sqlRequired"), "negative");
    return;
  }

  try {
    executing.value = true;
    queryResult.value = null;
    const res = await executeSqlApi({
      datasourceId: selectedDatasource.value,
      sql: sqlContent.value.trim(),
      maxRows: maxRows.value
    });

    if (res.code === 10_000 && res.data) {
      // 为每行添加索引，用于 q-table row-key
      res.data.rows = res.data.rows.map((row, index) => ({ ...row, __rowIndex: index }));
      queryResult.value = res.data;
      showToast(t("sqlQuery.executeSuccess"), "positive");
    } else {
      showToast(res.message || t("sqlQuery.executeFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("sqlQuery.executeFail"), "negative");
    }
  } finally {
    executing.value = false;
  }
}

// ── 清空 ──
function handleClear() {
  sqlContent.value = "";
  queryResult.value = null;
}

// ── 导出 CSV ──
function handleExport() {
  if (!queryResult.value?.rows.length) return;

  const { columns, rows } = queryResult.value;
  const csvContent = [
    columns.join(","),
    ...rows.map((row) =>
      columns.map((col) => {
        const val = row[col];
        if (val === null || val === undefined) return "";
        const str = String(val);
        // 包含逗号或引号的值用双引号包裹并转义
        if (str.includes(",") || str.includes('"') || str.includes("\n")) {
          return `"${str.replace(/"/g, '""')}"`;
        }
        return str;
      }).join(",")
    )
  ].join("\n");

  // CSV 注入防护：以 = + - @ 开头的单元格前缀单引号
  const safeCsv = csvContent.replace(/^([=+\-@])/gm, "'$1");

  const blob = new Blob(["\uFEFF" + safeCsv], { type: "text/csv;charset=utf-8;" });
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = `query_result_${Date.now()}.csv`;
  link.click();
  URL.revokeObjectURL(link.href);
}

onMounted(() => {
  loadDatasourceOptions();
});
</script>

<template>
  <div class="sql-query-shell">
    <!-- 顶部控制栏 -->
    <div class="control-bar">
      <div class="row q-col-gutter-sm items-center no-wrap">
        <!-- 数据源选择 -->
        <div class="col-auto">
          <q-select
            v-model="selectedDatasource"
            filled
            square
            dense
            :options="datasourceOptions"
            :loading="datasourceLoading"
            :option-label="(o: { label: string; value: string } | undefined) => (o ? o.label : '')"
            option-value="value"
            emit-value
            map-options
            hide-bottom-space
            clearable
            transition-show="jump-up"
            transition-hide="jump-down"
            class="status-select"
            popup-content-class="status-select-popup"
            style="min-width: 280px"
          >
            <template v-if="!selectedDatasource" v-slot:selected>
              <span class="status-placeholder">{{ t('sqlQuery.datasourcePlaceholder') }}</span>
            </template>
          </q-select>
        </div>
        <!-- 最大行数 -->
        <div class="col-auto">
          <q-input
            v-model.number="maxRows"
            :label="t('sqlQuery.maxRows')"
            filled
            square
            dense
            type="number"
            min="1"
            max="10000"
            hide-bottom-space
            style="min-width: 140px"
          />
        </div>
        <q-space />
        <!-- 操作按钮 -->
        <div class="col-auto">
          <div class="row q-gutter-x-sm no-wrap">
            <q-btn
              color="primary"
              unelevated
              no-caps
              :loading="executing"
              class="action-btn"
              @click="handleExecute"
            >
              <q-icon name="sym_r_play_arrow" size="20px" class="q-mr-xs" />
              {{ executing ? t("sqlQuery.executing") : t("sqlQuery.execute") }}
            </q-btn>
            <q-btn
              color="grey-7"
              outline
              no-caps
              class="action-btn"
              @click="handleClear"
            >
              <q-icon name="sym_r_clear" size="20px" class="q-mr-xs" />
              {{ t("sqlQuery.clear") }}
            </q-btn>
          </div>
        </div>
      </div>
    </div>

    <!-- SQL 编辑器 -->
    <div class="editor-area">
      <div class="editor-header row items-center no-wrap">
        <q-icon name="sym_r_code" size="20px" class="q-mr-xs" color="grey-8" />
        <span class="editor-title">{{ t("sqlQuery.sqlEditor") }}</span>
        <q-space />
        <q-badge color="blue-2" text-color="blue-9" class="readonly-badge">
          {{ t("sqlQuery.onlyRead") }}
        </q-badge>
      </div>
      <textarea
        v-model="sqlContent"
        class="sql-textarea"
        :placeholder="t('sqlQuery.sqlPlaceholder')"
        spellcheck="false"
        @keydown.ctrl.enter="handleExecute"
      />
    </div>

    <!-- 查询结果 -->
    <div class="result-area">
      <div class="result-header row items-center no-wrap">
        <q-icon name="sym_r_table_chart" size="20px" class="q-mr-xs" color="grey-8" />
        <span class="result-title">{{ t("sqlQuery.result") }}</span>
        <q-space />
        <template v-if="queryResult">
          <span class="result-info">{{ t("sqlQuery.rowCount", { count: queryResult.rowCount }) }}</span>
          <span class="result-info q-ml-md">{{ t("sqlQuery.costMs", { ms: queryResult.costMs }) }}</span>
          <q-btn
            v-if="queryResult.rows.length"
            flat
            dense
            no-caps
            size="sm"
            color="primary"
            icon="sym_r_download"
            :label="t('sqlQuery.export')"
            class="q-ml-md"
            @click="handleExport"
          />
        </template>
      </div>
      <div class="result-body">
        <q-table
          v-if="queryResult && queryResult.rows.length"
          :rows="queryResult.rows"
          :columns="resultColumns"
          row-key="__rowIndex"
          flat
          :rows-per-page-options="[10, 20, 50, 100]"
          :class="['result-table']"
        >
          <template #body-cell="props">
            <q-td :props="props">
              {{ props.value === null || props.value === undefined ? '' : String(props.value) }}
            </q-td>
          </template>
        </q-table>
        <div v-else-if="queryResult && !queryResult.rows.length" class="result-empty">
          <q-icon name="sym_r_inbox" size="48px" color="grey-5" />
          <span class="text-grey-6">{{ t("common.noData") }}</span>
        </div>
        <div v-else class="result-empty">
          <q-icon name="sym_r_search" size="48px" color="grey-5" />
          <span class="text-grey-6">{{ t("sqlQuery.noResult") }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sql-query-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  gap: 8px;
}

/* ── 顶部控制栏 ── */
.control-bar {
  flex-shrink: 0;
  padding: 8px;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.action-btn {
  min-width: 72px;
  height: 40px;
  font-size: 13px;
  padding: 0 14px;
}

.status-select :deep(.q-field__native) {
  color: rgba(0, 0, 0, 0.87);
}

.status-select :deep(.q-field__control) {
  min-height: 40px;
}

/* ── SQL 编辑器 ── */
.editor-area {
  flex-shrink: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
  display: flex;
  flex-direction: column;
}

.editor-header {
  flex-shrink: 0;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.editor-title {
  font-size: 13px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.87);
}

.readonly-badge {
  font-size: 11px;
  padding: 2px 8px;
}

.sql-textarea {
  flex: 1 1 auto;
  min-height: 160px;
  max-height: 320px;
  width: 100%;
  padding: 12px;
  border: none;
  outline: none;
  resize: vertical;
  font-family: "SF Mono", "Monaco", "Menlo", "Consolas", monospace;
  font-size: 14px;
  line-height: 1.6;
  color: rgba(0, 0, 0, 0.87);
  background: #fafafa;
}

/* ── 查询结果 ── */
.result-area {
  flex: 1 1 auto;
  min-height: 200px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.result-header {
  flex-shrink: 0;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.result-title {
  font-size: 13px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.87);
}

.result-info {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.55);
}

.result-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}

.result-table {
  border: none;
}

.result-table :deep(.q-table__top) {
  padding: 4px 8px;
}

.result-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
  min-height: 200px;
}

/* ── 暗色模式 ── */
.body--dark .control-bar,
.body--dark .editor-area,
.body--dark .result-area {
  background: #1e1e1e;
  border-color: rgba(255, 255, 255, 0.08);
}

.body--dark .editor-header,
.body--dark .result-header {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.body--dark .editor-title,
.body--dark .result-title {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .sql-textarea {
  background: #252525;
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .result-info {
  color: rgba(255, 255, 255, 0.55);
}
</style>

<style>
.status-select-popup .q-item {
  min-height: 40px;
  padding: 0 16px;
}
</style>
