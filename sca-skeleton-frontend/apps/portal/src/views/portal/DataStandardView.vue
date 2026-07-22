<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { useQuasar } from "quasar";
import type { QTableColumn } from "quasar";

const { t } = useI18n({ useScope: "global" });
const $q = useQuasar();

type StandardStatus = "statusPublished" | "statusDraft" | "statusDeprecated";
type StandardCategory = "dataElement" | "referenceData" | "codeSet";

interface DataElementRow {
  code: string;
  name: string;
  dataType: string;
  length: number;
  domain: string;
  status: StandardStatus;
  updateTime: string;
}

interface ReferenceDataRow {
  id: string;
  setCode: string;
  setName: string;
  codeValue: string;
  codeName: string;
  sort: number;
  status: StandardStatus;
}

interface CodeSetRow {
  code: string;
  name: string;
  codeCount: number;
  status: StandardStatus;
  updateTime: string;
}

type StandardRow = DataElementRow | ReferenceDataRow | CodeSetRow;

// ═══════════════════════════════════════════════════════════════
// 本地 Mock 数据
// ═══════════════════════════════════════════════════════════════
const mockDataElements: DataElementRow[] = [
  { code: "DE001", name: "学号", dataType: "VARCHAR", length: 20, domain: "教务域", status: "statusPublished", updateTime: "2026-07-20" },
  { code: "DE002", name: "教职工号", dataType: "VARCHAR", length: 10, domain: "人事域", status: "statusPublished", updateTime: "2026-07-19" },
  { code: "DE003", name: "课程编号", dataType: "VARCHAR", length: 15, domain: "教务域", status: "statusPublished", updateTime: "2026-07-18" },
  { code: "DE004", name: "项目编号", dataType: "VARCHAR", length: 20, domain: "科研域", status: "statusPublished", updateTime: "2026-07-17" },
  { code: "DE005", name: "院系代码", dataType: "VARCHAR", length: 8, domain: "人事域", status: "statusPublished", updateTime: "2026-07-16" },
  { code: "DE006", name: "专业代码", dataType: "VARCHAR", length: 6, domain: "教务域", status: "statusDraft", updateTime: "2026-07-15" },
  { code: "DE007", name: "房间编号", dataType: "VARCHAR", length: 12, domain: "资产域", status: "statusPublished", updateTime: "2026-07-14" },
  { code: "DE008", name: "图书ISBN", dataType: "VARCHAR", length: 13, domain: "图书域", status: "statusDeprecated", updateTime: "2026-06-28" }
];

const mockReferenceData: ReferenceDataRow[] = [
  { id: "RF001", setCode: "REF_NATION", setName: "民族代码", codeValue: "01", codeName: "汉族", sort: 1, status: "statusPublished" },
  { id: "RF002", setCode: "REF_NATION", setName: "民族代码", codeValue: "02", codeName: "蒙古族", sort: 2, status: "statusPublished" },
  { id: "RF003", setCode: "REF_GENDER", setName: "性别代码", codeValue: "1", codeName: "男", sort: 1, status: "statusPublished" },
  { id: "RF004", setCode: "REF_GENDER", setName: "性别代码", codeValue: "2", codeName: "女", sort: 2, status: "statusPublished" },
  { id: "RF005", setCode: "REF_DEGREE", setName: "学历代码", codeValue: "1", codeName: "博士", sort: 1, status: "statusPublished" },
  { id: "RF006", setCode: "REF_DEGREE", setName: "学历代码", codeValue: "2", codeName: "硕士", sort: 2, status: "statusDraft" }
];

const mockCodeSets: CodeSetRow[] = [
  { code: "CD001", name: "院系代码集", codeCount: 24, status: "statusPublished", updateTime: "2026-07-20" },
  { code: "CD002", name: "专业代码集", codeCount: 186, status: "statusPublished", updateTime: "2026-07-19" },
  { code: "CD003", name: "民族代码集", codeCount: 56, status: "statusPublished", updateTime: "2026-07-18" },
  { code: "CD004", name: "学历代码集", codeCount: 8, status: "statusDraft", updateTime: "2026-07-15" },
  { code: "CD005", name: "学位代码集", codeCount: 12, status: "statusDeprecated", updateTime: "2026-06-20" }
];

const statusColorMap: Record<StandardStatus, string> = {
  statusPublished: "teal-7",
  statusDraft: "grey-7",
  statusDeprecated: "red-7"
};

// ═══════════════════════════════════════════════════════════════
// 搜索条件
// ═══════════════════════════════════════════════════════════════
interface SearchForm {
  keyword: string;
  category: StandardCategory;
  status: string;
}

const searchForm = reactive<SearchForm>({
  keyword: "",
  category: "dataElement",
  status: ""
});
const searchExpanded = ref(true);

const categoryOptions = computed(() => [
  { value: "dataElement" as StandardCategory, label: t("dataStandard.tabDataElement") },
  { value: "referenceData" as StandardCategory, label: t("dataStandard.tabReferenceData") },
  { value: "codeSet" as StandardCategory, label: t("dataStandard.tabCodeSet") }
]);

const statusOptions = computed(() => [
  { value: "statusPublished", label: t("dataStandard.statusPublished") },
  { value: "statusDraft", label: t("dataStandard.statusDraft") },
  { value: "statusDeprecated", label: t("dataStandard.statusDeprecated") }
]);

// ═══════════════════════════════════════════════════════════════
// 表格数据
// ═══════════════════════════════════════════════════════════════
const tableRows = ref<StandardRow[]>([]);
const tableTotal = ref(0);
const tableLoading = ref(false);
const tablePagination = ref({
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0,
  sortBy: "",
  descending: false
});
const curPage = ref(1);
const jumpToPage = ref<number | null>(null);

const columns = computed<QTableColumn[]>(() => {
  if (searchForm.category === "dataElement") {
    return [
      { name: "code", field: "code", label: t("dataStandard.colCode"), align: "left", sortable: true },
      { name: "name", field: "name", label: t("dataStandard.colName"), align: "left", sortable: true },
      { name: "dataType", field: "dataType", label: t("dataStandard.colDataType"), align: "left", sortable: true },
      { name: "length", field: "length", label: t("dataStandard.colLength"), align: "right", sortable: true },
      { name: "domain", field: "domain", label: t("dataStandard.colDomain"), align: "left", sortable: true },
      { name: "status", field: "status", label: t("dataStandard.colStatus"), align: "left", sortable: true },
      { name: "updateTime", field: "updateTime", label: t("dataStandard.colUpdateTime"), align: "left", sortable: true },
      { name: "actions", field: "actions", label: t("dataStandard.colActions"), align: "center", sortable: false }
    ];
  }
  if (searchForm.category === "referenceData") {
    return [
      { name: "setCode", field: "setCode", label: t("dataStandard.colRefSetCode"), align: "left", sortable: true },
      { name: "setName", field: "setName", label: t("dataStandard.colName"), align: "left", sortable: true },
      { name: "codeValue", field: "codeValue", label: t("dataStandard.colCodeValue"), align: "left", sortable: true },
      { name: "codeName", field: "codeName", label: t("dataStandard.colCodeName"), align: "left", sortable: true },
      { name: "sort", field: "sort", label: t("dataStandard.colSort"), align: "right", sortable: true },
      { name: "status", field: "status", label: t("dataStandard.colStatus"), align: "left", sortable: true }
    ];
  }
  return [
    { name: "code", field: "code", label: t("dataStandard.colCodeSetCode"), align: "left", sortable: true },
    { name: "name", field: "name", label: t("dataStandard.colName"), align: "left", sortable: true },
    { name: "codeCount", field: "codeCount", label: t("dataStandard.colCodeCount"), align: "right", sortable: true },
    { name: "status", field: "status", label: t("dataStandard.colStatus"), align: "left", sortable: true },
    { name: "updateTime", field: "updateTime", label: t("dataStandard.colUpdateTime"), align: "left", sortable: true }
  ];
});

const visibleColumns = computed(() => columns.value.map((c) => c.name));

function getRowKey(row: StandardRow): string {
  if ("id" in row) return row.id;
  if ("code" in row) return row.code;
  return "";
}

function compareValues(av: unknown, bv: unknown, dir: number): number {
  if (av == null && bv == null) return 0;
  if (av == null) return -1 * dir;
  if (bv == null) return 1 * dir;
  if (typeof av === "number" && typeof bv === "number") return (av - bv) * dir;
  return String(av).localeCompare(String(bv)) * dir;
}

function getSourceRows(): StandardRow[] {
  if (searchForm.category === "dataElement") return [...mockDataElements];
  if (searchForm.category === "referenceData") return [...mockReferenceData];
  return [...mockCodeSets];
}

function filterByKeyword(rows: StandardRow[], kw: string): StandardRow[] {
  if (!kw) return rows;
  const k = kw.toLowerCase();
  return rows.filter((r) => {
    const rec = r as Record<string, unknown>;
    return Object.values(rec).some((v) => v != null && String(v).toLowerCase().includes(k));
  });
}

let initialLoadDone = false;
let loadRequestId = 0;

// ═══════════════════════════════════════════════════════════════
// 数据加载（模拟接口分页）
// ═══════════════════════════════════════════════════════════════
async function loadTableData(
  props?: {
    pagination: {
      page: number;
      rowsPerPage: number;
      rowsNumber?: number;
      sortBy?: string;
      descending?: boolean;
    };
  }
) {
  if (props && !initialLoadDone) return;

  const requestId = ++loadRequestId;
  tableLoading.value = true;

  if (props?.pagination) {
    tablePagination.value.sortBy = props.pagination.sortBy ?? "";
    tablePagination.value.descending = props.pagination.descending ?? false;
  }

  const pageSize = Number(tablePagination.value.rowsPerPage) || 10;
  const pageNum = curPage.value || 1;

  await new Promise((resolve) => setTimeout(resolve, 300));
  if (requestId !== loadRequestId) return;

  let rows = getSourceRows();
  rows = filterByKeyword(rows, searchForm.keyword?.trim().toLowerCase() ?? "");
  if (searchForm.status) {
    rows = rows.filter((r) => (r as { status: string }).status === searchForm.status);
  }

  const sortBy = tablePagination.value.sortBy;
  if (sortBy) {
    const dir = tablePagination.value.descending ? -1 : 1;
    rows = [...rows].sort((a, b) => {
      const av = (a as Record<string, unknown>)[sortBy];
      const bv = (b as Record<string, unknown>)[sortBy];
      return compareValues(av, bv, dir);
    });
  }

  const total = rows.length;
  const start = (pageNum - 1) * pageSize;
  tableRows.value = rows.slice(start, start + pageSize);
  tableTotal.value = total;
  tablePagination.value.rowsNumber = total;
  tablePagination.value.page = pageNum;
  tablePagination.value.rowsPerPage = pageSize;

  tableLoading.value = false;
}

function handleSearch() {
  tablePagination.value.page = 1;
  curPage.value = 1;
  loadTableData();
}

function handleCategoryChange() {
  tablePagination.value.sortBy = "";
  tablePagination.value.descending = false;
  tablePagination.value.page = 1;
  curPage.value = 1;
  loadTableData();
}

function onPageChange(page: number) {
  curPage.value = Number(page);
  tablePagination.value.page = Number(page);
  loadTableData();
}

function handleJumpToPage() {
  const page = Number(jumpToPage.value);
  const maxPage = Math.ceil(tableTotal.value / tablePagination.value.rowsPerPage);
  if (page && page >= 1 && page <= maxPage) {
    curPage.value = page;
    tablePagination.value.page = page;
    loadTableData();
  }
  jumpToPage.value = null;
}

function handleReset() {
  searchForm.keyword = "";
  searchForm.status = "";
  tablePagination.value.sortBy = "";
  tablePagination.value.descending = false;
  tablePagination.value.page = 1;
  curPage.value = 1;
  loadTableData();
}

function handleRefresh() {
  loadTableData();
}

function viewDetail(row: StandardRow) {
  const label =
    "name" in row ? row.name : "setName" in row ? row.setName : row.code;
  $q.notify({ type: "info", message: `${t("common.viewDetail")}: ${label}`, position: "top" });
}

onMounted(() => {
  loadTableData();
  initialLoadDone = true;
});
</script>

<template>
  <div class="standard-list-shell">
    <div class="right-panel">
      <!-- ── 搜索区域 ── -->
      <div class="search-area">
        <div class="search-area-header row items-center no-wrap">
          <div class="row items-center no-wrap cursor-pointer" @click="searchExpanded = !searchExpanded">
            <q-icon name="sym_r_search" size="20px" class="q-mr-xs" color="grey-8" />
            <span class="search-area-title">{{ t("common.searchCondition") }}</span>
          </div>
          <q-space />
          <q-btn
            flat
            dense
            round
            size="20px"
            :icon="searchExpanded ? 'sym_r_expand_less' : 'sym_r_expand_more'"
            class="search-collapse-btn"
            @click="searchExpanded = !searchExpanded"
          >
            <q-tooltip style="white-space: nowrap">{{
              searchExpanded ? t("common.collapseSearch") : t("common.expandSearch")
            }}</q-tooltip>
          </q-btn>
        </div>

        <div v-show="searchExpanded" class="search-area-body">
          <div class="row q-col-gutter-sm items-end">
            <div class="col">
              <q-input
                v-model="searchForm.keyword"
                filled
                square
                dense
                :placeholder="t('common.keyword')"
                hide-bottom-space
                clearable
                @keyup.enter="handleSearch"
              />
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.category"
                filled
                square
                dense
                :options="categoryOptions"
                option-label="label"
                option-value="value"
                emit-value
                map-options
                hide-bottom-space
                transition-show="jump-up"
                transition-hide="jump-down"
                class="status-select"
                popup-content-class="status-select-popup"
                @update:model-value="handleCategoryChange"
              />
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.status"
                filled
                square
                dense
                :options="statusOptions"
                option-label="label"
                option-value="value"
                emit-value
                map-options
                hide-bottom-space
                clearable
                transition-show="jump-up"
                transition-hide="jump-down"
                class="status-select"
                popup-content-class="status-select-popup"
              >
                <template v-if="!searchForm.status" v-slot:selected>
                  <span class="status-placeholder">{{ t('common.status') }}</span>
                </template>
              </q-select>
            </div>
            <div class="col-auto">
              <div class="row q-gutter-x-sm no-wrap">
                <q-btn color="primary" unelevated no-caps class="search-btn" @click="handleSearch">
                  <q-icon name="sym_r_search" size="20px" class="q-mr-xs" />
                  {{ t("common.search") }}
                </q-btn>
                <q-btn color="grey-7" outline no-caps class="search-btn" @click="handleReset">
                  <q-icon name="sym_r_refresh" size="20px" class="q-mr-xs" />
                  {{ t("common.reset") }}
                </q-btn>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ── 工具栏区域 ── -->
      <div class="toolbar-area row items-center no-wrap">
        <div class="toolbar-left row items-center no-wrap">
          <q-btn color="grey-7" outline dense no-caps class="toolbar-btn" @click.stop="handleRefresh">
            <q-icon name="sym_r_refresh" size="20px" class="q-mr-xs" />
            {{ t("common.refresh") }}
          </q-btn>
        </div>
        <q-space />
      </div>

      <!-- ── 表格区域 ── -->
      <q-table
        v-model:pagination="tablePagination"
        :rows="tableRows"
        :columns="columns"
        :visible-columns="visibleColumns"
        :row-key="getRowKey"
        :loading="tableLoading"
        :rows-per-page-options="[10, 20, 50, 100]"
        flat
        :class="['standard-table', { 'standard-table--empty': !tableRows.length }]"
        @request="loadTableData"
      >
        <!-- 编码列（数据元 / 代码集） -->
        <template #body-cell-code="props">
          <q-td :props="props">
            <span class="mono-code">{{ props.value }}</span>
          </q-td>
        </template>

        <!-- 参考数据集编码列 -->
        <template #body-cell-setCode="props">
          <q-td :props="props">
            <span class="mono-code">{{ props.value }}</span>
          </q-td>
        </template>

        <!-- 数据类型列 -->
        <template #body-cell-dataType="props">
          <q-td :props="props">
            <span class="mono-code type-badge">{{ props.value }}</span>
          </q-td>
        </template>

        <!-- 代码值列 -->
        <template #body-cell-codeValue="props">
          <q-td :props="props">
            <span class="mono-code code-value">{{ props.value }}</span>
          </q-td>
        </template>

        <!-- 代码数量列 -->
        <template #body-cell-codeCount="props">
          <q-td :props="props">
            <span class="code-count">{{ props.value }}</span>
          </q-td>
        </template>

        <!-- 状态列 -->
        <template #body-cell-status="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              :color="statusColorMap[props.value as StandardStatus]"
              :label="t(`dataStandard.${props.value}`)"
              rounded
              class="standard-type-badge"
            />
          </q-td>
        </template>

        <!-- 操作列 -->
        <template #body-cell-actions="props">
          <q-td :props="props" class="q-gutter-x-xs actions-cell">
            <q-btn flat dense round size="sm" color="info" icon="sym_r_visibility" @click.stop="viewDetail(props.row)">
              <q-tooltip>{{ t('common.viewDetail') }}</q-tooltip>
            </q-btn>
          </q-td>
        </template>

        <!-- 空数据 -->
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

        <!-- 自定义底部分页栏 -->
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
              @update:model-value="handleSearch"
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
.standard-list-shell {
  display: flex;
  height: calc(100vh - 64px - 40px - 24px);
  min-height: 0;
  padding: 12px 24px;
  box-sizing: border-box;
}

/* ═══ 右侧面板 ═══ */
.right-panel {
  flex: 1 1 auto;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ── 搜索区域 ── */
.search-area {
  flex-shrink: 0;
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.body--dark .search-area {
  background: #1e1e1e;
  border-color: rgba(255, 255, 255, 0.08);
}

.search-area-header {
  height: 40px;
  padding: 0 8px 0 12px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  user-select: none;
}

.body--dark .search-area-header {
  background: #252525;
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

.search-area-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.body--dark .search-area-title {
  color: rgba(255, 255, 255, 0.87);
}

.search-area-header .cursor-pointer {
  padding: 4px 0;
}

.search-area-header .cursor-pointer:hover {
  opacity: 0.85;
}

.search-area-body {
  padding: 8px;
}

.search-btn {
  min-width: 72px;
  height: 40px;
  padding: 0 14px;
  font-size: 13px;
}

.search-collapse-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.87);
  border-radius: 50%;
}

.body--dark .search-collapse-btn {
  color: rgba(255, 255, 255, 0.87);
}

.search-collapse-btn :deep(.q-btn__wrapper) {
  min-height: 32px;
  padding: 0;
}

.search-collapse-btn :deep(.q-icon.material-symbols-rounded),
.search-collapse-btn :deep(.material-symbols-rounded) {
  font-size: 20px !important;
}

.search-collapse-btn:hover {
  background: rgba(128, 128, 128, 0.28);
}

.status-select :deep(.q-field__native) {
  color: rgba(0, 0, 0, 0.87);
}

.body--dark .status-select :deep(.q-field__native) {
  color: rgba(255, 255, 255, 0.87);
}

.status-select :deep(.q-field__control) {
  min-height: 40px;
  min-width: 160px;
}

/* ── 工具栏区域 ── */
.toolbar-area {
  flex-shrink: 0;
  padding: 8px 1px;
}

.toolbar-left {
  gap: 6px;
}

.toolbar-btn {
  height: 32px;
  font-size: 13px;
  padding: 0 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.toolbar-area :deep(.q-btn) {
  height: 32px;
  font-size: 13px;
}

/* ── 表格 ── */
.standard-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.body--dark .standard-table {
  background: #1e1e1e;
  border-color: rgba(255, 255, 255, 0.08);
}

.standard-table :deep(.q-table__top) {
  display: none;
}

.standard-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.standard-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.standard-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.standard-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.standard-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.body--dark .standard-table :deep(thead tr th) {
  color: rgba(255, 255, 255, 0.8) !important;
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.standard-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ── 空数据状态 ── */
.standard-table--empty :deep(.q-table__container) {
  height: 100%;
}

.standard-table--empty :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
}

.standard-table--empty :deep(.q-table__bottom) {
  flex: 0 0 auto;
}

/* 行悬停 */
.standard-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.standard-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

.body--dark .standard-table :deep(tbody td) {
  border-bottom-color: rgba(255, 255, 255, 0.12) !important;
}

/* Badge 统一样式 */
.standard-type-badge {
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
.standard-table :deep(.q-table__bottom) {
  padding: 3px 16px 4px;
  font-size: 13px;
  min-height: 42px;
  background: #fff;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.body--dark .standard-table :deep(.q-table__bottom) {
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

/* —— 单元格元素 —— */
.mono-code {
  font-family: "JetBrains Mono", monospace;
  font-size: 12px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
}

.body--dark .mono-code {
  color: rgba(255, 255, 255, 0.88);
}

.type-badge {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(25, 118, 210, 0.08);
  color: #1976d2;
}

.body--dark .type-badge {
  background: rgba(66, 165, 245, 0.16);
  color: #64b5f6;
}

.code-value {
  color: #009688;
}

.body--dark .code-value {
  color: #4db6ac;
}

.code-count {
  font-family: "JetBrains Mono", monospace;
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
}

.body--dark .code-count {
  color: rgba(255, 255, 255, 0.88);
}
</style>

<!-- 非 scoped：下拉弹出层 -->
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
