<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useEscCloseDrawer } from "../../composables/useEscCloseDrawer";
import type { QTableColumn } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysTenantPackage, TenantPackagePageRequest } from "../../types/auth";
import {
  getTenantPackagePageApi,
  getTenantPackageByIdApi,
  deleteTenantPackageApi,
  batchDeleteTenantPackageApi
} from "../../apis/tenant-package";
import { useConfirmDialog } from "@repo/ui";
import TenantPackageDrawerContent from "./TenantPackageDrawerContent.vue";

const { t } = useI18n({ useScope: "global" });
const { confirmDialog } = useConfirmDialog();

// ═══════════════════════════════════════════════════════════════
// 搜索条件
// ═══════════════════════════════════════════════════════════════

const searchForm = reactive<TenantPackagePageRequest>({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  status: ""
});

const searchExpanded = ref(true);

// ── 套餐状态选项 ──
const statusOptions = [
  { label: "tenantPackageMgmt.statusEnabled", value: "enabled" },
  { label: "tenantPackageMgmt.statusDisabled", value: "disabled" }
];

const statusLabelOf = (s: string): string =>
  ({
    enabled: t("tenantPackageMgmt.statusEnabled"),
    disabled: t("tenantPackageMgmt.statusDisabled")
  }[s] ?? s);

const statusColorOf = (s: string): string =>
  ({
    enabled: "green-7",
    disabled: "grey-6"
  }[s] ?? "grey-5");

// ── 限额显示：-1 表示无限制 ──
const formatLimit = (val: number | null | undefined): string => {
  if (val === null || val === undefined || val === -1) {
    return t("tenantPackageMgmt.unlimited");
  }
  return String(val);
};

const isUnlimited = (val: number | null | undefined): boolean =>
  val === null || val === undefined || val === -1;

// ═══════════════════════════════════════════════════════════════
// 本地抽屉 — 添加 / 编辑 / 查看
// ═══════════════════════════════════════════════════════════════

type DrawerMode = "add" | "edit" | "view";

const drawerOpen = ref(false);
useEscCloseDrawer(drawerOpen);
const drawerMode = ref<DrawerMode>("add");
const drawerPkg = ref<SysTenantPackage | undefined>(undefined);

const drawerTitle = computed(() => {
  if (drawerMode.value === "add") return t("tenantPackageMgmt.addPackage");
  if (drawerMode.value === "edit") return t("tenantPackageMgmt.editPackage");
  return t("tenantPackageMgmt.viewPackage");
});

const drawerIcon = computed(() => {
  if (drawerMode.value === "add") return "sym_r_add";
  if (drawerMode.value === "edit") return "sym_r_edit";
  return "sym_r_visibility";
});

function openPkgDrawer(mode: DrawerMode, pkg?: SysTenantPackage) {
  drawerMode.value = mode;
  drawerPkg.value = pkg;
  drawerOpen.value = true;
}

function closePkgDrawer() {
  drawerOpen.value = false;
}

function handleDrawerSaved() {
  closePkgDrawer();
  loadTableData();
}

// ═══════════════════════════════════════════════════════════════
// 表格数据
// ═══════════════════════════════════════════════════════════════

const tableRows = ref<SysTenantPackage[]>([]);
const tableTotal = ref(0);
const tableLoading = ref(false);
const tablePagination = ref({
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0,
  sortBy: "",
  descending: false
});
const selectedRows = ref<SysTenantPackage[]>([]);
const sortState = ref<{ sortBy: string; descending: boolean }>({ sortBy: "", descending: false });
const jumpToPage = ref<number | null>(null);
const curPage = ref(1);

// ── 表格列定义 ──
const columns = computed<QTableColumn<SysTenantPackage>[]>(() => [
  {
    name: "name",
    field: "name",
    label: t("tenantPackageMgmt.name"),
    align: "left",
    sortable: true
  },
  {
    name: "code",
    field: "code",
    label: t("tenantPackageMgmt.code"),
    align: "left",
    sortable: true
  },
  {
    name: "userLimit",
    field: "userLimit",
    label: t("tenantPackageMgmt.userLimit"),
    align: "left",
    sortable: false
  },
  {
    name: "apiLimit",
    field: "apiLimit",
    label: t("tenantPackageMgmt.apiLimit"),
    align: "left",
    sortable: false
  },
  {
    name: "storageLimit",
    field: "storageLimit",
    label: t("tenantPackageMgmt.storageLimit"),
    align: "left",
    sortable: false
  },
  {
    name: "expireDays",
    field: "expireDays",
    label: t("tenantPackageMgmt.expireDays"),
    align: "left",
    sortable: false
  },
  {
    name: "permissionCount",
    field: "permissionCount",
    label: t("tenantPackageMgmt.permissionCount"),
    align: "left",
    sortable: true
  },
  {
    name: "sort",
    field: "sort",
    label: t("tenantPackageMgmt.sort"),
    align: "left",
    sortable: true
  },
  {
    name: "status",
    field: "status",
    label: t("tenantPackageMgmt.status"),
    align: "left",
    sortable: true
  },
  {
    name: "createTime",
    field: "createTime",
    label: t("tenantPackageMgmt.createTime"),
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
  name: "name",
  code: "code",
  status: "status",
  sort: "sort",
  permissionCount: "permission_count",
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

  const params: TenantPackagePageRequest = {
    pageNum,
    pageSize,
    keyword: searchForm.keyword || undefined,
    status: searchForm.status || undefined,
    sortField,
    sortOrder: sortState.value.sortBy ? (sortState.value.descending ? "desc" : "asc") : undefined
  };

  try {
    const result = await getTenantPackagePageApi(params);

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
  searchForm.status = "";
  // 清空排序状态（与 q-table 的 pagination.sortBy / descending 保持一致）
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
  openPkgDrawer("add");
}

// 批量删除
async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    showToast(t("common.selectRowsFirst"), "warning");
    return;
  }

  try {
    await confirmDialog(t("tenantPackageMgmt.batchDeleteConfirm", { count: selectedRows.value.length }));
  } catch {
    return;
  }

  try {
    const result = await batchDeleteTenantPackageApi(selectedRows.value.map((r) => r.id));
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
async function handleView(pkg: SysTenantPackage) {
  const res = await getTenantPackageByIdApi(pkg.id);
  if (res.code === 10_000 && res.data) {
    openPkgDrawer("view", res.data);
  } else {
    showToast(res.message || t("common.loadFail"), "negative");
  }
}

// 编辑
async function handleEdit(pkg: SysTenantPackage) {
  const res = await getTenantPackageByIdApi(pkg.id);
  if (res.code === 10_000 && res.data) {
    openPkgDrawer("edit", res.data);
  } else {
    showToast(res.message || t("common.loadFail"), "negative");
  }
}

// 删除
async function handleDelete(pkg: SysTenantPackage) {
  try {
    await confirmDialog(t("tenantPackageMgmt.deleteConfirm", { name: pkg.name }));
  } catch {
    return;
  }

  try {
    const result = await deleteTenantPackageApi(pkg.id);
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
// 生命周期
// ═══════════════════════════════════════════════════════════════

onMounted(() => {
  loadTableData();
  initialLoadDone = true;
});
</script>

<template>
  <div class="pkg-list-shell">
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
                :placeholder="t('tenantPackageMgmt.keywordPlaceholder')"
                hide-bottom-space
                clearable
                @keyup.enter="handleSearch"
              />
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.status"
                filled
                square
                dense
                :options="statusOptions"
                :option-label="(o) => (o ? t(o.label) : '')"
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
                  <span class="status-placeholder">{{ t('tenantPackageMgmt.statusPlaceholder') }}</span>
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
            {{ t('tenantPackageMgmt.createPackage') }}
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
        :class="['pkg-table', { 'pkg-table--empty': !tableRows.length }]"
        @request="loadTableData"
      >
        <!-- 套餐名称列 -->
        <template #body-cell-name="props">
          <q-td :props="props">
            <span>{{ props.row.name }}</span>
          </q-td>
        </template>

        <!-- 套餐编码列 -->
        <template #body-cell-code="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 状态列 -->
        <template #body-cell-status="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              :color="statusColorOf(props.value)"
              :label="statusLabelOf(props.value)"
              rounded
              class="pkg-type-badge"
            />
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 用户数限制列 -->
        <template #body-cell-userLimit="props">
          <q-td :props="props">
            <span :class="{ 'text-grey-6': isUnlimited(props.value) }">{{ formatLimit(props.value) }}</span>
          </q-td>
        </template>

        <!-- API调用限制列 -->
        <template #body-cell-apiLimit="props">
          <q-td :props="props">
            <span :class="{ 'text-grey-6': isUnlimited(props.value) }">{{ formatLimit(props.value) }}</span>
          </q-td>
        </template>

        <!-- 存储限制列 -->
        <template #body-cell-storageLimit="props">
          <q-td :props="props">
            <span :class="{ 'text-grey-6': isUnlimited(props.value) }">{{ isUnlimited(props.value) ? formatLimit(props.value) : formatLimit(props.value) + ' GB' }}</span>
          </q-td>
        </template>

        <!-- 有效期天数列 -->
        <template #body-cell-expireDays="props">
          <q-td :props="props">
            <span :class="{ 'text-grey-6': isUnlimited(props.value) }">{{ isUnlimited(props.value) ? formatLimit(props.value) : formatLimit(props.value) + ' ' + t('tenantPackageMgmt.days') }}</span>
          </q-td>
        </template>

        <!-- 权限数量列 -->
        <template #body-cell-permissionCount="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              color="blue-7"
              :label="props.value"
              rounded
              class="pkg-type-badge"
            />
            <span v-else class="text-grey-5">0</span>
          </q-td>
        </template>

        <!-- 排序列 -->
        <template #body-cell-sort="props">
          <q-td :props="props">
            <span>{{ props.value }}</span>
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

  <!-- ═══ 本地右侧抽屉：添加 / 编辑 / 查看套餐 ═══ -->
  <Teleport to="body">
    <Transition name="pkg-drawer-slide">
      <div v-if="drawerOpen" class="pkg-local-drawer-mask" @click.self="closePkgDrawer">
        <div class="pkg-local-drawer">
          <div class="pkg-drawer-shell">
            <div class="pkg-drawer-header row items-center no-wrap">
              <q-icon :name="drawerIcon" size="20px" class="q-mr-sm" />
              <span class="pkg-drawer-title">{{ drawerTitle }}</span>
              <q-space />
              <q-btn
                flat
                dense
                round
                icon="sym_r_close"
                class="pkg-drawer-close-btn"
                @click="closePkgDrawer"
              >
                <q-tooltip>{{ t("common.close") }}</q-tooltip>
              </q-btn>
            </div>
            <div class="pkg-drawer-body">
              <TenantPackageDrawerContent
                :mode="drawerMode"
                :pkg="drawerPkg"
                @close="closePkgDrawer"
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
.pkg-list-shell {
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

.status-select :deep(.q-field__native) {
  /* 与搜索文本框（q-input）输入文字颜色保持一致：rgba(0, 0, 0, 0.87) */
  color: rgba(0, 0, 0, 0.87);
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
.pkg-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.pkg-table :deep(.q-table__top) {
  display: none;
}

.pkg-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.pkg-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.pkg-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.pkg-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.pkg-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.pkg-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ── 空数据状态 ── */
.pkg-table--empty :deep(.q-table__container) {
  height: 100%;
}

.pkg-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.pkg-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.pkg-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

/* 行悬停 */
.pkg-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.pkg-table :deep(tbody tr.q-tr--selected td) {
  background: rgba(0, 121, 107, 0.06) !important;
}

.pkg-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

/* Badge 统一样式 */
.pkg-type-badge {
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
.pkg-table :deep(.q-table__bottom) {
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
.pkg-table :deep(.q-checkbox__inner) {
  font-size: 32px;
}

/* ═══ 本地右侧抽屉 ═══ */
.pkg-local-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 5000;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: flex-end;
}

.pkg-local-drawer {
  width: 680px;
  max-width: 100vw;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.pkg-drawer-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.pkg-drawer-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 65px;
  padding: 0 16px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.pkg-drawer-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.pkg-drawer-close-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  font-size: 20px;
}

.pkg-drawer-close-btn :deep(.q-btn__wrapper) {
  min-width: 32px;
  min-height: 32px;
  padding: 0;
}

.pkg-drawer-close-btn :deep(.q-icon) {
  font-size: 20px;
}

.pkg-drawer-close-btn:hover {
  background: rgba(128, 128, 128, 0.2);
}

.pkg-drawer-body {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 16px;
}

/* 抽屉滑入/滑出动画 */
.pkg-drawer-slide-enter-active,
.pkg-drawer-slide-leave-active {
  transition: opacity 0.25s ease;
}

.pkg-drawer-slide-enter-active .pkg-local-drawer,
.pkg-drawer-slide-leave-active .pkg-local-drawer {
  transition: transform 0.25s ease;
}

.pkg-drawer-slide-enter-from,
.pkg-drawer-slide-leave-to {
  opacity: 0;
}

.pkg-drawer-slide-enter-from .pkg-local-drawer,
.pkg-drawer-slide-leave-to .pkg-local-drawer {
  transform: translateX(100%);
}
</style>

<!-- 非 scoped：状态选择下拉弹出层 & 抽屉暗色模式（Teleport to body，无法用 scoped 覆盖） -->
<style>
.status-select-popup .q-item {
  min-height: 40px;
  padding: 0 16px;
}

.rows-per-page-popup .q-item {
  min-height: 36px;
  padding: 0 16px;
}

/* 抽屉暗色模式 */
.body--dark .pkg-local-drawer {
  background: #1e1e1e !important;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.4);
}

.body--dark .pkg-drawer-header {
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .pkg-drawer-title {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .pkg-drawer-close-btn {
  color: rgba(255, 255, 255, 0.6) !important;
}

.body--dark .pkg-drawer-close-btn:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .pkg-drawer-body {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .pkg-local-drawer-mask {
  background: rgba(0, 0, 0, 0.5);
}
</style>
