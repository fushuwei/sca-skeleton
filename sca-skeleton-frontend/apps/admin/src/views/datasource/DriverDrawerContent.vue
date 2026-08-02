<template>
  <q-drawer v-model="show" side="right" :width="480" bordered>
    <div class="q-pa-md">
      <div class="text-h6 q-mb-md">{{ editData ? '编辑驱动' : '新增驱动' }}</div>
      <q-form @submit.prevent="handleSubmit" class="q-gutter-md">
        <q-select v-model="form.dbType" :options="dbTypeOptions" emit-value map-options label="数据库类型 *" outlined dense :rules="[v => !!v || '必选']" />
        <q-input v-model="form.driverName" label="驱动名称 *" outlined dense :rules="[v => !!v || '必填']" />
        <q-input v-model="form.driverClass" label="驱动类名 *" outlined dense hint="如 com.mysql.cj.jdbc.Driver" :rules="[v => !!v || '必填']" />
        <q-input v-model="form.driverVersion" label="驱动版本 *" outlined dense hint="如 8.0.33" :rules="[v => !!v || '必填']" />
        <q-file v-model="jarFile" label="上传 JAR 文件" accept=".jar,.zip" outlined dense max-file-size="524288000" @update:model-value="onFileChange">
          <template #prepend><q-icon name="attach_file" /></template>
        </q-file>
        <div v-if="sha256" class="text-caption text-grey-7">SHA256: {{ sha256 }}</div>
        <q-input v-model="form.urlTemplate" label="JDBC URL 模板" outlined dense hint="如 jdbc:mysql://{host}:{port}/{database}" />
        <q-input v-model="form.remark" label="备注" outlined dense type="textarea" />
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
import { createDriverApi, updateDriverApi, type Driver } from "../../apis/datasource";

const props = defineProps<{ modelValue: boolean; editData: Driver | null }>();
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
const jarFile = ref<File | null>(null);
const sha256 = ref("");
const submitting = ref(false);

watch(() => props.modelValue, (v) => {
  if (v) {
    if (props.editData) {
      form.value = { ...props.editData };
      sha256.value = props.editData.jarSha256 || "";
    } else {
      form.value = { dbType: "", driverName: "", driverClass: "", driverVersion: "", urlTemplate: "", remark: "" };
      sha256.value = "";
    }
    jarFile.value = null;
  }
});

function onFileChange(file: File | null) {
  if (!file) return;
  form.value.fileSize = file.size;
  form.value.driverName = form.value.driverName || file.name.replace(/\.(jar|zip)$/, "");
}

async function handleSubmit() {
  submitting.value = true;
  try {
    if (props.editData) {
      await updateDriverApi({ id: props.editData.id, ...form.value });
      $q.notify({ type: "positive", message: "更新成功" });
    } else {
      await createDriverApi(form.value);
      $q.notify({ type: "positive", message: "创建成功" });
    }
    show.value = false;
    emit("saved");
  } finally { submitting.value = false; }
}
</script>
