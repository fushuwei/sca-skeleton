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
  fileInputKey.value++;
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

// ── 驱动文件列表（添加模式：待上传文件 / 已上传信息） ──
// 注意：这些 ref 必须在 watch(...) 之前声明，否则 initForm()->resetForm()
// 会在它们初始化之前访问（TDZ），导致 "Cannot access 'xxx' before initialization"。
const jarFiles = ref<File[]>([]);
const uploading = ref(false);
const uploadInfo = ref<DriverUploadResponse | null>(null);
const detectedClasses = ref<string[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);
const fileInputKey = ref(0);
const dragActive = ref(false);

watch(() => props.driver, initForm, { immediate: true });

// 触发隐藏文件选择框
function triggerFilePick() {
  if (uploading.value || !form.dbType) return;
  fileInput.value?.click();
}

// 选择文件（追加到待上传列表，可多次选择）
function onFilesChange(e: Event) {
  const input = e.target as HTMLInputElement;
  const picked = Array.from(input.files || []);
  // 重置 input 以允许多次选择同一文件
  input.value = "";
  addFiles(picked);
}

// 拖拽到上传区
function onDrop(e: DragEvent) {
  dragActive.value = false;
  const files = Array.from(e.dataTransfer?.files || []);
  addFiles(files);
}

// 通用：校验并追加文件到待上传列表
function addFiles(picked: File[]) {
  if (!picked.length) return;

  if (!form.dbType) {
    showToast(t("driverMgmt.dbTypeRequired"), "warning");
    return;
  }
  // 校验全部为 .jar 文件
  if (picked.some((f) => !f.name.endsWith(".jar"))) {
    showToast(t("driverMgmt.jarFileHint"), "warning");
    return;
  }

  // 去重后追加到待上传列表
  const existing = new Set(jarFiles.value.map((f) => f.name));
  picked.forEach((f) => {
    if (!existing.has(f.name)) {
      jarFiles.value.push(f);
      existing.add(f.name);
    }
  });
}

// 从待上传列表移除单个文件
function removePendingFile(name: string) {
  jarFiles.value = jarFiles.value.filter((f) => f.name !== name);
}

// 统一上传整个待上传列表
async function handleUpload() {
  if (jarFiles.value.length === 0) {
    showToast(t("driverMgmt.jarFileRequired"), "warning");
    return;
  }
  if (!form.dbType) {
    showToast(t("driverMgmt.dbTypeRequired"), "warning");
    return;
  }
  uploading.value = true;
  try {
    const result = await uploadDriverApi(form.dbType, jarFiles.value);
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
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("driverMgmt.uploadFail"), "negative");
    }
  } finally {
    uploading.value = false;
  }
}

function selectDetectedClass(cls: string) {
  if (drawerReadonly.value) return;
  form.driverClass = cls;
}

// 待上传文件的总大小（用于列表头部展示）
const pendingTotalSize = computed(() =>
  jarFiles.value.reduce((sum, f) => sum + (f.size || 0), 0)
);

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
              <div v-if="scope.opt" class="row items-center no-wrap">
                <DbTypeIcon :db-type="scope.opt.value" :size="20" class="q-mr-xs" />
                <span>{{ scope.opt.label }}</span>
              </div>
            </template>
            <template v-slot:option="scope">
              <q-item v-bind="scope.itemProps">
                <q-item-section avatar style="min-width: auto; padding-right: 8px;">
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

        <!-- ── 驱动文件（新增模式：拖拽上传区 + 文件列表） ── -->
        <div v-if="!isEdit" class="col-12 driver-files-section">

          <!-- 拖拽上传区（未上传阶段） -->
          <div
            v-if="!uploadInfo"
            class="file-dropzone"
            :class="{ 'file-dropzone--drag': dragActive }"
            @click="triggerFilePick"
            @dragover.prevent="dragActive = true"
            @dragleave="dragActive = false"
            @drop.prevent="onDrop"
          >
            <div class="file-dropzone__icon">
              <q-icon name="sym_r_upload_file" size="30px" />
            </div>
            <div class="file-dropzone__title">{{ t('driverMgmt.dropzoneTitle') }}</div>
            <div class="file-dropzone__subtitle">{{ t('driverMgmt.dropzoneSubtitle') }}</div>
            <q-btn
              class="file-dropzone__btn q-mt-sm"
              outline
              unelevated
              no-caps
              color="primary"
              size="sm"
              icon="sym_r_folder_open"
              :label="t('driverMgmt.chooseFile')"
              :disable="!form.dbType || uploading"
              @click.stop="triggerFilePick"
            />
          </div>

          <!-- 待上传文件列表（选中后出现） -->
          <transition name="filelist">
            <div v-if="!uploadInfo && jarFiles.length" class="file-list">
              <div class="file-list__head">
                <q-icon name="sym_r_inbox" size="15px" />
                <span>{{ t('driverMgmt.pendingFiles') }}</span>
                <span v-if="jarFiles.length" class="driver-files-count">
                  {{ t('driverMgmt.driverFilesCount', { count: jarFiles.length }) }}
                </span>
                <q-space />
                <span class="file-list__total">{{ t('driverMgmt.totalFileSize') }}：{{ formatFileSize(pendingTotalSize) }}</span>
              </div>

              <div class="file-list__body">
                <div
                  v-for="file in jarFiles"
                  :key="file.name"
                  class="file-row"
                >
                  <div class="file-row__icon">
                    <q-icon name="sym_r_description" size="18px" />
                  </div>
                  <div class="file-row__meta">
                    <div class="file-row__name ellipsis">{{ file.name }}</div>
                    <div class="file-row__size">{{ formatFileSize(file.size) }}</div>
                  </div>
                  <q-btn
                    flat
                    round
                    dense
                    size="sm"
                    color="grey"
                    class="file-row__remove"
                    icon="sym_r_close"
                    :aria-label="t('driverMgmt.removeFile')"
                    @click="removePendingFile(file.name)"
                  >
                    <q-tooltip>{{ t('driverMgmt.removeFile') }}</q-tooltip>
                  </q-btn>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="file-list__actions row justify-end q-gutter-sm no-wrap">
                <q-btn
                  outline
                  unelevated
                  no-caps
                  color="grey-7"
                  size="sm"
                  icon="sym_r_add"
                  :label="t('driverMgmt.addFile')"
                  :disable="uploading"
                  @click="triggerFilePick"
                />
                <q-btn
                  unelevated
                  no-caps
                  color="primary"
                  size="sm"
                  icon="sym_r_cloud_upload"
                  :label="t('driverMgmt.startUpload')"
                  :disable="!jarFiles.length"
                  :loading="uploading"
                  @click="handleUpload"
                />
              </div>
            </div>
          </transition>

          <!-- 已上传状态 -->
          <transition name="filelist">
            <div v-if="uploadInfo" class="file-uploaded">
              <div class="file-uploaded__banner">
                <q-icon name="sym_r_check_circle" size="20px" />
                <div>
                  <div class="file-uploaded__title">{{ t('driverMgmt.uploadSuccess') }}</div>
                  <div class="file-uploaded__summary">
                    {{ t('driverMgmt.driverFilesCount', { count: uploadInfo.jarFileNames?.length || 0 }) }}
                    · {{ t('driverMgmt.totalFileSize') }} {{ formatFileSize(uploadInfo.fileSize) }}
                  </div>
                </div>
              </div>

              <div class="file-list__body">
                <div
                  v-for="name in uploadInfo.jarFileNames || []"
                  :key="name"
                  class="file-row"
                >
                  <div class="file-row__icon">
                    <q-icon name="sym_r_description" size="18px" />
                  </div>
                  <div class="file-row__meta">
                    <div class="file-row__name ellipsis">{{ name }}</div>
                  </div>
                  <q-icon name="sym_r_check" size="18px" class="file-row__ok" />
                </div>
              </div>

              <!-- 探测到的驱动类 -->
              <div v-if="detectedClasses.length" class="file-detected">
                <div class="file-detected__label">{{ t('driverMgmt.detectedClasses') }}</div>
                <div class="file-detected__list">
                  <q-badge
                    v-for="cls in detectedClasses"
                    :key="cls"
                    class="detected-class-badge cursor-pointer q-mr-xs q-mb-xs"
                    :color="form.driverClass === cls ? 'primary' : 'blue-2'"
                    :text-color="form.driverClass === cls ? 'white' : 'blue-9'"
                    @click="selectDetectedClass(cls)"
                  >
                    {{ cls }}
                  </q-badge>
                </div>
              </div>
              <div v-else-if="uploadInfo" class="file-detected file-detected--manual">
                {{ t('driverMgmt.driverClassManual') }}
              </div>
            </div>
          </transition>

          <!-- 隐藏的文件选择器（由“选择文件/添加文件”触发，可多次追加） -->
          <input
            ref="fileInput"
            :key="fileInputKey"
            type="file"
            accept=".jar"
            multiple
            class="hidden-file-input"
            @change="onFilesChange"
          />
        </div>

        <!-- 编辑/查看模式下显示已有驱动文件列表（支持多文件） -->
        <div v-else class="col-12">
          <div class="text-caption text-grey-8 q-mb-xs">
            {{ t('driverMgmt.driverFiles') }}
            <span class="q-ml-xs text-grey-6">({{ t('driverMgmt.driverFilesCount', { count: props.driver?.files?.length || 0 }) }})</span>
          </div>
          <div class="jar-info-box">
            <div
              v-for="file in props.driver?.files || []"
              :key="file.fileName"
              class="row items-center no-wrap q-py-xs"
            >
              <q-icon name="sym_r_description" size="18px" class="q-mr-sm text-grey-6" />
              <div class="text-body2 text-grey-8 ellipsis" style="max-width: 60%">
                {{ file.fileName }}
              </div>
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

/* 文件列表行 */
.file-row + .file-row {
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

/* 隐藏的原生文件选择器 */
.hidden-file-input {
  display: none;
}

/* 文件列表小按钮 */
.drawer-action-btn-sm {
  min-width: 88px;
}

/* ── 驱动文件区（新增模式：拖拽上传 + 文件列表） ── */
.driver-files-section {
  margin-top: 4px;
}

.driver-files-count {
  font-size: 12px;
  color: #26a69a;
  background: rgba(38, 166, 154, 0.1);
  border-radius: 10px;
  padding: 1px 10px;
  font-weight: 500;
}

/* 拖拽上传区 */
.file-dropzone {
  border: 1.5px dashed #c8cfd8;
  border-radius: 8px;
  background: #fafbfc;
  padding: 22px 16px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}
.file-dropzone:hover {
  border-color: #1976d2;
  background: #f5f9ff;
}
.file-dropzone--drag {
  border-color: #1976d2;
  background: #eef4ff;
}
.file-dropzone--drag .file-dropzone__icon {
  transform: translateY(-2px);
}
.file-dropzone__icon {
  color: #1976d2;
  display: flex;
  justify-content: center;
  transition: transform 0.2s ease;
}
.file-dropzone__title {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.8);
}
.file-dropzone__subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: #9aa3af;
}
.file-dropzone__btn {
  border-radius: 6px;
}

/* 文件列表容器 */
.file-list {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.file-list__head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.6);
  background: #f7f8fa;
  border-bottom: 1px solid #eef0f3;
}
.file-list__total {
  font-weight: 500;
  color: #9aa3af;
}
.file-list__body {
  max-height: 220px;
  overflow-y: auto;
}
.file-list__actions {
  padding: 10px 12px;
  border-top: 1px solid #eef0f3;
  background: #fafbfc;
}
.file-list__actions .q-btn {
  border-radius: 6px;
}

/* 文件行 */
.file-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
}
.file-row + .file-row {
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}
.file-row:hover {
  background: #fafbfc;
}
.file-row__icon {
  color: #1976d2;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.file-row__meta {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.file-row__name {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.85);
  font-weight: 500;
}
.file-row__size {
  font-size: 12px;
  color: #9aa3af;
  flex-shrink: 0;
}
.file-row__remove {
  flex-shrink: 0;
  color: #9aa3af;
}
.file-row__ok {
  color: #26a69a;
  flex-shrink: 0;
}

/* 上传成功状态 */
.file-uploaded {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.file-uploaded__banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #f0faf7;
  border-bottom: 1px solid #e0f2ec;
}
.file-uploaded__banner .q-icon {
  color: #26a69a;
}
.file-uploaded__title {
  font-size: 13px;
  font-weight: 600;
  color: #00897b;
}
.file-uploaded__summary {
  margin-top: 2px;
  font-size: 12px;
  color: #6e8b84;
}

/* 探测到的驱动类 */
.file-detected {
  padding: 10px 12px;
  border-top: 1px solid #eef0f3;
}
.file-detected__label {
  font-size: 12px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.6);
  margin-bottom: 4px;
}
.file-detected__list {
  display: flex;
  flex-wrap: wrap;
}
.file-detected--manual {
  color: #9aa3af;
  font-size: 12px;
}

/* 文件列表过渡动画 */
.filelist-enter-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.filelist-leave-active {
  transition: opacity 0.1s ease;
}
.filelist-enter-from {
  opacity: 0;
  transform: translateY(-6px);
}
.filelist-leave-to {
  opacity: 0;
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
