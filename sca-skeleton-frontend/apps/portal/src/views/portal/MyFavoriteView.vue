<script setup lang="ts">
import { ref, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useQuasar } from "quasar";

const { t } = useI18n({ useScope: "global" });
const $q = useQuasar();

type ResourceType = "table" | "api" | "file" | "report";
type Domain = "admission" | "research" | "personnel" | "academic" | "finance" | "library";

interface FavoriteRow {
  id: number;
  name: string;
  type: ResourceType;
  domain: Domain;
  owner: string;
  favoriteTime: string;
}

const favorites = ref<FavoriteRow[]>([
  { id: 1, name: "本科生招生数据（2015-2025）", type: "table", domain: "admission", owner: "招生办公室", favoriteTime: "2026-07-20 09:15" },
  { id: 2, name: "科研项目立项明细", type: "table", domain: "research", owner: "科研处", favoriteTime: "2026-07-18 14:30" },
  { id: 3, name: "教职工薪酬统计表", type: "file", domain: "personnel", owner: "人事处", favoriteTime: "2026-07-16 11:08" },
  { id: 4, name: "学生成绩分析数据集", type: "report", domain: "academic", owner: "教务处", favoriteTime: "2026-07-14 16:42" },
  { id: 5, name: "财务预算执行情况", type: "table", domain: "finance", owner: "财务处", favoriteTime: "2026-07-12 10:25" },
  { id: 6, name: "图书借阅统计分析", type: "report", domain: "library", owner: "图书馆", favoriteTime: "2026-07-10 13:50" }
]);

const typeColorMap: Record<string, string> = {
  table: "blue",
  api: "purple",
  file: "teal",
  report: "orange"
};

const domainColorMap: Record<string, string> = {
  admission: "teal",
  research: "blue",
  personnel: "purple",
  academic: "orange",
  finance: "red",
  library: "cyan-9"
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

function domainLabel(domain: Domain): string {
  const map: Record<Domain, string> = {
    admission: "招生域",
    research: "科研域",
    personnel: "人事域",
    academic: "教务域",
    finance: "财务域",
    library: "图书域"
  };
  return map[domain];
}

const columns = computed(() => [
  { name: "name", label: t("myFavorite.colName"), field: "name", align: "left" as const, sortable: true },
  { name: "type", label: t("myFavorite.colType"), field: "type", align: "left" as const, sortable: true },
  { name: "domain", label: t("myFavorite.colDomain"), field: "domain", align: "left" as const, sortable: true },
  { name: "owner", label: t("myFavorite.colOwner"), field: "owner", align: "left" as const },
  { name: "favoriteTime", label: t("myFavorite.colFavoriteTime"), field: "favoriteTime", align: "left" as const, sortable: true },
  { name: "actions", label: t("myFavorite.colActions"), field: "actions", align: "center" as const }
]);

const tableTotal = computed(() => favorites.value.length);

const tablePagination = ref({
  page: 1,
  rowsPerPage: 10,
  sortBy: "favoriteTime",
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

function viewDetail(row: FavoriteRow): void {
  $q.notify({ type: "info", message: `${t("myFavorite.viewDetail")}: ${row.name}`, position: "top" });
}

function unfavorite(row: FavoriteRow): void {
  favorites.value = favorites.value.filter((f) => f.id !== row.id);
  $q.notify({ type: "positive", message: `${t("myFavorite.unfavorite")}: ${row.name}`, position: "top" });
}
</script>

<template>
  <div class="my-favorite-page-wrapper">
    <header class="page-header">
      <h1 class="page-title">{{ t("myFavorite.pageTitle") }}</h1>
      <p class="page-desc">{{ t("myFavorite.pageDesc") }}</p>
    </header>

    <div class="my-favorite-list-shell">
      <q-table
        :rows="favorites"
        :columns="columns"
        v-model:pagination="tablePagination"
        row-key="id"
        :rows-per-page-options="[10, 20, 50, 100]"
        flat
        :class="['favorite-table', { 'favorite-table--empty': !favorites.length }]"
      >
        <template #body-cell-name="props">
          <q-td :props="props">
            <div class="resource-name">
              <q-icon name="sym_r_star" size="16px" class="star-icon" />
              <span>{{ props.row.name }}</span>
            </div>
          </q-td>
        </template>

        <template #body-cell-type="props">
          <q-td :props="props">
            <q-badge
              :color="typeColorMap[props.row.type]"
              :label="typeLabel(props.row.type)"
              rounded
              class="favorite-type-badge"
            />
          </q-td>
        </template>

        <template #body-cell-domain="props">
          <q-td :props="props">
            <q-badge
              :color="domainColorMap[props.row.domain]"
              :label="domainLabel(props.row.domain)"
              rounded
              outline
              class="favorite-type-badge"
            />
          </q-td>
        </template>

        <template #body-cell-actions="props">
          <q-td :props="props" class="q-gutter-x-xs actions-cell">
            <q-btn flat dense round size="sm" color="teal" icon="sym_r_visibility" @click.stop="viewDetail(props.row)">
              <q-tooltip>{{ t('myFavorite.viewDetail') }}</q-tooltip>
            </q-btn>
            <q-btn flat dense round size="sm" color="negative" icon="sym_r_star_border" @click.stop="unfavorite(props.row)">
              <q-tooltip>{{ t('myFavorite.unfavorite') }}</q-tooltip>
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
.my-favorite-page-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 8px;
  background: #f5f5f5;
}

.body--dark .my-favorite-page-wrapper {
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
.my-favorite-list-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* ── 表格 ── */
.favorite-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.body--dark .favorite-table {
  background: #1e1e1e;
  border-color: rgba(255, 255, 255, 0.08);
}

.favorite-table :deep(.q-table__top) {
  display: none;
}

.favorite-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.favorite-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.favorite-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.favorite-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.favorite-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.body--dark .favorite-table :deep(thead tr th) {
  color: rgba(255, 255, 255, 0.8) !important;
  background: #2a2a2a !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.favorite-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ── 空数据状态 ── */
.favorite-table--empty :deep(.q-table__container) {
  height: 100%;
}

.favorite-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.favorite-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.favorite-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

/* 行悬停 */
.favorite-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.favorite-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

.body--dark .favorite-table :deep(tbody td) {
  border-bottom-color: rgba(255, 255, 255, 0.12) !important;
}

/* Badge 统一样式 */
.favorite-type-badge {
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
.favorite-table :deep(.q-table__bottom) {
  padding: 3px 16px 4px;
  font-size: 13px;
  min-height: 42px;
  background: #fff;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.body--dark .favorite-table :deep(.q-table__bottom) {
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
.resource-name {
