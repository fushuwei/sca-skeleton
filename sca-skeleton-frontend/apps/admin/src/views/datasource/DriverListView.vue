<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useEscCloseDrawer } from "../../composables/useEscCloseDrawer";
import type { QTableColumn } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { Driver, DriverFile, DriverPageRequest } from "../../apis/datasource";
import {
  getDriverPageApi,
  getDriverByIdApi,
  deleteDriverApi,
  batchDeleteDriverApi
} from "../../apis/datasource";
import { useConfirmDialog } from "@repo/ui";
import DbTypeIcon from "../../components/DbTypeIcon.vue";
import DriverDrawerContent from "./DriverDrawerContent.vue";

const { t } = useI18n({ useScope: "global" });
const { confirmDialog } = useConfirmDialog();

// ═══════════════════════════════════════════════════════════════
// 数据库类型选项（与后端 DbType 枚举一致）
// ═══════════════════════════════════════════════════════════════

const DB_TYPE_OPTIONS = [
  { label: "MySQL", value: "MYSQL" },
  { label: "Oracle", value: "ORACLE" },
  { label: "PostgreSQL", value: "POSTGRESQL" },
  { label: "SQLServer", value: "SQLSERVER" },
  { label: "达梦数据库", value: "DAMENG" },
  { label: "Kingbase", value: "KINGBASE" },
  { label: "MongoDB", value: "MONGODB" },
  { label: "ClickHouse", value: "CLICKHOUSE" },
  { label: "OceanBase", value: "OCEANBASE" },
  { label: "GaussDB", value: "GAUSSDB" }
];

// ═══════════════════════════════════════════════════════════════
// 搜索条件
// ═══════════════════════════════════════════════════════════════

const searchForm = reactive<DriverPageRequest>({
  pageNum: 1,
  pageSize: 10,
  dbType: undefined,
  keyword: ""
});

const searchExpanded = ref(true);

// ═══════════════════════════════════════════════════════════════
// 本地抽屉 — 添加 / 编辑 / 查看
// ═══════════════════════════════════════════════════════════════

type DrawerMode = "add" | "edit" | "view";

const drawerOpen = ref(false);
useEscCloseDrawer(drawerOpen);
const drawerMode = ref<DrawerMode>("add");
const drawerDriver = ref<Driver | undefined>(undefined);

const drawerTitle = computed(() => {
  if (drawerMode.value === "add") return t("driverMgmt.addDriver");
  if (drawerMode.value === "edit") return t("driverMgmt.editDriver");
  return t("driverMgmt.viewDriver");
});

const drawerIcon = computed(() => {
  if (drawerMode.value === "add") return "sym_r_add";
  if (drawerMode.value === "edit") return "sym_r_edit";
  return "sym_r_visibility";
});

function openDriverDrawer(mode: DrawerMode, driver?: Driver) {
  drawerMode.value = mode;
  drawerDriver.value = driver;
  drawerOpen.value = true;
}

function closeDriverDrawer() {
  drawerOpen.value = false;
}

function handleDrawerSaved() {
  closeDriverDrawer();
  loadTableData();
}

// ═══════════════════════════════════════════════════════════════
// 表格数据
// ═══════════════════════════════════════════════════════════════

const tableRows = ref<Driver[]>([]);
const tableTotal = ref(0);
const tableLoading = ref(false);
const tablePagination = ref({
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0,
  sortBy: "",
  descending: false
});
const selectedRows = ref<Driver[]>([]);
const sortState = ref<{ sortBy: string; descending: boolean }>({ sortBy: "", descending: false });
const jumpToPage = ref<number | null>(null);
const curPage = ref(1);

// ── 表格列定义 ──
const columns = computed<QTableColumn<Driver>[]>(() => [
  {
    name: "driverName",
    field: "name",
    label: t("driverMgmt.driverName"),
    align: "left",
    sortable: true
  },
  {
    name: "dbType",
    field: "dbType",
    label: t("driverMgmt.dbType"),
    align: "left",
    sortable: true
  },
  {
    name: "driverClass",
    field: "driverClass",
    label: t("driverMgmt.driverClass"),
    align: "left",
    sortable: true,
    format: (val: string) => (val ? val : "-")
  },
  {
    name: "files",
    field: "files",
    label: t("driverMgmt.driverFiles"),
    align: "left",
    sortable: false
  },
  {
    name: "fileSize",
    field: "totalFileSize",
    label: t("driverMgmt.fileSize"),
    align: "left",
    sortable: true,
    format: (val: number | string) => (val ? formatFileSize(val) : "-")
  },
  {
    name: "createTime",
    field: "createTime",
    label: t("driverMgmt.createTime"),
    align: "left",
    sortable: true,
    format: (val: string) => (val ? new Date(val).toLocaleString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false }) : "-")
  },
  {
    name: "actions",
    field: "id",
    label: t("common.actions"),
    align: "center",
    sortable: false
  }
]);

const visibleColumns = ref(columns.value.map((c) => c.name));

// ── 前端列名 → 后端排序列名映射 ──
const SORT_FIELD_MAP: Record<string, string> = {
  driverName: "name",
  dbType: "db_type",
  driverClass: "driver_class",
  fileSize: "total_file_size",
  createTime: "create_time"
};

let initialLoadDone = false;
let loadRequestId = 0;

// ═══════════════════════════════════════════════════════════════
// 数据加载
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

  const params: DriverPageRequest = {
    pageNum,
    pageSize,
    dbType: searchForm.dbType || undefined,
    keyword: searchForm.keyword || undefined,
    sortField,
    sortOrder: sortState.value.sortBy ? (sortState.value.descending ? "desc" : "asc") : undefined
  };

  try {
    const result = await getDriverPageApi(params);

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
    if (!isNotificationHandled(error)) {
      showToast(t("common.loadFail"), "negative");
    }
  } finally {
    if (requestId === loadRequestId) {
      tableLoading.value = false;
    }
  }
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
  searchForm.dbType = undefined;
  sortState.value.sortBy = "";
  sortState.value.descending = false;
  tablePagination.value.sortBy = "";
  tablePagination.value.descending = false;
  tablePagination.value.page = 1;
  curPage.value = 1;
  loadTableData();
}

// ═══════════════════════════════════════════════════════════════
// 操作
// ═══════════════════════════════════════════════════════════════

function handleCreate() {
  openDriverDrawer("add");
}

// 批量删除
async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    showToast(t("common.selectRowsFirst"), "warning");
    return;
  }

  try {
    await confirmDialog(t("driverMgmt.batchDeleteConfirm", { count: selectedRows.value.length }));
  } catch {
    return;
  }

  try {
    const result = await batchDeleteDriverApi(selectedRows.value.map((r) => r.id));
    if (result.code === 10_000) {
      showToast(t("common.deleteSuccess"), "positive");
      selectedRows.value = [];
      loadTableData();
    } else {
      showToast(result.message || t("common.deleteFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("common.deleteFail"), "negative");
    }
  }
}

// 查看
async function handleView(driver: Driver) {
  const res = await getDriverByIdApi(driver.id);
  if (res.code === 10_000 && res.data) {
    openDriverDrawer("view", res.data);
  } else {
    showToast(res.message || t("common.loadFail"), "negative");
  }
}

// 编辑
async function handleEdit(driver: Driver) {
  const res = await getDriverByIdApi(driver.id);
  if (res.code === 10_000 && res.data) {
    openDriverDrawer("edit", res.data);
  } else {
    showToast(res.message || t("common.loadFail"), "negative");
  }
}

// 删除
async function handleDelete(driver: Driver) {
  try {
    await confirmDialog(t("driverMgmt.deleteConfirm", { name: driver.name }));
  } catch {
    return;
  }

  try {
    const result = await deleteDriverApi(driver.id);
    if (result.code === 10_000) {
      showToast(t("common.deleteSuccess"), "positive");
      loadTableData();
    } else {
      showToast(result.message || t("common.deleteFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("common.deleteFail"), "negative");
    }
  }
}

// ═══════════════════════════════════════════════════════════════
// 工具函数
// ═══════════════════════════════════════════════════════════════

function formatFileSize(bytes: number | string): string {
  const n = Number(bytes);
  if (!n) return "-";
  if (n < 1024) return n + " B";
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + " KB";
  if (n < 1024 * 1024 * 1024) return (n / 1024 / 1024).toFixed(1) + " MB";
  return (n / 1024 / 1024 / 1024).toFixed(2) + " GB";
}

function getDbTypeLabel(dbType: string): string {
  return DB_TYPE_OPTIONS.find((o) => o.value === dbType)?.label ?? dbType;
}

// ═══════════════════════════════════════════════════════════════
// 生命周期
// ═══════════════════════════════════════════════════════════════

onMounted(() => {
  loadTableData();
  initialLoadDone = true;
});
</script>

<template>
  <div class="driver-list-shell">
    <!-- ═══ 内容区 ═══ -->
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
                :placeholder="t('driverMgmt.keywordPlaceholder')"
                hide-bottom-space
                clearable
                @keyup.enter="handleSearch"
              />
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.dbType"
                filled
                square
                dense
                :options="DB_TYPE_OPTIONS"
                option-value="value"
                emit-value
                map-options
                hide-bottom-space
                clearable
                transition-show="jump-up"
                transition-hide="jump-down"
                class="status-select db-type-select"
                popup-content-class="status-select-popup"
              >
                <!-- 选中时展示图标 + 标签 -->
                <template v-slot:selected>
                  <div v-if="searchForm.dbType" class="row items-center no-wrap">
                    <DbTypeIcon :db-type="searchForm.dbType" :size="18" class="q-mr-xs" />
                    <span>{{ getDbTypeLabel(searchForm.dbType) }}</span>
                  </div>
                  <span v-else class="status-placeholder">{{ t('driverMgmt.dbTypePlaceholder') }}</span>
                </template>
                <!-- 下拉选项：图标 + 标签 -->
                <template v-slot:option="scope">
                  <q-item v-bind="scope.itemProps">
                    <q-item-section avatar style="min-width: auto; padding-right: 8px;">
                      <DbTypeIcon :db-type="scope.opt.value" :size="20" />
                    </q-item-section>
                    <q-item-section>
                      <q-item-label>{{ scope.opt.label }}</q-item-label>
                    </q-item-section>
                  </q-item>
                </template>
              </q-select>
            </div>
            <div class="col-auto">
              <div class="row q-gutter-x-sm no-wrap">
                <q-btn
                  color="primary"
                  unelevated
                  no-caps
                  class="search-btn"
                  @click="handleSearch"
                >
                  <q-icon name="sym_r_search" size="20px" class="q-mr-xs" />
                  {{ t("common.search") }}
                </q-btn>
                <q-btn
                  color="grey-7"
                  outline
                  no-caps
                  class="search-btn"
                  @click="handleReset"
                >
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
          <q-btn
            color="primary"
            unelevated
            dense
            no-caps
            class="toolbar-btn"
            @click.stop="handleCreate"
          >
            <q-icon name="sym_r_add" size="20px" class="q-mr-xs" />
            {{ t('driverMgmt.createDriver') }}
          </q-btn>
          <q-btn
            color="white"
            text-color="negative"
            outline
            dense
            no-caps
            class="toolbar-btn"
            :disable="!selectedRows.length"
            @click.stop="handleBatchDelete"
          >
            <q-icon name="sym_r_delete" size="20px" class="q-mr-xs" />
            {{ t('common.batchDelete') }}
          </q-btn>
        </div>
        <q-space />
      </div>

      <!-- ── 表格区域 ── -->
      <q-table
        v-model:selected="selectedRows"
        v-model:pagination="tablePagination"
        :rows="tableRows"
        :columns="columns"
        :visible-columns="visibleColumns"
        row-key="id"
        :loading="tableLoading"
        :rows-per-page-options="[10, 20, 50, 100]"
        selection="multiple"
        flat
        :class="['driver-table', { 'driver-table--empty': !tableRows.length }]"
        @request="loadTableData"
      >
        <!-- 驱动名称列 -->
        <template #body-cell-driverName="props">
          <q-td :props="props">
            <span>{{ props.row.name }}</span>
          </q-td>
        </template>

        <!-- 数据库类型列 -->
        <template #body-cell-dbType="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              color="blue-2"
              text-color="blue-9"
              :label="getDbTypeLabel(props.row.dbType)"
              rounded
              class="db-type-badge"
            />
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 驱动类名列 -->
        <template #body-cell-driverClass="props">
          <q-td :props="props">
            <span v-if="props.value" class="mono-text">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 驱动文件列：显示文件名，用“、”分隔，超长截断 -->
        <template #body-cell-files="props">
          <q-td :props="props">
            <span v-if="props.row.files?.length" class="file-names-cell">
              <span class="file-names-text">{{ props.row.files.map((f: DriverFile) => f.fileName).join("、") }}</span>
              <q-tooltip>
                <div class="q-gutter-y-xs">
                  <div v-for="f in props.row.files" :key="f.fileName" class="row items-center">
                    <span>{{ f.fileName }}</span>
                  </div>
                </div>
              </q-tooltip>
            </span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 操作列 -->
        <template #body-cell-actions="props">
          <q-td :props="props" class="q-gutter-x-xs actions-cell">
            <q-btn
              flat
              dense
              round
              size="sm"
              color="info"
              icon="sym_r_visibility"
              @click.stop="handleView(props.row)"
            >
              <q-tooltip>{{ t("common.view") }}</q-tooltip>
            </q-btn>
            <q-btn
              flat
              dense
              round
              size="sm"
              color="primary"
              icon="sym_r_edit"
              @click.stop="handleEdit(props.row)"
            >
              <q-tooltip>{{ t("common.edit") }}</q-tooltip>
            </q-btn>
            <q-btn
              flat
              dense
              round
              size="sm"
              color="negative"
              icon="sym_r_delete"
              @click.stop="handleDelete(props.row)"
            >
              <q-tooltip>{{ t("common.delete") }}</q-tooltip>
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
              {{ t("common.totalRows", { count: tableTotal }) }}<template v-if="selectedRows.length">，{{ t("common.selectedRows", { count: selectedRows.length }) }}</template>
            </span>
            <q-space />
            <q-pagination
              v-model="curPage"
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

  <!-- ═══ 本地右侧抽屉：添加 / 编辑 / 查看驱动 ═══ -->
  <Teleport to="body">
    <Transition name="driver-drawer-slide">
      <div
        v-if="drawerOpen"
        class="driver-local-drawer-mask"
        v-mask-close="closeDriverDrawer"
      >
        <div class="driver-local-drawer">
          <div class="driver-drawer-shell">
            <div class="driver-drawer-header row items-center no-wrap">
              <q-icon :name="drawerIcon" size="20px" class="q-mr-sm" />
              <span class="driver-drawer-title">{{ drawerTitle }}</span>
              <q-space />
              <q-btn
                flat
                dense
                round
                icon="sym_r_close"
                class="driver-drawer-close-btn"
                @click="closeDriverDrawer"
              >
                <q-tooltip>{{ t("common.close") }}</q-tooltip>
              </q-btn>
            </div>
            <div class="driver-drawer-body">
              <DriverDrawerContent
                :mode="drawerMode"
                :driver="drawerDriver"
                @close="closeDriverDrawer"
                @saved="handleDrawerSaved"
              />
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ═══ 整体壳层 ═══ */
.driver-list-shell {
  display: flex;
  height: calc(100vh - 64px - 40px - 44px - 16px);
  min-height: 0;
  gap: 8px;
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
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.search-area-header {
  height: 40px;
  padding: 0 8px 0 12px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  user-select: none;
}

.search-area-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
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

.status-select :deep(.q-field__native) {
  /* 与搜索文本框（q-input）输入文字颜色保持一致：rgba(0, 0, 0, 0.87) */
  color: rgba(0, 0, 0, 0.87);
}

.status-select :deep(.q-field__control) {
  min-height: 40px;
  min-width: 160px;
}

.db-type-select :deep(.q-field__control) {
  min-width: 180px;
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
.driver-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.driver-table :deep(.q-table__top) {
  display: none;
}

.driver-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.driver-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.driver-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.driver-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.driver-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.driver-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ── 空数据状态 ── */
.driver-table--empty :deep(.q-table__container) {
  height: 100%;
}

.driver-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.driver-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.driver-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

/* 行悬停 */
.driver-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.driver-table :deep(tbody tr.q-tr--selected td) {
  background: rgba(0, 121, 107, 0.06) !important;
}

.driver-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

/* 等宽字体（驱动类名、SHA256 等技术字段） */
.mono-text {
  font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
  font-size: 12px;
}

/* 数据库类型徽章 */
.db-type-badge {
font-size: 11px;
padding: 3px 10px;
font-weight: 500;
}

/* 驱动文件列：文件名超长截断 */
.file-names-cell {
display: inline-block;
max-width: 390px;
overflow: hidden;
text-overflow: ellipsis;
white-space: nowrap;
cursor: default;
}

.file-names-text {
display: inline-block;
max-width: 100%;
overflow: hidden;
text-overflow: ellipsis;
white-space: nowrap;
vertical-align: bottom;
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
.driver-table :deep(.q-table__bottom) {
  padding: 3px 16px 4px;
  font-size: 13px;
  min-height: 42px;
  background: #fff;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
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

/* 复选框尺寸 */
.driver-table :deep(.q-checkbox__inner) {
  font-size: 32px;
}

/* ═══ 本地右侧抽屉 ═══ */
.driver-local-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 5000;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: flex-end;
}

.driver-local-drawer {
  width: 680px;
  max-width: 100vw;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.driver-drawer-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.driver-drawer-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 65px;
  padding: 0 16px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.driver-drawer-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.driver-drawer-close-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  font-size: 20px;
}

.driver-drawer-close-btn :deep(.q-btn__wrapper) {
  min-width: 32px;
  min-height: 32px;
  padding: 0;
}

.driver-drawer-close-btn :deep(.q-icon) {
  font-size: 20px;
}

.driver-drawer-close-btn:hover {
  background: rgba(128, 128, 128, 0.2);
}

.driver-drawer-body {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 16px;
}

/* 抽屉滑入/滑出动画 */
.driver-drawer-slide-enter-active,
.driver-drawer-slide-leave-active {
  transition: opacity 0.25s ease;
}

.driver-drawer-slide-enter-active .driver-local-drawer,
.driver-drawer-slide-leave-active .driver-local-drawer {
  transition: transform 0.25s ease;
}

.driver-drawer-slide-enter-from,
.driver-drawer-slide-leave-to {
  opacity: 0;
}

.driver-drawer-slide-enter-from .driver-local-drawer,
.driver-drawer-slide-leave-to .driver-local-drawer {
  transform: translateX(100%);
}
</style>

<!-- 非 scoped：每页条数下拉弹出层 & 抽屉暗色模式（Teleport to body，无法用 scoped 覆盖） -->
<style>
.status-select-popup .q-item {
  min-height: 40px;
  padding: 0 16px;
}

/* 数据库类型下拉选项中的 avatar 区域紧凑化 */
.status-select-popup .q-item__section--avatar {
  min-width: 28px;
  padding-right: 8px;
}

.rows-per-page-popup .q-item {
  min-height: 36px;
  padding: 0 16px;
}

/* 列表页暗色模式已迁移至全局 admin-layout-dark.scss */

/* 抽屉暗色模式 */
.body--dark .driver-local-drawer {
  background: #1e1e1e !important;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.4);
}

.body--dark .driver-drawer-header {
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .driver-drawer-title {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .driver-drawer-close-btn {
  color: rgba(255, 255, 255, 0.6) !important;
}

.body--dark .driver-drawer-close-btn:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .driver-drawer-body {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .driver-local-drawer-mask {
  background: rgba(0, 0, 0, 0.5);
}
</style>
