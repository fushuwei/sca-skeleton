<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useI18n } from "vue-i18n";
import { Dark } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { Datasource, SqlExecuteResponse } from "../../apis/datasource";
import { getDatasourcePageApi, executeSqlApi } from "../../apis/datasource";
import SqlCodeEditor from "./sql-query/SqlCodeEditor.vue";
import DbObjectTree from "./sql-query/DbObjectTree.vue";

const { t } = useI18n({ useScope: "global" });

const isMac = /Mac|iPhone|iPad/i.test(navigator.platform || navigator.userAgent);
const execShortcut = isMac ? "⌘ + Enter" : "Ctrl + Enter";

// ============================================================
// 工作台偏好持久化
// ============================================================

const PREFS_KEY = "sqlQueryWorkbench";

interface WorkbenchPrefs {
  datasource?: string;
  sql?: string;
  maxRows?: number;
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
      datasource: selectedDatasource.value,
      sql: sqlContent.value,
      maxRows: maxRows.value,
      sidebarVisible: sidebarVisible.value,
      sidebarWidth: sidebarWidth.value || prefs.sidebarWidth,
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
const selectedDatasource = ref<string>(prefs.datasource || "");

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

function refreshTree() {
  treePanelRef.value?.refresh();
}

// ============================================================
// 编辑器 / 查询执行
// ============================================================

const editorRef = ref<InstanceType<typeof SqlCodeEditor>>();
const sqlContent = ref(prefs.sql || "");
const schemaMap = ref<Record<string, string[]>>({});

const MAX_ROWS_OPTIONS = [100, 500, 1000, 2000, 5000, 10_000];
const maxRows = ref<number>(prefs.maxRows && MAX_ROWS_OPTIONS.includes(prefs.maxRows) ? prefs.maxRows : 1000);

const executing = ref(false);
const queryResult = ref<SqlExecuteResponse | null>(null);
/** 每次成功查询递增，作为结果表 key 重置排序与分页状态 */
const resultNonce = ref(0);

interface ExecMessage {
  id: number;
  time: string;
  ok: boolean;
  text: string;
  sql?: string;
}

const messages = ref<ExecMessage[]>([]);
const messagesEl = ref<HTMLElement>();
const unreadError = ref(false);
let messageId = 0;

function pushMessage(ok: boolean, text: string, sql?: string) {
  messages.value.push({
    id: ++messageId,
    time: new Date().toTimeString().slice(0, 8),
    ok,
    text,
    sql: sql && sql.length > 500 ? sql.slice(0, 500) + " …" : sql
  });
  void nextTick(() => {
    messagesEl.value?.scrollTo({ top: messagesEl.value.scrollHeight });
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
}

const resultColumns = computed<ResultColumn[]>(() => {
  if (!queryResult.value) return [];
  const columns: ResultColumn[] = [{
    name: "__rowNum",
    label: "#",
    field: (row: Record<string, unknown>) => Number(row.__rowIndex) + 1,
    align: "right",
    sortable: false
  }];
  for (const col of queryResult.value.columns) {
    columns.push({ name: col, label: col, field: col, align: "left", sortable: true });
  }
  return columns;
});

const isTruncated = computed(() =>
  !!queryResult.value && queryResult.value.rowCount >= maxRows.value
);

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
// 面板布局（可拖拽分栏）
// ============================================================

const sidebarVisible = ref(prefs.sidebarVisible ?? true);
const sidebarWidth = ref(prefs.sidebarVisible === false ? 0 : prefs.sidebarWidth || 270);
const editorPct = ref(prefs.editorPct || 45);

function toggleSidebar() {
  sidebarVisible.value = !sidebarVisible.value;
  sidebarWidth.value = sidebarVisible.value ? prefs.sidebarWidth || 270 : 0;
}

watch(
  sidebarWidth,
  (width) => {
    if (width > 0) {
      sidebarVisible.value = true;
      prefs.sidebarWidth = width;
    } else {
      // 拖拽分栏到最左侧等价于折叠侧栏
      sidebarVisible.value = false;
    }
  }
);

watch(
  [selectedDatasource, sqlContent, maxRows, sidebarVisible, sidebarWidth, editorPct],
  savePrefs
);

onMounted(() => {
  void loadDatasourceOptions();
});
</script>

<template>
  <div class="sql-workbench">
    <q-splitter
      v-model="sidebarWidth"
      unit="px"
      :limits="[0, 420]"
      class="sq-split"
    >
      <!-- ══ 左侧：对象浏览（标题工具栏 + 对象树） ══ -->
      <template #before>
        <aside v-show="sidebarVisible" class="sq-left">
          <div class="sq-left-head">
            <q-icon name="sym_r_account_tree" size="16px" class="sq-left-head-icon" />
            <span class="sq-left-head-title">{{ t('sqlQuery.explorer') }}</span>
          </div>

          <div class="sq-left-tools">
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
              <template v-if="!selectedDatasource" v-slot:selected>
                <span class="status-placeholder">{{ t('sqlQuery.datasourcePlaceholder') }}</span>
              </template>
            </q-select>

            <q-btn
              flat
              dense
              round
              size="sm"
              icon="sym_r_refresh"
              class="sq-tool-btn"
              :loading="datasourceLoading"
              @click="loadDatasourceOptions(); refreshTree()"
            >
              <q-tooltip>{{ t('sqlQuery.datasourceRefresh') }}</q-tooltip>
            </q-btn>

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

          <div class="sq-left-body">
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
          </div>
        </aside>
      </template>

      <template #separator>
        <div class="sq-split-grip sq-split-grip--v">
          <q-icon name="sym_r_drag_indicator" size="11px" />
        </div>
      </template>

      <!-- ══ 右侧：操作栏 + SQL 编辑器 + 查询结果 ══ -->
      <template #after>
        <div class="sq-right">
          <!-- 操作栏 -->
          <div class="sq-opbar">
            <q-btn
              flat
              dense
              round
              size="sm"
              :icon="sidebarVisible ? 'sym_r_left_panel_close' : 'sym_r_left_panel_open'"
              class="sq-tool-btn"
              @click="toggleSidebar"
            >
              <q-tooltip>{{ t('sqlQuery.explorer') }}</q-tooltip>
            </q-btn>

            <q-separator vertical inset spaced />

            <q-btn
              unelevated
              no-caps
              color="primary"
              :loading="executing"
              class="sq-exec-btn"
              @click="handleExecute"
            >
              <q-icon name="sym_r_play_arrow" size="18px" class="q-mr-xs" />
              {{ executing ? t('sqlQuery.executing') : t('sqlQuery.execute') }}
              <q-tooltip>{{ execShortcut }}</q-tooltip>
            </q-btn>

            <q-btn
              outline
              dense
              no-caps
              color="grey-7"
              icon="sym_r_delete_sweep"
              :label="t('sqlQuery.clear')"
              @click="handleClear"
            />

            <q-space />

            <q-badge color="amber-3" text-color="amber-10" class="sq-readonly-badge">
              {{ t('sqlQuery.onlyRead') }}
              <q-tooltip>{{ t('sqlQuery.onlyReadTip') }}</q-tooltip>
            </q-badge>

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
                  <q-icon name="sym_r_code" size="15px" class="sq-pane-head-icon" />
                  <span class="sq-pane-head-title">{{ t('sqlQuery.sqlEditor') }}</span>
                  <q-space />
                  <span class="sq-kbd-hint">{{ execShortcut }}</span>
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
                    <q-tab name="result" icon="sym_r_table_chart" :label="t('sqlQuery.result')" />
                    <q-tab name="messages" @click="openMessagesTab">
                      <q-icon name="sym_r_terminal" size="16px" class="q-mr-xs" />
                      <span>{{ t('sqlQuery.messages') }}</span>
                      <span v-if="unreadError" class="sq-tab-dot" />
                    </q-tab>
                  </q-tabs>

                  <q-space />

                  <template v-if="queryResult">
                    <span class="sq-stat">{{ t('sqlQuery.rowCount', { count: queryResult.rowCount }) }}</span>
                    <span class="sq-stat">{{ t('sqlQuery.costMs', { ms: queryResult.costMs }) }}</span>
                    <span v-if="isTruncated" class="sq-stat sq-stat--warn">
                      <q-icon name="sym_r_info" size="13px" class="q-mr-xs" />
                      {{ t('sqlQuery.truncated', { max: maxRows }) }}
                    </span>
                    <q-btn
                      v-if="queryResult.rows.length"
                      flat
                      dense
                      no-caps
                      size="sm"
                      color="primary"
                      icon="sym_r_download"
                      :label="t('sqlQuery.export')"
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
                      flat
                      dense
                      :rows="queryResult.rows"
                      :columns="resultColumns"
                      row-key="__rowIndex"
                      :loading="executing"
                      :pagination="{ rowsPerPage: 100 }"
                      :rows-per-page-options="[20, 50, 100, 200]"
                      class="sq-result-table"
                    >
                      <template #body-cell="props">
                        <q-td :props="props">
                          <span
                            v-if="props.value === null || props.value === undefined"
                            class="sq-null"
                          >NULL</span>
                          <template v-else>{{ props.value }}</template>
                        </q-td>
                      </template>
                      <template #no-data>
                        <div class="sq-no-data">{{ t('common.noData') }}</div>
                      </template>
                    </q-table>

                    <!-- 欢迎空态 -->
                    <div v-else class="sq-empty">
                      <div class="sq-empty-icon">
                        <q-icon name="sym_r_manage_search" size="34px" />
                      </div>
                      <div class="sq-empty-title">{{ t('sqlQuery.emptyTitle') }}</div>
                      <div class="sq-empty-desc">
                        {{ t('sqlQuery.emptyDesc', { key: execShortcut }) }}
                      </div>
                    </div>
                  </q-tab-panel>

                  <!-- 执行消息 -->
                  <q-tab-panel name="messages" class="sq-messages-panel">
                    <div ref="messagesEl" class="sq-messages">
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
      </template>
    </q-splitter>
  </div>
</template>

<style scoped>
/* ═══ 工作台壳层（高度与系统列表页约定一致，保证面板始终撑满视口） ═══ */
.sql-workbench {
  display: flex;
  height: calc(100vh - 64px - 40px - 44px - 16px);
  min-height: 560px;
}

.sq-split {
  width: 100%;
}

.sq-split,
.sq-split :deep(.q-splitter__panel),
.sq-split-v,
.sq-split-v :deep(.q-splitter__panel) {
  height: 100%;
}

.sq-split :deep(.q-splitter__separator) {
  width: 5px;
  background: rgba(0, 0, 0, 0.08);
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

.sq-split-grip--v {
  writing-mode: vertical-lr;
}

/* ═══ 左侧面板：对象浏览 ═══ */
.sq-left {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  min-height: 320px;
  background: #fafbfc;
}

.sq-left-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.sq-left-head-icon {
  color: rgba(0, 0, 0, 0.55);
}

.sq-left-head-title {
  font-size: 13px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.75);
}

.sq-left-tools {
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

.sq-filter-input {
  flex: 1 1 100%;
}

.sq-tool-btn {
  color: rgba(0, 0, 0, 0.55);
}

.sq-left-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}

/* ═══ 右侧面板 ═══ */
.sq-right {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
}

/* 操作栏 */
.sq-opbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 48px;
  padding: 0 10px;
  background: #fff;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.sq-exec-btn {
  height: 32px;
  padding: 0 14px;
  font-size: 13px;
}

.sq-readonly-badge {
  font-size: 11px;
  padding: 2px 8px;
}

.sq-maxrows-select {
  width: 190px;
}

.sq-split-v {
  flex: 1 1 auto;
  min-height: 0;
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

.sq-pane-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  background: #fafbfc;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.sq-pane-head-icon {
  color: rgba(0, 0, 0, 0.55);
}

.sq-pane-head-title {
  font-size: 12px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.72);
}

.sq-kbd-hint {
  font-size: 11px;
  color: rgba(0, 0, 0, 0.38);
  font-family: "SF Mono", "Monaco", "Menlo", "Consolas", monospace;
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

/* 结果面板头：标签 + 统计 */
.sq-result-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 38px;
  padding-right: 12px;
  background: #fafbfc;
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

.sq-stat--warn {
  color: #b26a00;
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

/* 结果表格（与系统列表页 q-table 规范一致） */
.sq-result-table {
  flex: 1 1 auto;
  min-height: 0;
}

.sq-result-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
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
</style>
