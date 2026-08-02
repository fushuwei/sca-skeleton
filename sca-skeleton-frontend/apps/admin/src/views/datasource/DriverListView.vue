<template>
  <div class="q-pa-md">
    <!-- 搜索栏 -->
    <q-card flat bordered class="q-mb-md">
      <q-card-section class="row q-gutter-md items-center">
        <q-select
          v-model="searchForm.dbType"
          :options="dbTypeOptions"
          emit-value
          map-options
          label="数据库类型"
          clearable
          dense
          outlined
          style="min-width: 180px"
          @update:model-value="handleSearch"
        />
        <q-input
          v-model="searchForm.keyword"
          label="驱动名称/类名"
          clearable
          dense
          outlined
          style="min-width: 220px"
          @keyup.enter="handleSearch"
        />
        <q-select
          v-model="searchForm.status"
          :options="statusOptions"
          emit-value
          map-options
          label="状态"
          clearable
          dense
          outlined
          style="min-width: 120px"
          @update:model-value="handleSearch"
        />
        <q-space />
        <q-btn
          color="primary"
          icon="add"
          label="新增驱动"
          no-caps
          unelevated
          @click="handleCreate"
        />
      </q-card-section>
    </q-card>

    <!-- 表格 -->
    <q-table
      :rows="tableData"
      :columns="columns"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      bordered
      flat
      @request="onRequest"
    >
      <template #body-cell-dbType="props">
        <q-td :props="props">
          <q-badge :color="getDbTypeColor(props.row.dbType)" :label="props.row.dbType" />
        </q-td>
      </template>

      <template #body-cell-status="props">
        <q-td :props="props">
          <q-badge
            :color="props.row.status === 'enabled' ? 'green' : 'grey'"
            :label="props.row.status === 'enabled' ? '启用' : '禁用'"
          />
        </q-td>
      </template>

      <template #body-cell-fileSize="props">
        <q-td :props="props">
          {{ formatFileSize(props.row.fileSize) }}
        </q-td>
      </template>

      <template #body-cell-actions="props">
        <q-td :props="props" class="q-gutter-xs">
          <q-btn
            flat
            dense
            size="sm"
            color="primary"
            icon="edit"
            @click="handleEdit(props.row)"
          >
            <q-tooltip>编辑</q-tooltip>
          </q-btn>
          <q-btn
            v-if="props.row.status === 'enabled'"
            flat
            dense
            size="sm"
            color="orange"
            icon="block"
            @click="handleDisable(props.row)"
          >
            <q-tooltip>禁用</q-tooltip>
          </q-btn>
          <q-btn
            v-else
            flat
            dense
            size="sm"
            color="green"
            icon="check_circle"
            @click="handleEnable(props.row)"
          >
            <q-tooltip>启用</q-tooltip>
          </q-btn>
          <q-btn
            flat
            dense
            size="sm"
            color="red"
            icon="delete"
            @click="handleDelete(props.row)"
          >
            <q-tooltip>删除</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <DriverDrawerContent
      v-model="drawerVisible"
      :edit-data="editingRow"
      @saved="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useQuasar } from "quasar";
import {
  getDriverPageApi,
  enableDriverApi,
  disableDriverApi,
  deleteDriverApi,
  type Driver,
  type DriverPageRequest
} from "../../apis/datasource";
import DriverDrawerContent from "./DriverDrawerContent.vue";

const $q = useQuasar();

const drawerVisible = ref(false);
const editingRow = ref<Driver | null>(null);

// 表格列定义
const columns = [
  { name: "dbType", label: "数据库类型", field: "dbType", align: "left" as const, sortable: true },
  { name: "driverName", label: "驱动名称", field: "driverName", align: "left" as const, sortable: true },
  { name: "driverClass", label: "驱动类名", field: "driverClass", align: "left" as const },
  { name: "driverVersion", label: "版本", field: "driverVersion", align: "left" as const, sortable: true },
  { name: "fileSize", label: "文件大小", field: "fileSize", align: "right" as const },
  { name: "status", label: "状态", field: "status", align: "center" as const },
  { name: "actions", label: "操作", field: "actions", align: "center" as const }
];

// 搜索表单
const searchForm = ref<DriverPageRequest>({
  dbType: undefined,
  keyword: "",
  status: undefined,
  pageNum: 1,
  pageSize: 10
});

// 表格数据
const tableData = ref<Driver[]>([]);
const loading = ref(false);
const pagination = ref({
  page: 1,
  rowsPerPage: 10,
  rowsNumber: 0
});

// 下拉选项
const dbTypeOptions = [
  { label: "MySQL", value: "MYSQL" },
  { label: "Oracle", value: "ORACLE" },
  { label: "PostgreSQL", value: "POSTGRESQL" },
  { label: "SQLServer", value: "SQLSERVER" },
  { label: "达梦 DM", value: "DAMENG" },
  { label: "人大金仓", value: "KINGBASE" },
  { label: "MongoDB", value: "MONGODB" },
  { label: "ClickHouse", value: "CLICKHOUSE" },
  { label: "OceanBase (MySQL)", value: "OCEANBASE_MYSQL" },
  { label: "OceanBase (Oracle)", value: "OCEANBASE_ORACLE" },
  { label: "GaussDB", value: "GAUSSDB" }
];

const statusOptions = [
  { label: "启用", value: "enabled" },
  { label: "禁用", value: "disabled" }
];

// 加载数据
async function loadData() {
  loading.value = true;
  try {
    const res = await getDriverPageApi(searchForm.value);
    if (res.data) {
      tableData.value = res.data.records;
      pagination.value.rowsNumber = res.data.total;
    }
  } finally {
    loading.value = false;
  }
}

// 搜索
function handleSearch() {
  searchForm.value.pageNum = 1;
  pagination.value.page = 1;
  loadData();
}

// 分页
function onRequest(props: { pagination: { page: number; rowsPerPage: number } }) {
  searchForm.value.pageNum = props.pagination.page;
  searchForm.value.pageSize = props.pagination.rowsPerPage;
  pagination.value.page = props.pagination.page;
  pagination.value.rowsPerPage = props.pagination.rowsPerPage;
  loadData();
}

// 新增
function handleCreate() {
  editingRow.value = null;
  drawerVisible.value = true;
}

// 编辑
function handleEdit(row: Driver) {
  editingRow.value = { ...row };
  drawerVisible.value = true;
}

// 启用
async function handleEnable(row: Driver) {
  await enableDriverApi(row.id);
  $q.notify({ type: "positive", message: "驱动已启用" });
  loadData();
}

// 禁用
async function handleDisable(row: Driver) {
  await disableDriverApi(row.id);
  $q.notify({ type: "warning", message: "驱动已禁用" });
  loadData();
}

// 删除
function handleDelete(row: Driver) {
  $q.dialog({
    title: "确认删除",
    message: `确定要删除驱动「${row.driverName}」吗？`,
    cancel: true,
    persistent: true
  }).onOk(async () => {
    await deleteDriverApi(row.id);
    $q.notify({ type: "positive", message: "删除成功" });
    loadData();
  });
}

// 工具函数
function getDbTypeColor(dbType: string): string {
  const colors: Record<string, string> = {
    MYSQL: "blue",
    ORACLE: "red",
    POSTGRESQL: "teal",
    SQLSERVER: "orange",
    MONGODB: "green",
    CLICKHOUSE: "purple",
    DAMENG: "cyan",
    KINGBASE: "indigo",
    OCEANBASE_MYSQL: "deep-orange",
    OCEANBASE_ORACLE: "pink",
    GAUSSDB: "brown"
  };
  return colors[dbType] || "grey";
}

function formatFileSize(bytes: number): string {
  if (!bytes) return "-";
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / (1024 * 1024)).toFixed(1) + " MB";
}

onMounted(() => {
  loadData();
});
</script>
