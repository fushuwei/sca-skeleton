<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useQuasar } from "quasar";

const { t } = useI18n({ useScope: "global" });
const $q = useQuasar();

type RequestType = "access" | "download" | "api";
type RequestStatus = "pending" | "approved" | "rejected" | "revoked";

interface RequestRow {
  requestNo: string;
  resource: string;
  type: RequestType;
  reason: string;
  status: RequestStatus;
  applyTime: string;
}

const requests: RequestRow[] = [
  { requestNo: "REQ202607220001", resource: "本科生招生数据（2015-2025）", type: "access", reason: "用于 2026 招生季数据分析与预测建模", status: "pending", applyTime: "2026-07-22 08:30:15" },
  { requestNo: "REQ202607210003", resource: "科研项目经费明细表", type: "download", reason: "学院年度科研经费执行情况审计", status: "approved", applyTime: "2026-07-21 14:22:08" },
  { requestNo: "REQ202607200007", resource: "教务管理系统 API v2.1", type: "api", reason: "对接学院教务看板，实时获取选课数据", status: "rejected", applyTime: "2026-07-20 10:15:42" },
  { requestNo: "REQ202607190012", resource: "教职工基本信息表", type: "access", reason: "人事处年度人员结构分析报告", status: "approved", applyTime: "2026-07-19 16:40:33" },
  { requestNo: "REQ202607180005", resource: "学生成绩分析数据集", type: "download", reason: "教学质量评估数据支撑", status: "pending", applyTime: "2026-07-18 09:05:21" },
  { requestNo: "REQ202607170009", resource: "财务预算执行情况", type: "access", reason: "编制下一年度部门预算参考", status: "revoked", applyTime: "2026-07-17 11:30:55" },
  { requestNo: "REQ202607160002", resource: "图书借阅统计分析", type: "download", reason: "图书馆年度阅读推广活动效果评估", status: "approved", applyTime: "2026-07-16 15:18:47" },
  { requestNo: "REQ202607150011", resource: "一卡通消费数据 API", type: "api", reason: "校园生活数据分析平台对接", status: "rejected", applyTime: "2026-07-15 13:25:10" }
];

const activeTab = ref<"all" | RequestStatus>("all");

const filteredRows = computed(() => {
  if (activeTab.value === "all") return requests;
  return requests.filter((r) => r.status === activeTab.value);
});

const tabCounts = computed(() => ({
  all: requests.length,
  pending: requests.filter((r) => r.status === "pending").length,
  approved: requests.filter((r) => r.status === "approved").length,
  rejected: requests.filter((r) => r.status === "rejected").length
}));

const typeColorMap: Record<string, string> = {
  access: "blue",
  download: "teal",
  api: "purple"
};

const statusColorMap: Record<string, string> = {
  pending: "orange",
  approved: "teal",
  rejected: "red",
  revoked: "grey"
};

function typeLabel(type: RequestType): string {
  const map: Record<RequestType, string> = {
    access: t("myRequest.typeAccess"),
    download: t("myRequest.typeDownload"),
    api: t("myRequest.typeApi")
  };
  return map[type];
}

function statusLabel(status: RequestStatus): string {
  const map: Record<RequestStatus, string> = {
    pending: t("myRequest.statusPending"),
    approved: t("myRequest.statusApproved"),
    rejected: t("myRequest.statusRejected"),
    revoked: t("myRequest.statusRevoked")
  };
  return map[status];
}

const columns = computed(() => [
  { name: "requestNo", label: t("myRequest.colRequestNo"), field: "requestNo", align: "left" as const, sortable: true },
  { name: "resource", label: t("myRequest.colResource"), field: "resource", align: "left" as const, sortable: true },
  { name: "type", label: t("myRequest.colType"), field: "type", align: "left" as const, sortable: true },
  { name: "reason", label: t("myRequest.colReason"), field: "reason", align: "left" as const },
  { name: "status", label: t("myRequest.colStatus"), field: "status", align: "left" as const, sortable: true },
  { name: "applyTime", label: t("myRequest.colApplyTime"), field: "applyTime", align: "left" as const, sortable: true },
  { name: "actions", label: t("myRequest.colActions"), field: "actions", align: "center" as const }
]);

const tableTotal = computed(() => filteredRows.value.length);

const tablePagination = ref({
  page: 1,
  rowsPerPage: 10,
  sortBy: "applyTime",
  descending: true
});
const curPage = ref(1);
const jumpToPage = ref<number | null>(null);

watch(activeTab, () => {
  tablePagination.value.page = 1;
  curPage.value = 1;
});

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

function viewDetail(row: RequestRow): void {
  $q.notify({ type: "info", message: `${t("myRequest.viewDetail")}: ${row.requestNo}`, position: "top" });
}

function revoke(row: RequestRow): void {
  $q.notify({ type: "warning", message: `${t("myRequest.revoke")}: ${row.requestNo}`, position: "top" });
}
</script>

<template>
  <div class="my-request-page-wrapper">
    <header class="page-header">
      <h1 class="page-title">{{ t("myRequest.pageTitle") }}</h1>
      <p class="page-desc">{{ t("myRequest.pageDesc") }}</p>
    </header>

    <!-- ── Tab 筛选 ── -->
    <q-tabs
      v-model="activeTab"
      dense
      no-caps
      align="left"
      class="filter-tabs"
      active-color="primary"
      indicator-color="primary"
    >
      <q-tab name="all" class="filter-tab">
        <div class="tab-label">
          <span>{{ t('myRequest.tabAll') }}</span>
          <span class="tab-count">{{ tabCounts.all }}</span>
        </div>
      </q-tab>
      <q-tab name="pending" class="filter-tab">
        <div class="tab-label">
          <span>{{ t('myRequest.tabPending') }}</span>
          <span class="tab-count">{{ tabCounts.pending }}</span>
        </div>
      </q-tab>
      <q-tab name="approved" class="filter-tab">
        <div class="tab-label">
          <span>{{ t('myRequest.tabApproved') }}</span>
          <span class="tab-count">{{ tabCounts.approved }}</span>
        </div>
      </q-tab>
      <q-tab name="rejected" class="filter-tab">
        <div class="tab-label">
          <span>{{ t('myRequest.tabRejected') }}</span>
          <span class="tab-count">{{ tabCounts.rejected }}</span>
        </div>
      </q-tab>
    </q-tabs>

    <div class="my-request-list-shell">
      <q-table
        :rows="filteredRows"
        :columns="columns"
        v-model:pagination="tablePagination"
        row-key="requestNo"
        :rows-per-page-options="[10, 20, 50, 100]"
        flat
        :class="['request-table', { 'request-table--empty': !filteredRows.length }]"
      >
        <template #body-cell-type="props">
          <q-td :props="props">
            <q-badge
              :color="typeColorMap[props.row.type]"
              :label="typeLabel(props.row.type)"
              rounded
              class="request-type-badge"
            />
          </q-td>
        </template>

        <template #body-cell-status="props">
          <q-td :props="props">
            <q-badge
              :color="statusColorMap[props.row.status]"
              :label="statusLabel(props.row.status)"
              rounded
              class="request-type-badge"
            />
          </q-td>
        </template>

        <template #body-cell-actions="props">
          <q-td :props="props" class="q-gutter-x-xs actions-cell">
            <q-btn flat dense round size="sm" color="teal" icon="sym_r_visibility" @click.stop="viewDetail(props.row)">
              <q-tooltip>{{ t('myRequest.viewDetail') }}</q-tooltip>
            </q-btn>
            <q-btn
              v-if="props.row.status === 'pending'"
              flat
              dense
              round
              size="sm"
              color="negative"
              icon="sym_r_block"
              @click.stop="revoke(props.row)"
            >
              <q-tooltip>{{ t('myRequest.revoke') }}</q-tooltip>
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
.my-request-page-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 8px;
  background: #f5f5f5;
}

.body--dark .my-request-page-wrapper {
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

/* ═══ Tabs ═══ */
.filter-tabs {
  flex-shrink: 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
}

.body--dark .filter-tabs {
  border-bottom-color: rgba(255, 255, 255, 0.08);
  background: #1e1e1e;
}

.filter-tab {
  padding: 0 16px;
  min-height: 40px;
  text-transform: none;
  font-size: 14px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.6);
  opacity: 1 !important;
}

.body--dark .filter-tab {
  color: rgba(255, 255, 255, 0.6);
}

.filter-tab.q-tab--active {
  color: #009688;
  font-weight: 600;
}

.body--dark .filter-tab.q-tab--active {
  color: #4db6ac;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 600;
  font-family: "JetBrains Mono", monospace;
  background: rgba(0, 0, 0, 0.08);
  color: rgba(0, 0, 0, 0.55);
}

.filter-tab.q-tab--active .tab-count {
  background: rgba(0, 150, 136, 0.12);
  color: #009688;
}

.body--dark .tab-count {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .filter-tab.q-tab--active .tab-count {
  background: rgba(77, 182, 172, 0.16);
  color: #4db6ac;
}

/* ═══ 列表壳层 ═══ */
.my-request-list-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* ── 表格 ── */
.request-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
  border-top: none;
}

.body--dark .request-table {
  background: #1e1e1e;
  border-color: rgba(255, 255, 255, 0.08);
}

.request-table :deep(.q-table__top) {
  display: none;
}

.request-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.request-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.request-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.request-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.request-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.body--dark .request-table :deep(thead tr th) {
  color: rgba(255, 255, 255, 0.8) !important;
  background: #2a2a2a !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.request-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ── 空数据状态 ── */
.request-table--empty :deep(.q-table__container) {
  height: 100%;
}

.request-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.request-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.request-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

/* 行悬停 */
.request-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.request-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

.body--dark .request-table :deep(tbody td) {
  border-bottom-color: rgba(255, 255, 255, 0.12) !important;
}

/* Badge 统一样式 */
.request-type-badge {
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
.request-table :deep(.q-table__bottom) {
  padding: 3px 16px 4px;
  font-size: 13px;
  min-height: 42px;
  background: #fff;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.body--dark .request-table :deep(.q-table__bottom) {
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
</style>

<!-- 非 scoped：下拉弹出层 -->
<style>
.rows-per-page-popup .q-item {
  min-height: 36px;
  padding: 0 16px;
}
</style>
