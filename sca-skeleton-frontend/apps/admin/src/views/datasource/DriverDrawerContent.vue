<script setup lang="ts">
import { ref, reactive, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { Driver } from "../../apis/datasource";
import {
  createDriverApi,
  updateDriverApi,
  checkDriverNameExistsApi
} from "../../apis/datasource";
import { detectDriverClassesInJars } from "../../utils/driverClassDetector";
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
// 提交（含文件上传）进度：0-100，到 100 后切换为不定进度（服务端处理阶段）
const uploadPercent = ref(0);

const form = reactive({
  id: "",
  dbType: "",
  driverName: "",
  driverClass: "",
  urlTemplate: "",
  allowedParams: "",
  remark: "",
  fileSize: 0 as number,
  version: 0 as number | undefined
});

// ── 驱动名称唯一性异步校验（仅新增模式，失焦触发） ──
const nameChecking = ref(false);
let nameCheckSeq = 0;

async function validateDriverNameUnique(value: string): Promise<boolean | string> {
  const name = value?.trim();
  if (!name) {
    // 空值由必填规则负责提示
    return true;
  }
  const seq = ++nameCheckSeq;
  nameChecking.value = true;
  try {
    const result = await checkDriverNameExistsApi(name);
    if (seq !== nameCheckSeq) {
      // 已有更新的校验请求，丢弃过期结果
      return true;
    }
    if (result.code === 10_000 && result.data === true) {
      return t("driverMgmt.driverNameExists");
    }
    return true;
  } catch {
    // 检查请求失败时不阻塞用户，最终由提交时的后端校验兜底
    return true;
  } finally {
    if (seq === nameCheckSeq) {
      nameChecking.value = false;
    }
  }
}

const formRules = computed(() => ({
  driverName: [
    (v: string) => !!v?.trim() || t("driverMgmt.driverNameRequired"),
    ...(props.mode === "add" ? [validateDriverNameUnique] : [])
  ],
  dbType: [(v: string) => !!v || t("driverMgmt.dbTypeRequired")],
  driverClass: [(v: string) => !!v?.trim() || t("driverMgmt.driverClassRequired")]
}));

// ── 驱动文件（新增模式：文件仅保存在浏览器中，随表单提交一并上传） ──
// 注意：这些 ref 必须在 watch(...) 之前声明，否则 initForm()->resetForm()
// 会在它们初始化之前访问（TDZ），导致 "Cannot access 'xxx' before initialization"。
const jarFiles = ref<File[]>([]);
const detectedClasses = ref<string[]>([]);
const detecting = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);
const fileInputKey = ref(0);
const dragActive = ref(false);
const addDragActive = ref(false);

function resetForm() {
  form.id = "";
  form.dbType = "";
  form.driverName = "";
  form.driverClass = "";
  form.urlTemplate = "";
  form.allowedParams = "";
  form.remark = "";
  form.fileSize = 0;
  form.version = 0;
  jarFiles.value = [];
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

watch(() => props.driver, initForm, { immediate: true });

// 触发隐藏文件选择框
function triggerFilePick() {
  if (drawerReadonly.value || formLoading.value) return;
  fileInput.value?.click();
}

// 选择文件（追加到文件列表，可多次选择）
function onFilesChange(e: Event) {
  const input = e.target as HTMLInputElement;
  const picked = Array.from(input.files || []);
  // 重置 input 以允许多次选择同一文件
  input.value = "";
  void addFiles(picked);
}

// 拖拽到上传区
function onDrop(e: DragEvent) {
  dragActive.value = false;
  addDragActive.value = false;
  const files = Array.from(e.dataTransfer?.files || []);
  void addFiles(files);
}

// 通用：校验并追加文件，随后刷新客户端驱动类探测结果
async function addFiles(picked: File[]) {
  if (!picked.length || drawerReadonly.value || formLoading.value) return;

  // 校验全部为 .jar 文件
  if (picked.some((f) => !f.name.toLowerCase().endsWith(".jar"))) {
    showToast(t("driverMgmt.dropzoneSubtitle"), "warning");
    return;
  }

  // 按文件名去重后追加
  const existing = new Set(jarFiles.value.map((f) => f.name));
  const added = picked.filter((f) => !existing.has(f.name));
  if (!added.length) return;
  jarFiles.value.push(...added);
  await refreshDetectedClasses();
}

// 客户端探测驱动类（读取 JAR 内 META-INF/services/java.sql.Driver，不上传文件）
async function refreshDetectedClasses() {
  detecting.value = true;
  try {
    detectedClasses.value = await detectDriverClassesInJars(jarFiles.value);
    // 驱动类名为空时自动回填第一个探测结果
    if (!form.driverClass && detectedClasses.value.length) {
      form.driverClass = detectedClasses.value[0];
    }
  } finally {
    detecting.value = false;
  }
}

// 从文件列表移除单个文件
function removeFile(name: string) {
  if (formLoading.value) return;
  jarFiles.value = jarFiles.value.filter((f) => f.name !== name);
  void refreshDetectedClasses();
}

// 清空全部文件
function clearFiles() {
  if (formLoading.value) return;
  jarFiles.value = [];
  detectedClasses.value = [];
}

function selectDetectedClass(cls: string) {
  if (drawerReadonly.value) return;
  form.driverClass = cls;
}

// 待上传文件的总大小（用于列表尾部展示）
const pendingTotalSize = computed(() =>
  jarFiles.value.reduce((sum, f) => sum + (f.size || 0), 0)
);

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  if (props.mode === "add") {
    // 新增：表单字段与文件随同一次 multipart 请求提交
    if (!jarFiles.value.length) {
      showToast(t("driverMgmt.jarFileRequired"), "warning");
      return;
    }

    const data: Record<string, unknown> = {
      driverName: form.driverName,
      dbType: form.dbType,
      driverClass: form.driverClass,
      urlTemplate: form.urlTemplate || undefined,
      allowedParams: form.allowedParams || undefined,
      remark: form.remark || undefined
    };

    try {
      formLoading.value = true;
      uploadPercent.value = 0;
      const result = await createDriverApi(data, [...jarFiles.value], (percent) => {
        uploadPercent.value = percent;
      });
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
    return;
  }

  // 编辑：仅更新表单字段（驱动文件不可修改）
  const data: Record<string, unknown> = {
    id: form.id,
    version: form.version,
    driverName: form.driverName,
    urlTemplate: form.urlTemplate || undefined,
    allowedParams: form.allowedParams || undefined,
    remark: form.remark || undefined
  };

  try {
    formLoading.value = true;
    const result = await updateDriverApi(data);
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
        <!-- 驱动名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.driverName"
            :label="t('driverMgmt.driverName')"
            filled
            square
            :rules="formRules.driverName"
            lazy-rules
            :loading="nameChecking"
            :disable="isEdit || drawerReadonly"
            :readonly="drawerReadonly"
            :hint="t('driverMgmt.driverNameHint')"
            hide-bottom-space
            class="required-field"
          />
        </div>
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

        <!-- ── 驱动文件（新增模式：文件随表单提交，单一卡片式上传区） ── -->
        <div v-if="props.mode === 'add'" class="col-12 driver-files-section">
          <div class="text-caption text-grey-8 q-mb-xs">
            {{ t('driverMgmt.driverFiles') }}<span class="text-negative"> *</span>
          </div>

          <!-- 空状态：拖拽上传区（整个区域可点击 / 键盘可操作） -->
          <div
            v-if="!jarFiles.length"
            class="file-dropzone"
            :class="{ 'file-dropzone--drag': dragActive }"
            role="button"
            tabindex="0"
            :aria-label="t('driverMgmt.dropzoneTitle')"
            @click="triggerFilePick"
            @keydown.enter.prevent="triggerFilePick"
            @keydown.space.prevent="triggerFilePick"
            @dragover.prevent="dragActive = true"
            @dragleave="dragActive = false"
            @drop.prevent="onDrop"
          >
            <q-icon name="sym_r_upload_file" size="30px" class="file-dropzone__icon" />
            <div class="file-dropzone__title">{{ t('driverMgmt.dropzoneTitle') }}</div>
            <div class="file-dropzone__subtitle">{{ t('driverMgmt.dropzoneSubtitle') }}</div>
          </div>

          <!-- 已选文件：统一文件卡片（文件行 + 内嵌添加条 + 汇总行） -->
          <div v-else class="file-card">
            <div class="file-card__rows">
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
                  @click="removeFile(file.name)"
                >
                  <q-tooltip>{{ t('driverMgmt.removeFile') }}</q-tooltip>
                </q-btn>
              </div>
            </div>

            <!-- 继续添加：卡片内嵌的细拖拽条 -->
            <div
              class="file-card__add"
              :class="{ 'file-card__add--drag': addDragActive }"
              role="button"
              tabindex="0"
              @click="triggerFilePick"
              @keydown.enter.prevent="triggerFilePick"
              @keydown.space.prevent="triggerFilePick"
              @dragover.prevent="addDragActive = true"
              @dragleave="addDragActive = false"
              @drop.prevent="onDrop"
            >
              <q-icon name="sym_r_add" size="16px" />
              <span>{{ t('driverMgmt.addMoreFiles') }}</span>
            </div>

            <div class="file-card__foot">
              <span>
                {{ t('driverMgmt.driverFilesCount', { count: jarFiles.length }) }}
                · {{ t('driverMgmt.totalFileSize') }} {{ formatFileSize(pendingTotalSize) }}
              </span>
              <span class="file-card__clear" @click="clearFiles">
                {{ t('driverMgmt.clearFiles') }}
              </span>
            </div>
          </div>

          <!-- 客户端探测到的驱动类（点击回填驱动类名） -->
          <template v-if="jarFiles.length && !drawerReadonly">
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
            <div v-else-if="!detecting" class="file-detected file-detected--manual">
              {{ t('driverMgmt.driverClassManual') }}
            </div>
          </template>

          <!-- 隐藏的文件选择器（由上传区点击触发，可多次追加） -->
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
            :hint="t('driverMgmt.driverClassHint')"
            hide-bottom-space
            class="required-field"
          />
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
            :hint="t('driverMgmt.urlTemplateHint')"
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
            :hint="t('driverMgmt.allowedParamsHint')"
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
      <div v-if="!drawerReadonly" class="driver-drawer-footer">
        <q-linear-progress
          v-if="formLoading && props.mode === 'add'"
          :value="uploadPercent / 100"
          :indeterminate="uploadPercent >= 100"
          color="primary"
          height="3px"
          rounded
          class="q-mb-md"
        />
        <div class="row justify-end q-gutter-sm">
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

/* JAR 信息展示盒（编辑/查看模式） */
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

/* 隐藏的原生文件选择器 */
.hidden-file-input {
  display: none;
}

/* ── 驱动文件区（新增模式） ── */
.driver-files-section {
  margin-top: 4px;
}

/* 空状态拖拽上传区：整个区域即唯一点击入口 */
.file-dropzone {
  border: 1.5px dashed #c8cfd8;
  border-radius: 8px;
  background: #fafbfc;
  padding: 26px 16px;
  text-align: center;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s ease, background 0.2s ease;
}
.file-dropzone:hover,
.file-dropzone:focus-visible {
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

/* 已选文件统一卡片：文件行 + 内嵌添加条 + 汇总行，单一边框 */
.file-card {
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: #fff;
  overflow: hidden;
}
.file-card__rows {
  max-height: 220px;
  overflow-y: auto;
}
.file-card__add {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 10px 12px 0;
  padding: 9px 12px;
  border: 1px dashed #c8cfd8;
  border-radius: 6px;
  font-size: 12px;
  color: #9aa3af;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}
.file-card__add:hover,
.file-card__add:focus-visible {
  border-color: #1976d2;
  color: #1976d2;
  background: #f5f9ff;
}
.file-card__add--drag {
  border-color: #1976d2;
  color: #1976d2;
  background: #eef4ff;
}
.file-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  font-size: 12px;
  color: #9aa3af;
}
.file-card__clear {
  cursor: pointer;
  transition: color 0.15s ease;
}
.file-card__clear:hover {
  color: var(--q-negative);
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

/* 探测到的驱动类 */
.file-detected {
  margin-top: 8px;
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

/* 上传区暗色模式 */
.body--dark .file-dropzone {
  background: #252525;
  border-color: rgba(255, 255, 255, 0.18);
}
.body--dark .file-dropzone:hover,
.body--dark .file-dropzone:focus-visible,
.body--dark .file-dropzone--drag {
  border-color: #80cbc4;
  background: #2a2f2e;
}
.body--dark .file-dropzone__title {
  color: rgba(255, 255, 255, 0.85);
}
.body--dark .file-card {
  background: #252525;
  border-color: rgba(255, 255, 255, 0.08);
}
.body--dark .file-row:hover {
  background: #2c2c2c;
}
.body--dark .file-row + .file-row {
  border-top-color: rgba(255, 255, 255, 0.05);
}
.body--dark .file-row__name {
  color: rgba(255, 255, 255, 0.85);
}
.body--dark .file-card__add {
  border-color: rgba(255, 255, 255, 0.18);
}
.body--dark .file-card__add:hover,
.body--dark .file-card__add:focus-visible,
.body--dark .file-card__add--drag {
  border-color: #80cbc4;
  color: #80cbc4;
  background: #2a2f2e;
}
.body--dark .file-detected__label {
  color: rgba(255, 255, 255, 0.6);
}
</style>
