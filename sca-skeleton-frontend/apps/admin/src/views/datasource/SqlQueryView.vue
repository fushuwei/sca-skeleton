<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { Dark } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import { format } from "sql-formatter";
import type { Datasource, SqlExecuteResponse } from "../../apis/datasource";
import { getDatasourcePageApi, executeSqlApi } from "../../apis/datasource";
import SqlCodeEditor from "./sql-query/SqlCodeEditor.vue";
import DbObjectTree from "./sql-query/DbObjectTree.vue";
import SqCellText from "./sql-query/SqCellText.vue";
import DbTypeIcon from "../../components/DbTypeIcon.vue";

const { t } = useI18n({ useScope: "global" });

// ============================================================
// 工作台偏好持久化
// ============================================================

// 仅持久化面板布局状态（宽度、分栏比例），不持久化数据源和 SQL 内容
const PREFS_KEY = "sqlQueryWorkbench";

interface WorkbenchPrefs {
  sidebarVisible?: boolean;
  sidebarWidth?: number;
  editorPct?: number;
}

function loadPrefs(): WorkbenchPrefs {
  try {
    const raw = localStorage.getItem(PREFS_KEY);
    return raw ? (JSON.parse(raw) as WorkbenchPrefs) : {};
  } catch {
    return {};
  }
}

const prefs = loadPrefs();

let saveTimer: ReturnType<typeof setTimeout> | null = null;
function savePrefs() {
  if (saveTimer) clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    const data: WorkbenchPrefs = {
      sidebarVisible: sidebarVisible.value,
      sidebarWidth: sidebarWidth.value,
      editorPct: editorPct.value
    };
    try {
      localStorage.setItem(PREFS_KEY, JSON.stringify(data));
    } catch {
      // 本地存储不可用时静默忽略
    }
  }, 300);
}

// ============================================================
// 数据源
// ============================================================

interface DatasourceOption {
  label: string;
  value: string;
  dbType: string;
  databaseName: string;
}

const datasourceOptions = ref<DatasourceOption[]>([]);
const datasourceLoading = ref(false);
const selectedDatasource = ref<string>("");

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
        label: ds.name,
        value: ds.id,
        dbType: ds.dbType,
        databaseName: ds.databaseName
      }));
      // 持久化的数据源可能已被删除/停用
      if (selectedDatasource.value && !datasourceOptions.value.some((o) => o.value === selectedDatasource.value)) {
        selectedDatasource.value = "";
      }
    }
  } catch {
    // 静默失败
  } finally {
    datasourceLoading.value = false;
  }
}

const currentDatasource = computed(() =>
  datasourceOptions.value.find((o) => o.value === selectedDatasource.value)
);
const currentDbType = computed(() => currentDatasource.value?.dbType ?? "");
/** 数据源配置的数据库（数据中台：SQL 执行与元数据均基于该库） */
const currentDatabase = computed(() => currentDatasource.value?.databaseName ?? "");
const dark = computed(() => Dark.isActive);

watch(selectedDatasource, () => {
  treeFilter.value = "";
  queryResult.value = null;
});

// ============================================================
// 左侧面板：对象树与筛选
// ============================================================

const treePanelRef = ref<InstanceType<typeof DbObjectTree>>();
const treeFilter = ref("");

/** 左侧滚动区内容样式：至少撑满视口高度，使对象树空态提示垂直居中 */
const treeContentStyle = {
  minHeight: "100%",
  display: "flex",
  flexDirection: "column" as const
};

function refreshTree() {
  treePanelRef.value?.refresh();
}

// ============================================================
// 编辑器 / 查询执行
// ============================================================

const editorRef = ref<InstanceType<typeof SqlCodeEditor>>();
const sqlContent = ref("");
const schemaMap = ref<Record<string, string[]>>({});

const MAX_ROWS_OPTIONS = [10, 20, 50, 100, 200, 500, 1000];
const maxRows = ref<number>(100);

const executing = ref(false);
const queryResult = ref<SqlExecuteResponse | null>(null);
/** 每次成功查询递增，作为结果表 key 重置排序与分页状态 */
const resultNonce = ref(0);
/** 结果表客户端分页（分页栏样式与各功能模块列表页保持一致） */
const resultPagination = ref({ page: 1, rowsPerPage: 100 });
const resultJumpPage = ref<number | null>(null);

interface ExecMessage {
  id: number;
  time: string;
  ok: boolean;
  text: string;
  sql?: string;
}

const messages = ref<ExecMessage[]>([]);
const unreadError = ref(false);
let messageId = 0;

function pushMessage(ok: boolean, text: string, sql?: string) {
  messages.value.unshift({
    id: ++messageId,
    time: new Date().toTimeString().slice(0, 8),
    ok,
    text,
    sql: sql && sql.length > 500 ? sql.slice(0, 500) + " …" : sql
  });
}

const resultTab = ref<"result" | "messages">("result");

function openMessagesTab() {
  resultTab.value = "messages";
  unreadError.value = false;
}

async function handleExecute() {
  if (!selectedDatasource.value) {
    showToast(t("sqlQuery.datasourceRequired"), "negative");
    return;
  }
  if (!sqlContent.value.trim()) {
    showToast(t("sqlQuery.sqlRequired"), "negative");
    return;
  }

  const sql = sqlContent.value.trim();
  try {
    executing.value = true;
    queryResult.value = null;

    const res = await executeSqlApi({
      datasourceId: selectedDatasource.value,
      sql,
      maxRows: maxRows.value
    });

    if (res.code === 10_000 && res.data) {
      // 为每行附加行号，供序号列与 row-key 使用
      res.data.rows = res.data.rows.map((row, index) => ({ ...row, __rowIndex: index }));
      queryResult.value = res.data;
      resultNonce.value++;
      resultPagination.value.page = 1;
      pushMessage(true, t("sqlQuery.msgSuccess", { count: res.data.rowCount, ms: res.data.costMs }), sql);
      resultTab.value = "result";
    } else {
      pushMessage(false, res.message || t("sqlQuery.executeFail"), sql);
      if (resultTab.value !== "messages") unreadError.value = true;
    }
  } catch (error) {
    const text = error instanceof Error && error.message
      ? t("sqlQuery.msgFail", { message: error.message })
      : t("sqlQuery.executeFail");
    pushMessage(false, text, sql);
    if (resultTab.value !== "messages") unreadError.value = true;
    if (!isNotificationHandled(error)) {
      showToast(t("sqlQuery.executeFail"), "negative");
    }
  } finally {
    executing.value = false;
  }
}

function handleClear() {
  sqlContent.value = "";
  queryResult.value = null;
  editorRef.value?.focus();
}

// ── SQL 格式化 ──

/** dbType → sql-formatter 语言枚举 */
function formatterLanguageOf(dbType: string): "sql" | "mysql" | "postgresql" | "mariadb" | "sqlite" | "tsql" | "plsql" | "bigquery" | "redshift" {
  switch (dbType) {
    case "MYSQL":
    case "CLICKHOUSE":
      return "mysql";
    case "POSTGRESQL":
    case "GAUSSDB":
    case "KINGBASE":
      return "postgresql";
    case "ORACLE":
    case "DAMENG":
      return "plsql";
    case "SQLSERVER":
      return "tsql";
    default:
      return "sql";
  }
}

function handleFormatSql() {
  if (!sqlContent.value.trim()) {
    return;
  }
  try {
    const formatted = format(sqlContent.value, {
      language: formatterLanguageOf(currentDbType.value),
      tabWidth: 2,
      keywordCase: "upper"
    });
    sqlContent.value = formatted;
  } catch (error) {
    const reason = error instanceof Error && error.message
      ? error.message
      : t("sqlQuery.formatFail");
    showToast(t("sqlQuery.formatFail") + ": " + reason, "negative");
  }
}

// ── 对象树联动 ──

function handleTreeRun(sql: string) {
  sqlContent.value = sql;
  void handleExecute();
}

function handleInsert(text: string) {
  editorRef.value?.insertAtCursor(text);
}

function handleSchemaChange(schema: Record<string, string[]>) {
  schemaMap.value = schema;
}

// ============================================================
// 结果表格（q-table，排序/分页由表格自身承载）
// ============================================================

interface ResultColumn {
  name: string;
  label: string;
  field: string | ((row: Record<string, unknown>) => unknown);
  align: "left" | "right";
  sortable: boolean;
  style?: string;
}

const resultColumns = computed<ResultColumn[]>(() => {
  if (!queryResult.value) return [];
  const columns: ResultColumn[] = [{
    name: "__rowNum",
    label: "#",
    field: (row: Record<string, unknown>) => Number(row.__rowIndex) + 1,
    align: "right",
    sortable: false,
    style: "width: 50px"
  }];
  for (const col of queryResult.value.columns) {
    columns.push({ name: col, label: col, field: col, align: "left", sortable: true });
  }
  return columns;
});

function onResultPageChange(page: number) {
  resultPagination.value.page = Number(page);
}

function onResultRowsPerPageChange() {
  resultPagination.value.page = 1;
}

function handleResultJumpPage() {
  const page = Number(resultJumpPage.value);
  const maxPage = Math.ceil((queryResult.value?.rows.length ?? 0) / resultPagination.value.rowsPerPage) || 1;
  if (page && page >= 1 && page <= maxPage) {
    resultPagination.value.page = page;
  }
  resultJumpPage.value = null;
}

// ============================================================
// 导出 CSV
// ============================================================

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

// ============================================================
// 面板布局（左面板宽度 + 编辑器/结果纵向分栏）
// ============================================================

const sidebarVisible = ref(true);
const sidebarWidth = ref(300);
const editorPct = ref(45);
/** 拖拽进行中标记：为 true 时禁用 width 过渡，保证拖拽跟手不卡顿 */
const isResizing = ref(false);

function toggleSidebar() {
  sidebarVisible.value = !sidebarVisible.value;
}


/** 左侧面板拖拽调整宽度 */
let resizeStartX = 0;
let resizeStartWidth = 0;

function beginResize(e: PointerEvent) {
  e.preventDefault();
  resizeStartX = e.clientX;
  resizeStartWidth = sidebarWidth.value;
  isResizing.value = true;
  window.addEventListener("pointermove", onResizeMove, { capture: true });
  window.addEventListener("pointerup", endResize, { capture: true });
  window.addEventListener("pointercancel", endResize, { capture: true });
  document.body.style.cursor = "ew-resize";
  document.body.style.userSelect = "none";
}

function onResizeMove(e: PointerEvent) {
  const delta = e.clientX - resizeStartX;
  sidebarWidth.value = Math.round(Math.min(430, Math.max(300, resizeStartWidth + delta)));
}

function endResize() {
  window.removeEventListener("pointermove", onResizeMove, { capture: true });
  window.removeEventListener("pointerup", endResize, { capture: true });
  window.removeEventListener("pointercancel", endResize, { capture: true });
  isResizing.value = false;
  document.body.style.cursor = "";
  document.body.style.userSelect = "";
}

watch(
  [sidebarVisible, sidebarWidth, editorPct],
  savePrefs
);

onMounted(() => {
  void loadDatasourceOptions();
});
</script>

<template>
  <div class="sql-workbench">
    <!-- ═══ 左侧：对象浏览 ═══ -->
    <div class="left-panel" :class="{ 'left-panel--collapsed': !sidebarVisible, 'left-panel--no-transition': isResizing }">
      <!-- 折叠状态仅展示竖向提示条 -->
      <div v-if="!sidebarVisible" class="left-panel-collapsed-bar">
        <q-btn
          flat
          dense
          round
          icon="sym_r_chevron_right"
          size="20px"
          class="left-panel-toggle-btn"
          @click="toggleSidebar"
        >
          <q-tooltip anchor="center right" self="center left">{{
            t("sqlQuery.expandExplorer")
          }}</q-tooltip>
        </q-btn>
      </div>

      <template v-else>
        <!-- 头部：标题 + 折叠按钮 -->
        <div class="left-panel-header">
          <div class="left-panel-header-title row items-center no-wrap">
            <q-icon name="sym_r_database_search" size="20px" class="q-mr-xs" />
            <span>{{ t("sqlQuery.explorer") }}</span>
          </div>
          <q-btn
            flat
            dense
            round
            size="20px"
            icon="sym_r_chevron_left"
            class="left-panel-collapse-btn"
            @click="toggleSidebar"
          >
            <q-tooltip>{{ t("sqlQuery.collapseExplorer") }}</q-tooltip>
          </q-btn>
        </div>

        <!-- 工具区：数据源选择 + 刷新 + 筛选 -->
        <div class="left-panel-tools">
          <q-select
            v-model="selectedDatasource"
            filled
            square
            dense
            :options="datasourceOptions"
            :loading="datasourceLoading"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            hide-bottom-space
            clearable
            transition-show="jump-up"
            transition-hide="jump-down"
            class="status-select sq-ds-select"
            popup-content-class="status-select-popup"
          >
            <template #append>
              <q-icon
                name="sym_r_refresh"
                class="cursor-pointer q-field__focusable-action sq-ds-refresh"
                :class="{ 'sq-ds-refresh--loading': datasourceLoading }"
                size="24px"
                @click="loadDatasourceOptions(); refreshTree()"
              >
                <q-tooltip>{{ t('sqlQuery.datasourceRefresh') }}</q-tooltip>
              </q-icon>
            </template>
            <template v-slot:selected>
              <div v-if="currentDatasource" class="row items-center no-wrap">
                <DbTypeIcon :db-type="currentDatasource.dbType" :size="18" class="q-mr-xs" />
                <span class="ellipsis">{{ currentDatasource.label }}</span>
              </div>
              <span v-else class="status-placeholder">{{ t('sqlQuery.datasourcePlaceholder') }}</span>
            </template>
            <template v-slot:option="scope">
              <q-item v-bind="scope.itemProps">
                <q-item-section avatar style="min-width: auto; padding-right: 8px;">
                  <DbTypeIcon :db-type="scope.opt.dbType" :size="18" />
                </q-item-section>
                <q-item-section>{{ scope.opt.label }}</q-item-section>
              </q-item>
            </template>
          </q-select>

          <q-input
            v-model="treeFilter"
            filled
            square
            dense
            clearable
            hide-bottom-space
            :placeholder="t('sqlQuery.treeFilterPlaceholder')"
            class="sq-filter-input"
          >
            <template #prepend>
              <q-icon name="sym_r_search" size="16px" />
            </template>
          </q-input>
        </div>

        <!-- 对象树 -->
        <q-scroll-area
          class="left-panel-scroll"
          :content-style="treeContentStyle"
          :content-active-style="treeContentStyle"
        >
          <DbObjectTree
            ref="treePanelRef"
            :datasource-id="selectedDatasource"
            :db-type="currentDbType"
            :database="currentDatabase"
            :max-rows="maxRows"
            :filter="treeFilter"
            @run="handleTreeRun"
            @insert="handleInsert"
            @schema-change="handleSchemaChange"
          />
        </q-scroll-area>
      </template>

      <!-- 拖拽调整宽度手柄 -->
      <div
        v-if="sidebarVisible"
        class="left-panel-resize-handle"
        @pointerdown.prevent="beginResize"
      />
    </div>

    <!-- ═══ 右侧：操作栏 + SQL 编辑器 + 查询结果 ═══ -->
    <div class="right-panel">
      <!-- 操作栏 -->
      <div class="sq-opbar">
        <q-btn
          outline
          no-caps
          color="blue-6"
          class="sq-op-btn"
          @click="handleFormatSql"
        >
          <q-icon name="sym_r_format_align_left" size="18px" class="q-mr-xs" />
          {{ t('sqlQuery.formatSql') }}
        </q-btn>

        <q-btn
          unelevated
          no-caps
          color="primary"
          :loading="executing"
          class="sq-op-btn"
          @click="handleExecute"
        >
          <q-icon name="sym_r_play_arrow" size="20px" class="q-mr-xs" />
          {{ t('sqlQuery.execute') }}
        </q-btn>

        <q-btn
          outline
          no-caps
          color="deep-orange-6"
          class="sq-op-btn"
          @click="handleClear"
        >
          <q-icon name="sym_r_delete_forever" size="20px" class="q-mr-xs" />
          {{ t('sqlQuery.clear') }}
        </q-btn>

        <q-space />

        <q-select
          v-model="maxRows"
          filled
          square
          dense
          :options="MAX_ROWS_OPTIONS"
          :prefix="t('sqlQuery.maxRows')"
          hide-bottom-space
          class="status-select sq-maxrows-select"
          popup-content-class="status-select-popup"
        >
          <q-tooltip>{{ t('sqlQuery.maxRowsTip') }}</q-tooltip>
        </q-select>
      </div>

      <!-- 编辑器 / 结果 纵向分栏 -->
      <q-splitter
        v-model="editorPct"
        horizontal
        :limits="[20, 80]"
        class="sq-split-v"
      >
        <template #before>
          <section class="sq-pane sq-editor-pane">
            <div class="sq-pane-head">
              <q-icon name="sym_r_code" size="20px" class="sq-pane-head-icon" />
              <span class="sq-pane-head-title">{{ t('sqlQuery.sqlEditor') }}</span>
            </div>
            <div class="sq-editor-body">
              <SqlCodeEditor
                ref="editorRef"
                v-model="sqlContent"
                :db-type="currentDbType"
                :dark="dark"
                :placeholder-text="t('sqlQuery.sqlPlaceholder')"
                :schema="schemaMap"
                @execute="handleExecute"
              />
            </div>
          </section>
        </template>

        <template #separator>
          <div class="sq-split-grip sq-split-grip--h">
            <q-icon name="sym_r_drag_indicator" size="11px" />
          </div>
        </template>

        <template #after>
          <section class="sq-pane sq-result-pane">
            <q-linear-progress
              v-if="executing"
              indeterminate
              color="primary"
              class="sq-progress"
            />

            <div class="sq-result-head">
              <q-tabs
                v-model="resultTab"
                dense
                align="left"
                no-caps
                active-color="primary"
                indicator-color="primary"
                class="sq-result-tabs"
              >
                <q-tab name="result" @click="">
                  <div class="row items-center no-wrap">
                    <q-icon name="sym_r_table_chart" size="20px" class="q-mr-xs" />
                    <span>{{ t('sqlQuery.result') }}</span>
                  </div>
                </q-tab>
                <q-tab name="messages" @click="openMessagesTab">
                  <div class="row items-center no-wrap">
                    <q-icon name="sym_r_terminal" size="20px" class="q-mr-xs" />
                    <span>{{ t('sqlQuery.messages') }}</span>
                    <span v-if="unreadError" class="sq-tab-dot" />
                  </div>
                </q-tab>
              </q-tabs>

              <q-space />

              <template v-if="queryResult">
                <span class="sq-stat">{{ t('sqlQuery.rowCount', { count: queryResult.rowCount }) }}</span>
                <span class="sq-stat-sep">·</span>
                <span class="sq-stat">{{ t('sqlQuery.costMs', { ms: queryResult.costMs }) }}</span>
                <q-btn
                  v-if="queryResult.rows.length"
                  flat
                  dense
                  no-caps
                  size="sm"
                  color="primary"
                  icon="sym_r_download"
                  :label="t('sqlQuery.export')"
                  class="sq-export-btn"
                  @click="handleExport"
                />
              </template>
            </div>

            <q-tab-panels v-model="resultTab" class="sq-result-panels">
              <!-- 结果表格 -->
              <q-tab-panel name="result" class="sq-result-panel">
                <q-table
                  v-if="queryResult"
                  :key="resultNonce"
                  v-model:pagination="resultPagination"
                  flat
                  :rows="queryResult.rows"
                  :columns="resultColumns"
                  row-key="__rowIndex"
                  :loading="executing"
                  :rows-per-page-options="[10, 20, 50, 100]"
                  :class="['sq-result-table', { 'sq-result-table--empty': !queryResult.rows.length }]"
                >
                  <template #body-cell="props">
                    <q-td :props="props">
                      <span v-if="props.col.name === '__rowNum'">{{ props.value }}</span>
                      <SqCellText v-else :value="props.value" />
                    </q-td>
                  </template>

                  <!-- 空数据 — 与驱动管理等列表页空态保持一致 -->
                  <template #no-data>
                    <div class="column items-center justify-center q-py-xl text-grey-7 empty-state-content">
                      <q-icon name="sym_r_database_search" size="56px" class="q-mb-sm" />
                      <div class="text-body1 text-weight-medium q-mb-xs">
                        {{ t('common.noData') }}
                      </div>
                      <div class="text-caption text-grey-6">
                        {{ t('common.noDataHint') }}
                      </div>
                    </div>
                  </template>

                  <!-- 自定义底部分页栏 — 与驱动管理等列表页保持一致 -->
                  <template #bottom="props">
                    <div class="row items-center full-width table-bottom">
                      <span>{{ t("common.totalRows", { count: queryResult.rows.length }) }}</span>
                      <q-space />
                      <q-pagination
                        v-model="resultPagination.page"
                        :max="props.pagesNumber"
                        :max-pages="7"
                        size="sm"
                        color="primary"
                        boundary-links
                        direction-links
                        icon-first="keyboard_double_arrow_left"
                        icon-prev="keyboard_arrow_left"
                        icon-next="keyboard_arrow_right"
                        icon-last="keyboard_double_arrow_right"
                        @update:model-value="onResultPageChange"
                      />
                      <span class="text-caption text-grey-7 q-ml-md q-mr-sm">{{ t("common.rowsPerPageLabel") }}</span>
                      <q-select
                        v-model="resultPagination.rowsPerPage"
                        :options="[10, 20, 50, 100]"
                        dense
                        flat
                        borderless
                        class="rows-per-page-select"
                        popup-content-class="rows-per-page-popup"
                        @update:model-value="onResultRowsPerPageChange"
                      >
                        <template #append>
                          <span class="text-caption">{{ t("common.rowsPerPageUnit") }}</span>
                        </template>
                      </q-select>
                      <span class="text-caption text-grey-7 q-ml-md">{{ t("common.jumpToLabel") }}</span>
                      <q-input
                        v-model.number="resultJumpPage"
                        dense
                        flat
                        borderless
                        class="jump-to-page-input"
                        input-class="text-center"
                        :placeholder="String((props.pagesNumber || 1) <= 1 ? 1 : (resultPagination.page >= (props.pagesNumber || 1) ? 1 : resultPagination.page + 1))"
                        @keyup.enter="handleResultJumpPage"
                      />
                      <span class="text-caption text-grey-7">{{ t("common.jumpToUnit") }}</span>
                    </div>
                  </template>
                </q-table>

                <!-- 欢迎空态 -->
                <div v-else class="sq-empty">
                  <div class="sq-empty-icon">
                    <q-icon name="sym_r_manage_search" size="34px" />
                  </div>
                  <div class="sq-empty-title">{{ t('sqlQuery.emptyTitle') }}</div>
                  <div class="sq-empty-desc">
                    {{ t('sqlQuery.emptyDesc') }}
                  </div>
                </div>
              </q-tab-panel>

              <!-- 执行消息 -->
              <q-tab-panel name="messages" class="sq-messages-panel">
                <div class="sq-messages">
                  <div
                    v-for="msg in messages"
                    :key="msg.id"
                    class="sq-msg"
                    :class="msg.ok ? 'sq-msg--ok' : 'sq-msg--fail'"
                  >
                    <div class="sq-msg-line">
                      <q-icon
                        :name="msg.ok ? 'sym_r_check_circle' : 'sym_r_error'"
                        size="15px"
                        class="sq-msg-icon"
                      />
                      <span class="sq-msg-time">{{ msg.time }}</span>
                      <span class="sq-msg-text">{{ msg.text }}</span>
                    </div>
                    <div v-if="msg.sql" class="sq-msg-sql">{{ msg.sql }}</div>
                  </div>
                  <div v-if="!messages.length" class="sq-no-data">
                    {{ t('common.noData') }}
                  </div>
                </div>
              </q-tab-panel>
            </q-tab-panels>
          </section>
        </template>
      </q-splitter>
    </div>
  </div>
</template>

<style scoped>
/* ═══ 工作台壳层 — 与用户管理页面 .user-list-shell 保持一致 ═══ */
.sql-workbench {
  display: flex;
  height: calc(100vh - 64px - 40px - 44px - 16px);
  min-height: 0;
  gap: 8px;
}

/* ═══ 左侧面板 — 与用户管理页面 .left-panel 保持一致 ═══ */
.left-panel {
  position: relative;
  flex-shrink: 0;
  width: v-bind(sidebarWidth + 'px');
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
  overflow: hidden;
  transition: width 0.22s ease;
  will-change: width;
}

.left-panel--no-transition {
  transition: none !important;
}

.left-panel--collapsed {
  width: 40px;
  min-width: 40px;
  background: #fafafa;
}

.left-panel-collapsed-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
}

/* ═══ 图标按钮 — 与用户管理页面 .left-panel-collapse-btn 保持一致 ═══ */
.left-panel-toggle-btn,
.left-panel-collapse-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.87);
  border-radius: 50%;
}

.left-panel-toggle-btn :deep(.q-btn__wrapper),
.left-panel-collapse-btn :deep(.q-btn__wrapper) {
  min-height: 32px;
  padding: 0;
}

.left-panel-toggle-btn :deep(.q-icon.material-symbols-rounded),
.left-panel-toggle-btn :deep(.material-symbols-rounded),
.left-panel-collapse-btn :deep(.q-icon.material-symbols-rounded),
.left-panel-collapse-btn :deep(.material-symbols-rounded) {
  font-size: 20px !important;
}

.left-panel-toggle-btn:hover,
.left-panel-collapse-btn:hover {
  background: rgba(128, 128, 128, 0.28);
}

/* 左侧面板头部 */
.left-panel-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px;
  padding: 0 8px 0 12px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.left-panel-header-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

/* 左侧面板工具区 */
.left-panel-tools {
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.sq-ds-select {
  flex: 1 1 auto;
  min-width: 0;
}

.sq-ds-select.status-select :deep(.q-field__control) {
  min-height: 32px;
  min-width: 0;
}

/* 刷新按钮 — 与 clearable 清空按钮（.q-field__focusable-action）交互完全一致：
   颜色继承字段控件，悬停不变色而是 opacity 0.6 → 1，无背景圆环 */
.sq-ds-refresh {
  color: inherit;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.sq-ds-refresh:hover {
  opacity: 1;
}

.sq-ds-refresh--loading {
  animation: sq-spin 0.8s linear infinite;
}

@keyframes sq-spin {
  to {
    transform: rotate(360deg);
  }
}

.sq-filter-input {
  flex: 1 1 100%;
}

.sq-filter-input :deep(.q-field__control) {
  min-height: 32px;
}

/* 左侧面板滚动区 */
.left-panel-scroll {
  flex: 1 1 auto;
  min-height: 0;
}

/* 拖拽手柄 */
.left-panel-resize-handle {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 6px;
  cursor: ew-resize;
  z-index: 10;
  touch-action: none;
}

/* ═══ 右侧面板 — 与用户管理页面 .right-panel 保持一致 ═══ */
.right-panel {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 操作栏 */
.sq-opbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 48px;
  padding: 0 12px;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
  margin-bottom: 8px;
}

.sq-op-btn {
  height: 32px;
  padding: 0 14px;
  font-size: 13px;
}

.sq-maxrows-select {
  width: 190px;
}

.sq-maxrows-select.status-select :deep(.q-field__control) {
  min-height: 32px;
  min-width: 0;
}

/* 纵向分栏 */
.sq-split-v {
  flex: 1 1 auto;
  min-height: 0;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
  overflow: hidden;
}

.sq-split-v,
.sq-split-v :deep(.q-splitter__panel) {
  height: 100%;
}

.sq-split-v :deep(.q-splitter__separator) {
  height: 5px;
  background: rgba(0, 0, 0, 0.08);
}

.sq-split-grip {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: rgba(0, 0, 0, 0.35);
}

.sq-split-grip--h {
  flex-direction: row;
}

/* 面板通用 */
.sq-pane {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  position: relative;
  background: #fff;
}

/* 即便父级高度链异常，编辑器与结果面板也保持可交互的最小高度 */
.sq-editor-pane {
  min-height: 180px;
}

.sq-result-pane {
  min-height: 220px;
}

/* 面板头部 — 与用户管理页面 .search-area-header 保持一致 */
.sq-pane-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 40px;
  padding: 0 8px 0 12px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.sq-pane-head-icon {
  color: rgba(0, 0, 0, 0.55);
}

.sq-pane-head-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.sq-editor-body {
  flex: 1 1 auto;
  min-height: 0;
}

.sq-progress {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
}

/* 结果面板头：标签 + 统计 — 与 .sq-pane-head 保持一致 */
.sq-result-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 8px 0 12px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.sq-result-tabs {
  flex: 0 0 auto;
  height: 100%;
}

.sq-tab-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #e53935;
  margin-left: 6px;
}

.sq-stat {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.55);
  margin-left: 14px;
  display: inline-flex;
  align-items: center;
}

.sq-stat-sep {
  color: rgba(0, 0, 0, 0.25);
  margin-left: 6px;
}

/* 导出按钮 — 与前面统计文案保持间距，避免重叠 */
.sq-export-btn {
  margin-left: 16px;
}

/* 结果/消息面板 */
.sq-result-panels {
  flex: 1 1 auto;
  min-height: 0;
}

.sq-result-panels :deep(.q-tab-panels__panel) {
  height: 100%;
}

.sq-result-panel,
.sq-messages-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
}

/* 结果表格 — 与用户管理页面 .user-table 保持一致 */
.sq-result-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.sq-result-table :deep(.q-table__top) {
  display: none;
}

.sq-result-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sq-result-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.sq-result-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.sq-result-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
  width: auto !important;
}

.sq-result-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.sq-result-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* 空数据状态 */
.sq-result-table--empty :deep(.q-table__container) {
  height: 100%;
}

.sq-result-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.sq-result-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.sq-result-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

.sq-result-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.sq-result-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

.sq-result-table :deep(.q-table__bottom) {
  padding: 3px 16px 4px;
  font-size: 13px;
  min-height: 42px;
  background: #fff;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.sq-null {
  font-style: italic;
  color: rgba(0, 0, 0, 0.35);
}

.sq-no-data {
  padding: 24px;
  text-align: center;
  font-size: 12.5px;
  color: rgba(0, 0, 0, 0.45);
}

/* 空数据内容 — 与驱动管理等列表页保持一致 */
.empty-state-content {
  text-align: center;
}

/* 分页底栏 — 与驱动管理等列表页保持一致 */
.table-bottom {
  min-height: 40px;
}

.table-bottom :deep(.q-pagination__content .q-btn) {
  width: 30px !important;
  height: 30px !important;
  min-width: 30px !important;
  min-height: 30px !important;
  border-radius: 50% !important;
  padding: 0 !important;
  font-size: 10px !important;
}

.table-bottom :deep(.q-pagination__content .q-btn .q-focus-helper) {
  border-radius: 50%;
}

.table-bottom :deep(.q-pagination__content .q-btn .q-icon) {
  font-size: 20px;
}

.table-bottom :deep(.q-pagination__content .q-btn.q-btn--standard) {
  font-weight: 700;
}

.table-bottom :deep(.rows-per-page-select .q-field__control) {
  min-height: 24px;
  padding: 0;
  height: 24px;
}

.table-bottom :deep(.rows-per-page-select .q-field__native) {
  min-height: 24px;
  font-size: 12px;
  padding: 0;
}

.table-bottom :deep(.rows-per-page-select .q-field__marginal) {
  height: 24px;
}

.table-bottom :deep(.jump-to-page-input) {
  width: 40px;
  font-size: 12px;
}

.table-bottom :deep(.jump-to-page-input .q-field__control) {
  min-height: 24px;
  padding: 0;
  height: 24px;
}

.table-bottom :deep(.jump-to-page-input .q-field__native) {
  min-height: 24px;
  font-size: 12px;
  padding: 0;
}

/* 空态 */
.sq-empty {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 24px;
}

.sq-empty-icon {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: rgba(0, 0, 0, 0.28);
  background: rgba(0, 0, 0, 0.04);
}

.sq-empty-title {
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.72);
}

.sq-empty-desc {
  font-size: 12.5px;
  color: rgba(0, 0, 0, 0.42);
}

/* 消息面板 */
.sq-messages {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  padding: 8px 12px;
  font-family: "SF Mono", "Monaco", "Menlo", "Consolas", monospace;
  font-size: 12px;
}

.sq-msg {
  padding: 5px 0;
  border-bottom: 1px dashed rgba(0, 0, 0, 0.05);
}

.sq-msg-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.sq-msg-icon {
  align-self: center;
}

.sq-msg--ok .sq-msg-icon {
  color: #2e7d32;
}

.sq-msg--fail .sq-msg-icon {
  color: #e53935;
}

.sq-msg-time {
  flex-shrink: 0;
  color: rgba(0, 0, 0, 0.38);
}

.sq-msg-text {
  color: rgba(0, 0, 0, 0.82);
  word-break: break-all;
}

.sq-msg--fail .sq-msg-text {
  color: #c62828;
}

.sq-msg-sql {
  margin: 4px 0 0 23px;
  padding: 4px 8px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.035);
  color: rgba(0, 0, 0, 0.55);
  white-space: pre-wrap;
  word-break: break-all;
}

/* 暗色模式 — 与用户管理页面设计令牌保持一致 */
.body--dark .left-panel,
.body--dark .sq-opbar,
.body--dark .sq-split-v,
.body--dark .sq-pane {
  background: #1e1e1e !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .left-panel-header,
.body--dark .sq-pane-head,
.body--dark .sq-result-head {
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .left-panel-header-title,
.body--dark .sq-pane-head-title {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .left-panel-toggle-btn,
.body--dark .left-panel-collapse-btn {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .left-panel-toggle-btn:hover,
.body--dark .left-panel-collapse-btn:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .left-panel--collapsed {
  background: #252525 !important;
}

.body--dark .sq-split-v :deep(.q-splitter__separator) {
  background: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .sq-pane-head-icon {
  color: rgba(255, 255, 255, 0.55) !important;
}

.body--dark .sq-stat {
  color: rgba(255, 255, 255, 0.55) !important;
}

.body--dark .sq-null {
  color: rgba(255, 255, 255, 0.35) !important;
}

.body--dark .sq-result-table {
  background: #1e1e1e !important;
  border-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .sq-result-table :deep(thead tr th) {
  background: #252525 !important;
  color: rgba(255, 255, 255, 0.8) !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .sq-result-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.06) !important;
}

.body--dark .sq-result-table :deep(tbody td) {
  border-bottom-color: rgba(255, 255, 255, 0.12) !important;
}

.body--dark .sq-result-table :deep(.q-table__bottom) {
  background: #1e1e1e !important;
  color: rgba(255, 255, 255, 0.72);
  border-top-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .sq-result-table :deep(.q-table__bottom .q-pagination .q-btn:not(.q-btn--flat)) {
  background: rgba(0, 150, 136, 0.85) !important;
  color: #fff !important;
}

.body--dark .sq-result-table :deep(.q-table__bottom .q-pagination .q-btn.q-btn--flat) {
  color: rgba(0, 150, 136, 0.85) !important;
}

.body--dark .empty-state-content {
  color: rgba(255, 255, 255, 0.55) !important;
}

.body--dark .sq-result-table :deep(.empty-state-content .text-grey-6) {
  color: rgba(255, 255, 255, 0.45) !important;
}

.body--dark .table-bottom :deep(.text-grey-7) {
  color: rgba(255, 255, 255, 0.55) !important;
}

.body--dark .sq-no-data {
  color: rgba(255, 255, 255, 0.45) !important;
}

.body--dark .sq-empty-title {
  color: rgba(255, 255, 255, 0.72) !important;
}

.body--dark .sq-empty-desc {
  color: rgba(255, 255, 255, 0.42) !important;
}

.body--dark .sq-empty-icon {
  color: rgba(255, 255, 255, 0.28) !important;
  background: rgba(255, 255, 255, 0.04) !important;
}

.body--dark .sq-msg-time {
  color: rgba(255, 255, 255, 0.38) !important;
}

.body--dark .sq-msg-text {
  color: rgba(255, 255, 255, 0.82) !important;
}

.body--dark .sq-msg-sql {
  background: rgba(255, 255, 255, 0.035) !important;
  color: rgba(255, 255, 255, 0.55) !important;
}

.body--dark .sq-msg {
  border-bottom-color: rgba(255, 255, 255, 0.05) !important;
}
</style>

<!-- 非 scoped：状态选择下拉弹出层（teleport 到 body） -->
<style>
.status-select-popup .q-item {
  min-height: 40px;
  padding: 0 16px;
}

.rows-per-page-popup .q-item {
  min-height: 36px;
  padding: 0 16px;
}
</style>
