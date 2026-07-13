<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useEscCloseDrawer } from "../../composables/useEscCloseDrawer";
import type { QTableColumn } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysLoginLog, LoginLogPageRequest } from "../../types/auth";
import {
  getLoginLogPageApi,
  batchDeleteLoginLogApi,
  clearAllLoginLogApi
} from "../../apis/login-log";
import { useConfirmDialog } from "@repo/ui";
import { useAuthStore } from "../../stores/auth";
import SearchDateTimePicker from "../../components/SearchDateTimePicker.vue";

const { t } = useI18n({ useScope: "global" });
const { confirmDialog } = useConfirmDialog();
const authStore = useAuthStore();

// 仅超级管理员可批量删除/清空日志
const isSuperAdmin = computed(() => authStore.profile?.isSuperadmin === 1);

// ═══════════════════════════════════════════════════════════════
// 搜索条件
// ═══════════════════════════════════════════════════════════════

const searchForm = reactive<LoginLogPageRequest>({
  pageNum: 1,
  pageSize: 10,
  username: "",
  isSuccess: undefined,
  clientIp: "",
  startTime: "",
  endTime: ""
});

const searchExpanded = ref(true);

// ── 状态选项 ──
const statusOptions = [
  { label: "loginLog.success", value: 1 },
  { label: "loginLog.failed", value: 0 }
];

const statusLabelOf = (isSuccess: number): string =>
  isSuccess === 1 ? t("loginLog.success") : t("loginLog.failed");

const statusColorOf = (isSuccess: number): string =>
  isSuccess === 1 ? "green-7" : "red-7";

// ═══════════════════════════════════════════════════════════════
// 详情抽屉
// ═══════════════════════════════════════════════════════════════

const drawerOpen = ref(false);
useEscCloseDrawer(drawerOpen);
const detailData = ref<SysLoginLog | null>(null);

const drawerTitle = computed(() => t("loginLog.detailTitle"));
const drawerIcon = computed(() => "sym_r_visibility");

function closeDrawer() {
  drawerOpen.value = false;
}

function handleViewDetail(row: SysLoginLog) {
  detailData.value = row;
  drawerOpen.value = true;
}

// ═══════════════════════════════════════════════════════════════
// 表格数据
// ═══════════════════════════════════════════════════════════════

const tableRows = ref<SysLoginLog[]>([]);
const tableTotal = ref(0);
const tableLoading = ref(false);
const tablePagination = ref({
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0,
  sortBy: "",
  descending: false
});
const selectedRows = ref<SysLoginLog[]>([]);
const sortState = ref<{ sortBy: string; descending: boolean }>({ sortBy: "", descending: false });
const jumpToPage = ref<number | null>(null);
const curPage = ref(1);

// ── 表格列定义 ──
const columns = computed<QTableColumn<SysLoginLog>[]>(() => [
  {
    name: "loginTime",
    field: "loginTime",
    label: t("loginLog.loginTime"),
    align: "left",
    sortable: true,
    format: (val: string) => formatDateTime(val)
  },
  {
    name: "username",
    field: "username",
    label: t("loginLog.username"),
    align: "left",
    sortable: true
  },
  {
    name: "realName",
    field: "realName",
    label: t("loginLog.realName"),
    align: "left",
    sortable: false
  },
  {
    name: "clientIp",
    field: "clientIp",
    label: t("loginLog.clientIp"),
    align: "left",
    sortable: true
  },
  {
    name: "location",
    field: "location",
    label: t("loginLog.location"),
    align: "left",
    sortable: false
  },
  {
    name: "device",
    field: "device",
    label: t("loginLog.device"),
    align: "center",
    sortable: true
  },
  {
    name: "browser",
    field: "browser",
    label: t("loginLog.browser"),
    align: "center",
    sortable: true
  },
  {
    name: "os",
    field: "os",
    label: t("loginLog.os"),
    align: "center",
    sortable: true
  },
  {
    name: "isSuccess",
    field: "isSuccess",
    label: t("loginLog.status"),
    align: "center",
    sortable: true
  },
  {
    name: "costMs",
    field: "costMs",
    label: t("loginLog.costMs"),
    align: "center",
    sortable: true,
    format: (val: number) => (val != null ? `${val} ms` : "-")
  },
  {
    name: "actions",
    field: "id",
    label: t("common.actions"),
    align: "center",
    sortable: false
  }
]);

// ── 前端列名 → 后端排序列名映射 ──
const SORT_FIELD_MAP: Record<string, string> = {
  loginTime: "login_time",
  username: "username",
  clientIp: "client_ip",
  isSuccess: "is_success",
  costMs: "cost_ms",
  device: "device",
  browser: "browser",
  os: "os"
};

const visibleColumns = ref(columns.value.map((c) => c.name));

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

  const params: LoginLogPageRequest = {
    pageNum,
    pageSize,
    username: searchForm.username || undefined,
    isSuccess: searchForm.isSuccess,
    clientIp: searchForm.clientIp || undefined,
    startTime: searchForm.startTime || undefined,
    endTime: searchForm.endTime || undefined,
    orderBy,
    orderDirection: sortState.value.sortBy ? (sortState.value.descending ? "desc" : "asc") : undefined
  };

  try {
    const result = await getLoginLogPageApi(params);

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
  searchForm.username = "";
  searchForm.isSuccess = undefined;
  searchForm.clientIp = "";
  searchForm.startTime = "";
  searchForm.endTime = "";
  tablePagination.value.page = 1;
  curPage.value = 1;
  loadTableData();
}

// ═══════════════════════════════════════════════════════════════
// 操作
// ═══════════════════════════════════════════════════════════════

// 批量删除
async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    showToast(t("common.selectRowsFirst"), "warning");
    return;
  }

  try {
    await confirmDialog(t("loginLog.batchDeleteConfirm", { count: selectedRows.value.length }));
  } catch {
    return;
  }

  try {
    const result = await batchDeleteLoginLogApi(selectedRows.value.map((r) => r.id));
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

// 清空全部
async function handleClearAll() {
  try {
    await confirmDialog(t("loginLog.clearAllConfirm"));
  } catch {
    return;
  }

  try {
    const result = await clearAllLoginLogApi();
    if (result.code === 10_000) {
      showToast(t("loginLog.clearAllSuccess"), "positive");
      selectedRows.value = [];
      loadTableData();
    } else {
      showToast(result.message || t("common.operationFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("common.operationFail"), "negative");
    }
  }
}

// ═══════════════════════════════════════════════════════════════
// 辅助函数
// ═══════════════════════════════════════════════════════════════

function formatDateTime(dateStr: string): string {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleString("zh-CN", {
    year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false
  });
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
  <div class="login-log-list-shell">
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
                v-model="searchForm.username"
                filled
                square
                dense
                :placeholder="t('loginLog.username')"
                hide-bottom-space
                clearable
                @keyup.enter="handleSearch"
              />
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.isSuccess"
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
                <template v-if="searchForm.isSuccess === undefined" v-slot:selected>
                  <span class="status-placeholder">{{ t('loginLog.status') }}</span>
                </template>
              </q-select>
            </div>
            <div class="col">
              <q-input
                v-model="searchForm.clientIp"
                filled
                square
                dense
                :placeholder="t('loginLog.clientIp')"
                hide-bottom-space
                clearable
                @keyup.enter="handleSearch"
              />
            </div>
            <div class="col-auto datetime-picker-col">
              <SearchDateTimePicker
                v-model="searchForm.startTime"
                :placeholder="t('loginLog.startTime')"
                :max="searchForm.endTime"
                clearable
              />
            </div>
            <div class="col-auto datetime-picker-col">
              <SearchDateTimePicker
                v-model="searchForm.endTime"
                :placeholder="t('loginLog.endTime')"
                :min="searchForm.startTime"
                clearable
              />
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
            v-if="isSuperAdmin"
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
            {{ t('loginLog.batchDelete') }}
          </q-btn>
          <q-btn
            v-if="isSuperAdmin"
            color="white"
            text-color="negative"
            outline
            dense
            no-caps
            class="toolbar-btn"
            @click.stop="handleClearAll"
          >
            <q-icon name="sym_r_delete_forever" size="20px" class="q-mr-xs" />
            {{ t('loginLog.clearAll') }}
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
        :class="['login-log-table', { 'login-log-table--empty': !tableRows.length }]"
        @request="loadTableData"
      >
        <!-- 登录用户名列 -->
        <template #body-cell-username="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 真实姓名列 -->
        <template #body-cell-realName="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 客户端 IP 列 -->
        <template #body-cell-clientIp="props">
          <q-td :props="props">
            <span v-if="props.value" class="mono-text">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 登录位置列 -->
        <template #body-cell-location="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 设备类型列 -->
        <template #body-cell-device="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              color="blue-grey-6"
              :label="props.value"
              rounded
              class="log-type-badge"
            />
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 浏览器列 -->
        <template #body-cell-browser="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 操作系统列 -->
        <template #body-cell-os="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 状态列 -->
        <template #body-cell-isSuccess="props">
          <q-td :props="props">
            <q-badge
              :color="statusColorOf(props.value)"
              :label="statusLabelOf(props.value)"
              rounded
              class="log-type-badge"
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
              color="info"
              icon="sym_r_visibility"
              @click.stop="handleViewDetail(props.row)"
            >
              <q-tooltip>{{ t("common.view") }}</q-tooltip>
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

  <!-- ═══ 本地右侧抽屉：查看详情 ═══ -->
  <Teleport to="body">
    <Transition name="log-drawer-slide">
      <div v-if="drawerOpen" class="log-local-drawer-mask" @click.self="closeDrawer">
        <div class="log-local-drawer">
          <div class="log-drawer-shell">
            <div class="log-drawer-header row items-center no-wrap">
              <q-icon :name="drawerIcon" size="20px" class="q-mr-sm" />
              <span class="log-drawer-title">{{ drawerTitle }}</span>
              <q-space />
              <q-btn
                flat
                dense
                round
                icon="sym_r_close"
                class="log-drawer-close-btn"
                @click="closeDrawer"
              >
                <q-tooltip>{{ t("common.close") }}</q-tooltip>
              </q-btn>
            </div>
            <div class="log-drawer-body">
              <template v-if="detailData">
                <!-- ── 概览信息卡片 ── -->
                <div class="detail-section">
                  <div class="detail-section-header row items-center no-wrap q-mb-sm">
                    <q-icon name="sym_r_info" size="20px" class="q-mr-xs" color="grey-8" />
                    <span class="detail-section-title">{{ t('loginLog.overviewInfo') }}</span>
                  </div>
                  <div class="detail-grid">
                    <div class="detail-field">
                      <div class="detail-field-label">{{ t("loginLog.username") }}</div>
                      <div class="detail-field-value">{{ detailData.username || "-" }}</div>
                    </div>
                    <div class="detail-field">
                      <div class="detail-field-label">{{ t("loginLog.realName") }}</div>
                      <div class="detail-field-value">{{ detailData.realName || "-" }}</div>
                    </div>
                    <div class="detail-field">
                      <div class="detail-field-label">{{ t("loginLog.status") }}</div>
                      <div class="detail-field-value">
                        <q-badge
                          :color="statusColorOf(detailData.isSuccess)"
                          :label="statusLabelOf(detailData.isSuccess)"
                          rounded
                          class="log-type-badge"
                        />
                      </div>
                    </div>
                    <div class="detail-field">
                      <div class="detail-field-label">{{ t("loginLog.loginTime") }}</div>
                      <div class="detail-field-value">{{ formatDateTime(detailData.loginTime) }}</div>
                    </div>
                    <div class="detail-field">
                      <div class="detail-field-label">{{ t("loginLog.costMs") }}</div>
                      <div class="detail-field-value detail-field-value--mono">
                        {{ detailData.costMs != null ? `${detailData.costMs} ms` : "-" }}
                      </div>
                    </div>
                    <div class="detail-field">
                      <div class="detail-field-label">{{ t("loginLog.clientIp") }}</div>
                      <div class="detail-field-value detail-field-value--mono">{{ detailData.clientIp || "-" }}</div>
                    </div>
                    <div class="detail-field">
                      <div class="detail-field-label">{{ t("loginLog.location") }}</div>
                      <div class="detail-field-value">{{ detailData.location || "-" }}</div>
                    </div>
                    <div class="detail-field detail-field--full">
                      <div class="detail-field-label">{{ t("loginLog.errorMessage") }}</div>
                      <div class="detail-field-value">{{ detailData.errorMessage || "-" }}</div>
                    </div>
                  </div>
                </div>

                <!-- ── 设备信息卡片 ── -->
                <div class="detail-section q-mt-md">
                  <div class="detail-section-header row items-center no-wrap q-mb-sm">
                    <q-icon name="sym_r_devices" size="20px" class="q-mr-xs" color="grey-8" />
                    <span class="detail-section-title">{{ t('loginLog.deviceInfo') }}</span>
                  </div>
                  <div class="detail-grid">
                    <div class="detail-field">
                      <div class="detail-field-label">{{ t("loginLog.device") }}</div>
                      <div class="detail-field-value">
                        <q-badge
                          v-if="detailData.device"
                          color="blue-grey-6"
                          :label="detailData.device"
                          rounded
                          class="log-type-badge"
                        />
                        <span v-else class="text-grey-5">-</span>
                      </div>
                    </div>
                    <div class="detail-field">
                      <div class="detail-field-label">{{ t("loginLog.browser") }}</div>
                      <div class="detail-field-value">{{ detailData.browser || "-" }}</div>
                    </div>
                    <div class="detail-field">
                      <div class="detail-field-label">{{ t("loginLog.os") }}</div>
                      <div class="detail-field-value">{{ detailData.os || "-" }}</div>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ═══ 整体壳层 ═══ */
.login-log-list-shell {
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
  color: rgba(0, 0, 0, 0.72);
}

.status-select :deep(.q-field__control) {
  min-height: 40px;
  min-width: 120px;
}

/* 日期选择器固定宽度，避免 error/clear 图标出现时宽度变化 */
.datetime-picker-col {
  width: 250px;
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
.login-log-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.login-log-table :deep(.q-table__top) {
  display: none;
}

.login-log-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.login-log-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.login-log-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.login-log-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.login-log-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.login-log-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ── 空数据状态 ── */
.login-log-table--empty :deep(.q-table__container) {
  height: 100%;
}

.login-log-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.login-log-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.login-log-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

/* 行悬停 */
.login-log-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.login-log-table :deep(tbody tr.q-tr--selected td) {
  background: rgba(0, 121, 107, 0.06) !important;
}

.login-log-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

/* Badge 统一样式 */
.log-type-badge {
  font-size: 11px;
  padding: 3px 10px;
  font-weight: 500;
}

/* 等宽字体（IP 等） */
.mono-text {
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 12px;
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
.login-log-table :deep(.q-table__bottom) {
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
.login-log-table :deep(.q-checkbox__inner) {
  font-size: 32px;
}

/* ═══ 详情抽屉卡片样式 ═══ */
.detail-section {
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 4px;
  padding: 12px;
}

.detail-section-header {
  height: 24px;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

/* 字段网格布局 */
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 16px;
}

.detail-field {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.detail-field--full {
  grid-column: 1 / -1;
}

.detail-field-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  line-height: 1.5;
}

.detail-field-value {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.87);
  line-height: 1.5;
  word-break: break-all;
}

.detail-field-value--mono {
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 12px;
}

/* 详情卡片暗色模式 */
.body--dark .detail-section {
  border-color: rgba(255, 255, 255, 0.08);
}

.body--dark .detail-section-title {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .detail-section-header .q-icon {
  color: rgba(255, 255, 255, 0.72) !important;
}

.body--dark .detail-field-label {
  color: rgba(255, 255, 255, 0.45);
}

.body--dark .detail-field-value {
  color: rgba(255, 255, 255, 0.87);
}

/* ═══ 本地右侧抽屉 ═══ */
.log-local-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 5000;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: flex-end;
}

.log-local-drawer {
  width: 720px;
  max-width: 100vw;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.log-drawer-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.log-drawer-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 65px;
  padding: 0 16px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.log-drawer-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.log-drawer-close-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  font-size: 20px;
}

.log-drawer-close-btn :deep(.q-btn__wrapper) {
  min-width: 32px;
  min-height: 32px;
  padding: 0;
}

.log-drawer-close-btn :deep(.q-icon) {
  font-size: 20px;
}

.log-drawer-close-btn:hover {
  background: rgba(128, 128, 128, 0.2);
}

.log-drawer-body {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 16px;
}

/* 抽屉滑入/滑出动画 */
.log-drawer-slide-enter-active,
.log-drawer-slide-leave-active {
  transition: opacity 0.25s ease;
}

.log-drawer-slide-enter-active .log-local-drawer,
.log-drawer-slide-leave-active .log-local-drawer {
  transition: transform 0.25s ease;
}

.log-drawer-slide-enter-from,
.log-drawer-slide-leave-to {
  opacity: 0;
}

.log-drawer-slide-enter-from .log-local-drawer,
.log-drawer-slide-leave-to .log-local-drawer {
  transform: translateX(100%);
}
</style>

<!-- 非 scoped：状态下拉弹出层 & 抽屉暗色模式（Teleport to body，无法用 scoped 覆盖） -->
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
.body--dark .log-local-drawer {
  background: #1e1e1e !important;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.4);
}

.body--dark .log-drawer-header {
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .log-drawer-title {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .log-drawer-close-btn {
  color: rgba(255, 255, 255, 0.6) !important;
}

.body--dark .log-drawer-close-btn:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .log-drawer-body {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .log-local-drawer-mask {
  background: rgba(0, 0, 0, 0.5);
}
</style>
