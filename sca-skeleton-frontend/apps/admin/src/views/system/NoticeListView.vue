<script setup lang="ts">
import { ref, reactive, onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useEscCloseDrawer } from "../../composables/useEscCloseDrawer";
import type { QTableColumn } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysNotice, NoticePageRequest } from "../../types/auth";
import {
  getNoticePageApi,
  getNoticeByIdApi,
  updateNoticeStatusApi,
  updateNoticeTopApi,
  deleteNoticeApi,
  batchDeleteNoticeApi
} from "../../apis/notice";
import { useConfirmDialog } from "@repo/ui";
import NoticeDrawerContent from "./NoticeDrawerContent.vue";

const { t } = useI18n({ useScope: "global" });
const { confirmDialog } = useConfirmDialog();

// ═══════════════════════════════════════════════════════════════
// 搜索条件
// ═══════════════════════════════════════════════════════════════

const searchForm = reactive<NoticePageRequest>({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  type: "",
  level: "",
  status: ""
});

const searchExpanded = ref(true);

// ── 状态选项 ──
const statusOptions = [
  { label: "noticeMgmt.statusDraft", value: "draft" },
  { label: "noticeMgmt.statusPublished", value: "published" },
  { label: "noticeMgmt.statusRevoked", value: "revoked" },
  { label: "noticeMgmt.statusArchived", value: "archived" }
];

// ── 类型选项 ──
const typeOptions = [
  { label: "noticeMgmt.typeNotice", value: "notice" },
  { label: "noticeMgmt.typeAnnouncement", value: "announcement" },
  { label: "noticeMgmt.typeSystem", value: "system" },
  { label: "noticeMgmt.typeOther", value: "other" }
];

// ── 级别选项 ──
const levelOptions = [
  { label: "noticeMgmt.levelNormal", value: "normal" },
  { label: "noticeMgmt.levelImportant", value: "important" },
  { label: "noticeMgmt.levelUrgent", value: "urgent" }
];

const statusColorOf = (s: string): string =>
  ({
    draft: "grey-7",
    published: "positive",
    revoked: "orange-7",
    archived: "blue-7"
  }[s] ?? "grey-5");

const statusLabelOf = (s: string): string =>
  ({
    draft: t("noticeMgmt.statusDraft"),
    published: t("noticeMgmt.statusPublished"),
    revoked: t("noticeMgmt.statusRevoked"),
    archived: t("noticeMgmt.statusArchived")
  }[s] ?? s);

const typeLabelOf = (s: string): string =>
  ({
    notice: t("noticeMgmt.typeNotice"),
    announcement: t("noticeMgmt.typeAnnouncement"),
    system: t("noticeMgmt.typeSystem"),
    other: t("noticeMgmt.typeOther")
  }[s] ?? s);

const typeColorOf = (s: string): string =>
  ({
    notice: "blue-6",
    announcement: "teal-6",
    system: "purple-6",
    other: "grey-6"
  }[s] ?? "grey-6");

const levelLabelOf = (s: string): string =>
  ({
    normal: t("noticeMgmt.levelNormal"),
    important: t("noticeMgmt.levelImportant"),
    urgent: t("noticeMgmt.levelUrgent")
  }[s] ?? s);

const levelColorOf = (s: string): string =>
  ({
    normal: "grey-6",
    important: "orange-7",
    urgent: "red-7"
  }[s] ?? "grey-6");

// ═══════════════════════════════════════════════════════════════
// 本地抽屉
// ═══════════════════════════════════════════════════════════════

type DrawerMode = "add" | "edit" | "view";

const drawerOpen = ref(false);
useEscCloseDrawer(drawerOpen);
const drawerMode = ref<DrawerMode>("add");
const drawerNotice = ref<SysNotice | undefined>(undefined);

const drawerTitle = computed(() => {
  if (drawerMode.value === "add") return t("noticeMgmt.addNotice");
  if (drawerMode.value === "edit") return t("noticeMgmt.editNotice");
  return t("noticeMgmt.viewNotice");
});

const drawerIcon = computed(() => {
  if (drawerMode.value === "add") return "sym_r_add";
  if (drawerMode.value === "edit") return "sym_r_edit";
  return "sym_r_visibility";
});

function openNoticeDrawer(mode: DrawerMode, notice?: SysNotice) {
  drawerMode.value = mode;
  drawerNotice.value = notice;
  drawerOpen.value = true;
}

function closeNoticeDrawer() {
  drawerOpen.value = false;
}

function handleDrawerSaved() {
  closeNoticeDrawer();
  loadTableData();
}

// ═══════════════════════════════════════════════════════════════
// 表格数据
// ═══════════════════════════════════════════════════════════════

const tableRows = ref<SysNotice[]>([]);
const tableTotal = ref(0);
const tableLoading = ref(false);
const tablePagination = ref({
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0,
  sortBy: "",
  descending: false
});
const selectedRows = ref<SysNotice[]>([]);
const sortState = ref<{ sortBy: string; descending: boolean }>({ sortBy: "", descending: false });
const jumpToPage = ref<number | null>(null);
const curPage = ref(1);

// ── 表格列定义 ──
const columns = computed<QTableColumn<SysNotice>[]>(() => [
  {
    name: "title",
    field: "title",
    label: t("noticeMgmt.title"),
    align: "left",
    sortable: true,
    classes: "sticky-col-left",
    headerClasses: "sticky-col-left"
  },
  {
    name: "type",
    field: "type",
    label: t("noticeMgmt.type"),
    align: "left",
    sortable: true
  },
  {
    name: "level",
    field: "level",
    label: t("noticeMgmt.level"),
    align: "left",
    sortable: true
  },
  {
    name: "status",
    field: "status",
    label: t("noticeMgmt.status"),
    align: "left",
    sortable: true
  },
  {
    name: "publisherName",
    field: "publisherName",
    label: t("noticeMgmt.publisher"),
    align: "left",
    sortable: false
  },
  {
    name: "publishTime",
    field: "publishTime",
    label: t("noticeMgmt.publishTime"),
    align: "left",
    sortable: true,
    format: (val: string | null) =>
      val ? new Date(val).toLocaleString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false }) : "-"
  },
  {
    name: "isTop",
    field: "isTop",
    label: t("noticeMgmt.isTop"),
    align: "center",
    sortable: true
  },
  {
    name: "readCount",
    field: "readCount",
    label: t("noticeMgmt.readCount"),
    align: "center",
    sortable: true
  },
  {
    name: "createTime",
    field: "createTime",
    label: t("noticeMgmt.createTime"),
    align: "left",
    sortable: true,
    format: (val: string) =>
      val ? new Date(val).toLocaleString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false }) : "-"
  },
  {
    name: "actions",
    field: "id",
    label: t("common.actions"),
    align: "center",
    sortable: false,
    classes: "sticky-col-right",
    headerClasses: "sticky-col-right"
  }
]);

const visibleColumns = ref(columns.value.map((c) => c.name));

// ── 前端列名 → 后端排序列名映射 ──
const SORT_FIELD_MAP: Record<string, string> = {
  title: "title",
  type: "type",
  level: "level",
  status: "status",
  publishTime: "publish_time",
  isTop: "is_top",
  readCount: "read_count",
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

  const params: NoticePageRequest = {
    pageNum,
    pageSize,
    keyword: searchForm.keyword || undefined,
    type: searchForm.type || undefined,
    level: searchForm.level || undefined,
    status: searchForm.status || undefined,
    sortField,
    sortOrder: sortState.value.sortBy ? (sortState.value.descending ? "desc" : "asc") : undefined
  };

  try {
    const result = await getNoticePageApi(params);
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
  searchForm.type = "";
  searchForm.level = "";
  searchForm.status = "";
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
  openNoticeDrawer("add");
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    showToast(t("common.selectRowsFirst"), "warning");
    return;
  }
  try {
    await confirmDialog(t("noticeMgmt.batchDeleteConfirm", { count: selectedRows.value.length }));
  } catch {
    return;
  }
  try {
    const result = await batchDeleteNoticeApi(selectedRows.value.map((r) => r.id));
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

async function handleView(row: SysNotice) {
  const res = await getNoticeByIdApi(row.id);
  if (res.code === 10_000 && res.data) {
    openNoticeDrawer("view", res.data);
  } else {
    showToast(res.message || t("common.loadFail"), "negative");
  }
}

async function handleEdit(row: SysNotice) {
  if (row.status === "published" || row.status === "revoked") {
    showToast(t("noticeMgmt.cannotEditPublished"), "warning");
    return;
  }
  const res = await getNoticeByIdApi(row.id);
  if (res.code === 10_000 && res.data) {
    openNoticeDrawer("edit", res.data);
  } else {
    showToast(res.message || t("common.loadFail"), "negative");
  }
}

async function handleDelete(row: SysNotice) {
  try {
    await confirmDialog(t("noticeMgmt.deleteConfirm", { title: row.title }));
  } catch {
    return;
  }
  try {
    const result = await deleteNoticeApi(row.id);
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

// 发布
async function handlePublish(row: SysNotice) {
  try {
    await confirmDialog(t("noticeMgmt.publishConfirm", { title: row.title }));
  } catch {
    return;
  }
  try {
    const result = await updateNoticeStatusApi({ id: row.id, status: "published" });
    if (result.code === 10_000) {
      showToast(t("noticeMgmt.publishSuccess"), "positive");
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

// 撤回
async function handleRevoke(row: SysNotice) {
  try {
    await confirmDialog(t("noticeMgmt.revokeConfirm", { title: row.title }));
  } catch {
    return;
  }
  try {
    const result = await updateNoticeStatusApi({ id: row.id, status: "revoked" });
    if (result.code === 10_000) {
      showToast(t("noticeMgmt.revokeSuccess"), "positive");
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

// 归档
async function handleArchive(row: SysNotice) {
  try {
    await confirmDialog(t("noticeMgmt.archiveConfirm", { title: row.title }));
  } catch {
    return;
  }
  try {
    const result = await updateNoticeStatusApi({ id: row.id, status: "archived" });
    if (result.code === 10_000) {
      showToast(t("noticeMgmt.archiveSuccess"), "positive");
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

// 置顶
async function handlePinTop(row: SysNotice) {
  try {
    await confirmDialog(t("noticeMgmt.pinTopConfirm", { title: row.title }));
  } catch {
    return;
  }
  try {
    const result = await updateNoticeTopApi({ id: row.id, isTop: 1 });
    if (result.code === 10_000) {
      showToast(t("noticeMgmt.pinTopSuccess"), "positive");
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

// 取消置顶
async function handleUnpinTop(row: SysNotice) {
  try {
    await confirmDialog(t("noticeMgmt.unpinTopConfirm", { title: row.title }));
  } catch {
    return;
  }
  try {
    const result = await updateNoticeTopApi({ id: row.id, isTop: 0 });
    if (result.code === 10_000) {
      showToast(t("noticeMgmt.unpinTopSuccess"), "positive");
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
// 生命周期
// ═══════════════════════════════════════════════════════════════

onMounted(() => {
  loadTableData();
  initialLoadDone = true;
});
</script>

<template>
  <div class="notice-list-shell">
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
                :placeholder="t('noticeMgmt.keywordPlaceholder')"
                hide-bottom-space
                clearable
                @keyup.enter="handleSearch"
              />
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.type"
                filled
                square
                dense
                :options="typeOptions"
                :option-label="(o: { label: string; value: string }) => (o ? t(o.label) : '')"
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
                  <span class="status-placeholder">{{ t('noticeMgmt.typePlaceholder') }}</span>
                </template>
              </q-select>
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.level"
                filled
                square
                dense
                :options="levelOptions"
                :option-label="(o: { label: string; value: string }) => (o ? t(o.label) : '')"
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
                <template v-if="!searchForm.level" v-slot:selected>
                  <span class="status-placeholder">{{ t('noticeMgmt.levelPlaceholder') }}</span>
                </template>
              </q-select>
            </div>
            <div class="col-auto">
              <q-select
                v-model="searchForm.status"
                filled
                square
                dense
                :options="statusOptions"
                :option-label="(o: { label: string; value: string }) => (o ? t(o.label) : '')"
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
                  <span class="status-placeholder">{{ t('noticeMgmt.statusPlaceholder') }}</span>
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
            {{ t('noticeMgmt.createNotice') }}
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
        :class="['notice-table', { 'notice-table--empty': !tableRows.length }]"
        @request="loadTableData"
      >
        <!-- 标题列 -->
        <template #body-cell-title="props">
          <q-td :props="props">
            <div class="row items-center no-wrap">
              <q-icon
                v-if="props.row.isTop === 1"
                name="sym_r_push_pin"
                size="20px"
                color="orange-7"
                class="q-mr-xs"
              />
              <span class="ellipsis-2-lines notice-title-text">{{ props.value }}</span>
            </div>
          </q-td>
        </template>

        <!-- 类型列 -->
        <template #body-cell-type="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              :color="typeColorOf(props.value)"
              :label="typeLabelOf(props.value)"
              rounded
              class="notice-type-badge"
            />
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 级别列 -->
        <template #body-cell-level="props">
          <q-td :props="props">
            <q-badge
              v-if="props.value"
              :color="levelColorOf(props.value)"
              :label="levelLabelOf(props.value)"
              rounded
              class="notice-level-badge"
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
              class="notice-status-badge"
            />
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 发布人列 -->
        <template #body-cell-publisherName="props">
          <q-td :props="props">
            <span v-if="props.value">{{ props.value }}</span>
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 置顶列 -->
        <template #body-cell-isTop="props">
          <q-td :props="props">
            <q-badge
              :color="props.value === 1 ? 'orange-7' : 'grey-6'"
              :label="props.value === 1 ? t('common.yes') : t('common.no')"
              rounded
              class="notice-type-badge"
            />
          </q-td>
        </template>

        <!-- 已读次数列 -->
        <template #body-cell-readCount="props">
          <q-td :props="props">
            <span class="text-weight-medium">{{ props.value || 0 }}</span>
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
              :disable="props.row.status !== 'draft'"
              @click.stop="handleEdit(props.row)"
            >
              <q-tooltip>{{ t("common.edit") }}</q-tooltip>
            </q-btn>
            <q-btn
              flat
              dense
              round
              size="sm"
              color="positive"
              icon="sym_r_campaign"
              :disable="props.row.status !== 'draft'"
              @click.stop="handlePublish(props.row)"
            >
              <q-tooltip>{{ t("noticeMgmt.publish") }}</q-tooltip>
            </q-btn>
            <q-btn
              flat
              dense
              round
              size="sm"
              color="orange"
              icon="sym_r_undo"
              :disable="props.row.status !== 'published'"
              @click.stop="handleRevoke(props.row)"
            >
              <q-tooltip>{{ t("noticeMgmt.revoke") }}</q-tooltip>
            </q-btn>
            <q-btn
              flat
              dense
              round
              size="sm"
              color="blue"
              icon="sym_r_archive"
              :disable="props.row.status !== 'revoked' && props.row.status !== 'published'"
              @click.stop="handleArchive(props.row)"
            >
              <q-tooltip>{{ t("noticeMgmt.archive") }}</q-tooltip>
            </q-btn>
            <q-btn
              v-if="props.row.isTop !== 1"
              flat
              dense
              round
              size="sm"
              color="amber-8"
              icon="sym_r_push_pin"
              :disable="props.row.status === 'draft' || props.row.status === 'archived'"
              @click.stop="handlePinTop(props.row)"
            >
              <q-tooltip>{{ t("noticeMgmt.pinTop") }}</q-tooltip>
            </q-btn>
            <q-btn
              v-if="props.row.isTop === 1"
              flat
              dense
              round
              size="sm"
              color="grey-6"
              icon="sym_r_push_pin"
              @click.stop="handleUnpinTop(props.row)"
            >
              <q-tooltip>{{ t("noticeMgmt.unpinTop") }}</q-tooltip>
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

  <!-- ═══ 本地右侧抽屉：添加 / 编辑 / 查看通知公告 ═══ -->
  <Teleport to="body">
    <Transition name="notice-drawer-slide">
      <div
        v-if="drawerOpen"
        v-mask-close="closeNoticeDrawer"
        class="notice-local-drawer-mask"
      >
        <div class="notice-local-drawer">
          <div class="notice-drawer-shell">
            <div class="notice-drawer-header row items-center no-wrap">
              <q-icon :name="drawerIcon" size="20px" class="q-mr-sm" />
              <span class="notice-drawer-title">{{ drawerTitle }}</span>
              <q-space />
              <q-btn
                flat
                dense
                round
                icon="sym_r_close"
                class="notice-drawer-close-btn"
                @click="closeNoticeDrawer"
              >
                <q-tooltip>{{ t("common.close") }}</q-tooltip>
              </q-btn>
            </div>
            <div class="notice-drawer-body">
              <NoticeDrawerContent
                :mode="drawerMode"
                :notice="drawerNotice"
                @close="closeNoticeDrawer"
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
/* ═══ 整体壳层 — 填满 tab 内容区 ═══ */
.notice-list-shell {
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

.status-select :deep(.q-field__control) {
  min-height: 40px;
  min-width: 140px;
}

.status-select :deep(.q-field__native) {
  color: rgba(0, 0, 0, 0.87);
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
.notice-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.notice-table :deep(.q-table__top) {
  display: none;
}

.notice-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.notice-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.notice-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.notice-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.notice-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.notice-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ═══ 固定列（Sticky Columns）═══ */
/* 复选框列固定宽度（需与下方 .sticky-col-left 的 left 值保持一致）
   计算依据：padding 16px + checkbox 32px + padding 16px = 64px */
.notice-table :deep(th:first-child:not([colspan])),
.notice-table :deep(td:first-child:not([colspan])) {
  width: 64px !important;
  min-width: 64px !important;
}

/* 复选框列（第一列）—— 固定在左侧 */
.notice-table :deep(thead tr th:first-child:not([colspan])) {
  position: sticky;
  left: 0;
  z-index: 1;
}

.notice-table :deep(tbody td:first-child:not([colspan])) {
  position: sticky;
  left: 0;
  z-index: 1;
  background: #fff;
}

/* 标题列 —— 固定在左侧，偏移量 = 复选框列宽度 */
.notice-table :deep(thead tr th.sticky-col-left) {
  position: sticky;
  left: 64px;
  z-index: 1;
}

.notice-table :deep(tbody td.sticky-col-left) {
  position: sticky;
  left: 64px;
  z-index: 1;
  background: #fff;
}

/* 操作列 —— 固定在右侧 */
.notice-table :deep(thead tr th.sticky-col-right) {
  position: sticky;
  right: 0;
  z-index: 1;
}

.notice-table :deep(tbody td.sticky-col-right) {
  position: sticky;
  right: 0;
  z-index: 1;
  background: #fff;
}

/* 固定列分隔阴影 */
.notice-table :deep(thead tr th.sticky-col-left),
.notice-table :deep(tbody td.sticky-col-left) {
  box-shadow: 4px 0 6px -1px rgba(0, 0, 0, 0.12);
}

.notice-table :deep(thead tr th.sticky-col-right),
.notice-table :deep(tbody td.sticky-col-right) {
  box-shadow: -4px 0 6px -1px rgba(0, 0, 0, 0.12);
}

/* 固定列行悬停背景色（使用不透明色，防止横向滚动时内容穿透） */
.notice-table :deep(tbody tr:hover td:first-child:not([colspan])),
.notice-table :deep(tbody tr:hover td.sticky-col-left),
.notice-table :deep(tbody tr:hover td.sticky-col-right) {
  background: #f7fbfb !important;
}

/* 固定列选中行背景色 */
.notice-table :deep(tbody tr.q-tr--selected td:first-child:not([colspan])),
.notice-table :deep(tbody tr.q-tr--selected td.sticky-col-left),
.notice-table :deep(tbody tr.q-tr--selected td.sticky-col-right) {
  background: #f0f6f4 !important;
}

/* 固定列暗色模式 —— 使用不透明深色，防止横向滚动时内容穿透 */
.body--dark .notice-table :deep(tbody td:first-child:not([colspan])),
.body--dark .notice-table :deep(tbody td.sticky-col-left),
.body--dark .notice-table :deep(tbody td.sticky-col-right) {
  background: #1e1e1e !important;
}

.body--dark .notice-table :deep(tbody tr:hover td:first-child:not([colspan])),
.body--dark .notice-table :deep(tbody tr:hover td.sticky-col-left),
.body--dark .notice-table :deep(tbody tr:hover td.sticky-col-right) {
  background: #1d2120 !important;
}

.body--dark .notice-table :deep(tbody tr.q-tr--selected td:first-child:not([colspan])),
.body--dark .notice-table :deep(tbody tr.q-tr--selected td.sticky-col-left),
.body--dark .notice-table :deep(tbody tr.q-tr--selected td.sticky-col-right) {
  background: #1c2323 !important;
}

/* ── 空数据状态 ── */
.notice-table--empty :deep(.q-table__container) {
  height: 100%;
}

.notice-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.notice-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.notice-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

/* 行悬停 */
.notice-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.notice-table :deep(tbody tr.q-tr--selected td) {
  background: rgba(0, 121, 107, 0.06) !important;
}

.notice-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

/* Badge 统一样式 */
.notice-type-badge,
.notice-level-badge,
.notice-status-badge {
  font-size: 11px;
  padding: 3px 10px;
  font-weight: 500;
}

/* 标题文字省略 */
.notice-title-text {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
.notice-table :deep(.q-table__bottom) {
  padding: 3px 16px 4px;
  font-size: 13px;
  min-height: 42px;
  background: #fff;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.table-bottom {
  min-height: 40px;
}

/* 分页器按钮 */
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

/* 每页条数选择器 */
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

/* 跳转至页码输入框 */
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
.notice-table :deep(.q-checkbox__inner) {
  font-size: 32px;
}

/* ═══ 本地右侧抽屉 ═══ */
.notice-local-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 5000;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: flex-end;
}

.notice-local-drawer {
  width: 680px;
  max-width: 100vw;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.notice-drawer-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.notice-drawer-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 65px;
  padding: 0 16px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.notice-drawer-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.notice-drawer-close-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  font-size: 20px;
}

.notice-drawer-close-btn :deep(.q-btn__wrapper) {
  min-width: 32px;
  min-height: 32px;
  padding: 0;
}

.notice-drawer-close-btn :deep(.q-icon) {
  font-size: 20px;
}

.notice-drawer-close-btn:hover {
  background: rgba(128, 128, 128, 0.2);
}

.notice-drawer-body {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

/* 抽屉滑入/滑出动画 */
.notice-drawer-slide-enter-active,
.notice-drawer-slide-leave-active {
  transition: opacity 0.25s ease;
}

.notice-drawer-slide-enter-active .notice-local-drawer,
.notice-drawer-slide-leave-active .notice-local-drawer {
  transition: transform 0.25s ease;
}

.notice-drawer-slide-enter-from,
.notice-drawer-slide-leave-to {
  opacity: 0;
}

.notice-drawer-slide-enter-from .notice-local-drawer,
.notice-drawer-slide-leave-to .notice-local-drawer {
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

/* ═══ 暗色模式 — 抽屉（列表页暗色样式见 admin-layout-dark.scss 的 .notice-list-shell） ═══ */
.body--dark .notice-local-drawer {
  background: #1e1e1e !important;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.4);
}

.body--dark .notice-drawer-header {
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .notice-drawer-title {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .notice-drawer-close-btn {
  color: rgba(255, 255, 255, 0.6) !important;
}

.body--dark .notice-drawer-close-btn:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .notice-drawer-body {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .notice-local-drawer-mask {
  background: rgba(0, 0, 0, 0.5);
}

/* 抽屉内表单深色模式 — 与 UserDrawerContent 一致 */
.body--dark .notice-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .notice-drawer-form .q-field__native,
.body--dark .notice-drawer-form .q-field__prefix,
.body--dark .notice-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .notice-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .notice-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .notice-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .notice-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

/* 抽屉底部按钮区域分隔线 */
.body--dark .notice-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}

/* 抽屉内编辑器深色模式 */
.body--dark .notice-content-editor {
  background: #2d2d2d;
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .notice-content-editor .q-field__control {
  background: #2d2d2d;
}

.body--dark .notice-content-editor .q-field__native {
  color: rgba(255, 255, 255, 0.87);
}
</style>
