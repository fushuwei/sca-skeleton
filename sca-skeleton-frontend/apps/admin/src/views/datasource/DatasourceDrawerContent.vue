<template>
  <q-drawer v-model="show" side="right" :width="480" bordered>
    <div class="q-pa-md">
      <div class="text-h6 q-mb-md">{{ editData ? '编辑数据源' : '新增数据源' }}</div>
      <q-form @submit.prevent="handleSubmit" class="q-gutter-md">
        <q-input v-model="form.name" label="数据源名称 *" outlined dense :rules="[v => !!v || '必填']" />
        <q-select
          v-model="form.dbType"
          :options="dbTypeOptions"
          emit-value
          map-options
          label="数据库类型 *"
          outlined
          dense
          :rules="[v => !!v || '必选']"
          @update:model-value="onDbTypeChange"
        />
        <q-select
          v-model="form.driverId"
          :options="driverOptions"
          emit-value
          map-options
          label="关联驱动"
          outlined
          dense
          :loading="driverLoading"
          option-label="driverName"
          option-value="id"
        />
        <div class="row q-gutter-md">
          <q-input v-model="form.host" label="主机 *" outlined dense class="col" :rules="[v => !!v || '必填']" />
          <q-input v-model.number="form.port" type="number" label="端口 *" outlined dense style="width: 100px" :rules="[v => !!v || '必填']" />
        </div>
        <q-input v-model="form.databaseName" label="数据库名/Schema" outlined dense />
        <q-input v-model="form.username" label="用户名 *" outlined dense :rules="[v => !!v || '必填']" />
        <q-input
          v-model="form.password"
          :label="editData ? '密码（留空不修改）' : '密码 *'"
          :type="showPwd ? 'text' : 'password'"
          outlined
          dense
          :rules="editData ? [] : [v => !!v || '必填']"
        >
          <template #append>
            <q-icon :name="showPwd ? 'visibility' : 'visibility_off'" class="cursor-pointer" @click="showPwd = !showPwd" />
          </template>
        </q-input>
        <q-input
          v-model="form.connectionParams"
          label="连接参数"
          outlined
          dense
          hint='JSON 格式: {"useSSL": false, "serverTimezone": "Asia/Shanghai"}'
        />
        <q-input
          v-model="form.poolConfig"
          label="连接池配置（JSON）"
          outlined
          dense
          hint='如 {"maxPoolSize": 10}'
        />
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
import { ref, watch, computed, onMounted } from "vue";
import { useQuasar } from "quasar";
import {
  createDatasourceApi,
  updateDatasourceApi,
  getDriverOptionsApi,
  getDbTypesApi,
  type Datasource,
  type DriverOption,
  type DbTypeOption
} from "../../apis/datasource";

const props = defineProps<{ modelValue: boolean; editData: Datasource | null }>();
const emit = defineEmits<{ "update:modelValue": [v: boolean]; saved: [] }>();

const $q = useQuasar();
const show = computed({
  get: () => props.modelValue,
  set: v => emit("update:modelValue", v)
});

const dbTypeOptions = ref<DbTypeOption[]>([]);
const form = ref<Record<string, string | number>>({});
const showPwd = ref(false);
const submitting = ref(false);
const driverOptions = ref<DriverOption[]>([]);
const driverLoading = ref(false);

onMounted(async () => {
  try {
    const res = await getDbTypesApi();
    dbTypeOptions.value = (res.data || []).map(t => ({
      name: t.name,
      displayName: t.displayName,
      urlPrefix: t.urlPrefix,
      defaultPort: t.defaultPort
    }));
  } catch {
    // 降级：使用硬编码选项
    dbTypeOptions.value = [
      { name: "MYSQL", displayName: "MySQL", urlPrefix: "jdbc:mysql://", defaultPort: 3306 },
      { name: "POSTGRESQL", displayName: "PostgreSQL", urlPrefix: "jdbc:postgresql://", defaultPort: 5432 },
      { name: "ORACLE", displayName: "Oracle", urlPrefix: "jdbc:oracle:thin:@", defaultPort: 1521 }
    ];
  }
});

watch(
  () => props.modelValue,
  v => {
    if (v) {
      if (props.editData) {
        form.value = { ...props.editData, password: "" };
        // 编辑模式下加载已有 dbType 对应的驱动选项
        if (props.editData.dbType) {
          loadDriverOptions(props.editData.dbType);
        }
      } else {
        form.value = {
          name: "",
          dbType: "",
          driverId: "",
          host: "",
          port: 3306,
          databaseName: "",
          username: "",
          password: "",
          connectionParams: "",
          poolConfig: ""
        };
        driverOptions.value = [];
      }
    }
  }
);

async function onDbTypeChange(dbType: string) {
  if (!dbType) return;
  // 自动填充默认端口
  const option = dbTypeOptions.value.find(o => o.name === dbType);
  if (option && !form.value.port) {
    form.value.port = option.defaultPort;
  }
  await loadDriverOptions(dbType);
}

async function loadDriverOptions(dbType: string) {
  driverLoading.value = true;
  try {
    const res = await getDriverOptionsApi(dbType);
    driverOptions.value = res.data || [];
  } finally {
    driverLoading.value = false;
  }
}

async function handleSubmit() {
  submitting.value = true;
  try {
    if (props.editData) {
      await updateDatasourceApi({
        id: props.editData.id,
        ...form.value,
        version: props.editData.version
      });
      $q.notify({ type: "positive", message: "更新成功" });
    } else {
      await createDatasourceApi(form.value);
      $q.notify({ type: "positive", message: "创建成功" });
    }
    show.value = false;
    emit("saved");
  } finally {
    submitting.value = false;
  }
}
</script>
