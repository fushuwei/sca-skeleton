<template>
  <q-drawer v-model="show" side="right" :width="480" bordered>
    <div class="q-pa-md">
      <div class="text-h6 q-mb-md">{{ editData ? '编辑驱动' : '新增驱动' }}</div>
      <q-form @submit.prevent="handleSubmit" class="q-gutter-md">
        <q-select
          v-model="form.dbType"
          :options="dbTypeOptions"
          emit-value
          map-options
          label="数据库类型 *"
          outlined
          dense
          :disable="!!editData"
          :rules="[v => !!v || '必选']"
        />
        <q-input v-model="form.driverName" label="驱动名称 *" outlined dense :rules="[v => !!v || '必填']" />
        <q-input v-model="form.driverClass" label="驱动类名 *" outlined dense hint="如 com.mysql.cj.jdbc.Driver" :rules="[v => !!v || '必填']" />
        <q-input v-model="form.driverVersion" label="驱动版本 *" outlined dense hint="如 8.0.33" :rules="[v => !!v || '必填']" />

        <!-- JAR 上传区域 -->
        <div v-if="!editData">
          <q-file
            v-model="jarFile"
            label="上传 JAR 文件 *"
            accept=".jar"
            outlined
            dense
            max-file-size="524288000"
            :loading="uploading"
            @update:model-value="onFileChange"
          >
            <template #prepend><q-icon name="attach_file" /></template>
          </q-file>
          <div v-if="uploadInfo" class="q-mt-xs text-caption text-grey-7">
            SHA256: {{ uploadInfo.jarSha256?.substring(0, 16) }}... | 大小: {{ formatFileSize(uploadInfo.fileSize) }}
          </div>
          <div v-if="detectedClasses.length > 0" class="q-mt-xs">
            <q-badge v-for="cls in detectedClasses" :key="cls" class="q-mr-xs q-mb-xs" color="blue-2" text-color="blue-9">
              {{ cls }}
            </q-badge>
          </div>
        </div>

        <!-- 编辑模式下显示已有 JAR 信息 -->
        <div v-else class="text-caption text-grey-7">
          SHA256: {{ editData.jarSha256?.substring(0, 16) }}... | 大小: {{ formatFileSize(editData.fileSize) }}
          <div class="text-grey-5 q-mt-xs">JAR 文件不可更改，如需更换请新建驱动</div>
        </div>

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
import {
  createDriverApi,
  updateDriverApi,
  uploadDriverApi,
  type Driver,
  type DriverUploadResponse
} from "../../apis/datasource";

const props = defineProps<{ modelValue: boolean; editData: Driver | null }>();
const emit = defineEmits<{ "update:modelValue": [v: boolean]; saved: [] }>();

const $q = useQuasar();
const show = computed({
  get: () => props.modelValue,
  set: v => emit("update:modelValue", v)
});

const dbTypeOptions = [
  { label: "MySQL", value: "MYSQL" },
  { label: "Oracle", value: "ORACLE" },
  { label: "PostgreSQL", value: "POSTGRESQL" },
  { label: "SQLServer", value: "SQLSERVER" },
  { label: "达梦", value: "DAMENG" },
  { label: "人大金仓", value: "KINGBASE" },
  { label: "MongoDB", value: "MONGODB" },
  { label: "ClickHouse", value: "CLICKHOUSE" },
  { label: "OceanBase(MySQL)", value: "OCEANBASE_MYSQL" },
  { label: "OceanBase(Oracle)", value: "OCEANBASE_ORACLE" },
  { label: "GaussDB", value: "GAUSSDB" }
];

const form = ref<Record<string, string | number>>({});
const jarFile = ref<File | null>(null);
const uploadInfo = ref<DriverUploadResponse | null>(null);
const detectedClasses = ref<string[]>([]);
const uploading = ref(false);
const submitting = ref(false);

watch(
  () => props.modelValue,
  v => {
    if (v) {
      if (props.editData) {
        form.value = { ...props.editData };
        uploadInfo.value = null;
        detectedClasses.value = [];
      } else {
        form.value = {
          dbType: "",
          driverName: "",
          driverClass: "",
          driverVersion: "",
          urlTemplate: "",
          remark: ""
        };
        uploadInfo.value = null;
        detectedClasses.value = [];
      }
      jarFile.value = null;
    }
  }
);

async function onFileChange(file: File | null) {
  if (!file) {
    uploadInfo.value = null;
    detectedClasses.value = [];
    return;
  }
  if (!form.value.dbType) {
    $q.notify({ type: "warning", message: "请先选择数据库类型" });
    jarFile.value = null;
    return;
  }

  uploading.value = true;
  try {
    const res = await uploadDriverApi(form.value.dbType as string, file);
    uploadInfo.value = res.data;
    detectedClasses.value = res.data.detectedDriverClasses || [];
    // 自动填充驱动类名（用户可修改）
    if (res.data.driverClass && !form.value.driverClass) {
      form.value.driverClass = res.data.driverClass;
    }
    // 自动填充驱动名称（取文件名去扩展名）
    if (!form.value.driverName) {
      form.value.driverName = file.name.replace(/\.jar$/i, "");
    }
    $q.notify({ type: "positive", message: "JAR 上传成功，已探测驱动类" });
  } catch {
    jarFile.value = null;
  } finally {
    uploading.value = false;
  }
}

async function handleSubmit() {
  // 新建模式下校验必须上传 JAR
  if (!props.editData && !uploadInfo.value) {
    $q.notify({ type: "warning", message: "请先上传驱动 JAR 文件" });
    return;
  }

  submitting.value = true;
  try {
    if (props.editData) {
      await updateDriverApi({
        id: props.editData.id,
        driverName: form.value.driverName,
        urlTemplate: form.value.urlTemplate,
        allowedParams: form.value.allowedParams,
        remark: form.value.remark,
        version: props.editData.version
      });
      $q.notify({ type: "positive", message: "更新成功" });
    } else {
      await createDriverApi({
        dbType: form.value.dbType,
        driverName: form.value.driverName,
        driverClass: form.value.driverClass,
        driverVersion: form.value.driverVersion,
        jarSha256: uploadInfo.value?.jarSha256,
        objectKey: uploadInfo.value?.objectKey,
        fileSize: uploadInfo.value?.fileSize,
        urlTemplate: form.value.urlTemplate,
        remark: form.value.remark
      });
      $q.notify({ type: "positive", message: "创建成功" });
    }
    show.value = false;
    emit("saved");
  } finally {
    submitting.value = false;
  }
}

function formatFileSize(bytes: number): string {
  if (!bytes) return "-";
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / (1024 * 1024)).toFixed(1) + " MB";
}
</script>
