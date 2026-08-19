<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useEscCloseDrawer } from "../../composables/useEscCloseDrawer";
import type { QTableColumn } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysConfig, ConfigPageRequest } from "../../types/auth";
import {
  getConfigPageApi,
  getConfigByIdApi,
  updateConfigStatusApi,
  deleteConfigApi,
  batchDeleteConfigApi
} from "../../apis/config";
import { useConfirmDialog } from "@repo/ui";
import ConfigDrawerContent from "./ConfigDrawerContent.vue";

const { t } = useI18n({ useScope: "global" });
const { confirmDialog } = useConfirmDialog();

// ═══════════════════════════════════════════════════════════════
// 搜索条件
// ═══════════════════════════════════════════════════════════════

const searchForm = reactive<ConfigPageRequest>({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  type: "",
  status: ""
});

const searchExpanded = ref(true);

const statusOptions = [
  { label: "configMgmt.statusEnabled", value: "enabled" },
  { label: "configMgmt.statusDisabled", value: "disabled" }
];

const typeOptions = [
  { label: "configMgmt.typeString", value: "string" },
  { label: "configMgmt.typeNumber", value: "number" },
  { label: "configMgmt.typeBoolean", value: "boolean" },
  { label: "configMgmt.typeDatetime", value: "datetime" },
  { label: "configMgmt.typeJson", value: "json" }
];

const statusColorOf = (s: string): string =>
  ({ enabled: "positive", disabled: "grey-7" }[s] ?? "grey-5");

const statusLabelOf = (s: string): string =>
  ({ enabled: t("configMgmt.statusEnabled"), disabled: t("configMgmt.statusDisabled") }[s] ?? s);

const typeLabelOf = (s: string): string =>
  ({
    string: t("configMgmt.typeString"),
    number: t("configMgmt.typeNumber"),
    boolean: t("configMgmt.typeBoolean"),
    datetime: t("configMgmt.typeDatetime"),
    json: t("configMgmt.typeJson")
  }[s] ?? s);

const typeColorOf = (s: string): string =>
  ({
    string: "blue-6",
    number: "purple-6",
    boolean: "orange-7",
    datetime: "teal-6",
    json: "indigo-6"
  }[s] ?? "grey-6");

// ═══════════════════════════════════════════════════════════════
// 本地抽屉
// ═══════════════════════════════════════════════════════════════

type DrawerMode = "add" | "edit" | "view";

const drawerOpen = ref(false);
useEscCloseDrawer(drawerOpen);
const drawerMode = ref<DrawerMode>("add");
const drawerConfig = ref<SysConfig | undefined>(undefined);

const drawerTitle = computed(() => {
  if (drawerMode.value === "add") return t("configMgmt.addConfig");
  if (drawerMode.value === "edit") return t("configMgmt.editConfig");
  return t("configMgmt.viewConfig");
});

const drawerIcon = computed(() => {
  if (drawerMode.value === "add") return "sym_r_add";
  if (drawerMode.value === "edit") return "sym_r_edit";
  return "sym_r_visibility";
});

function openConfigDrawer(mode: DrawerMode, config?: SysConfig) {
  drawerMode.value = mode;
  drawerConfig.value = config;
  drawerOpen.value = true;
}

function closeConfigDrawer() { drawerOpen.value = false; }

function handleDrawerSaved() {
  closeConfigDrawer();
  loadTableData();
}

// ═══════════════════════════════════════════════════════════════
// 表格数据
// ═══════════════════════════════════════════════════════════════

const tableRows = ref<SysConfig[]>([]);
const tableTotal = ref(0);
const tableLoading = ref(false);
const tablePagination = ref({
  page: 1, rowsPerPage: 10, rowsNumber: 0, sortBy: "", descending: false
});
const selectedRows = ref<SysConfig[]>([]);
const sortState = ref<{ sortBy: string; descending: boolean }>({ sortBy: "", descending: false });
const jumpToPage = ref<number | null>(null);
const curPage = ref(1);

const columns = computed<QTableColumn<SysConfig>[]>(() => [
  { name: "name", field: "name", label: t("configMgmt.name"), align: "left", sortable: true },
  { name: "configKey", field: "configKey", label: t("configMgmt.configKey"), align: "left", sortable: true },
  { name: "configValue", field: "configValue", label: t("configMgmt.configValue"), align: "left", sortable: false },
  { name: "type", field: "type", label: t("configMgmt.type"), align: "left", sortable: true },
  { name: "status", field: "status", label: t("configMgmt.status"), align: "left", sortable: true },
  { name: "isBuiltin", field: "isBuiltin", label: t("configMgmt.isBuiltin"), align: "left", sortable: false },
  {
    name: "createTime", field: "createTime", label: t("configMgmt.createTime"), align: "left", sortable: true,
    format: (val: string) => (val ? new Date(val).toLocaleString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false }) : "-")
  },
  { name: "actions", field: "id", label: t("common.actions"), align: "center", sortable: false }
]);

const visibleColumns = ref(columns.value.map((c) => c.name));

const SORT_FIELD_MAP: Record<string, string> = {
  name: "name", configKey: "config_key", type: "type", status: "status", createTime: "create_time"
};

let initialLoadDone = false;
let loadRequestId = 0;

async function loadTableData(props?: { pagination: { page: number; rowsPerPage: number; rowsNumber?: number; sortBy?: string; descending?: boolean } }) {
  if (props && !initialLoadDone) return;
  const requestId = ++loadRequestId;
  tableLoading.value = true;
  const pageSize = Number(props?.pagination?.rowsPerPage ?? tablePagination.value.rowsPerPage) || 10;
  const pageNum = curPage.value || 1;
  if (props?.pagination) {
    sortState.value.sortBy = props.pagination.sortBy ?? "";
    sortState.value.descending = props.pagination.descending ?? false;
    tablePagination.value.sortBy = props.pagination.sortBy ?? "";
    tablePagination.value.descending = props.pagination.descending ?? false;
  }
  const sortBy = sortState.value.sortBy || undefined;
  const sortField = sortBy ? (SORT_FIELD_MAP[sortBy] ?? sortBy) : undefined;
  const params: ConfigPageRequest = {
    pageNum, pageSize,
    keyword: searchForm.keyword || undefined,
    type: searchForm.type || undefined,
    status: searchForm.status || undefined,
    sortField,
    sortOrder: sortState.value.sortBy ? (sortState.value.descending ? "desc" : "asc") : undefined
  };
  try {
    const result = await getConfigPageApi(params);
    if (requestId !== loadRequestId) return;
    if (result.code === 10_000) {
      tableRows.value = result.data.records ?? [];
      tableTotal.value = Number(result.data.total) || 0;
      tablePagination.value.page = Number(result.data.current) || pageNum;
      tablePagination.value.rowsPerPage = Number(result.data.size) || pageSize;
      tablePagination.value.rowsNumber = Number(result.data.total) || 0;
      curPage.value = Number(result.data.current) || pageNum;
    } else {
      showToast(result.message || t("common.loadFail"), "negative");
    }
  } catch (error) {
    if (requestId !== loadRequestId) return;
    if (!isNotificationHandled(error)) showToast(t("common.loadFail"), "negative");
  } finally {
    if (requestId === loadRequestId) tableLoading.value = false;
  }
}

function handleSearch() { tablePagination.value.page = 1; curPage.value = 1; loadTableData(); }
function onPageChange(page: number) { curPage.value = Number(page); tablePagination.value.page = Number(page); loadTableData(); }
function handleJumpToPage() {
  const page = Number(jumpToPage.value);
  const maxPage = Math.ceil(tableTotal.value / tablePagination.value.rowsPerPage);
  if (page && page >= 1 && page <= maxPage) { curPage.value = page; tablePagination.value.page = page; loadTableData(); }
  jumpToPage.value = null;
}
function handleReset() {
  searchForm.keyword = ""; searchForm.type = ""; searchForm.status = "";
  sortState.value.sortBy = ""; sortState.value.descending = false;
  tablePagination.value.sortBy = ""; tablePagination.value.descending = false;
  tablePagination.value.page = 1; curPage.value = 1;
  loadTableData();
}

// ═══════════════════════════════════════════════════════════════
// 操作
// ═══════════════════════════════════════════════════════════════

function handleCreate() { openConfigDrawer("add"); }

async function handleBatchDelete() {
  if (!selectedRows.value.length) { showToast(t("common.selectRowsFirst"), "warning"); return; }
  try { await confirmDialog(t("configMgmt.batchDeleteConfirm", { count: selectedRows.value.length })); } catch { return; }
  try {
    const result = await batchDeleteConfigApi(selectedRows.value.map((r) => r.id));
    if (result.code === 10_000) { showToast(t("common.deleteSuccess"), "positive"); selectedRows.value = []; loadTableData(); }
    else { showToast(result.message || t("common.deleteFail"), "negative"); }
  } catch (error) { if (!isNotificationHandled(error)) showToast(t("common.deleteFail"), "negative"); }
}

async function handleToggleStatus(row: SysConfig) {
  const newStatus = row.status === "enabled" ? "disabled" : "enabled";
  try {
    const result = await updateConfigStatusApi({ id: row.id, status: newStatus });
    if (result.code === 10_000) { showToast(t("common.operationSuccess"), "positive"); loadTableData(); }
    else { showToast(result.message || t("common.operationFail"), "negative"); }
  } catch (error) { if (!isNotificationHandled(error)) showToast(t("common.operationFail"), "negative"); }
}

async function handleView(row: SysConfig) {
  const res = await getConfigByIdApi(row.id);
  if (res.code === 10_000 && res.data) { openConfigDrawer("view", res.data); }
  else { showToast(res.message || t("common.loadFail"), "negative"); }
}

async function handleEdit(row: SysConfig) {
  const res = await getConfigByIdApi(row.id);
  if (res.code === 10_000 && res.data) { openConfigDrawer("edit", res.data); }
  else { showToast(res.message || t("common.loadFail"), "negative"); }
}

async function handleDelete(row: SysConfig) {
  try { await confirmDialog(t("configMgmt.deleteConfirm", { name: row.name })); } catch { return; }
  try {
    const result = await deleteConfigApi(row.id);
    if (result.code === 10_000) { showToast(t("common.deleteSuccess"), "positive"); loadTableData(); }
    else { showToast(result.message || t("common.deleteFail"), "negative"); }
  } catch (error) { if (!isNotificationHandled(error)) showToast(t("common.deleteFail"), "negative"); }
}

onMounted(() => { loadTableData(); initialLoadDone = true; });
</script>

<template>
  <div class="config-list-shell">
    <div class="right-panel">
      <!-- 搜索区域 -->
      <div class="search-area">
        <div class="search-area-header row items-center no-wrap">
          <div class="row items-center no-wrap cursor-pointer" @click="searchExpanded = !searchExpanded">
            <q-icon name="sym_r_search" size="20px" class="q-mr-xs" color="grey-8" />
            <span class="search-area-title">{{ t("common.searchCondition") }}</span>
          </div>
          <q-space />
          <q-btn flat dense round size="20px"
            :icon="searchExpanded ? 'sym_r_expand_less' : 'sym_r_expand_more'"
            class="search-collapse-btn" @click="searchExpanded = !searchExpanded">
            <q-tooltip style="white-space: nowrap">{{ searchExpanded ? t("common.collapseSearch") : t("common.expandSearch") }}</q-tooltip>
          </q-btn>
        </div>
        <div v-show="searchExpanded" class="search-area-body">
          <div class="row q-col-gutter-sm items-end">
            <div class="col">
              <q-input v-model="searchForm.keyword" filled square dense
                :placeholder="t('configMgmt.keywordPlaceholder')" hide-bottom-space clearable
                @keyup.enter="handleSearch" />
            </div>
            <div class="col-auto">
              <q-select v-model="searchForm.type" filled square dense :options="typeOptions"
                :option-label="(o: { label: string; value: string }) => (o ? t(o.label) : '')"
                option-value="value" emit-value map-options hide-bottom-space clearable
                transition-show="jump-up" transition-hide="jump-down"
                class="status-select" popup-content-class="status-select-popup">
                <template v-if="!searchForm.type" v-slot:selected>
                  <span class="status-placeholder">{{ t('configMgmt.typePlaceholder') }}</span>
                </template>
              </q-select>
            </div>
            <div class="col-auto">
              <q-select v-model="searchForm.status" filled square dense :options="statusOptions"
                :option-label="(o: { label: string; value: string }) => (o ? t(o.label) : '')"
                option-value="value" emit-value map-options hide-bottom-space clearable
                transition-show="jump-up" transition-hide="jump-down"
                class="status-select" popup-content-class="status-select-popup">
                <template v-if="!searchForm.status" v-slot:selected>
                  <span class="status-placeholder">{{ t('configMgmt.statusPlaceholder') }}</span>
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

      <!-- 工具栏 -->
      <div class="toolbar-area row items-center no-wrap">
        <div class="toolbar-left row items-center no-wrap">
          <q-btn color="primary" unelevated dense no-caps class="toolbar-btn" @click.stop="handleCreate">
            <q-icon name="sym_r_add" size="20px" class="q-mr-xs" />
            {{ t('configMgmt.createConfig') }}
          </q-btn>
          <q-btn color="white" text-color="negative" outline dense no-caps class="toolbar-btn"
            :disable="!selectedRows.length" @click.stop="handleBatchDelete">
            <q-icon name="sym_r_delete" size="20px" class="q-mr-xs" />
            {{ t('common.batchDelete') }}
          </q-btn>
        </div>
        <q-space />
      </div>

      <!-- 表格 -->
      <q-table v-model:selected="selectedRows" v-model:pagination="tablePagination"
        :rows="tableRows" :columns="columns" :visible-columns="visibleColumns"
        row-key="id" :loading="tableLoading" :rows-per-page-options="[10, 20, 50, 100]"
        selection="multiple" flat
        :class="['config-table', { 'config-table--empty': !tableRows.length }]"
        @request="loadTableData">
        <template #body-cell-name="props">
          <q-td :props="props"><span>{{ props.row.name }}</span></q-td>
        </template>
        <template #body-cell-configKey="props">
          <q-td :props="props">
            <span v-if="props.row.configKey" class="config-key-text">{{ props.row.configKey }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>
        <template #body-cell-configValue="props">
          <q-td :props="props">
            <span v-if="props.row.configValue" class="config-value-text" :title="props.row.configValue">{{ props.row.configValue }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>
        <template #body-cell-type="props">
          <q-td :props="props">
            <q-badge v-if="props.row.type" :color="typeColorOf(props.row.type)" :label="typeLabelOf(props.row.type)" rounded class="config-type-badge" />
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>
        <template #body-cell-status="props">
          <q-td :props="props">
            <q-badge v-if="props.row.status" :color="statusColorOf(props.row.status)" :label="statusLabelOf(props.row.status)" rounded class="config-status-badge" />
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>
        <template #body-cell-isBuiltin="props">
          <q-td :props="props">
            <q-badge :color="props.value === 1 ? 'red-7' : 'grey-6'"
              :label="props.value === 1 ? t('common.yes') : t('common.no')" rounded
              class="config-type-badge" />
          </q-td>
        </template>
        <template #body-cell-actions="props">
          <q-td :props="props" class="q-gutter-x-xs actions-cell">
            <q-btn flat dense round size="sm"
              :color="props.row.status === 'enabled' ? 'orange-7' : 'green-7'"
              :icon="props.row.status === 'enabled' ? 'sym_r_block' : 'sym_r_check_circle'"
              @click.stop="handleToggleStatus(props.row)">
              <q-tooltip>{{ props.row.status === 'enabled' ? t('common.disable') : t('common.enable') }}</q-tooltip>
            </q-btn>
            <q-btn flat dense round size="sm" color="info" icon="sym_r_visibility" @click.stop="handleView(props.row)">
              <q-tooltip>{{ t("common.view") }}</q-tooltip>
            </q-btn>
            <q-btn flat dense round size="sm" color="primary" icon="sym_r_edit" @click.stop="handleEdit(props.row)">
              <q-tooltip>{{ t("common.edit") }}</q-tooltip>
            </q-btn>
            <q-btn flat dense round size="sm" color="negative" icon="sym_r_delete" @click.stop="handleDelete(props.row)">
              <q-tooltip>{{ t("common.delete") }}</q-tooltip>
            </q-btn>
          </q-td>
        </template>
        <template #no-data>
          <div class="column items-center justify-center q-py-xl text-grey-7 empty-state-content">
            <q-icon name="sym_r_database_search" size="56px" class="q-mb-sm" />
            <div class="text-body1 text-weight-medium q-mb-xs">{{ t("common.noData") }}</div>
            <div class="text-caption text-grey-6">{{ t("common.noDataHint") }}</div>
          </div>
        </template>
        <template #bottom="props">
          <div class="row items-center full-width table-bottom">
            <span>{{ t("common.totalRows", { count: tableTotal }) }}<template v-if="selectedRows.length">，{{ t("common.selectedRows", { count: selectedRows.length }) }}</template></span>
            <q-space />
            <q-pagination v-model="curPage" :max="props.pagesNumber" :max-pages="7" size="sm"
              color="primary" boundary-links direction-links
              icon-first="keyboard_double_arrow_left" icon-prev="keyboard_arrow_left"
              icon-next="keyboard_arrow_right" icon-last="keyboard_double_arrow_right"
              @update:model-value="onPageChange" />
            <span class="text-caption text-grey-7 q-ml-md q-mr-sm">{{ t("common.rowsPerPageLabel") }}</span>
            <q-select v-model="tablePagination.rowsPerPage" :options="[10, 20, 50, 100]"
              dense flat borderless class="rows-per-page-select" popup-content-class="rows-per-page-popup"
              @update:model-value="handleSearch">
              <template #append><span class="text-caption">{{ t("common.rowsPerPageUnit") }}</span></template>
            </q-select>
            <span class="text-caption text-grey-7 q-ml-md">{{ t("common.jumpToLabel") }}</span>
            <q-input v-model.number="jumpToPage" dense flat borderless class="jump-to-page-input"
              input-class="text-center"
              :placeholder="String((props.pagesNumber || 1) <= 1 ? 1 : (curPage >= (props.pagesNumber || 1) ? 1 : curPage + 1))"
              @keyup.enter="handleJumpToPage" />
            <span class="text-caption text-grey-7">{{ t("common.jumpToUnit") }}</span>
          </div>
        </template>
      </q-table>
    </div>
  </div>

  <!-- 抽屉 -->
  <Teleport to="body">
    <Transition name="config-drawer-slide">
      <div v-if="drawerOpen" v-mask-close="closeConfigDrawer" class="config-local-drawer-mask">
        <div class="config-local-drawer">
          <div class="config-drawer-shell">
            <div class="config-drawer-header row items-center no-wrap">
              <q-icon :name="drawerIcon" size="20px" class="q-mr-sm" />
              <span class="config-drawer-title">{{ drawerTitle }}</span>
              <q-space />
              <q-btn flat dense round icon="sym_r_close" class="config-drawer-close-btn" @click="closeConfigDrawer">
                <q-tooltip>{{ t("common.close") }}</q-tooltip>
              </q-btn>
            </div>
            <div class="config-drawer-body">
              <ConfigDrawerContent :mode="drawerMode" :config="drawerConfig"
                @close="closeConfigDrawer" @saved="handleDrawerSaved" />
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.config-list-shell { display: flex; height: calc(100vh - 64px - 40px - 44px - 16px); min-height: 0; gap: 8px; }
.right-panel { flex: 1 1 auto; min-width: 0; display: flex; flex-direction: column; overflow: hidden; }

.search-area { flex-shrink: 0; background: #fff; border: 1px solid rgba(0, 0, 0, 0.08); border-radius: 0; }
.search-area-header { height: 40px; padding: 0 8px 0 12px; background: #fafafa; border-bottom: 1px solid rgba(0, 0, 0, 0.06); user-select: none; }
.search-area-title { font-size: 14px; font-weight: 600; color: rgba(0, 0, 0, 0.87); }
.search-area-header .cursor-pointer { padding: 4px 0; }
.search-area-header .cursor-pointer:hover { opacity: 0.85; }
.search-area-body { padding: 8px; }
.search-btn { min-width: 72px; height: 40px; padding: 0 14px; font-size: 13px; }
.search-collapse-btn { width: 32px; height: 32px; min-width: 32px; min-height: 32px; padding: 0; color: rgba(0, 0, 0, 0.87); border-radius: 50%; }
.search-collapse-btn :deep(.q-btn__wrapper) { min-height: 32px; padding: 0; }
.search-collapse-btn :deep(.q-icon.material-symbols-rounded), .search-collapse-btn :deep(.material-symbols-rounded) { font-size: 20px !important; }
.search-collapse-btn:hover { background: rgba(128, 128, 128, 0.28); }
.status-select :deep(.q-field__control) { min-height: 40px; min-width: 140px; }
.status-select :deep(.q-field__native) { color: rgba(0, 0, 0, 0.87); }
.toolbar-area { flex-shrink: 0; padding: 8px 1px; }
.toolbar-left { gap: 6px; }
.toolbar-btn { height: 32px; font-size: 13px; padding: 0 12px; white-space: nowrap; flex-shrink: 0; }
.toolbar-area :deep(.q-btn) { height: 32px; font-size: 13px; }

.config-table { flex: 1 1 auto; min-height: 0; background: #fff; border: 1px solid rgba(0, 0, 0, 0.08); border-radius: 0; }
.config-table :deep(.q-table__top) { display: none; }
.config-table :deep(.q-table__container) { display: flex; flex-direction: column; height: 100%; }
.config-table :deep(.q-table__middle) { flex: 1 1 0; min-height: 0; overflow: auto; display: flex; flex-direction: column; overscroll-behavior: none; }
.config-table :deep(thead) { position: sticky; top: 0; z-index: 2; }
.config-table :deep(.q-table__middle > table) { flex: 0 0 auto; }
.config-table :deep(thead tr th) { font-weight: 700 !important; font-size: 13px !important; color: rgba(0, 0, 0, 0.8) !important; background: #fafafa !important; white-space: nowrap; border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important; }
.config-table :deep(thead tr:first-child th) { border-top: none; }
.config-table--empty :deep(.q-table__container) { height: 100%; }
.config-table--empty :deep(.q-table__middle) { flex: 0 0 auto; overflow: visible; }
.config-table--empty :deep(.q-table__bottom) { flex: 1 1 0; min-height: 0; display: flex; align-items: center; justify-content: center; border-top: none !important; }
.config-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) { display: none; }
.config-table :deep(tbody tr:hover td) { background: rgba(0, 121, 107, 0.03) !important; }
.config-table :deep(tbody tr.q-tr--selected td) { background: rgba(0, 121, 107, 0.06) !important; }
.config-table :deep(tbody td) { font-size: 13px; border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important; }

.config-key-text { font-family: "JetBrains Mono", "Fira Code", "SF Mono", "Consolas", monospace; font-size: 12px; color: rgba(0, 0, 0, 0.75); background: rgba(0, 0, 0, 0.04); padding: 2px 6px; border-radius: 3px; }
.config-value-text { display: inline-block; max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; color: rgba(0, 0, 0, 0.7); vertical-align: middle; }
.config-type-badge, .config-status-badge { font-size: 11px; padding: 3px 10px; font-weight: 500; }

.actions-cell { white-space: nowrap; }
.actions-cell :deep(.q-btn) { width: 32px; height: 32px; }
.actions-cell :deep(.q-btn .q-icon) { font-size: 20px; }
.empty-state-content { text-align: center; }

.config-table :deep(.q-table__bottom) { padding: 3px 16px 4px; font-size: 13px; min-height: 42px; background: #fff; border-top: 1px solid rgba(0, 0, 0, 0.08); }
.table-bottom { min-height: 40px; }
.table-bottom :deep(.q-pagination__content .q-btn) { width: 30px !important; height: 30px !important; min-width: 30px !important; min-height: 30px !important; border-radius: 50% !important; padding: 0 !important; font-size: 10px !important; }
.table-bottom :deep(.q-pagination__content .q-btn .q-focus-helper) { border-radius: 50%; }
.table-bottom :deep(.q-pagination__content .q-btn .q-icon) { font-size: 20px; }
.table-bottom :deep(.q-pagination__content .q-btn.q-btn--standard) { font-weight: 700; }
.table-bottom :deep(.rows-per-page-select .q-field__control) { min-height: 24px; padding: 0; height: 24px; }
.table-bottom :deep(.rows-per-page-select .q-field__native) { min-height: 24px; font-size: 12px; padding: 0; }
.table-bottom :deep(.rows-per-page-select .q-field__marginal) { height: 24px; }
.table-bottom :deep(.jump-to-page-input) { width: 40px; font-size: 12px; }
.table-bottom :deep(.jump-to-page-input .q-field__control) { min-height: 24px; padding: 0; height: 24px; }
.table-bottom :deep(.jump-to-page-input .q-field__native) { min-height: 24px; font-size: 12px; padding: 0; }
.config-table :deep(.q-checkbox__inner) { font-size: 32px; }

.config-local-drawer-mask { position: fixed; inset: 0; z-index: 5000; background: rgba(0, 0, 0, 0.3); display: flex; justify-content: flex-end; }
.config-local-drawer { width: 680px; max-width: 100vw; height: 100%; background: #fff; box-shadow: -4px 0 12px rgba(0, 0, 0, 0.12); display: flex; flex-direction: column; overflow: hidden; }
.config-drawer-shell { display: flex; flex-direction: column; height: 100%; overflow: hidden; }
.config-drawer-header { flex-shrink: 0; display: flex; align-items: center; height: 65px; padding: 0 16px; background: #fafafa; border-bottom: 1px solid rgba(0, 0, 0, 0.08); }
.config-drawer-title { font-size: 15px; font-weight: 600; color: rgba(0, 0, 0, 0.87); }
.config-drawer-close-btn { width: 32px; height: 32px; min-width: 32px; min-height: 32px; padding: 0; color: rgba(0, 0, 0, 0.6); border-radius: 50%; font-size: 20px; }
.config-drawer-close-btn :deep(.q-btn__wrapper) { min-width: 32px; min-height: 32px; padding: 0; }
.config-drawer-close-btn :deep(.q-icon) { font-size: 20px; }
.config-drawer-close-btn:hover { background: rgba(128, 128, 128, 0.2); }
.config-drawer-body { flex: 1 1 auto; min-height: 0; display: flex; flex-direction: column; overflow: hidden; padding: 0; }
.config-drawer-slide-enter-active, .config-drawer-slide-leave-active { transition: opacity 0.25s ease; }
.config-drawer-slide-enter-active .config-local-drawer, .config-drawer-slide-leave-active .config-local-drawer { transition: transform 0.25s ease; }
.config-drawer-slide-enter-from, .config-drawer-slide-leave-to { opacity: 0; }
.config-drawer-slide-enter-from .config-local-drawer, .config-drawer-slide-leave-to .config-local-drawer { transform: translateX(100%); }
</style>

<!-- 非 scoped：每页条数下拉弹出层 & 抽屉暗色模式（Teleport to body，无法用 scoped 覆盖） -->
<style>
.rows-per-page-popup .q-item {
  min-height: 36px;
  padding: 0 16px;
}

/* ═══ 暗色模式 — 抽屉（列表页暗色样式见 admin-layout-dark.scss 的 .config-list-shell） ═══ */
.body--dark .config-local-drawer {
  background: #1e1e1e !important;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.4);
}

.body--dark .config-drawer-header {
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .config-drawer-title {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .config-drawer-close-btn {
  color: rgba(255, 255, 255, 0.6) !important;
}

.body--dark .config-drawer-close-btn:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .config-drawer-body {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .config-local-drawer-mask {
  background: rgba(0, 0, 0, 0.5);
}
</style>
