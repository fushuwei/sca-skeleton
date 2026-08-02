<template>
  <div class="q-pa-md">
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
          label="名称/主机"
          clearable
          dense
          outlined
          style="min-width: 200px"
          @keyup.enter="handleSearch"
        />
        <q-select
          v-model="searchForm.enabled"
          :options="enabledOptions"
          emit-value
          map-options
          label="状态"
          clearable
          dense
          outlined
          style="min-width: 100px"
          @update:model-value="handleSearch"
        />
        <q-space />
        <q-btn
          color="primary"
          icon="add"
          label="新增数据源"
          no-caps
          unelevated
          @click="handleCreate"
        />
      </q-card-section>
    </q-card>

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

      <template #body-cell-enabled="props">
        <q-td :props="props">
          <q-badge
            :color="props.row.enabled === 1 ? 'green' : 'grey'"
            :label="props.row.enabled === 1 ? '启用' : '禁用'"
          />
        </q-td>
      </template>

      <template #body-cell-connectionState="props">
        <q-td :props="props">
          <q-badge :color="getStateColor(props.row.connectionState)" :label="getStateLabel(props.row.connectionState)" />
        </q-td>
      </template>

      <template #body-cell-actions="props">
        <q-td :props="props" class="q-gutter-xs">
          <q-btn flat dense size="sm" color="primary" icon="edit" @click="handleEdit(props.row)">
            <q-tooltip>编辑</q-tooltip>
          </q-btn>
          <q-btn flat dense size="sm" color="blue" icon="plug" @click="handleTest(props.row)">
            <q-tooltip>测试连接</q-tooltip>
          </q-btn>
          <q-btn
            v-if="props.row.enabled === 1"
            flat dense size="sm" color="orange" icon="block" @click="handleDisable(props.row)"
          >
            <q-tooltip>禁用</q-tooltip>
          </q-btn>
          <q-btn v-else flat dense size="sm" color="green" icon="check_circle" @click="handleEnable(props.row)">
            <q-tooltip>启用</q-tooltip>
          </q-btn>
          <q-btn flat dense size="sm" color="red" icon="delete" @click="handleDelete(props.row)">
            <q-tooltip>删除</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <DatasourceDrawerContent
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
  getDatasourcePageApi,
  enableDatasourceApi,
  disableDatasourceApi,
  deleteDatasourceApi,
  testDatasourceApi,
  type Datasource
} from "../../apis/datasource";
import DatasourceDrawerContent from "./DatasourceDrawerContent.vue";

const $q = useQuasar();

const columns = [
  { name: "name", label: "数据源名称", field: "name", align: "left" as const, sortable: true },
  { name: "dbType", label: "数据库类型", field: "dbType", align: "left" as const },
  { name: "host", label: "主机:端口", field: "host", align: "left" as const },
  { name: "databaseName", label: "数据库", field: "databaseName", align: "left" as const },
  { name: "enabled", label: "管理态", field: "enabled", align: "center" as const },
  { name: "connectionState", label: "运行态", field: "connectionState", align: "center" as const },
  { name: "actions", label: "操作", field: "actions", align: "center" as const }
];

const searchForm = ref({ dbType: "", keyword: "", enabled: undefined as number | undefined, pageNum: 1, pageSize: 10 });
const tableData = ref<Datasource[]>([]);
const loading = ref(false);
const pagination = ref({ page: 1, rowsPerPage: 10, rowsNumber: 0 });

const dbTypeOptions = [
  { label: "MySQL", value: "MYSQL" }, { label: "Oracle", value: "ORACLE" },
  { label: "PostgreSQL", value: "POSTGRESQL" }, { label: "SQLServer", value: "SQLSERVER" },
  { label: "达梦", value: "DAMENG" }, { label: "人大金仓", value: "KINGBASE" },
  { label: "MongoDB", value: "MONGODB" }, { label: "ClickHouse", value: "CLICKHOUSE" },
  { label: "OceanBase(MySQL)", value: "OCEANBASE_MYSQL" }, { label: "OceanBase(Oracle)", value: "OCEANBASE_ORACLE" },
  { label: "GaussDB", value: "GAUSSDB" }
];
const enabledOptions = [{ label: "启用", value: 1 }, { label: "禁用", value: 0 }];

const drawerVisible = ref(false);
const editingRow = ref<Datasource | null>(null);

async function loadData() {
  loading.value = true;
  try {
    const res = await getDatasourcePageApi(searchForm.value);
    if (res.data) { tableData.value = res.data.records; pagination.value.rowsNumber = res.data.total; }
  } finally { loading.value = false; }
}

function handleSearch() { searchForm.value.pageNum = 1; pagination.value.page = 1; loadData(); }
function onRequest(props: { pagination: { page: number; rowsPerPage: number } }) {
  searchForm.value.pageNum = props.pagination.page; searchForm.value.pageSize = props.pagination.rowsPerPage;
  pagination.value.page = props.pagination.page; pagination.value.rowsPerPage = props.pagination.rowsPerPage;
  loadData();
}

function handleCreate() { editingRow.value = null; drawerVisible.value = true; }
function handleEdit(row: Datasource) { editingRow.value = { ...row }; drawerVisible.value = true; }

async function handleTest(row: Datasource) {
  $q.loading.show({ message: "测试连接中..." });
  try {
    await testDatasourceApi(row.id);
    $q.notify({ type: "positive", message: "连接成功" });
    loadData();
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } }; message?: string };
    const msg = err?.response?.data?.message || err?.message || "连接失败";
    $q.notify({ type: "negative", message: msg, timeout: 5000 });
  } finally {
    $q.loading.hide();
  }
}

async function handleEnable(row: Datasource) { await enableDatasourceApi(row.id); $q.notify({ type: "positive", message: "已启用" }); loadData(); }
async function handleDisable(row: Datasource) { await disableDatasourceApi(row.id); $q.notify({ type: "warning", message: "已禁用" }); loadData(); }
function handleDelete(row: Datasource) {
  $q.dialog({ title: "确认删除", message: `确定删除「${row.name}」？`, cancel: true, persistent: true })
    .onOk(async () => { await deleteDatasourceApi(row.id); $q.notify({ type: "positive", message: "删除成功" }); loadData(); });
}

function getDbTypeColor(dbType: string): string {
  const c: Record<string, string> = { MYSQL: "blue", ORACLE: "red", POSTGRESQL: "teal", SQLSERVER: "orange", MONGODB: "green", CLICKHOUSE: "purple", DAMENG: "cyan", KINGBASE: "indigo", OCEANBASE_MYSQL: "deep-orange", OCEANBASE_ORACLE: "pink", GAUSSDB: "brown" };
  return c[dbType] || "grey";
}
function getStateColor(s: string): string { return s === "online" ? "green" : s === "error" ? "red" : "grey"; }
function getStateLabel(s: string): string { return s === "online" ? "在线" : s === "error" ? "异常" : "离线"; }

onMounted(() => loadData());
</script>
