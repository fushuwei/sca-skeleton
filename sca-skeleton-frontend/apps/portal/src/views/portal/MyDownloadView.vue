<script setup lang="ts">
import { ref, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useQuasar } from "quasar";

const { t } = useI18n({ useScope: "global" });
const $q = useQuasar();

type ResourceType = "table" | "api" | "file" | "report";
type DownloadStatus = "success" | "failed" | "expired";

interface DownloadRow {
  resource: string;
  type: ResourceType;
  size: string;
  downloadTime: string;
  status: DownloadStatus;
}

const downloads: DownloadRow[] = [
  { resource: "本科生招生数据（2015-2025）", type: "table", size: "12.5 MB", downloadTime: "2026-07-22 09:32:18", status: "success" },
  { resource: "科研项目立项明细", type: "table", size: "8.3 MB", downloadTime: "2026-07-21 15:10:42", status: "success" },
  { resource: "教职工信息表", type: "file", size: "5.7 MB", downloadTime: "2026-07-20 11:25:33", status: "success" },
  { resource: "学生成绩分析数据集", type: "report", size: "3.2 MB", downloadTime: "2026-07-19 14:48:09", status: "failed" },
  { resource: "财务预算执行情况", type: "table", size: "6.8 MB", downloadTime: "2026-07-15 10:22:55", status: "expired" },
  { resource: "教务管理系统 API 文档", type: "api", size: "1.4 MB", downloadTime: "2026-07-14 16:35:20", status: "success" }
];

const typeColorMap: Record<string, string> = {
  table: "blue",
  api: "purple",
  file: "teal",
  report: "orange"
};

const statusColorMap: Record<string, string> = {
  success: "teal",
  failed: "red",
  expired: "orange"
};

function typeLabel(type: ResourceType): string {
  const map: Record<ResourceType, string> = {
    table: t("resourceCatalog.typeTable"),
    api: t("resourceCatalog.typeApi"),
    file: t("resourceCatalog.typeFile"),
    report: t("dataMarket.tabReport")
  };
  return map[type];
}

function statusLabel(status: DownloadStatus): string {
  const map: Record<DownloadStatus, string> = {
    success: t("myDownload.statusSuccess"),
    failed: t("myDownload.statusFailed"),
    expired: t("myDownload.statusExpired")
  };
  return map[status];
}

const columns = computed(() => [
  { name: "resource", label: t("myDownload.colResource"), field: "resource", align: "left" as const, sortable: true },
  { name: "type", label: t("myDownload.colType"), field: "type", align: "left" as const, sortable: true },
  { name: "size", label: t("myDownload.colSize"), field: "size", align: "right" as const, sortable: true },
  { name: "downloadTime", label: t("myDownload.colDownloadTime"), field: "downloadTime", align: "left" as const, sortable: true },
  { name: "status", label: t("myDownload.colStatus"), field: "status", align: "left" as const, sortable: true },
  { name: "actions", label: t("myDownload.colActions"), field: "actions", align: "center" as const }
]);

const tableTotal = computed(() => downloads.length);

const tablePagination = ref({
  page: 1,
  rowsPerPage: 10,
  sortBy: "downloadTime",
  descending: true
});
const curPage = ref(1);
const jumpToPage = ref<number | null>(null);

function onPageChange(page: number): void {
  curPage.value = Number(page);
  tablePagination.value.page = Number(page);
}

function handleJumpToPage(): void {
  const page = Number(jumpToPage.value);
  const maxPage = Math.ceil(tableTotal.value / tablePagination.value.rowsPerPage);
  if (page && page >= 1 && page <= maxPage) {
    curPage.value = page;
    tablePagination.value.page = page;
  }
  jumpToPage.value = null;
}

function onRowsPerPageChange(): void {
  curPage.value = 1;
  tablePagination.value.page = 1;
}

function reDownload(row: DownloadRow): void {
  if (row.status === "expired") {
    $q.notify({ type: "warning", message: t("myDownload.statusExpired"), position: "top" });
    return;
  }
  $q.notify({ type: "positive", message: `${t("myDownload.reDownload")}: ${row.resource}`, position: "top" });
}

function copyLink(row: DownloadRow): void {
  const link = `https://data.donghu.edu.cn/dl/${row.resource.replace(/\s/g, "-").toLowerCase()}`;
  navigator.clipboard.writeText(link).then(() => {
    $q.notify({ type: "positive", message: t("common.copied"), position: "top" });
  }).catch(() => {
    $q.notify({ type: "positive", message: t("common.copied"), position: "top" });
  });
}
</script>

<template>
  <div class="my-download-page-wrapper">
    <header class="page-header">
      <h1 class="page-title">{{ t("myDownload.pageTitle") }}</h1>
      <p class="page-desc">{{ t("myDownload.pageDesc") }}</p>
    </header>

    <div class="my-download-list-shell">
      <q-table
        :rows="downloads"
        :columns="columns"
        v-model:pagination="tablePagination"
        row-key="resource"
        :rows-per-page-options="[10, 20, 50, 100]"
        flat
        :class="['download-table', { 'download-table--empty': !downloads.length }]"
      >
        <template #body-cell-type="props">
          <q-td :props="props">
            <q-badge
              :color="typeColorMap[props.row.type]"
              :label="typeLabel(props.row.type)"
              rounded
              class="download-type-badge"
            />
          </q-td>
        </template>

        <template #body-cell-size="props">
          <q-td :props="props" class="mono-cell">{{ props.row.size }}</q-td>
        </template>

        <template #body-cell-status="props">
          <q-td :props="props">
            <q-badge
              :color="statusColorMap[props.row.status]"
              :label="statusLabel(props.row.status)"
              rounded
              class="download-type-badge"
            />
          </q-td>
        </template>

        <template #body-cell-actions="props">
          <q-td :props="props" class="q-gutter-x-xs actions-cell">
            <q-btn flat dense round size="sm" color="teal" icon="sym_r_download" @click.stop="reDownload(props.row)">
              <q-tooltip>{{ t('myDownload.reDownload') }}</q-tooltip>
            </q-btn>
            <q-btn flat dense round size="sm" color="grey-7" icon="sym_r_content_copy" @click.stop="copyLink(props.row)">
              <q-tooltip>{{ t('myDownload.copyLink') }}</q-tooltip>
            </q-btn>
          </q-td>
        </template>

        <template #no-data>
          <div class="column items-center justify-center q-py-xl text-grey-7 empty-state-content">
            <q-icon name="sym_r_database_search" size="56px" class="q-mb-sm" />
            <div class="text-body1 text-weight-medium q-mb-xs">
              {{ t("common.noData") }}
            </div>
            <div class="text-caption text-grey-6">
              {{ t("common.noDataHint") }}
            </div>
          </div>
        </template>

        <template #bottom="props">
          <div class="row items-center full-width table-bottom">
            <span>
              {{ t("common.totalRows", { count: tableTotal }) }}
            </span>
            <q-space />
            <q-pagination
              v-model="curPage"
              :max="props.pagesNumber"
              size="sm"
              color="primary"
              boundary-links
              direction-links
              icon-first="keyboard_double_arrow_left"
              icon-prev="keyboard_arrow_left"
              icon-next="keyboard_arrow_right"
              icon-last="keyboard_double_arrow_right"
              @update:model-value="onPageChange"
            />
            <span class="text-caption text-grey-7 q-ml-md q-mr-sm">{{ t("common.rowsPerPageLabel") }}</span>
            <q-select
              v-model="tablePagination.rowsPerPage"
              :options="[10, 20, 50, 100]"
              dense
              flat
              borderless
              class="rows-per-page-select"
              popup-content-class="rows-per-page-popup"
              @update:model-value="onRowsPerPageChange"
            >
              <template #append>
                <span class="text-caption">{{ t("common.rowsPerPageUnit") }}</span>
              </template>
            </q-select>
            <span class="text-caption text-grey-7 q-ml-md">{{ t("common.jumpToLabel") }}</span>
            <q-input
              v-model.number="jumpToPage"
              dense
              flat
              borderless
              class="jump-to-page-input"
              input-class="text-center"
              :placeholder="String((props.pagesNumber || 1) <= 1 ? 1 : (curPage >= (props.pagesNumber || 1) ? 1 : curPage + 1))"
              @keyup.enter="handleJumpToPage"
            />
            <span class="text-caption text-grey-7">{{ t("common.jumpToUnit") }}</span>
          </div>
        </template>
      </q-table>
    </div>
  </div>
</template>

<style scoped>
/* ═══ 整体壳层 ═══ */
.my-download-page-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 8px 24px;
  background: #f5f5f5;
}

.body--dark .my-download-page-wrapper {
  background: #1a1a1a;
}

/* ═══ 页面头部 ═══ */
.page-header {
  flex-shrink: 0;
  margin-bottom: 8px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  margin: 0 0 4px 0;
  line-height: 1.4;
}

.body--dark .page-title {
  color: rgba(255, 255, 255, 0.92);
}

.page-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  margin: 0;
  line-height: 1.5;
}

.body--dark .page-desc {
  color: rgba(255, 255, 255, 0.55);
}

/* ═══ 列表壳层 ═══ */
.my-download-list-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* ── 表格 ── */
.download-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.body--dark .download-table {
  background: #1e1e1e;
  border-color: rgba(255, 255, 255, 0.08);
}

.download-table :deep(.q-table__top) {
  display: none;
}

.download-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.download-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.download-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.download-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.download-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.body--dark .download-table :deep(thead tr th) {
  color: rgba(255, 255, 255, 0.8) !important;
  background: #2a2a2a !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.download-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ── 空数据状态 ── */
.download-table--empty :deep(.q-table__container) {
  height: 100%;
}

.download-table--empty :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
}

.download-table--empty :deep(.q-table__bottom) {
  flex: 0 0 auto;
}

/* 行悬停 */
.download-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.download-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

.body--dark .download-table :deep(tbody td) {
  border-bottom-color: rgba(255, 255, 255, 0.12) !important;
}

/* Badge 统一样式 */
.download-type-badge {
  font-size: 11px;
  padding: 3px 10px;
  font-weight: 500;
}

/* 操作按钮列 */
.actions-cell {
  white-space: nowrap;
}

.actions-cell :deep(.q-btn) {
  width: 32px;
  height: 32px;
}

.actions-cell :deep(.q-btn .q-icon) {
  font-size: 20px;
}

/* 空数据内容 */
.empty-state-content {
  text-align: center;
}

/* 分页底栏 */
.download-table :deep(.q-table__bottom) {
  padding: 3px 16px 4px;
  font-size: 13px;
  min-height: 42px;
  background: #fff;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.body--dark .download-table :deep(.q-table__bottom) {
  background: #1e1e1e;
  border-top-color: rgba(255, 255, 255, 0.08);
}

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

/* 单元格元素 */
.mono-cell {
  font-family: "JetBrains Mono", monospace;
  font-size: 13px;
}
</style>

<!-- 非 scoped：下拉弹出层 -->
<style>
.rows-per-page-popup .q-item {
  min-height: 36px;
  padding: 0 16px;
}
</style>
