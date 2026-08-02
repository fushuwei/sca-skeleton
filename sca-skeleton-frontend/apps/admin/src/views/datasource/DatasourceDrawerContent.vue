<template>
  <q-drawer v-model="show" side="right" :width="480" bordered>
    <div class="q-pa-md">
      <div class="text-h6 q-mb-md">{{ editData ? '编辑数据源' : '新增数据源' }}</div>
      <q-form @submit.prevent="handleSubmit" class="q-gutter-md">
        <q-input v-model="form.name" label="数据源名称 *" outlined dense :rules="[v => !!v || '必填']" />
        <q-select
          v-model="form.dbType" :options="dbTypeOptions" emit-value map-options
          label="数据库类型 *" outlined dense :rules="[v => !!v || '必选']"
          @update:model-value="onDbTypeChange"
        />
        <q-select
          v-model="form.driverId" :options="driverOptions" emit-value map-options
          label="关联驱动" outlined dense :loading="driverLoading"
        />
        <div class="row q-gutter-md">
          <q-input v-model="form.host" label="主机 *" outlined dense class="col" :rules="[v => !!v || '必填']" />
          <q-input v-model.number="form.port" type="number" label="端口 *" outlined dense style="width: 100px" :rules="[v => !!v || '必填']" />
        </div>
        <q-input v-model="form.databaseName" label="数据库名/Schema" outlined dense />
        <q-input v-model="form.username" label="用户名 *" outlined dense :rules="[v => !!v || '必填']" />
        <q-input
          v-model="form.password" :label="editData ? '密码（留空不修改）' : '密码 *'"
          :type="showPwd ? 'text' : 'password'" outlined dense
          :rules="editData ? [] : [v => !!v || '必填']"
        >
          <template #append>
            <q-icon :name="showPwd ? 'visibility' : 'visibility_off'" class="cursor-pointer" @click="showPwd = !showPwd" />
          </template>
        </q-input>
        <q-input v-model="form.connectionParams" label="连接参数（JSON）" outlined dense hint="如 {\"useSSL\": false}" />
        <q-input v-model="form.poolConfig" label="连接池配置（JSON）" outlined dense hint="如 {\"maxPoolSize\": 10}" />
        <div class="row q-gutter-md q-mt-sm">
          <q-btn label="取消" flat color="grey" @click="show = false" />
          <q-space />
          <q-btn label="保存" type="submit" color="primary" unelevated :loading="submitting" />
        </div>
      </q-form>
    </div>
  </q-drawer>
</template>

<script setup lang="ts">
import { ref, watch, computed } from "vue";
import { useQuasar } from "quasar";
import { createDatasourceApi, updateDatasourceApi, getDriverOptionsApi, type Datasource, type DriverOption } from "../../apis/datasource";

const props = defineProps<{ modelValue: boolean; editData: Datasource | null }>();
const emit = defineEmits<{ "update:modelValue": [v: boolean]; saved: [] }>();

const $q = useQuasar();
const show = computed({ get: () => props.modelValue, set: v => emit("update:modelValue", v) });

const dbTypeOptions = [
  { label: "MySQL", value: "MYSQL" }, { label: "Oracle", value: "ORACLE" },
  { label: "PostgreSQL", value: "POSTGRESQL" }, { label: "SQLServer", value: "SQLSERVER" },
  { label: "达梦", value: "DAMENG" }, { label: "人大金仓", value: "KINGBASE" },
  { label: "MongoDB", value: "MONGODB" }, { label: "ClickHouse", value: "CLICKHOUSE" },
  { label: "OceanBase(MySQL)", value: "OCEANBASE_MYSQL" }, { label: "OceanBase(Oracle)", value: "OCEANBASE_ORACLE" },
  { label: "GaussDB", value: "GAUSSDB" }
];

const form = ref<Record<string, unknown>>({});
const showPwd = ref(false);
const submitting = ref(false);
const driverOptions = ref<DriverOption[]>([]);
const driverLoading = ref(false);

watch(() => props.modelValue, (v) => {
  if (v) {
    if (props.editData) {
      form.value = { ...props.editData, password: "" };
    } else {
      form.value = { name: "", dbType: "", driverId: "", host: "", port: 3306, databaseName: "", username: "", password: "", connectionParams: "", poolConfig: "" };
    }
  }
});

async function onDbTypeChange(dbType: string) {
  if (!dbType) return;
  driverLoading.value = true;
  try {
    const res = await getDriverOptionsApi(dbType);
    driverOptions.value = res.data || [];
  } finally { driverLoading.value = false; }
}

async function handleSubmit() {
  submitting.value = true;
  try {
    if (props.editData) {
      await updateDatasourceApi({ id: props.editData.id, ...form.value });
      $q.notify({ type: "positive", message: "更新成功" });
    } else {
      await createDatasourceApi(form.value);
      $q.notify({ type: "positive", message: "创建成功" });
    }
    show.value = false;
    emit("saved");
  } finally { submitting.value = false; }
}
</script>
