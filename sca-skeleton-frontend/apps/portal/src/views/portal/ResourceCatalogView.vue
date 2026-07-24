<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast } from "@repo/shared";
import type { QTableColumn } from "quasar";

const { t } = useI18n({ useScope: "global" });

type ResourceType = "typeTable" | "typeApi" | "typeFile" | "typeStream";
type ResourceStatus = "statusPublished" | "statusDraft" | "statusDeprecated";

interface ResourceRow {
  id: string;
  name: string;
  type: ResourceType;
  domain: string;
  owner: string;
  status: ResourceStatus;
  updateTime: string;
}

// ═══════════════════════════════════════════════════════════════
// 本地 Mock 数据（API 就绪后替换为接口调用）
// ═══════════════════════════════════════════════════════════════
const mockResources: ResourceRow[] = [
  { id: "R001", name: "本科生招生数据表", type: "typeTable", domain: "招生域", owner: "招生办公室", status: "statusPublished", updateTime: "2026-07-20" },
  { id: "R002", name: "科研项目立项表", type: "typeTable", domain: "科研域", owner: "科研处", status: "statusPublished", updateTime: "2026-07-19" },
  { id: "R003", name: "教职工信息表", type: "typeTable", domain: "人事域", owner: "人事处", status: "statusPublished", updateTime: "2026-07-18" },
  { id: "R004", name: "学生成绩表", type: "typeTable", domain: "教务域", owner: "教务处", status: "statusPublished", updateTime: "2026-07-21" },
  { id: "R005", name: "财务预算执行表", type: "typeTable", domain: "财务域", owner: "财务处", status: "statusDraft", updateTime: "2026-07-17" },
  { id: "R006", name: "图书借阅记录", type: "typeTable", domain: "图书域", owner: "图书馆", status: "statusPublished", updateTime: "2026-07-15" },
  { id: "R007", name: "一卡通消费流水", type: "typeStream", domain: "一卡通域", owner: "信息中心", status: "statusPublished", updateTime: "2026-07-21" },
  { id: "R008", name: "教室排课表", type: "typeTable", domain: "教务域", owner: "教务处", status: "statusPublished", updateTime: "2026-07-14" },
  { id: "R009", name: "毕业去向统计数据", type: "typeFile", domain: "招生域", owner: "招生办公室", status: "statusDeprecated", updateTime: "2026-06-30" },
  { id: "R010", name: "教学质量评估 API", type: "typeApi", domain: "教务域", owner: "教务处", status: "statusPublished", updateTime: "2026-07-12" }
];

// ═══════════════════════════════════════════════════════════════
// 搜索条件
// ═══════════════════════════════════════════════════════════════
interface SearchForm {
  keyword: string;
  domain: string;
  type: string;
}

const searchForm = reactive<SearchForm>({ keyword: "", domain: "", type: "" });
const searchExpanded = ref(true);

const domainOptions = computed(() => {
  const set = new Set(mockResources.map((r) => r.domain));
  return Array.from(set);
});

const typeOptions = computed(() => [
  { value: "typeTable", label: t("resourceCatalog.typeTable") },
  { value: "typeApi", label: t("resourceCatalog.typeApi") },
  { value: "typeFile", label: t("resourceCatalog.typeFile") },
  { value: "typeStream", label: t("resourceCatalog.typeStream") }
]);

const typeColorMap: Record<ResourceType, string> = {
  typeTable: "blue-7",
  typeApi: "purple-7",
  typeFile: "teal-7",
  typeStream: "orange-7"
};

const statusColorMap: Record<ResourceStatus, string> = {
  statusPublished: "teal-7",
  statusDraft: "grey-7",
  statusDeprecated: "red-7"
};

// ═══════════════════════════════════════════════════════════════
// 表格数据
// ═══════════════════════════════════════════════════════════════
const tableRows = ref<ResourceRow[]>([]);
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

const columns = computed<QTableColumn<ResourceRow>[]>(() => [
  { name: "name", field: "name", label: t("resourceCatalog.colName"), align: "left", sortable: true },
  { name: "type", field: "type", label: t("resourceCatalog.colType"), align: "left", sortable: true },
  { name: "domain", field: "domain", label: t("resourceCatalog.colDomain"), align: "left", sortable: true },
  { name: "owner", field: "owner", label: t("resourceCatalog.colOwner"), align: "left", sortable: true },
  { name: "status", field: "status", label: t("resourceCatalog.colStatus"), align: "left", sortable: true },
  { name: "updateTime", field: "updateTime", label: t("resourceCatalog.colUpdateTime"), align: "left", sortable: true },
  { name: "actions", field: "id", label: t("resourceCatalog.colActions"), align: "center", sortable: false }
]);

const visibleColumns = ref(columns.value.map((c) => c.name));

function compareValues(av: unknown, bv: unknown, dir: number): number {
  if (av == null && bv == null) return 0;
  if (av == null) return -1 * dir;
  if (bv == null) return 1 * dir;
  if (typeof av === "number" && typeof bv === "number") return (av - bv) * dir;
  return String(av).localeCompare(String(bv)) * dir;
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

  // 模拟接口延迟
  await new Promise((resolve) => setTimeout(resolve, 300));
  if (requestId !== loadRequestId) return;

  // 过滤
  let rows: ResourceRow[] = [...mockResources];
  const kw = searchForm.keyword?.trim().toLowerCase();
  if (kw) {
    rows = rows.filter(
      (r) => r.name.toLowerCase().includes(kw) || r.id.toLowerCase().includes(kw)
    );
  }
  if (searchForm.domain) {
    rows = rows.filter((r) => r.domain === searchForm.domain);
  }
  if (searchForm.type) {
    rows = rows.filter((r) => r.type === searchForm.type);
  }

  // 排序
  const sortBy = tablePagination.value.sortBy;
  if (sortBy) {
    const dir = tablePagination.value.descending ? -1 : 1;
    rows = [...rows].sort((a, b) =>
      compareValues(a[sortBy as keyof ResourceRow], b[sortBy as keyof ResourceRow], dir)
    );
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
  searchForm.domain = "";
  searchForm.type = "";
  tablePagination.value.sortBy = "";
  tablePagination.value.descending = false;
  tablePagination.value.page = 1;
  curPage.value = 1;
  loadTableData();
}

function handleRefresh() {
  loadTableData();
}

function applyAccess(row: ResourceRow) {
  showToast(`${t("common.applyAccess")}: ${row.name}`, "info");
}

function viewDetail(row: ResourceRow) {
  showToast(`${t("common.viewDetail")}: ${row.name}`, "info");
}

onMounted(() => {
  loadTableData();
  initialLoadDone = true;
});
</script>

<template>
  <div class="resource-list-shell">
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
                :placeholder="t('resourceCatalog.searchPlaceholder')"
                hide-bottom-space
                clearable
                @keyup.enter="handleSearch"
              />
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.domain"
                filled
                square
                dense
                :options="domainOptions"
                hide-bottom-space
                clearable
                transition-show="jump-up"
                transition-hide="jump-down"
                class="status-select"
                popup-content-class="status-select-popup"
              >
                <template v-if="!searchForm.domain" v-slot:selected>
                  <span class="status-placeholder">{{ t('resourceCatalog.colDomain') }}</span>
                </template>
              </q-select>
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.type"
                filled
                square
                dense
                :options="typeOptions"
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
                <template v-if="!searchForm.type" v-slot:selected>
                  <span class="status-placeholder">{{ t('resourceCatalog.colType') }}</span>
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
        row-key="id"
        :loading="tableLoading"
        :rows-per-page-options="[10, 20, 50, 100]"
        flat
        :class="['resource-table', { 'resource-table--empty': !tableRows.length }]"
        @request="loadTableData"
      >
        <!-- 资源名称列 -->
        <template #body-cell-name="props">
          <q-td :props="props">
            <div class="cell-name">
              <q-icon name="sym_r_database" size="16px" class="cell-name__icon" />
              <span class="cell-name__text">{{ props.row.name }}</span>
            </div>
          </q-td>
        </template>

        <!-- 资源类型列 -->
        <template #body-cell-type="props">
          <q-td :props="props">
            <q-badge
              :color="typeColorMap[props.row.type as ResourceType]"
              :label="t(`resourceCatalog.${props.row.type}`)"
              rounded
              class="resource-type-badge"
            />
          </q-td>
        </template>

        <!-- 状态列 -->
        <template #body-cell-status="props">
          <q-td :props="props">
            <q-badge
              :color="statusColorMap[props.row.status as ResourceStatus]"
              :label="t(`resourceCatalog.${props.row.status}`)"
              rounded
              class="resource-type-badge"
            />
          </q-td>
        </template>

        <!-- 操作列 -->
        <template #body-cell-actions="props">
          <q-td :props="props" class="q-gutter-x-xs actions-cell">
            <q-btn flat dense round size="sm" color="info" icon="sym_r_visibility" @click.stop="viewDetail(props.row)">
              <q-tooltip>{{ t('common.viewDetail') }}</q-tooltip>
            </q-btn>
            <q-btn flat dense round size="sm" color="primary" icon="sym_r_lock_open" @click.stop="applyAccess(props.row)">
              <q-tooltip>{{ t('common.applyAccess') }}</q-tooltip>
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
.resource-list-shell {
  display: flex;
  height: calc(100vh - 64px - 40px - 24px);
  min-height: 0;
  padding: 12px 0;
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
.resource-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.body--dark .resource-table {
  background: #1e1e1e;
  border-color: rgba(255, 255, 255, 0.08);
}

.resource-table :deep(.q-table__top) {
  display: none;
}

.resource-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.resource-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.resource-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.resource-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.resource-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.body--dark .resource-table :deep(thead tr th) {
  color: rgba(255, 255, 255, 0.8) !important;
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.resource-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ── 空数据状态 ── */
.resource-table--empty :deep(.q-table__container) {
  height: 100%;
}

.resource-table--empty :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
}

.resource-table--empty :deep(.q-table__bottom) {
  flex: 0 0 auto;
}

/* 行悬停 */
.resource-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.resource-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

.body--dark .resource-table :deep(tbody td) {
  border-bottom-color: rgba(255, 255, 255, 0.12) !important;
}

/* Badge 统一样式 */
.resource-type-badge {
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
.resource-table :deep(.q-table__bottom) {
  padding: 3px 16px 4px;
  font-size: 13px;
  min-height: 42px;
  background: #fff;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.body--dark .resource-table :deep(.q-table__bottom) {
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
.cell-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cell-name__icon {
  color: #009688;
  flex-shrink: 0;
}

.body--dark .cell-name__icon {
  color: #4db6ac;
}

.cell-name__text {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.87);
}

.body--dark .cell-name__text {
  color: rgba(255, 255, 255, 0.9);
}
</style>

<!-- 非 scoped：下拉弹出层暗色模式 -->
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
