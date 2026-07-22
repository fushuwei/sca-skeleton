<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { useQuasar } from "quasar";
import type { QTableColumn } from "quasar";
import { useConfirmDialog } from "@repo/ui";
import ProfileSidebar from "../../components/ProfileSidebar.vue";

const { t } = useI18n({ useScope: "global" });
const $q = useQuasar();
const { confirmDialog } = useConfirmDialog();

type AppStatus = "active" | "revoked" | "expired";

interface AppRow {
  id: string;
  name: string;
  clientId: string;
  clientSecret: string;
  scopes: string[];
  status: AppStatus;
  createTime: string;
}

// ═══════════════════════════════════════════════════════════════
// 本地 Mock 数据（API 就绪后替换为接口调用）
// ═══════════════════════════════════════════════════════════════
const mockApps: AppRow[] = [
  {
    id: "APP001",
    name: "教务管理系统",
    clientId: "APP001_d8f3a2b9c7e1",
    clientSecret: "sk_9f2a7c4e1b8d6a3f5c0e7b2d9a4f1c6e",
    scopes: ["profile", "offline_access"],
    status: "active",
    createTime: "2025-09-10"
  },
  {
    id: "APP002",
    name: "科研管理平台",
    clientId: "APP002_a1b3c5d7e9f0",
    clientSecret: "sk_2b4d6f8a0c2e4b6d8f0a2c4e6b8d0f2a",
    scopes: ["all"],
    status: "active",
    createTime: "2025-11-22"
  },
  {
    id: "APP003",
    name: "数据分析工具",
    clientId: "APP003_f0e2d4c6b8a0",
    clientSecret: "sk_1a3c5e7b9d1f3a5c7e9b1d3f5a7c9e1b",
    scopes: ["profile"],
    status: "revoked",
    createTime: "2025-06-15"
  },
  {
    id: "APP004",
    name: "移动校园 APP",
    clientId: "APP004_b9d7f5a3c1e0",
    clientSecret: "sk_7c9e1b3d5f7a9c1e3b5d7f9a1c3e5b7d",
    scopes: ["profile"],
    status: "expired",
    createTime: "2024-12-03"
  }
];

// ═══════════════════════════════════════════════════════════════
// 搜索条件
// ═══════════════════════════════════════════════════════════════
interface SearchForm {
  keyword: string;
  status: string;
}

const searchForm = reactive<SearchForm>({ keyword: "", status: "" });
const searchExpanded = ref(true);

const statusOptions = computed(() => [
  { label: t("appIntegration.statusActive"), value: "active" },
  { label: t("appIntegration.statusRevoked"), value: "revoked" },
  { label: t("appIntegration.statusExpired"), value: "expired" }
]);

const statusColorMap: Record<string, string> = {
  active: "teal-7",
  revoked: "red-7",
  expired: "orange-7"
};

function statusLabel(status: AppStatus): string {
  const map: Record<AppStatus, string> = {
    active: t("appIntegration.statusActive"),
    revoked: t("appIntegration.statusRevoked"),
    expired: t("appIntegration.statusExpired")
  };
  return map[status];
}

// ═══════════════════════════════════════════════════════════════
// Secret 显示/隐藏
// ═══════════════════════════════════════════════════════════════
const revealedSecrets = ref<Set<string>>(new Set());

function toggleSecret(id: string): void {
  if (revealedSecrets.value.has(id)) {
    revealedSecrets.value.delete(id);
  } else {
    revealedSecrets.value.add(id);
  }
}

// ═══════════════════════════════════════════════════════════════
// 授权范围
// ═══════════════════════════════════════════════════════════════
function scopeColor(scope: string): string {
  if (scope === "all") return "red";
  if (scope === "data:write") return "orange";
  if (scope === "data:read") return "blue";
  return "teal";
}

const scopeOptions = [
  { label: "profile", value: "profile" },
  { label: "offline_access", value: "offline_access" },
  { label: "all", value: "all" },
  { label: "data:read", value: "data:read" },
  { label: "data:write", value: "data:write" }
];

// ═══════════════════════════════════════════════════════════════
// 表格数据
// ═══════════════════════════════════════════════════════════════
const tableRows = ref<AppRow[]>([]);
const tableTotal = ref(0);
const tableLoading = ref(false);
const tablePagination = ref({
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0,
  sortBy: "",
  descending: false
});
const selectedRows = ref<AppRow[]>([]);
const curPage = ref(1);
const jumpToPage = ref<number | null>(null);

const columns = computed<QTableColumn<AppRow>[]>(() => [
  { name: "name", field: "name", label: t("appIntegration.colAppName"), align: "left", sortable: true },
  { name: "clientId", field: "clientId", label: t("appIntegration.colClientId"), align: "left" },
  { name: "clientSecret", field: "clientSecret", label: t("appIntegration.colClientSecret"), align: "left" },
  { name: "scopes", field: "scopes", label: t("appIntegration.colScopes"), align: "left" },
  { name: "status", field: "status", label: t("appIntegration.colStatus"), align: "left", sortable: true },
  { name: "createTime", field: "createTime", label: t("appIntegration.colCreateTime"), align: "left", sortable: true },
  { name: "actions", field: "id", label: t("common.actions"), align: "center", sortable: false }
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
  let rows: AppRow[] = [...mockApps];
  const kw = searchForm.keyword?.trim().toLowerCase();
  if (kw) {
    rows = rows.filter(
      (r) => r.name.toLowerCase().includes(kw) || r.clientId.toLowerCase().includes(kw)
    );
  }
  if (searchForm.status) {
    rows = rows.filter((r) => r.status === searchForm.status);
  }

  // 排序
  const sortBy = tablePagination.value.sortBy;
  if (sortBy) {
    const dir = tablePagination.value.descending ? -1 : 1;
    rows = [...rows].sort((a, b) =>
      compareValues(a[sortBy as keyof AppRow], b[sortBy as keyof AppRow], dir)
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
  searchForm.status = "";
  tablePagination.value.sortBy = "";
  tablePagination.value.descending = false;
  tablePagination.value.page = 1;
  curPage.value = 1;
  loadTableData();
}

// ═══════════════════════════════════════════════════════════════
// 操作
// ═══════════════════════════════════════════════════════════════
function copyToClipboard(text: string): void {
  navigator.clipboard.writeText(text).then(() => {
    $q.notify({ type: "positive", message: t("common.copied"), position: "top" });
  }).catch(() => {
    $q.notify({ type: "positive", message: t("common.copied"), position: "top" });
  });
}

async function revokeApp(row: AppRow): Promise<void> {
  try {
    await confirmDialog({
      title: t("appIntegration.revoke"),
      message: t("appIntegration.revokeConfirm", { name: row.name })
    });
  } catch {
    return;
  }
  row.status = "revoked";
  revealedSecrets.value.delete(row.id);
  selectedRows.value = selectedRows.value.filter((r) => r.id !== row.id);
  $q.notify({ type: "positive", message: t("appIntegration.revokeSuccess"), position: "top" });
}

async function handleBatchRevoke(): Promise<void> {
  if (!selectedRows.value.length) {
    $q.notify({ type: "warning", message: t("appIntegration.selectRowsFirst"), position: "top" });
    return;
  }

  const activeApps = selectedRows.value.filter((r) => r.status === "active");
  if (!activeApps.length) {
    $q.notify({ type: "warning", message: t("appIntegration.selectRowsFirst"), position: "top" });
    return;
  }

  try {
    await confirmDialog({
      title: t("appIntegration.revoke"),
      message: t("appIntegration.batchRevokeConfirm", { count: activeApps.length })
    });
  } catch {
    return;
  }
  activeApps.forEach((app) => {
    const row = tableRows.value.find((r) => r.id === app.id);
    if (row) {
      row.status = "revoked";
      revealedSecrets.value.delete(row.id);
    }
  });
  selectedRows.value = [];
  $q.notify({ type: "positive", message: t("appIntegration.revokeSuccess"), position: "top" });
}

// ═══════════════════════════════════════════════════════════════
// 创建应用对话框
// ═══════════════════════════════════════════════════════════════
const showCreateDialog = ref(false);
const newApp = reactive<{ name: string; scopes: string[] }>({ name: "", scopes: [] });

function openCreateDialog(): void {
  newApp.name = "";
  newApp.scopes = [];
  showCreateDialog.value = true;
}

function createApp(): void {
  if (!newApp.name.trim()) {
    $q.notify({ type: "negative", message: t("appIntegration.appNamePlaceholder"), position: "top" });
    return;
  }
  const seq = String(mockApps.length + 1).padStart(3, "0");
  const app: AppRow = {
    id: `APP${seq}`,
    name: newApp.name.trim(),
    clientId: `APP${seq}_${Math.random().toString(16).slice(2, 14)}`,
    clientSecret: `sk_${Math.random().toString(16).slice(2, 18)}`,
    scopes: newApp.scopes.length ? [...newApp.scopes] : ["profile"],
    status: "active",
    createTime: "2026-07-23"
  };
  mockApps.unshift(app);
  showCreateDialog.value = false;
  $q.notify({ type: "positive", message: t("appIntegration.createSuccess"), position: "top" });
  loadTableData();
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
  <div class="app-integration-page-wrapper">
    <div class="profile-layout">
      <ProfileSidebar />
      <div class="profile-main">
        <div class="app-list-shell">
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
                    :placeholder="t('appIntegration.keywordPlaceholder')"
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
                      <span class="status-placeholder">{{ t('appIntegration.colStatus') }}</span>
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
              <q-btn
                color="primary"
                unelevated
                dense
                no-caps
                class="toolbar-btn"
                @click.stop="openCreateDialog"
              >
                <q-icon name="sym_r_add" size="20px" class="q-mr-xs" />
                {{ t('appIntegration.createApp') }}
              </q-btn>
              <q-btn
                color="white"
                text-color="negative"
                outline
                dense
                no-caps
                class="toolbar-btn"
                :disable="!selectedRows.length"
                @click.stop="handleBatchRevoke"
              >
                <q-icon name="sym_r_block" size="20px" class="q-mr-xs" />
                {{ t('appIntegration.revoke') }}
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
            :class="['app-table', { 'app-table--empty': !tableRows.length }]"
            @request="loadTableData"
          >
            <!-- 应用名称列 -->
            <template #body-cell-name="props">
              <q-td :props="props">
                <span>{{ props.row.name }}</span>
              </q-td>
            </template>

            <!-- Client ID 列 -->
            <template #body-cell-clientId="props">
              <q-td :props="props">
                <span class="mono-text">{{ props.row.clientId }}</span>
              </q-td>
            </template>

            <!-- Client Secret 列 -->
            <template #body-cell-clientSecret="props">
              <q-td :props="props">
                <div class="secret-cell">
                  <span class="mono-text">
                    {{ revealedSecrets.has(props.row.id) ? props.row.clientSecret : t("appIntegration.secretMasked") }}
                  </span>
                  <q-btn
                    flat
                    round
                    dense
                    size="sm"
                    :icon="revealedSecrets.has(props.row.id) ? 'sym_r_visibility_off' : 'sym_r_visibility'"
                    :color="revealedSecrets.has(props.row.id) ? 'teal' : 'grey-6'"
                    @click="toggleSecret(props.row.id)"
                  >
                    <q-tooltip>{{ t("appIntegration.revealSecret") }}</q-tooltip>
                  </q-btn>
                </div>
              </q-td>
            </template>

            <!-- 授权范围列 -->
            <template #body-cell-scopes="props">
              <q-td :props="props">
                <div class="scope-chips">
                  <q-chip
                    v-for="scope in props.row.scopes"
                    :key="scope"
                    dense
                    square
                    :color="scopeColor(scope)"
                    text-color="white"
                    class="scope-chip"
                  >
                    {{ scope }}
                  </q-chip>
                </div>
              </q-td>
            </template>

            <!-- 状态列 -->
            <template #body-cell-status="props">
              <q-td :props="props">
                <q-badge
                  :color="statusColorMap[props.row.status]"
                  :label="statusLabel(props.row.status)"
                  rounded
                  class="app-status-badge"
                />
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
                  color="teal"
                  icon="sym_r_content_copy"
                  @click.stop="copyToClipboard(props.row.clientId)"
                >
                  <q-tooltip>{{ t('appIntegration.copySecret') }}</q-tooltip>
                </q-btn>
                <q-btn
                  v-if="props.row.status === 'active'"
                  flat
                  dense
                  round
                  size="sm"
                  color="negative"
                  icon="sym_r_block"
                  @click.stop="revokeApp(props.row)"
                >
                  <q-tooltip>{{ t('appIntegration.revoke') }}</q-tooltip>
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
    </div>

    <!-- ═══ 创建应用对话框 ═══ -->
    <q-dialog v-model="showCreateDialog">
      <q-card flat class="create-dialog">
        <div class="create-dialog__header">
          <q-icon name="sym_r_add_circle" size="22px" class="create-dialog__icon" />
          <span class="create-dialog__title">{{ t("appIntegration.createDialog") }}</span>
        </div>
        <div class="create-dialog__body">
          <div class="dialog-field">
            <label class="dialog-label">{{ t("appIntegration.appNameLabel") }}</label>
            <q-input
              v-model="newApp.name"
              outlined
              dense
              :placeholder="t('appIntegration.appNamePlaceholder')"
              class="dialog-input"
            />
          </div>
          <div class="dialog-field">
            <label class="dialog-label">{{ t("appIntegration.scopesLabel") }}</label>
            <q-select
              v-model="newApp.scopes"
              :options="scopeOptions"
              emit-value
              map-options
              multiple
              outlined
              dense
              :placeholder="t('appIntegration.scopesPlaceholder')"
              class="dialog-input"
            >
              <template #selected-item="scope">
                <q-chip dense square :color="scopeColor(scope.opt.value)" text-color="white" class="scope-chip">
                  {{ scope.opt.label }}
                </q-chip>
              </template>
            </q-select>
          </div>
        </div>
        <div class="create-dialog__actions">
          <q-btn flat no-caps :label="t('common.cancel')" v-close-popup class="btn-cancel" />
          <q-btn unelevated no-caps :label="t('appIntegration.confirmCreate')" class="btn-primary" @click="createApp" />
        </div>
      </q-card>
    </q-dialog>
  </div>
</template>

<style scoped>
/* ═══ 整体壳层 ═══ */
.app-integration-page-wrapper {
  height: 100%;
  display: flex;
  overflow: hidden;
  padding: 8px 0;
  background: #f5f5f5;
}

.body--dark .app-integration-page-wrapper {
  background: #1a1a1a;
}

/* ═══ 布局 ═══ */
.profile-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 24px;
  width: 100%;
  height: 100%;
  min-height: 0;
}

.profile-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

@media (max-width: 1024px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}

/* ═══ 列表壳层 ═══ */
.app-list-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

/* ── 搜索区域 ── */
.search-area {
  flex-shrink: 0;
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
.app-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.body--dark .app-table {
  background: #1e1e1e;
  border-color: rgba(255, 255, 255, 0.08);
}

.app-table :deep(.q-table__top) {
  display: none;
}

.app-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.app-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.app-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.app-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.app-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.body--dark .app-table :deep(thead tr th) {
  color: rgba(255, 255, 255, 0.8) !important;
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.app-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ── 空数据状态 ── */
.app-table--empty :deep(.q-table__container) {
  height: 100%;
}

.app-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.app-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.app-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

/* 行悬停 */
.app-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.app-table :deep(tbody tr.q-tr--selected td) {
  background: rgba(0, 121, 107, 0.06) !important;
}

.app-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

.body--dark .app-table :deep(tbody td) {
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

/* Badge 统一样式 */
.app-status-badge {
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

/* Secret 单元格 */
.secret-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mono-text {
  font-family: "JetBrains Mono", monospace;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.75);
}

.body--dark .mono-text {
  color: rgba(255, 255, 255, 0.75);
}

/* Scope chips */
.scope-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.scope-chip {
  margin: 0;
}

/* 分页底栏 */
.app-table :deep(.q-table__bottom) {
  padding: 3px 16px 4px;
  font-size: 13px;
  min-height: 42px;
  background: #fff;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.body--dark .app-table :deep(.q-table__bottom) {
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

/* 复选框尺寸 */
.app-table :deep(.q-checkbox__inner) {
  font-size: 32px;
}

/* ═══════════════ 创建应用对话框 ═══════════════ */
.create-dialog {
  width: 480px;
  max-width: 90vw;
  background: #fff !important;
}

.body--dark .create-dialog {
  background: #2a2a2a !important;
}

.create-dialog__header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.body--dark .create-dialog__header {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.create-dialog__icon {
  color: #009688;
}

.body--dark .create-dialog__icon {
  color: #4db6ac;
}

.create-dialog__title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.body--dark .create-dialog__title {
  color: rgba(255, 255, 255, 0.92);
}

.create-dialog__body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.dialog-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dialog-label {
  font-size: 13px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.65);
}

.body--dark .dialog-label {
  color: rgba(255, 255, 255, 0.65);
}

.dialog-input :deep(.q-field__control) {
  background: rgba(0, 0, 0, 0.02);
}

.body--dark .dialog-input :deep(.q-field__control) {
  background: rgba(255, 255, 255, 0.04);
}

.create-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.body--dark .create-dialog__actions {
  border-top-color: rgba(255, 255, 255, 0.08);
}

.btn-primary {
  background: #009688 !important;
  color: #fff !important;
  padding: 0 20px;
  height: 38px;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.btn-primary:hover {
  background: #00796b !important;
}

.body--dark .btn-primary {
  background: #4db6ac !important;
  color: #002b27 !important;
}

.body--dark .btn-primary:hover {
  background: #009688 !important;
  color: #fff !important;
}

.btn-cancel {
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
  font-weight: 500;
  padding: 0 20px;
  height: 38px;
}

.body--dark .btn-cancel {
  color: rgba(255, 255, 255, 0.65);
}
</style>

<!-- 非 scoped：状态选择下拉弹出层 -->
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
