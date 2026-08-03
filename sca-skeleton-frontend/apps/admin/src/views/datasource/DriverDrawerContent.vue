<script setup lang="ts">
import { ref, reactive, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { Driver, DriverUploadResponse } from "../../apis/datasource";
import { createDriverApi, updateDriverApi, uploadDriverApi } from "../../apis/datasource";
import DbTypeIcon from "../../components/DbTypeIcon.vue";

const { t } = useI18n({ useScope: "global" });

const props = defineProps<{
  mode: "add" | "edit" | "view";
  driver?: Driver;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");
const isEdit = computed(() => props.mode === "edit");

// ── 数据库类型选项（与后端 DbType 枚举一致） ──
const DB_TYPE_OPTIONS = [
  { label: "MySQL", value: "MYSQL" },
  { label: "Oracle", value: "ORACLE" },
  { label: "PostgreSQL", value: "POSTGRESQL" },
  { label: "SQLServer", value: "SQLSERVER" },
  { label: "达梦数据库", value: "DAMENG" },
  { label: "Kingbase", value: "KINGBASE" },
  { label: "MongoDB", value: "MONGODB" },
  { label: "ClickHouse", value: "CLICKHOUSE" },
  { label: "OceanBase", value: "OCEANBASE" },
  { label: "GaussDB", value: "GAUSSDB" }
];

const formLoading = ref(false);
const form = reactive({
  id: "",
  dbType: "",
  driverName: "",
  driverClass: "",
  urlTemplate: "",
  allowedParams: "",
  remark: "",
  // 上传回填字段（创建时使用）
  uploadId: "",
  fileSize: 0 as number,
  version: 0 as number | undefined
});

// ── JAR 多文件上传相关 ──
const jarFiles = ref<File[]>([]);
const uploading = ref(false);
const uploadInfo = ref<DriverUploadResponse | null>(null);
const detectedClasses = ref<string[]>([]);

const formRules = computed(() => ({
  dbType: [(v: string) => !!v || t("driverMgmt.dbTypeRequired")],
  driverName: [(v: string) => !!v?.trim() || t("driverMgmt.driverNameRequired")],
  driverClass: [(v: string) => !!v?.trim() || t("driverMgmt.driverClassRequired")]
}));

function resetForm() {
  form.id = "";
  form.dbType = "";
  form.driverName = "";
  form.driverClass = "";
  form.urlTemplate = "";
  form.allowedParams = "";
  form.remark = "";
  form.uploadId = "";
  form.fileSize = 0;
  form.version = 0;
  jarFiles.value = [];
  uploadInfo.value = null;
  detectedClasses.value = [];
}

function initForm() {
  resetForm();
  if (props.driver) {
    form.id = props.driver.id;
    form.dbType = props.driver.dbType;
    form.driverName = props.driver.driverName;
    form.driverClass = props.driver.driverClass;
    form.urlTemplate = props.driver.urlTemplate || "";
    form.allowedParams = props.driver.allowedParams || "";
    form.remark = props.driver.remark || "";
    form.fileSize = props.driver.totalFileSize || 0;
    form.version = props.driver.version;
  }
}

watch(() => props.driver, initForm, { immediate: true });

// ── JAR 文件上传（多文件） ──
async function onFilesChange(files: File[] | null) {
  if (!files || files.length === 0) {
    uploadInfo.value = null;
    detectedClasses.value = [];
    form.uploadId = "";
    form.fileSize = 0;
    return;
  }
  if (!form.dbType) {
    showToast(t("driverMgmt.dbTypeRequired"), "warning");
    jarFiles.value = [];
    return;
  }
  // 校验全部为 .jar 文件
  for (const f of files) {
    if (!f.name.endsWith(".jar")) {
      showToast(t("driverMgmt.jarFileHint"), "warning");
      jarFiles.value = [];
      return;
    }
  }

  uploading.value = true;
  try {
    const result = await uploadDriverApi(form.dbType, files);
    if (result.code === 10_000 && result.data) {
      uploadInfo.value = result.data;
      detectedClasses.value = result.data.detectedDriverClasses || [];
      form.uploadId = result.data.uploadId;
      form.fileSize = result.data.fileSize;
      // 自动填充驱动类名（仅当为空时）
      if (!form.driverClass && result.data.driverClass) {
        form.driverClass = result.data.driverClass;
      }
      showToast(t("driverMgmt.uploadSuccess"), "positive");
    } else {
      showToast(result.message || t("driverMgmt.uploadFail"), "negative");
      jarFiles.value = [];
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("driverMgmt.uploadFail"), "negative");
    }
    jarFiles.value = [];
  } finally {
    uploading.value = false;
  }
}

function selectDetectedClass(cls: string) {
  if (drawerReadonly.value) return;
  form.driverClass = cls;
}

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  // 新增时校验 JAR 已上传
  if (props.mode === "add" && !form.uploadId) {
    showToast(t("driverMgmt.jarFileRequired"), "warning");
    return;
  }

  const data: Record<string, unknown> = {
    driverName: form.driverName,
    urlTemplate: form.urlTemplate || undefined,
    allowedParams: form.allowedParams || undefined,
    remark: form.remark || undefined
  };

  if (props.mode === "add") {
    data.dbType = form.dbType;
    data.driverClass = form.driverClass;
    data.uploadId = form.uploadId;
  } else {
    data.id = form.id;
    data.version = form.version;
  }

  try {
    formLoading.value = true;
    const result = props.mode === "add"
      ? await createDriverApi(data)
      : await updateDriverApi(data);

    if (result.code === 10_000) {
      showToast(t("driverMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("driverMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("driverMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}

// ── 工具函数 ──
function formatFileSize(bytes: number): string {
  if (!bytes) return "-";
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + " MB";
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + " GB";
}
</script>

<template>
  <div class="driver-drawer-content">
    <q-form class="driver-drawer-form" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 数据库类型 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.dbType"
            :label="t('driverMgmt.dbType')"
            filled
            square
            :options="DB_TYPE_OPTIONS"
            emit-value
            map-options
            :rules="formRules.dbType"
            :disable="isEdit || drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          >
            <template v-slot:selected-item="scope">
              <div v-if="scope.opt" class="row items-center no-wrap q-gutter-x-xs">
                <DbTypeIcon :db-type="scope.opt.value" :size="20" />
                <span>{{ scope.opt.label }}</span>
              </div>
            </template>
            <template v-slot:option="scope">
              <q-item v-bind="scope.itemProps">
                <q-item-section avatar>
                  <DbTypeIcon :db-type="scope.opt.value" :size="20" />
                </q-item-section>
                <q-item-section>
                  <q-item-label>{{ scope.opt.label }}</q-item-label>
                </q-item-section>
              </q-item>
            </template>
          </q-select>
        </div>
        <!-- 驱动名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.driverName"
            :label="t('driverMgmt.driverName')"
            filled
            square
            :rules="formRules.driverName"
            :disable="isEdit || drawerReadonly"
            :readonly="drawerReadonly"
            hint="驱动名称作为目录名，创建后不可修改"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 驱动类名 -->
        <div class="col-12">
          <q-input
            v-model.trim="form.driverClass"
            :label="t('driverMgmt.driverClass')"
            filled
            square
            :rules="formRules.driverClass"
            :disable="isEdit || drawerReadonly"
            :readonly="drawerReadonly"
            hint="如 com.mysql.cj.jdbc.Driver"
            hide-bottom-space
            class="required-field"
          />
        </div>

        <!-- JAR 上传区域（仅新增模式，支持多文件） -->
        <div v-if="!isEdit" class="col-12">
          <div class="text-caption text-grey-8 q-mb-xs">{{ t('driverMgmt.jarFile') }} <span class="text-negative">*</span></div>
          <q-file
            v-model="jarFiles"
            accept=".jar"
            filled
            square
            multiple
            :label="t('driverMgmt.jarFile')"
            :loading="uploading"
            :disable="drawerReadonly"
            :hint="t('driverMgmt.jarFileHint')"
            hide-bottom-space
            class="required-field"
            @update:model-value="onFilesChange"
          >
            <template #prepend>
              <q-icon name="sym_r_attach_file" />
            </template>
          </q-file>

          <!-- 上传后信息 -->
          <div v-if="uploadInfo" class="q-mt-xs">
            <div class="text-caption text-grey-7">
              <span>{{ t('driverMgmt.totalFileSize') }}: {{ formatFileSize(uploadInfo.fileSize) }}</span>
              <span class="q-ml-md">{{ t('driverMgmt.driverFilesCount', { count: uploadInfo.jarFileNames?.length || 0 }) }}</span>
            </div>
            <!-- 已上传文件名列表 -->
            <div v-if="uploadInfo.jarFileNames?.length" class="q-mt-xs">
              <q-badge
                v-for="name in uploadInfo.jarFileNames"
                :key="name"
                class="q-mr-xs q-mb-xs"
                color="blue-2"
                text-color="blue-9"
              >
                {{ name }}
              </q-badge>
            </div>
            <!-- 探测到的驱动类 -->
            <div v-if="detectedClasses.length > 0" class="q-mt-xs">
              <div class="text-caption text-grey-8 q-mb-xs">{{ t('driverMgmt.detectedClasses') }}:</div>
              <q-badge
                v-for="cls in detectedClasses"
                :key="cls"
                class="q-mr-xs q-mb-xs cursor-pointer detected-class-badge"
                :color="form.driverClass === cls ? 'primary' : 'blue-2'"
                :text-color="form.driverClass === cls ? 'white' : 'blue-9'"
                @click="selectDetectedClass(cls)"
              >
                {{ cls }}
              </q-badge>
            </div>
          </div>
        </div>

        <!-- 编辑/查看模式下显示已有驱动文件列表（支持多文件） -->
        <div v-else class="col-12">
          <div class="text-caption text-grey-8 q-mb-xs">
            {{ t('driverMgmt.driverFiles') }}
            <span class="q-ml-xs text-grey-6">({{ t('driverMgmt.driverFilesCount', { count: props.driver?.files?.length || 0 }) }})</span>
          </div>
          <div class="jar-info-box">
            <div
              v-for="(file, idx) in props.driver?.files || []"
              :key="file.fileName"
              class="row items-center no-wrap q-py-xs"
            >
              <q-icon name="sym_r_description" size="18px" class="q-mr-sm text-grey-6" />
              <div class="text-body2 text-grey-8 ellipsis" style="max-width: 60%">
                {{ file.fileName }}
              </div>
              <q-badge v-if="idx === 0" class="q-ml-sm" color="teal-2" text-color="teal-9" :label="t('driverMgmt.mainFile')" />
              <q-space />
              <div class="text-caption text-grey-6 q-ml-sm">{{ formatFileSize(file.fileSize) }}</div>
            </div>
            <div class="text-caption text-grey-7 q-mt-xs row items-center">
              <span>{{ t('driverMgmt.totalFileSize') }}: {{ formatFileSize(form.fileSize) }}</span>
            </div>
            <div class="text-caption text-grey-5 q-mt-xs">{{ t('driverMgmt.fileLocked') }}</div>
          </div>
        </div>

        <!-- JDBC URL 模板 -->
        <div class="col-12">
          <q-input
            v-model.trim="form.urlTemplate"
            :label="t('driverMgmt.urlTemplate')"
            filled
            square
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hint="如 jdbc:mysql://{host}:{port}/{database}"
            hide-bottom-space
          />
        </div>
        <!-- URL 参数白名单 -->
        <div class="col-12">
          <q-input
            v-model="form.allowedParams"
            :label="t('driverMgmt.allowedParams')"
            filled
            square
            type="textarea"
            rows="2"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hint='JSON 数组，如 ["useSSL", "serverTimezone"]'
            hide-bottom-space
          />
        </div>
        <!-- 备注 -->
        <div class="col-12">
          <q-input
            v-model="form.remark"
            :label="t('driverMgmt.remark')"
            filled
            square
            type="textarea"
            rows="3"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
      </div>

      <!-- 底部操作按钮 -->
      <div v-if="!drawerReadonly" class="driver-drawer-footer row justify-end q-gutter-sm">
        <q-btn
          color="grey-7"
          outline
          no-caps
          class="drawer-action-btn"
          @click="handleClose"
        >
          {{ t('common.cancel') }}
        </q-btn>
        <q-btn
          type="submit"
          color="primary"
          unelevated
          no-caps
          :loading="formLoading"
          class="drawer-action-btn"
        >
          {{ t('common.confirm') }}
        </q-btn>
      </div>
    </q-form>
  </div>
</template>

<style scoped>
.driver-drawer-content {
  padding: 0;
}

.driver-drawer-footer {
  flex-shrink: 0;
  padding: 12px 0 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  margin-top: 16px;
}

.drawer-action-btn {
  min-width: 72px;
}

/* 必填项星号红色高亮 */
.required-field :deep(.q-field__label::after) {
  content: " *";
  color: var(--q-negative);
}

:deep(.q-field__append > .q-icon:not(.text-negative)) {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

/* JAR 信息展示盒 */
.jar-info-box {
  padding: 8px 12px;
  background: #fafafa;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 0;
}

/* 探测到的驱动类徽章 */
.detected-class-badge {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 0;
}

/* 修复 prefix 右侧多余间距 */
:deep(.q-field__prefix) {
  padding-right: 0 !important;
}
</style>

<style>
.body--dark .driver-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .driver-drawer-form .q-field__native,
.body--dark .driver-drawer-form .q-field__prefix,
.body--dark .driver-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .driver-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .driver-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .driver-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .driver-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

/* 抽屉底部按钮区域分隔线 */
.body--dark .driver-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}

/* JAR 信息盒暗色模式 */
.body--dark .jar-info-box {
  background: #252525;
  border-color: rgba(255, 255, 255, 0.08);
}
</style>
