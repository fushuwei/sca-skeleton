<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useEscCloseDrawer } from "../../composables/useEscCloseDrawer";
import type { QTableColumn } from "quasar";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysDict, SysDictData, DictPageRequest, DictDataPageRequest } from "../../types/auth";
import {
  getDictPageApi,
  getDictByIdApi,
  deleteDictApi,
  batchDeleteDictApi,
  getDictDataPageApi,
  getDictDataByIdApi,
  deleteDictDataApi,
  batchDeleteDictDataApi
} from "../../apis/dict";
import { useConfirmDialog } from "@repo/ui";
import DictDrawerContent from "./DictDrawerContent.vue";
import DictDataDrawerContent from "./DictDataDrawerContent.vue";

const { t } = useI18n({ useScope: "global" });
const { confirmDialog } = useConfirmDialog();

// ═══════════════════════════════════════════════════════════════
// 左侧：字典列表
// ═══════════════════════════════════════════════════════════════

const dictSearchForm = reactive<DictPageRequest>({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  status: ""
});

const dictSearchExpanded = ref(true);

const dictStatusOptions = [
  { label: "dictMgmt.statusEnabled", value: "enabled" },
  { label: "dictMgmt.statusDisabled", value: "disabled" }
];

const statusColorOf = (s: string): string =>
  ({ enabled: "positive", disabled: "grey-7" }[s] ?? "grey-5");

const statusLabelOf = (s: string): string =>
  ({ enabled: t("dictMgmt.statusEnabled"), disabled: t("dictMgmt.statusDisabled") }[s] ?? s);

const dictRows = ref<SysDict[]>([]);
const dictTotal = ref(0);
const dictLoading = ref(false);
const dictPagination = ref({
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0,
  sortBy: "",
  descending: false
});
const selectedDicts = ref<SysDict[]>([]);
const dictSortState = ref<{ sortBy: string; descending: boolean }>({ sortBy: "", descending: false });
const dictJumpToPage = ref<number | null>(null);
const dictCurPage = ref(1);
const selectedDict = ref<SysDict | null>(null);

const dictColumns = computed<QTableColumn<SysDict>[]>(() => [
  { name: "name", field: "name", label: t("dictMgmt.name"), align: "left", sortable: true },
  { name: "code", field: "code", label: t("dictMgmt.code"), align: "left", sortable: true },
  { name: "status", field: "status", label: t("dictMgmt.status"), align: "left", sortable: true },
  { name: "actions", field: "id", label: t("common.actions"), align: "center", sortable: false }
]);

const dictVisibleColumns = ref(dictColumns.value.map((c) => c.name));

const DICT_SORT_FIELD_MAP: Record<string, string> = {
  name: "name", code: "code", status: "status"
};

let dictInitialLoadDone = false;
let dictLoadRequestId = 0;

async function loadDictData(props?: { pagination: { page: number; rowsPerPage: number; rowsNumber?: number; sortBy?: string; descending?: boolean } }) {
  if (props && !dictInitialLoadDone) return;
  const requestId = ++dictLoadRequestId;
  dictLoading.value = true;
  const pageSize = Number(props?.pagination?.rowsPerPage ?? dictPagination.value.rowsPerPage) || 10;
  const pageNum = dictCurPage.value || 1;
  if (props?.pagination) {
    dictSortState.value.sortBy = props.pagination.sortBy ?? "";
    dictSortState.value.descending = props.pagination.descending ?? false;
    dictPagination.value.sortBy = props.pagination.sortBy ?? "";
    dictPagination.value.descending = props.pagination.descending ?? false;
  }
  const sortBy = dictSortState.value.sortBy || undefined;
  const sortField = sortBy ? (DICT_SORT_FIELD_MAP[sortBy] ?? sortBy) : undefined;
  const params: DictPageRequest = {
    pageNum, pageSize,
    keyword: dictSearchForm.keyword || undefined,
    status: dictSearchForm.status || undefined,
    sortField,
    sortOrder: dictSortState.value.sortBy ? (dictSortState.value.descending ? "desc" : "asc") : undefined
  };
  try {
    const result = await getDictPageApi(params);
    if (requestId !== dictLoadRequestId) return;
    if (result.code === 10_000) {
      dictRows.value = result.data.records ?? [];
      dictTotal.value = Number(result.data.total) || 0;
      dictPagination.value.page = Number(result.data.current) || pageNum;
      dictPagination.value.rowsPerPage = Number(result.data.size) || pageSize;
      dictPagination.value.rowsNumber = Number(result.data.total) || 0;
      dictCurPage.value = Number(result.data.current) || pageNum;
    } else {
      showToast(result.message || t("common.loadFail"), "negative");
    }
  } catch (error) {
    if (requestId !== dictLoadRequestId) return;
    if (!isNotificationHandled(error)) showToast(t("common.loadFail"), "negative");
  } finally {
    if (requestId === dictLoadRequestId) dictLoading.value = false;
  }
}

function handleDictSearch() {
  dictPagination.value.page = 1;
  dictCurPage.value = 1;
  loadDictData();
}

function onDictPageChange(page: number) {
  dictCurPage.value = Number(page);
  dictPagination.value.page = Number(page);
  loadDictData();
}

function handleDictJumpToPage() {
  const page = Number(dictJumpToPage.value);
  const maxPage = Math.ceil(dictTotal.value / dictPagination.value.rowsPerPage);
  if (page && page >= 1 && page <= maxPage) {
    dictCurPage.value = page;
    dictPagination.value.page = page;
    loadDictData();
  }
  dictJumpToPage.value = null;
}

function handleDictReset() {
  dictSearchForm.keyword = "";
  dictSearchForm.status = "";
  dictSortState.value.sortBy = "";
  dictSortState.value.descending = false;
  dictPagination.value.sortBy = "";
  dictPagination.value.descending = false;
  dictPagination.value.page = 1;
  dictCurPage.value = 1;
  loadDictData();
}

function handleDictRowClick(_evt: Event, row: SysDict) {
  selectedDict.value = row;
}

/** 用于 q-table 的 row-attr，给选中行添加 class 使整行高亮 */
function dictRowAttr(row: SysDict) {
  return { class: selectedDict.value?.id === row.id ? "dict-row--selected" : "" };
}

// ═══════════════════════════════════════════════════════════════
// 右侧：字典数据列表
// ═══════════════════════════════════════════════════════════════

const dictDataSearchForm = reactive<DictDataPageRequest>({
  pageNum: 1, pageSize: 10, dictId: "", keyword: "", status: ""
});

const dictDataSearchExpanded = ref(true);

const dictDataRows = ref<SysDictData[]>([]);
const dictDataTotal = ref(0);
const dictDataLoading = ref(false);
const dictDataPagination = ref({
  page: 1, rowsPerPage: 10, rowsNumber: 0, sortBy: "", descending: false
});
const selectedDictData = ref<SysDictData[]>([]);
const dictDataSortState = ref<{ sortBy: string; descending: boolean }>({ sortBy: "", descending: false });
const dictDataJumpToPage = ref<number | null>(null);
const dictDataCurPage = ref(1);

const dictDataColumns = computed<QTableColumn<SysDictData>[]>(() => [
  { name: "label", field: "label", label: t("dictDataMgmt.label"), align: "left", sortable: true },
  { name: "value", field: "value", label: t("dictDataMgmt.value"), align: "left", sortable: true },
  { name: "sort", field: "sort", label: t("dictDataMgmt.sort"), align: "left", sortable: true },
  { name: "status", field: "status", label: t("dictDataMgmt.status"), align: "left", sortable: true },
  { name: "actions", field: "id", label: t("common.actions"), align: "center", sortable: false }
]);

const dictDataVisibleColumns = ref(dictDataColumns.value.map((c) => c.name));

const DICT_DATA_SORT_FIELD_MAP: Record<string, string> = {
  label: "label", value: "value", sort: "sort", status: "status"
};

let dictDataInitialLoadDone = false;
let dictDataLoadRequestId = 0;

async function loadDictDataList(props?: { pagination: { page: number; rowsPerPage: number; rowsNumber?: number; sortBy?: string; descending?: boolean } }) {
  if (!selectedDict.value) {
    dictDataRows.value = [];
    dictDataTotal.value = 0;
    dictDataPagination.value.rowsNumber = 0;
    return;
  }
  if (props && !dictDataInitialLoadDone) return;
  const requestId = ++dictDataLoadRequestId;
  dictDataLoading.value = true;
  const pageSize = Number(props?.pagination?.rowsPerPage ?? dictDataPagination.value.rowsPerPage) || 10;
  const pageNum = dictDataCurPage.value || 1;
  if (props?.pagination) {
    dictDataSortState.value.sortBy = props.pagination.sortBy ?? "";
    dictDataSortState.value.descending = props.pagination.descending ?? false;
    dictDataPagination.value.sortBy = props.pagination.sortBy ?? "";
    dictDataPagination.value.descending = props.pagination.descending ?? false;
  }
  const sortBy = dictDataSortState.value.sortBy || undefined;
  const sortField = sortBy ? (DICT_DATA_SORT_FIELD_MAP[sortBy] ?? sortBy) : undefined;
  const params: DictDataPageRequest = {
    pageNum, pageSize,
    dictId: selectedDict.value.id,
    keyword: dictDataSearchForm.keyword || undefined,
    status: dictDataSearchForm.status || undefined,
    sortField,
    sortOrder: dictDataSortState.value.sortBy ? (dictDataSortState.value.descending ? "desc" : "asc") : undefined
  };
  try {
    const result = await getDictDataPageApi(params);
    if (requestId !== dictDataLoadRequestId) return;
    if (result.code === 10_000) {
      dictDataRows.value = result.data.records ?? [];
      dictDataTotal.value = Number(result.data.total) || 0;
      dictDataPagination.value.page = Number(result.data.current) || pageNum;
      dictDataPagination.value.rowsPerPage = Number(result.data.size) || pageSize;
      dictDataPagination.value.rowsNumber = Number(result.data.total) || 0;
      dictDataCurPage.value = Number(result.data.current) || pageNum;
    } else {
      showToast(result.message || t("common.loadFail"), "negative");
    }
  } catch (error) {
    if (requestId !== dictDataLoadRequestId) return;
    if (!isNotificationHandled(error)) showToast(t("common.loadFail"), "negative");
  } finally {
    if (requestId === dictDataLoadRequestId) dictDataLoading.value = false;
  }
}

function handleDictDataSearch() {
  dictDataPagination.value.page = 1;
  dictDataCurPage.value = 1;
  loadDictDataList();
}

function onDictDataPageChange(page: number) {
  dictDataCurPage.value = Number(page);
  dictDataPagination.value.page = Number(page);
  loadDictDataList();
}

function handleDictDataJumpToPage() {
  const page = Number(dictDataJumpToPage.value);
  const maxPage = Math.ceil(dictDataTotal.value / dictDataPagination.value.rowsPerPage);
  if (page && page >= 1 && page <= maxPage) {
    dictDataCurPage.value = page;
    dictDataPagination.value.page = page;
    loadDictDataList();
  }
  dictDataJumpToPage.value = null;
}

function handleDictDataReset() {
  dictDataSearchForm.keyword = "";
  dictDataSearchForm.status = "";
  dictDataSortState.value.sortBy = "";
  dictDataSortState.value.descending = false;
  dictDataPagination.value.sortBy = "";
  dictDataPagination.value.descending = false;
  dictDataPagination.value.page = 1;
  dictDataCurPage.value = 1;
  loadDictDataList();
}

watch(() => selectedDict.value, (dict) => {
  if (dict) {
    dictDataSearchForm.dictId = dict.id;
    dictDataPagination.value.page = 1;
    dictDataCurPage.value = 1;
    dictDataSearchForm.keyword = "";
    dictDataSearchForm.status = "";
    loadDictDataList();
  } else {
    dictDataRows.value = [];
    dictDataTotal.value = 0;
    selectedDictData.value = [];
    dictDataPagination.value.rowsNumber = 0;
  }
});

// ═══════════════════════════════════════════════════════════════
// 抽屉 — 字典 添加 / 编辑 / 查看
// ═══════════════════════════════════════════════════════════════

type DrawerMode = "add" | "edit" | "view";

const dictDrawerOpen = ref(false);
useEscCloseDrawer(dictDrawerOpen);
const dictDrawerMode = ref<DrawerMode>("add");
const dictDrawerData = ref<SysDict | undefined>(undefined);

const dictDrawerTitle = computed(() => {
  if (dictDrawerMode.value === "add") return t("dictMgmt.addDict");
  if (dictDrawerMode.value === "edit") return t("dictMgmt.editDict");
  return t("dictMgmt.viewDict");
});

const dictDrawerIcon = computed(() => {
  if (dictDrawerMode.value === "add") return "sym_r_add";
  if (dictDrawerMode.value === "edit") return "sym_r_edit";
  return "sym_r_visibility";
});

function openDictDrawer(mode: DrawerMode, dict?: SysDict) {
  dictDrawerMode.value = mode;
  dictDrawerData.value = dict;
  dictDrawerOpen.value = true;
}

function closeDictDrawer() { dictDrawerOpen.value = false; }

function handleDictDrawerSaved() {
  closeDictDrawer();
  loadDictData();
}

// ═══════════════════════════════════════════════════════════════
// 抽屉 — 字典数据 添加 / 编辑 / 查看
// ═══════════════════════════════════════════════════════════════

const dictDataDrawerOpen = ref(false);
useEscCloseDrawer(dictDataDrawerOpen);
const dictDataDrawerMode = ref<DrawerMode>("add");
const dictDataDrawerData = ref<SysDictData | undefined>(undefined);

const dictDataDrawerTitle = computed(() => {
  if (dictDataDrawerMode.value === "add") return t("dictDataMgmt.addDictData");
  if (dictDataDrawerMode.value === "edit") return t("dictDataMgmt.editDictData");
  return t("dictDataMgmt.viewDictData");
});

const dictDataDrawerIcon = computed(() => {
  if (dictDataDrawerMode.value === "add") return "sym_r_add";
  if (dictDataDrawerMode.value === "edit") return "sym_r_edit";
  return "sym_r_visibility";
});

function openDictDataDrawer(mode: DrawerMode, dictData?: SysDictData) {
  dictDataDrawerMode.value = mode;
  dictDataDrawerData.value = dictData;
  dictDataDrawerOpen.value = true;
}

function closeDictDataDrawer() { dictDataDrawerOpen.value = false; }

function handleDictDataDrawerSaved() {
  closeDictDataDrawer();
  loadDictDataList();
}

// ═══════════════════════════════════════════════════════════════
// 字典操作
// ═══════════════════════════════════════════════════════════════

function handleCreateDict() { openDictDrawer("add"); }

async function handleBatchDeleteDict() {
  if (!selectedDicts.value.length) { showToast(t("common.selectRowsFirst"), "warning"); return; }
  try { await confirmDialog(t("dictMgmt.batchDeleteConfirm", { count: selectedDicts.value.length })); } catch { return; }
  try {
    const result = await batchDeleteDictApi(selectedDicts.value.map((r) => r.id));
    if (result.code === 10_000) {
      showToast(t("common.deleteSuccess"), "positive");
      if (selectedDict.value && !selectedDicts.value.find((d) => d.id === selectedDict.value?.id)) {
        // 当前选中的字典不在删除列表中，保持选中
      } else {
        selectedDict.value = null;
      }
      selectedDicts.value = [];
      loadDictData();
    } else { showToast(result.message || t("common.deleteFail"), "negative"); }
  } catch (error) { if (!isNotificationHandled(error)) showToast(t("common.deleteFail"), "negative"); }
}

async function handleToggleDictStatus(dict: SysDict) {
  const newStatus = dict.status === "enabled" ? "disabled" : "enabled";
  try {
    const res = await getDictByIdApi(dict.id);
    if (res.code !== 10_000 || !res.data) {
      showToast(res.message || t("common.loadFail"), "negative");
      return;
    }
    const result = await updateDictApi({
      id: dict.id,
      name: res.data.name,
      code: res.data.code,
      status: newStatus,
      remark: res.data.remark || undefined,
      version: res.data.version
    });
    if (result.code === 10_000) {
      showToast(newStatus === "enabled" ? t("common.enable") + t("common.operationSuccess") : t("common.disable") + t("common.operationSuccess"), "positive");
      loadDictData();
      if (selectedDict.value?.id === dict.id) {
        selectedDict.value = { ...selectedDict.value, status: newStatus };
      }
    } else {
      showToast(result.message || t("common.operationFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) showToast(t("common.operationFail"), "negative");
  }
}

async function handleEditDict(dict: SysDict) {
  const res = await getDictByIdApi(dict.id);
  if (res.code === 10_000 && res.data) { openDictDrawer("edit", res.data); }
  else { showToast(res.message || t("common.loadFail"), "negative"); }
}

async function handleDeleteDict(dict: SysDict) {
  try { await confirmDialog(t("dictMgmt.deleteConfirm", { name: dict.name })); } catch { return; }
  try {
    const result = await deleteDictApi(dict.id);
    if (result.code === 10_000) {
      showToast(t("common.deleteSuccess"), "positive");
      if (selectedDict.value?.id === dict.id) selectedDict.value = null;
      loadDictData();
    } else { showToast(result.message || t("common.deleteFail"), "negative"); }
  } catch (error) { if (!isNotificationHandled(error)) showToast(t("common.deleteFail"), "negative"); }
}

// ═══════════════════════════════════════════════════════════════
// 字典数据操作
// ═══════════════════════════════════════════════════════════════

function handleCreateDictData() {
  if (!selectedDict.value) { showToast(t("dictDataMgmt.selectDictFirst"), "warning"); return; }
  openDictDataDrawer("add");
}

async function handleBatchDeleteDictData() {
  if (!selectedDictData.value.length) { showToast(t("common.selectRowsFirst"), "warning"); return; }
  try { await confirmDialog(t("dictDataMgmt.batchDeleteConfirm", { count: selectedDictData.value.length })); } catch { return; }
  try {
    const result = await batchDeleteDictDataApi(selectedDictData.value.map((r) => r.id));
    if (result.code === 10_000) {
      showToast(t("common.deleteSuccess"), "positive");
      selectedDictData.value = [];
      loadDictDataList();
    } else { showToast(result.message || t("common.deleteFail"), "negative"); }
  } catch (error) { if (!isNotificationHandled(error)) showToast(t("common.deleteFail"), "negative"); }
}

async function handleToggleDictDataStatus(dictData: SysDictData) {
  const newStatus = dictData.status === "enabled" ? "disabled" : "enabled";
  try {
    const res = await getDictDataByIdApi(dictData.id);
    if (res.code !== 10_000 || !res.data) {
      showToast(res.message || t("common.loadFail"), "negative");
      return;
    }
    const result = await updateDictDataApi({
      id: dictData.id,
      dictId: res.data.dictId,
      label: res.data.label,
      value: res.data.value,
      status: newStatus,
      sort: res.data.sort,
      remark: res.data.remark || undefined
    });
    if (result.code === 10_000) {
      showToast(newStatus === "enabled" ? t("common.enable") + t("common.operationSuccess") : t("common.disable") + t("common.operationSuccess"), "positive");
      loadDictDataList();
    } else {
      showToast(result.message || t("common.operationFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) showToast(t("common.operationFail"), "negative");
  }
}

async function handleEditDictData(dictData: SysDictData) {
  const res = await getDictDataByIdApi(dictData.id);
  if (res.code === 10_000 && res.data) { openDictDataDrawer("edit", res.data); }
  else { showToast(res.message || t("common.loadFail"), "negative"); }
}

async function handleDeleteDictData(dictData: SysDictData) {
  try { await confirmDialog(t("dictDataMgmt.deleteConfirm", { label: dictData.label })); } catch { return; }
  try {
    const result = await deleteDictDataApi(dictData.id);
    if (result.code === 10_000) {
      showToast(t("common.deleteSuccess"), "positive");
      loadDictDataList();
    } else { showToast(result.message || t("common.deleteFail"), "negative"); }
  } catch (error) { if (!isNotificationHandled(error)) showToast(t("common.deleteFail"), "negative"); }
}

// ═══════════════════════════════════════════════════════════════
// 生命周期
// ═══════════════════════════════════════════════════════════════

onMounted(() => {
  loadDictData();
  dictInitialLoadDone = true;
});

watch(() => selectedDict.value, (val) => {
  if (val) dictDataInitialLoadDone = true;
}, { immediate: true });
</script>

<template>
  <div class="dict-list-shell">
    <!-- ═══ 左侧：字典列表 ═══ -->
    <div class="left-panel">
      <!-- ── 搜索区域 ── -->
      <div class="search-area">
        <div class="search-area-header row items-center no-wrap">
          <div class="row items-center no-wrap cursor-pointer" @click="dictSearchExpanded = !dictSearchExpanded">
            <q-icon name="sym_r_search" size="20px" class="q-mr-xs text-grey-8" />
            <span class="search-area-title">{{ t("common.searchCondition") }}</span>
          </div>
          <q-space />
          <q-btn
            flat
            dense
            round
            size="20px"
            :icon="dictSearchExpanded ? 'sym_r_expand_less' : 'sym_r_expand_more'"
            class="search-collapse-btn"
            @click="dictSearchExpanded = !dictSearchExpanded"
          >
            <q-tooltip style="white-space: nowrap">{{
              dictSearchExpanded ? t("common.collapseSearch") : t("common.expandSearch")
            }}</q-tooltip>
          </q-btn>
        </div>

        <div v-show="dictSearchExpanded" class="search-area-body">
          <div class="row q-col-gutter-sm items-end">
            <div class="col">
              <q-input
                v-model="dictSearchForm.keyword"
                filled
                square
                dense
                :placeholder="t('dictMgmt.keywordPlaceholder')"
                hide-bottom-space
                clearable
                @keyup.enter="handleDictSearch"
              />
            </div>
            <div class="col-auto">
              <q-select
                v-model="dictSearchForm.status"
                filled
                square
                dense
                :options="dictStatusOptions"
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
                <template v-if="!dictSearchForm.status" v-slot:selected>
                  <span class="status-placeholder">{{ t('dictMgmt.statusPlaceholder') }}</span>
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
                  @click="handleDictSearch"
                >
                  <q-icon name="sym_r_search" size="20px" class="q-mr-xs" />
                  {{ t("common.search") }}
                </q-btn>
                <q-btn
                  color="grey-7"
                  outline
                  no-caps
                  class="search-btn"
                  @click="handleDictReset"
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
            @click.stop="handleCreateDict"
          >
            <q-icon name="sym_r_add" size="20px" class="q-mr-xs" />
            {{ t('dictMgmt.createDict') }}
          </q-btn>
          <q-btn
            color="white"
            text-color="negative"
            outline
            dense
            no-caps
            class="toolbar-btn"
            :disable="!selectedDicts.length"
            @click.stop="handleBatchDeleteDict"
          >
            <q-icon name="sym_r_delete" size="20px" class="q-mr-xs" />
            {{ t('common.batchDelete') }}
          </q-btn>
        </div>
        <q-space />
      </div>

      <!-- ── 字典表格 ── -->
      <q-table
        v-model:selected="selectedDicts"
        v-model:pagination="dictPagination"
        :rows="dictRows"
        :columns="dictColumns"
        :visible-columns="dictVisibleColumns"
        row-key="id"
        :loading="dictLoading"
        :rows-per-page-options="[10, 20, 50, 100]"
        selection="multiple"
        flat
        :row-attr="dictRowAttr"
        :class="['dict-table', { 'dict-table--empty': !dictRows.length }]"
        @request="loadDictData"
        @row-click="handleDictRowClick"
      >
        <!-- 字典名称列 -->
        <template #body-cell-name="props">
          <q-td :props="props">
            <span>{{ props.row.name }}</span>
          </q-td>
        </template>

        <!-- 字典编码列 -->
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
              class="dict-type-badge"
            />
            <span v-else class="text-grey-5">-</span>
          </q-td>
        </template>

        <!-- 操作列 -->
        <template #body-cell-actions="props">
          <q-td :props="props" class="q-gutter-x-xs actions-cell" @click.stop>
            <q-btn
              flat
              dense
              round
              size="sm"
              :color="props.row.status === 'enabled' ? 'orange-7' : 'green-7'"
              :icon="props.row.status === 'enabled' ? 'sym_r_block' : 'sym_r_check_circle'"
              @click.stop="handleToggleDictStatus(props.row)"
            >
              <q-tooltip>{{ props.row.status === 'enabled' ? t('common.disable') : t('common.enable') }}</q-tooltip>
            </q-btn>
            <q-btn
              flat
              dense
              round
              size="sm"
              color="primary"
              icon="sym_r_edit"
              @click.stop="handleEditDict(props.row)"
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
              @click.stop="handleDeleteDict(props.row)"
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
              {{ t("common.totalRows", { count: dictTotal }) }}<template v-if="selectedDicts.length">，{{ t("common.selectedRows", { count: selectedDicts.length }) }}</template>
            </span>
            <q-space />
            <q-pagination
              v-model="dictCurPage"
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
              @update:model-value="onDictPageChange"
            />
            <span class="text-caption text-grey-7 q-ml-md q-mr-sm">{{ t("common.rowsPerPageLabel") }}</span>
            <q-select
              v-model="dictPagination.rowsPerPage"
              :options="[10, 20, 50, 100]"
              dense
              flat
              borderless
              class="rows-per-page-select"
              popup-content-class="rows-per-page-popup"
              @update:model-value="handleDictSearch"
            >
              <template #append>
                <span class="text-caption">{{ t("common.rowsPerPageUnit") }}</span>
              </template>
            </q-select>
            <span class="text-caption text-grey-7 q-ml-md">{{ t("common.jumpToLabel") }}</span>
            <q-input
              v-model.number="dictJumpToPage"
              dense
              flat
              borderless
              class="jump-to-page-input"
              input-class="text-center"
              :placeholder="String((props.pagesNumber || 1) <= 1 ? 1 : (dictCurPage >= (props.pagesNumber || 1) ? 1 : dictCurPage + 1))"
              @keyup.enter="handleDictJumpToPage"
            />
            <span class="text-caption text-grey-7">{{ t("common.jumpToUnit") }}</span>
          </div>
        </template>
      </q-table>
    </div>

    <!-- ═══ 右侧：字典数据列表 ═══ -->
    <div class="right-panel" :class="{ 'right-panel--no-border': selectedDict }">
      <!-- ── 右侧内容区 ── -->
      <template v-if="selectedDict">
        <!-- ── 搜索区域 ── -->
        <div class="search-area">
          <div class="search-area-header row items-center no-wrap">
            <div class="row items-center no-wrap cursor-pointer" @click="dictDataSearchExpanded = !dictDataSearchExpanded">
              <q-icon name="sym_r_search" size="20px" class="q-mr-xs text-grey-8" />
              <span class="search-area-title">{{ t("common.searchCondition") }}</span>
            </div>
            <q-space />
            <q-btn
              flat
              dense
              round
              size="20px"
              :icon="dictDataSearchExpanded ? 'sym_r_expand_less' : 'sym_r_expand_more'"
              class="search-collapse-btn"
              @click="dictDataSearchExpanded = !dictDataSearchExpanded"
            >
              <q-tooltip style="white-space: nowrap">{{
                dictDataSearchExpanded ? t("common.collapseSearch") : t("common.expandSearch")
              }}</q-tooltip>
            </q-btn>
          </div>

          <div v-show="dictDataSearchExpanded" class="search-area-body">
            <div class="row q-col-gutter-sm items-end">
              <div class="col">
                <q-input
                  v-model="dictDataSearchForm.keyword"
                  filled
                  square
                  dense
                  :placeholder="t('dictDataMgmt.keywordPlaceholder')"
                  hide-bottom-space
                  clearable
                  @keyup.enter="handleDictDataSearch"
                />
              </div>
              <div class="col-auto">
                <q-select
                  v-model="dictDataSearchForm.status"
                  filled
                  square
                  dense
                  :options="dictStatusOptions"
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
                  <template v-if="!dictDataSearchForm.status" v-slot:selected>
                    <span class="status-placeholder">{{ t('dictDataMgmt.statusPlaceholder') }}</span>
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
                    @click="handleDictDataSearch"
                  >
                    <q-icon name="sym_r_search" size="20px" class="q-mr-xs" />
                    {{ t("common.search") }}
                  </q-btn>
                  <q-btn
                    color="grey-7"
                    outline
                    no-caps
                    class="search-btn"
                    @click="handleDictDataReset"
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
              @click.stop="handleCreateDictData"
            >
              <q-icon name="sym_r_add" size="20px" class="q-mr-xs" />
              {{ t('dictDataMgmt.createDictData') }}
            </q-btn>
            <q-btn
              color="white"
              text-color="negative"
              outline
              dense
              no-caps
              class="toolbar-btn"
              :disable="!selectedDictData.length"
              @click.stop="handleBatchDeleteDictData"
            >
              <q-icon name="sym_r_delete" size="20px" class="q-mr-xs" />
              {{ t('common.batchDelete') }}
            </q-btn>
          </div>
          <q-space />
        </div>

        <!-- ── 字典数据表格 ── -->
        <q-table
          v-model:selected="selectedDictData"
          v-model:pagination="dictDataPagination"
          :rows="dictDataRows"
          :columns="dictDataColumns"
          :visible-columns="dictDataVisibleColumns"
          row-key="id"
          :loading="dictDataLoading"
          :rows-per-page-options="[10, 20, 50, 100]"
          selection="multiple"
          flat
          :class="['dict-data-table', { 'dict-data-table--empty': !dictDataRows.length }]"
          @request="loadDictDataList"
        >
          <!-- 字典标签列 -->
          <template #body-cell-label="props">
            <q-td :props="props">
              <span>{{ props.row.label }}</span>
            </q-td>
          </template>

          <!-- 字典值列 -->
          <template #body-cell-value="props">
            <q-td :props="props">
              <span v-if="props.value">{{ props.value }}</span>
              <span v-else class="text-grey-5">-</span>
            </q-td>
          </template>

          <!-- 排序列 -->
          <template #body-cell-sort="props">
            <q-td :props="props">
              <span>{{ props.value }}</span>
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
                class="dict-type-badge"
              />
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
                :color="props.row.status === 'enabled' ? 'orange-7' : 'green-7'"
                :icon="props.row.status === 'enabled' ? 'sym_r_block' : 'sym_r_check_circle'"
                @click.stop="handleToggleDictDataStatus(props.row)"
              >
                <q-tooltip>{{ props.row.status === 'enabled' ? t('common.disable') : t('common.enable') }}</q-tooltip>
              </q-btn>
              <q-btn
                flat
                dense
                round
                size="sm"
                color="primary"
                icon="sym_r_edit"
                @click.stop="handleEditDictData(props.row)"
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
                @click.stop="handleDeleteDictData(props.row)"
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
                {{ t("common.totalRows", { count: dictDataTotal }) }}<template v-if="selectedDictData.length">，{{ t("common.selectedRows", { count: selectedDictData.length }) }}</template>
              </span>
              <q-space />
              <q-pagination
                v-model="dictDataCurPage"
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
                @update:model-value="onDictDataPageChange"
              />
              <span class="text-caption text-grey-7 q-ml-md q-mr-sm">{{ t("common.rowsPerPageLabel") }}</span>
              <q-select
                v-model="dictDataPagination.rowsPerPage"
                :options="[10, 20, 50, 100]"
                dense
                flat
                borderless
                class="rows-per-page-select"
                popup-content-class="rows-per-page-popup"
                @update:model-value="handleDictDataSearch"
              >
                <template #append>
                  <span class="text-caption">{{ t("common.rowsPerPageUnit") }}</span>
                </template>
              </q-select>
              <span class="text-caption text-grey-7 q-ml-md">{{ t("common.jumpToLabel") }}</span>
              <q-input
                v-model.number="dictDataJumpToPage"
                dense
                flat
                borderless
                class="jump-to-page-input"
                input-class="text-center"
                :placeholder="String((props.pagesNumber || 1) <= 1 ? 1 : (dictDataCurPage >= (props.pagesNumber || 1) ? 1 : dictDataCurPage + 1))"
                @keyup.enter="handleDictDataJumpToPage"
              />
              <span class="text-caption text-grey-7">{{ t("common.jumpToUnit") }}</span>
            </div>
          </template>
        </q-table>
      </template>

      <!-- ── 右侧未选择字典时的空状态 ── -->
      <div v-else class="right-panel-placeholder column items-center justify-center text-grey-7 empty-state-content">
        <q-icon name="sym_r_database_search" size="56px" class="q-mb-sm" />
        <div class="text-body1 text-weight-medium q-mb-xs">
          {{ t('common.noData') }}
        </div>
        <div class="text-caption text-grey-6">
          {{ t('dictDataMgmt.selectDictFirst') }}
        </div>
      </div>
    </div>
  </div>

  <!-- ═══ 本地右侧抽屉：添加 / 编辑 / 查看字典 ═══ -->
  <Teleport to="body">
    <Transition name="dict-drawer-slide">
      <div
        v-if="dictDrawerOpen"
        v-mask-close="closeDictDrawer"
        class="dict-local-drawer-mask"
      >
        <div class="dict-local-drawer">
          <div class="dict-drawer-shell">
            <div class="dict-drawer-header row items-center no-wrap">
              <q-icon :name="dictDrawerIcon" size="20px" class="q-mr-sm" />
              <span class="dict-drawer-title">{{ dictDrawerTitle }}</span>
              <q-space />
              <q-btn
                flat
                dense
                round
                icon="sym_r_close"
                class="dict-drawer-close-btn"
                @click="closeDictDrawer"
              >
                <q-tooltip>{{ t("common.close") }}</q-tooltip>
              </q-btn>
            </div>
            <div class="dict-drawer-body">
              <DictDrawerContent
                :mode="dictDrawerMode"
                :dict="dictDrawerData"
                @close="closeDictDrawer"
                @saved="handleDictDrawerSaved"
              />
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- ═══ 本地右侧抽屉：添加 / 编辑 / 查看字典数据 ═══ -->
  <Teleport to="body">
    <Transition name="dict-drawer-slide">
      <div
        v-if="dictDataDrawerOpen"
        v-mask-close="closeDictDataDrawer"
        class="dict-local-drawer-mask"
      >
        <div class="dict-local-drawer">
          <div class="dict-drawer-shell">
            <div class="dict-drawer-header row items-center no-wrap">
              <q-icon :name="dictDataDrawerIcon" size="20px" class="q-mr-sm" />
              <span class="dict-drawer-title">{{ dictDataDrawerTitle }}</span>
              <q-space />
              <q-btn
                flat
                dense
                round
                icon="sym_r_close"
                class="dict-drawer-close-btn"
                @click="closeDictDataDrawer"
              >
                <q-tooltip>{{ t("common.close") }}</q-tooltip>
              </q-btn>
            </div>
            <div class="dict-drawer-body">
              <DictDataDrawerContent
                :mode="dictDataDrawerMode"
                :dict-data="dictDataDrawerData"
                :dict-id="selectedDict?.id"
                @close="closeDictDataDrawer"
                @saved="handleDictDataDrawerSaved"
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
.dict-list-shell {
  display: flex;
  height: calc(100vh - 64px - 40px - 44px - 16px);
  min-height: 0;
  gap: 8px;
}

/* ═══ 左侧面板 ═══ */
.left-panel {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ═══ 右侧面板 ═══ */
.right-panel {
  flex: 1 1 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.right-panel--no-border {
  border: 0;
}

.right-panel-placeholder {
  flex: 1 1 auto;
  min-height: 0;
  color: rgba(0, 0, 0, 0.45);
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

/* ── 表格（左侧字典表） ── */
.dict-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.dict-table :deep(.q-table__top) {
  display: none;
}

.dict-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.dict-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.dict-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.dict-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.dict-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.dict-table :deep(thead tr:first-child th) {
  border-top: none;
}

/* ── 空数据状态 ── */
.dict-table--empty :deep(.q-table__container) {
  height: 100%;
}

.dict-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.dict-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.dict-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

/* 行悬停 */
.dict-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.dict-table :deep(tbody tr.q-tr--selected td) {
  background: rgba(0, 121, 107, 0.06) !important;
}

.dict-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

/* 选中行高亮 — 整行 */
.dict-table :deep(tr.dict-row--selected td) {
  background: rgba(0, 121, 107, 0.08) !important;
}

.dict-table :deep(tr.dict-row--selected) {
  cursor: pointer;
}

/* ── 表格（右侧字典数据表） ── */
.dict-data-table {
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 0;
}

.dict-data-table :deep(.q-table__top) {
  display: none;
}

.dict-data-table :deep(.q-table__container) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.dict-data-table :deep(.q-table__middle) {
  flex: 1 1 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  overscroll-behavior: none;
}

.dict-data-table :deep(thead) {
  position: sticky;
  top: 0;
  z-index: 2;
}

.dict-data-table :deep(.q-table__middle > table) {
  flex: 0 0 auto;
}

.dict-data-table :deep(thead tr th) {
  font-weight: 700 !important;
  font-size: 13px !important;
  color: rgba(0, 0, 0, 0.8) !important;
  background: #fafafa !important;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08) !important;
}

.dict-data-table :deep(thead tr:first-child th) {
  border-top: none;
}

.dict-data-table--empty :deep(.q-table__container) {
  height: 100%;
}

.dict-data-table--empty :deep(.q-table__middle) {
  flex: 0 0 auto;
  overflow: visible;
}

.dict-data-table--empty :deep(.q-table__bottom) {
  flex: 1 1 0;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: none !important;
}

.dict-data-table--empty :deep(.q-table__bottom .q-table__bottom-nodata-icon) {
  display: none;
}

.dict-data-table :deep(tbody tr:hover td) {
  background: rgba(0, 121, 107, 0.03) !important;
}

.dict-data-table :deep(tbody tr.q-tr--selected td) {
  background: rgba(0, 121, 107, 0.06) !important;
}

.dict-data-table :deep(tbody td) {
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12) !important;
}

/* Badge 统一样式 */
.dict-type-badge {
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
.dict-table :deep(.q-table__bottom),
.dict-data-table :deep(.q-table__bottom) {
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
.dict-table :deep(.q-checkbox__inner),
.dict-data-table :deep(.q-checkbox__inner) {
  font-size: 32px;
}

/* ═══ 本地右侧抽屉 ═══ */
.dict-local-drawer-mask {
  position: fixed;
  inset: 0;
  z-index: 5000;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: flex-end;
}

.dict-local-drawer {
  width: 680px;
  max-width: 100vw;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dict-drawer-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.dict-drawer-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  height: 65px;
  padding: 0 16px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.dict-drawer-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.dict-drawer-close-btn {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  padding: 0;
  color: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  font-size: 20px;
}

.dict-drawer-close-btn :deep(.q-btn__wrapper) {
  min-width: 32px;
  min-height: 32px;
  padding: 0;
}

.dict-drawer-close-btn :deep(.q-icon) {
  font-size: 20px;
}

.dict-drawer-close-btn:hover {
  background: rgba(128, 128, 128, 0.2);
}

.dict-drawer-body {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

/* 抽屉滑入/滑出动画 */
.dict-drawer-slide-enter-active,
.dict-drawer-slide-leave-active {
  transition: opacity 0.25s ease;
}

.dict-drawer-slide-enter-active .dict-local-drawer,
.dict-drawer-slide-leave-active .dict-local-drawer {
  transition: transform 0.25s ease;
}

.dict-drawer-slide-enter-from,
.dict-drawer-slide-leave-to {
  opacity: 0;
}

.dict-drawer-slide-enter-from .dict-local-drawer,
.dict-drawer-slide-leave-to .dict-local-drawer {
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

/* ═══ 抽屉暗色模式 ═══ */
.body--dark .dict-local-drawer {
  background: #1e1e1e !important;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.4);
}

.body--dark .dict-drawer-header {
  background: #252525 !important;
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .dict-drawer-title {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .dict-drawer-close-btn {
  color: rgba(255, 255, 255, 0.6) !important;
}

.body--dark .dict-drawer-close-btn:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.body--dark .dict-drawer-body {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .dict-local-drawer-mask {
  background: rgba(0, 0, 0, 0.5);
}
</style>