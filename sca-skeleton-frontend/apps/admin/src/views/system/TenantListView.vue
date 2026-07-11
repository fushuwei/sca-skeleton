<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import type { QTableColumn } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysTenant, SysTenantPackage, TenantPageRequest } from "../../types/auth";
import {
  getTenantPageApi,
  deleteTenantApi
} from "../../apis/tenant";
import { getTenantPackageListApi } from "../../apis/tenant-package";
import { useConfirmDialog } from "@repo/ui";
import TenantDrawerContent from "./TenantDrawerContent.vue";

const { t } = useI18n({ useScope: "global" });
const { confirmDialog } = useConfirmDialog();

// ═══════════════════════════════════════════════════════════════
// 搜索条件
// ═══════════════════════════════════════════════════════════════

const searchForm = reactive<TenantPageRequest>({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  status: "",
  packageId: ""
});

const searchExpanded = ref(true);

// ── 租户状态选项 ──
const statusOptions = [
  { label: "tenantMgmt.statusNormal", value: "normal" },
  { label: "tenantMgmt.statusDisabled", value: "disabled" },
  { label: "tenantMgmt.statusExpired", value: "expired" },
  { label: "tenantMgmt.statusCancelled", value: "cancelled" }
];

const statusLabelOf = (s: string): string =>
  ({
    normal: t("tenantMgmt.statusNormal"),
    disabled: t("tenantMgmt.statusDisabled"),
    expired: t("tenantMgmt.statusExpired"),
    cancelled: t("tenantMgmt.statusCancelled")
  }[s] ?? s);

const statusColorOf = (s: string): string =>
  ({
    normal: "green-7",
    disabled: "grey-6",
    expired: "orange-7",
    cancelled: "red-7"
  }[s] ?? "grey-5");

// ── 套餐筛选选项 ──
const packageFilterOptions = ref<SysTenantPackage[]>([]);

async function loadPackageFilterOptions() {
  try {
    const result = await getTenantPackageListApi();
    if (result.code === 10_000 && result.data) {
      packageFilterOptions.value = result.data;
    }
  } catch {
    // 静默失败
  }
}

// ── 限额显示：-1 表示无限制 ──
const formatLimit = (val: number | null | undefined): string => {
  if (val === null || val === undefined || val === -1) {
    return t("tenantMgmt.unlimited");
  }
  return String(val);
};

const isUnlimited = (val: number | null | undefined): boolean =>
  val === null || val === undefined || val === -1;

// ── 过期时间显示：null 表示永不过期 ──
const formatExpireTime = (val: string | null | undefined): string => {
  if (!val) return t("tenantMgmt.neverExpires");
  return new Date(val).toLocaleString("zh-CN", {
    year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false
  });
};

const isNeverExpires = (val: string | null | undefined): boolean =>
  !val;

// ═══════════════════════════════════════════════════════════════
// 本地抽屉 — 添加 / 编辑 / 查看
// ═══════════════════════════════════════════════════════════════

type DrawerMode = "add" | "edit" | "view";

const drawerOpen = ref(false);
const drawerMode = ref<DrawerMode>("add");
const drawerTenant = ref<SysTenant | undefined>(undefined);

const drawerTitle = computed(() => {
  if (drawerMode.value === "add") return t("tenantMgmt.addTenant");
  if (drawerMode.value === "edit") return t("tenantMgmt.editTenant");
  return t("tenantMgmt.viewTenant");
});

const drawerIcon = computed(() => {
  if (drawerMode.value === "add") return "sym_r_add";
  if (drawerMode.value === "edit") return "sym_r_edit";
  return "sym_r_visibility";
});

function openTenantDrawer(mode: DrawerMode, tenant?: SysTenant) {
  drawerMode.value = mode;
  drawerTenant.value = tenant;
  drawerOpen.value = true;
}

function closeTenantDrawer() {
  drawerOpen.value = false;
}

function handleDrawerSaved() {
  closeTenantDrawer();
  loadTableData();
}

// ═══════════════════════════════════════════════════════════════
// 表格数据
// ═══════════════════════════════════════════════════════════════

const tableRows = ref<SysTenant[]>([]);
const tableTotal = ref(0);
const tableLoading = ref(false);
const tablePagination = ref({
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0,
  sortBy: "",
  descending: false
});
const selectedRows = ref<SysTenant[]>([]);
const sortState = ref<{ sortBy: string; descending: boolean }>({ sortBy: "", descending: false });
const jumpToPage = ref<number | null>(null);
const curPage = ref(1);

// ── 表格列定义 ──
const columns = computed<QTableColumn<SysTenant>[]>(() => [
  {
    name: "name",
    field: "name",
    label: t("tenantMgmt.name"),
    align: "left",
    sortable: true
  },
  {
    name: "code",
    field: "code",
    label: t("tenantMgmt.code"),
    align: "left",
    sortable: true
  },
  {
    name: "packageName",
    field: "packageName",
    label: t("tenantMgmt.package"),
    align: "left",
    sortable: false
  },
  {
    name: "status",
    field: "status",
    label: t("tenantMgmt.status"),
    align: "center",
    sortable: true
  },
  {
    name: "contactName",
    field: "contactName",
    label: t("tenantMgmt.contactName"),
    align: "left",
    sortable: false
  },
  {
    name: "contactPhone",
    field: "contactPhone",
    label: t("tenantMgmt.contactPhone"),
    align: "left",
    sortable: false
  },
  {
    name: "accountLimit",
    field: "accountLimit",
    label: t("tenantMgmt.accountLimit"),
    align: "center",
    sortable: true
  },
  {
    name: "expireTime",
    field: "expireTime",
    label: t("tenantMgmt.expireTime"),
    align: "center",
    sortable: true
  },
  {
    name: "createTime",
    field: "createTime",
    label: t("tenantMgmt.createTime"),
    align: "center",
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
  accountLimit: "account_limit",
  expireTime: "expire_time",
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
  const orderBy = sortBy ? (SORT_FIELD_MAP[sortBy] ?? sortBy) : undefined;

  const params: TenantPageRequest = {
    pageNum,
    pageSize,
    keyword: searchForm.keyword || undefined,
    status: searchForm.status || undefined,
    packageId: searchForm.packageId || undefined,
    orderBy,
    orderDirection: sortState.value.sortBy ? (sortState.value.descending ? "desc" : "asc") : undefined
  };

  try {
    const result = await getTenantPageApi(params);

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
  searchForm.packageId = "";
  tablePagination.value.page = 1;
  curPage.value = 1;
  loadTableData();
}

// ═══════════════════════════════════════════════════════════════
// 操作
// ═══════════════════════════════════════════════════════════════

function handleCreate() {
  openTenantDrawer("add");
}

// 批量删除
async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    showToast(t("common.selectRowsFirst"), "warning");
    return;
  }

  const builtinTenants = selectedRows.value.filter((r) => r.code === "default");
  if (builtinTenants.length) {
    showToast(
      t("tenantMgmt.cannotDeleteBuiltinBatch", {
        names: builtinTenants.map((r) => r.name).join("、")
      }),
      "warning"
    );
    return;
  }

  try {
    await confirmDialog(t("tenantMgmt.batchDeleteConfirm", { count: selectedRows.value.length }));
  } catch {
    return;
  }

  let successCount = 0;
  let failCount = 0;

  for (const tenant of selectedRows.value) {
    try {
      const result = await deleteTenantApi(tenant.id);
      if (result.code === 10_000) {
        successCount++;
      } else {
        failCount++;
      }
    } catch {
      failCount++;
    }
  }

  showToast(
    t("tenantMgmt.batchDeleteResult", { success: successCount, fail: failCount }),
    successCount > 0 ? "positive" : "negative"
  );

  selectedRows.value = [];
  loadTableData();
}

// 查看
function handleView(tenant: SysTenant) {
  openTenantDrawer("view", tenant);
}

// 编辑
function handleEdit(tenant: SysTenant) {
  openTenantDrawer("edit", tenant);
}

// 删除
async function handleDelete(tenant: SysTenant) {
  if (tenant.code === "default") {
    showToast(t("tenantMgmt.cannotDeleteBuiltin"), "warning");
    return;
  }

  try {
    await confirmDialog(t("tenantMgmt.deleteConfirm", { name: tenant.name }));
  } catch {
    return;
  }

  try {
    const result = await deleteTenantApi(tenant.id);
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
  loadPackageFilterOptions();
  loadTableData();
  initialLoadDone = true;
});
</script>

<template>
  <div class="tenant-list-shell">
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
                :placeholder="t('tenantMgmt.keywordPlaceholder')"
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
                  <span class="status-placeholder">{{ t('tenantMgmt.statusPlaceholder') }}</span>
                </template>
              </q-select>
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.packageId"
                filled
                square
                dense
                :options="packageFilterOptions"
                :option-label="(o: SysTenantPackage) => o ? o.name : ''"
                option-value="id"
                emit-value
                map-options
                hide-bottom-space
                clearable
                transition-show="jump-up"
                transition-hide="jump-down"
                class="package-select"
                popup-content-class="status-select-popup"
              >
                <template v-if="!searchForm.packageId" v-slot:selected>
                  <span class="status-placeholder">{{ t('tenantMgmt.packagePlaceholder') }}</span>
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
            {{ t('tenantMgmt.createTenant') }}
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
        :class="['tenant-table', { 'tenant-table--empty': !tableRows.length }]"
        @request="loadTableData"
      >
        <!-- 租户名称列 -->
        <template #body-cell-name="props">
          <q-td :props="props">
            <span>{{ props.row.name }}</span>
          </q-td>
        </template>

        <!-- 租户编码列 -->
        <template #body-cell-code="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 套餐名称列 -->
        <template #body-cell-packageName="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              color="teal-7"
              :label="props.value"
              rounded
              class="tenant-type-badge"
            />
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
              class="tenant-type-badge"
            />
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 联系人姓名列 -->
        <template #body-cell-contactName="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 联系人电话列 -->
        <template #body-cell-contactPhone="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 账号数量限制列 -->
        <template #body-cell-accountLimit="props">
          <q-td :props="props">
            <span :class="{ 'text-grey-6': isUnlimited(props.value) }">{{ formatLimit(props.value) }}</span>
          </q-td>
        </template>

        <!-- 过期时间列 -->
        <template #body-cell-expireTime="props">
          <q-td :props="props">
            <span :class="{ 'text-grey-6': isNeverExpires(props.value) }">{{ formatExpireTime(props.value) }}</span>
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
              :disable="props.row.code === 'default'"
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
              :disable="props.row.code === 'default'"
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

  <!-- ═══ 本地右侧抽屉：添加 / 编辑 / 查看租户 ═══ -->
  <Teleport to="body">
    <Transition name="tenant-drawer-slide">
      <div v-if="drawerOpen" class="tenant-local-drawer-mask" @click.self="closeTenantDrawer">
        <div class="tenant-local-drawer">
          <div class="tenant-drawer-shell">
            <div class="tenant-drawer-header row items-center no-wrap">
              <q-icon :name="drawerIcon" size="20px" class="q-mr-sm" />
              <span class="tenant-drawer-title">{{ drawerTitle }}</span>
              <q-space />
              <q-btn
                flat
                dense
                round
                icon="sym_r_close"
                class="tenant-drawer-close-btn"
                @click="closeTenantDrawer"
              />
            </div>
            <div class="tenant-drawer-body">
              <TenantDrawerContent
                :mode="drawerMode"
                :tenant="drawerTenant"
                @close="closeTenantDrawer"
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
.tenant-list-shell {
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

.status-select :deep(.q-field__native),
.package-select :deep(.q-field__native) {
  color: rgba(0, 0, 0, 0.72);
}

.status-select :deep(.q-field__control),
.package-select :deep(.q-field__control) {
  min-height: 40px;
  min-width: 160px;
}

.status-placeholder {
  color: rgba(0, 0, 0, 0.4);
  font-size: 14px;
  pointer-events: none;
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
.tenant-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.tenant-table :deep(.q-table__top) {
  display: none;
}

.tenant-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.tenant-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.tenant-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.tenant-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.tenant-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.tenant-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ── 空数据状态 ── */
.tenant-table--empty :deep(.q-table__container) {
  height: 100%;
}

.tenant-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.tenant-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.tenant-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

/* 行悬停 */
.tenant-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.tenant-table :deep(tbody tr.q-tr--selected td) {
  background: rgba(0, 121, 107, 0.06) !important;
}

.tenant-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

/* Badge 统一样式 */
.tenant-type-badge {
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
.tenant-table :deep(.q-table__bottom) {
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
.tenant-table :deep(.q-checkbox__inner) {
  font-size: 32px;
}

/* ═══ 本地右侧抽屉 ═══ */
.tenant-local-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 5000;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: flex-end;
}

.tenant-local-drawer {
  width: 680px;
  max-width: 100vw;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.tenant-drawer-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.tenant-drawer-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 65px;
  padding: 0 16px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.tenant-drawer-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.tenant-drawer-close-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  font-size: 20px;
}

.tenant-drawer-close-btn :deep(.q-btn__wrapper) {
  min-width: 32px;
  min-height: 32px;
  padding: 0;
}

.tenant-drawer-close-btn :deep(.q-icon) {
  font-size: 20px;
}

.tenant-drawer-close-btn:hover {
  background: rgba(128, 128, 128, 0.2);
}

.tenant-drawer-body {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 16px;
}

/* 抽屉滑入/滑出动画 */
.tenant-drawer-slide-enter-active,
.tenant-drawer-slide-leave-active {
  transition: opacity 0.25s ease;
}

.tenant-drawer-slide-enter-active .tenant-local-drawer,
.tenant-drawer-slide-leave-active .tenant-local-drawer {
  transition: transform 0.25s ease;
}

.tenant-drawer-slide-enter-from,
.tenant-drawer-slide-leave-to {
  opacity: 0;
}

.tenant-drawer-slide-enter-from .tenant-local-drawer,
.tenant-drawer-slide-leave-to .tenant-local-drawer {
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
.body--dark .tenant-local-drawer {
  background: #1e1e1e !important;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.4);
}

.body--dark .tenant-drawer-header {
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .tenant-drawer-title {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .tenant-drawer-close-btn {
  color: rgba(255, 255, 255, 0.6) !important;
}

.body--dark .tenant-drawer-close-btn:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .tenant-drawer-body {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .tenant-local-drawer-mask {
  background: rgba(0, 0, 0, 0.5);
}
</style>
