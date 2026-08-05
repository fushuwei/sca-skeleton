<script setup lang="ts">
import { ref, reactive, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import type { QSelect } from "quasar";
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

// MongoDB 使用官方 driver（MongoClients.create），无需 JDBC 驱动类
const isMongoDb = computed(() => form.dbType === "MONGODB");

// 驱动类下拉框引用（切换数据库类型时重置校验状态，清除残留的错误提示）
const driverClassSelectRef = ref<QSelect | null>(null);

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

// ── 各数据库类型默认 JDBC URL 模板（占位符 {host}/{port}/{database}，前缀与后端 DbType 枚举一致） ──
const DEFAULT_URL_TEMPLATES: Record<string, string> = {
  MYSQL: "jdbc:mysql://{host}:{port}/{database}",
  ORACLE: "jdbc:oracle:thin:@//{host}:{port}/{database}",
  POSTGRESQL: "jdbc:postgresql://{host}:{port}/{database}",
  SQLSERVER: "jdbc:sqlserver://{host}:{port};databaseName={database}",
  DAMENG: "jdbc:dm://{host}:{port}/{database}",
  KINGBASE: "jdbc:kingbase8://{host}:{port}/{database}",
  MONGODB: "mongodb://{host}:{port}/{database}",
  CLICKHOUSE: "jdbc:clickhouse://{host}:{port}/{database}",
  OCEANBASE: "jdbc:oceanbase://{host}:{port}/{database}",
  GAUSSDB: "jdbc:gaussdb://{host}:{port}/{database}"
};

// 判断当前 URL 模板是否为某个类型的默认模板（切换类型时据此决定是否自动覆盖，避免覆盖用户自定义内容）
const DEFAULT_URL_VALUES = new Set(Object.values(DEFAULT_URL_TEMPLATES));
function isDefaultUrlTemplate(value: string): boolean {
  return DEFAULT_URL_VALUES.has(value);
}

const formLoading = ref(false);

const form = reactive({
  id: "",
  dbType: "",
  name: "",
  driverClass: "",
  urlTemplate: "",
  remark: "",
  fileSize: 0 as number,
  version: 0 as number | undefined
});

// ── 驱动名称唯一性异步校验（仅 @blur 触发，不参与表单提交校验） ──
const nameChecking = ref(false);
const nameError = ref("");
let nameCheckSeq = 0;
// 上次已校验过的名称，避免失焦时值未变却重复请求
let lastCheckedName = "";

async function onNameBlur() {
  const name = form.name?.trim();
  if (!name) {
    nameError.value = "";
    return;
  }
  // 值与上次校验的一致，无需重复请求
  if (name === lastCheckedName) {
    return;
  }
  const seq = ++nameCheckSeq;
  nameChecking.value = true;
  try {
    const result = await checkDriverNameExistsApi(name);
    if (seq !== nameCheckSeq) {
      // 已有更新的校验请求，丢弃过期结果
      return;
    }
    lastCheckedName = name;
    nameError.value = result.code === 10_000 && result.data === true
      ? t("driverMgmt.driverNameExists")
      : "";
  } catch {
    // 检查请求失败时不阻塞用户，最终由提交时的后端校验兜底
    nameError.value = "";
  } finally {
    if (seq === nameCheckSeq) {
      nameChecking.value = false;
    }
  }
}

// 用户修改输入时清除错误提示并重置已校验标记，使下次失焦重新校验
watch(() => form.name, () => {
  nameError.value = "";
  lastCheckedName = "";
});

const formRules = computed(() => ({
  name: [
    (v: string) => !!v?.trim() || t("driverMgmt.driverNameRequired")
  ],
  dbType: [(v: string) => !!v || t("driverMgmt.dbTypeRequired")],
  driverClass: isMongoDb.value
    ? []
    : [(v: string) => !!v?.trim() || t("driverMgmt.driverClassRequired")]
}));

// 切换数据库类型时重置驱动类校验状态：
// Quasar q-select 缓存校验结果，rules 从必填变为 [] 后旧错误不会自动清除，
// 需要手动 resetValidation() 消除残留的红色边框和感叹号
watch(isMongoDb, () => {
  driverClassSelectRef.value?.resetValidation();
});

// 选择数据库类型时自动填充默认 JDBC URL 模板：
// 仅当模板为空或当前值仍为某类型的默认模板时覆盖，保留用户自定义内容
function onDbTypeChange(value: string) {
  if (drawerReadonly.value) return;
  const tpl = DEFAULT_URL_TEMPLATES[value];
  if (!tpl) return;
  if (!form.urlTemplate || isDefaultUrlTemplate(form.urlTemplate)) {
    form.urlTemplate = tpl;
  }
}

// ── 驱动文件管理 ──
// 注意：这些 ref 必须在 watch(...) 之前声明，否则 initForm()->resetForm()
// 会在它们初始化之前访问（TDZ），导致 "Cannot access 'xxx' before initialization"。

// 新增的浏览器端文件（add 和 edit 模式都用这个）
const jarFiles = ref<File[]>([]);
// 编辑模式下，已存在于服务端的文件名集合（用户可删除）
const existingFileNames = ref<string[]>([]);
// 编辑模式下，用户标记删除的已有文件名
const deletedFileNames = ref<Set<string>>(new Set());

// 单次提交的驱动文件总大小上限：20MB（与后端 spring.servlet.multipart 保持一致）
const MAX_UPLOAD_TOTAL_BYTES = 20 * 1024 * 1024;

// 禁止上传的脚本/可执行文件扩展名（防止注入攻击）
const BLOCKED_EXTENSIONS = [
  ".js", ".mjs", ".cjs",          // JavaScript
  ".sh", ".bash", ".zsh",         // Shell
  ".bat", ".cmd", ".com",         // Windows 批处理
  ".ps1", ".psm1",                // PowerShell
  ".vbs", ".vba", ".wsf",         // VBScript / Windows Script
  ".py", ".rb", ".php", ".pl",    // 脚本语言
  ".lua", ".tcl",                 // 脚本语言
  ".exe", ".dll", ".so", ".dylib", ".msi", ".scr"  // 可执行/二进制文件
];
const fileInput = ref<HTMLInputElement | null>(null);
const fileInputKey = ref(0);
const dragActive = ref(false);

// ── 驱动类下拉框选项 ──
// 所有候选驱动类（合并已有文件的 driverClasses + 新上传文件的客户端探测结果）
const driverClassOptions = ref<string[]>([]);

function resetForm() {
  form.id = "";
  form.dbType = "";
  form.name = "";
  form.driverClass = "";
  form.urlTemplate = "";
  form.remark = "";
  form.fileSize = 0;
  form.version = 0;
  jarFiles.value = [];
  existingFileNames.value = [];
  deletedFileNames.value = new Set();
  driverClassOptions.value = [];
  fileInputKey.value++;
  lastCheckedName = "";
}

function initForm() {
  resetForm();
  if (props.driver) {
    form.id = props.driver.id;
    form.dbType = props.driver.dbType;
    form.name = props.driver.name;
    form.driverClass = props.driver.driverClass;
    form.urlTemplate = props.driver.urlTemplate || "";
    form.remark = props.driver.remark || "";
    form.fileSize = Number(props.driver.totalFileSize) || 0;
    form.version = props.driver.version;
    // 编辑模式下，记录已有文件名
    if (props.driver.files) {
      existingFileNames.value = props.driver.files.map((f) => f.fileName);
    }
    // 编辑模式下，从已保存的文件记录中读取驱动类作为下拉选项
    refreshDriverClassOptions();
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
  const files = Array.from(e.dataTransfer?.files || []);
  void addFiles(files);
}

// 通用：校验并追加文件，随后刷新驱动类选项
async function addFiles(picked: File[]) {
  if (!picked.length || drawerReadonly.value || formLoading.value) return;

  // 校验：禁止上传脚本/可执行文件
  const blockedFile = picked.find((f) =>
    BLOCKED_EXTENSIONS.some((ext) => f.name.toLowerCase().endsWith(ext))
  );
  if (blockedFile) {
    showToast(t("driverMgmt.fileTypeBlocked"), "warning");
    return;
  }

  // 按文件名去重：排除新增文件、未删除的已有文件
  const currentNames = new Set([
    ...jarFiles.value.map((f) => f.name),
    ...existingFileNames.value.filter((n) => !deletedFileNames.value.has(n))
  ]);
  const added = picked.filter((f) => !currentNames.has(f.name));
  if (!added.length) return;

  // 校验追加后的文件总大小不超过 20MB
  const addedSize = added.reduce((sum, f) => sum + (f.size || 0), 0);
  if (totalFileSize.value + addedSize > MAX_UPLOAD_TOTAL_BYTES) {
    showToast(t("driverMgmt.totalSizeExceeded"), "warning");
    return;
  }

  jarFiles.value.push(...added);
  await refreshDriverClassOptions();
}

// 刷新驱动类下拉选项
// 合并来源：1. 已有文件的 driverClasses（后端入库时探测写入） 2. 新上传文件的客户端探测结果
// 移除文件后，若当前已选驱动类已无对应文件支撑，则同步清空选择
async function refreshDriverClassOptions() {
  const merged = new Set<string>();

  // 已有文件（未删除）的驱动类
  if (props.driver?.files) {
    for (const f of props.driver.files) {
      if (f.driverClasses && !deletedFileNames.value.has(f.fileName)) {
        for (const cls of f.driverClasses.split(",")) {
          const trimmed = cls.trim();
          if (trimmed) merged.add(trimmed);
        }
      }
    }
  }

  // 新上传文件的客户端探测结果
  const newClasses = await detectDriverClassesInJars(jarFiles.value);
  for (const cls of newClasses) {
    merged.add(cls);
  }

  driverClassOptions.value = [...merged];

  // 当前已选驱动类已不在有效选项中（对应文件已被移除），清空选择
  if (form.driverClass && !merged.has(form.driverClass.trim())) {
    form.driverClass = "";
  }

  // 驱动类为空时自动回填第一个探测结果
  if (!form.driverClass && driverClassOptions.value.length) {
    form.driverClass = driverClassOptions.value[0];
  }
}

// 从文件列表移除单个文件
function removeFile(name: string) {
  if (formLoading.value) return;
  // 先查新增文件
  const inNew = jarFiles.value.some((f) => f.name === name);
  if (inNew) {
    jarFiles.value = jarFiles.value.filter((f) => f.name !== name);
    void refreshDriverClassOptions();
    return;
  }
  // 否则是已有文件，标记删除
  deletedFileNames.value.add(name);
  deletedFileNames.value = new Set(deletedFileNames.value); // 触发响应式
  void refreshDriverClassOptions();
}

// 清空全部文件
function clearFiles() {
  if (formLoading.value) return;
  jarFiles.value = [];
  // 编辑模式下标记所有已有文件为删除
  for (const name of existingFileNames.value) {
    deletedFileNames.value.add(name);
  }
  deletedFileNames.value = new Set(deletedFileNames.value);
  refreshDriverClassOptions();
}

// 统一的文件展示列表（合并已有文件 + 新增文件）
interface DisplayFile {
  name: string;
  size: number;
  isNew: boolean;
}

const displayFiles = computed<DisplayFile[]>(() => {
  const existing = existingFileNames.value
    .filter((n) => !deletedFileNames.value.has(n))
    .map((name) => {
      const f = props.driver?.files?.find((f) => f.fileName === name);
      return { name, size: Number(f?.fileSize) || 0, isNew: false };
    });
  const newFiles = jarFiles.value.map((f) => ({ name: f.name, size: f.size, isNew: true }));
  return [...existing, ...newFiles];
});

// 所有文件的总大小（已有未删除 + 新增）
const totalFileSize = computed(() =>
  displayFiles.value.reduce((sum, f) => sum + (f.size || 0), 0)
);

// 是否有文件（用于判断是否显示文件列表）
const hasFiles = computed(() => displayFiles.value.length > 0);

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  // 名称异步校验未通过时阻止提交（最终由后端 create/update 接口兜底校验）
  if (nameError.value) {
    showToast(nameError.value, "warning");
    return;
  }

  if (props.mode === "add") {
    // 新增：表单字段与文件随同一次 multipart 请求提交
    if (!jarFiles.value.length) {
      showToast(t("driverMgmt.jarFileRequired"), "warning");
      return;
    }

    // 提交前兜底校验文件总大小
    if (totalFileSize.value > MAX_UPLOAD_TOTAL_BYTES) {
      showToast(t("driverMgmt.totalSizeExceeded"), "warning");
      return;
    }

    const data: Record<string, unknown> = {
      name: form.name,
      dbType: form.dbType,
      driverClass: form.driverClass || undefined,
      urlTemplate: form.urlTemplate || undefined,
      remark: form.remark || undefined
    };

    try {
      formLoading.value = true;
      const result = await createDriverApi(data, [...jarFiles.value]);
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

  // 编辑：表单字段 + 新增文件 + 删除文件信息随同一次请求提交
  const data: Record<string, unknown> = {
    id: form.id,
    version: form.version,
    name: form.name,
    dbType: form.dbType || undefined,
    driverClass: form.driverClass || undefined,
    urlTemplate: form.urlTemplate || undefined,
    remark: form.remark || undefined,
    deletedFileNames: deletedFileNames.value.size > 0 ? [...deletedFileNames.value] : undefined
  };

  // 校验：编辑后至少保留一个文件
  if (displayFiles.value.length === 0) {
    showToast(t("driverMgmt.jarFileRequired"), "warning");
    return;
  }

  try {
    formLoading.value = true;
    const result = await updateDriverApi(data, jarFiles.value.length > 0 ? [...jarFiles.value] : undefined);
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
function formatFileSize(bytes: number | string): string {
  const n = Number(bytes);
  if (!n) return "-";
  if (n < 1024) return n + " B";
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + " KB";
  if (n < 1024 * 1024 * 1024) return (n / 1024 / 1024).toFixed(1) + " MB";
  return (n / 1024 / 1024 / 1024).toFixed(2) + " GB";
}
</script>

<template>
  <div class="driver-drawer-content">
    <q-form class="driver-drawer-form" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 驱动名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.name"
            :label="t('driverMgmt.driverName')"
            filled
            square
            :rules="formRules.name"
            lazy-rules
            :loading="nameChecking"
            :error="!!nameError"
            :error-message="nameError"
            @blur="onNameBlur"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
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
            @update:model-value="onDbTypeChange"
            :rules="formRules.dbType"
            :disable="drawerReadonly"
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

        <!-- ── 文件（add 和 edit 模式共用同一上传区）：一体化上传面板 ── -->
        <!-- 标签、上传触发区、文件列表收在同一个虚线面板内，整体传达"一个上传组件"的心智 -->
        <div class="col-12">
          <div
            class="upload-panel"
            :class="{
              'upload-panel--drag': dragActive,
              'upload-panel--readonly': drawerReadonly
            }"
            @dragover.prevent="dragActive = true"
            @dragleave="dragActive = false"
            @drop.prevent="onDrop"
          >
            <!-- 面板头：左侧字段标签，右侧文件汇总与清空（有文件时展示） -->
            <div
              class="upload-panel__head"
              :class="{ 'upload-panel__head--border': !drawerReadonly || hasFiles }"
            >
              <span class="upload-panel__label">
                {{ t('driverMgmt.driverFiles') }}<span class="text-negative"> *</span>
              </span>
              <span v-if="hasFiles" class="upload-panel__summary">
                {{ t('driverMgmt.driverFilesCount', { count: displayFiles.length }) }}
                · {{ t('driverMgmt.totalFileSize') }} {{ formatFileSize(totalFileSize) }}
                <span
                  v-if="!drawerReadonly"
                  class="upload-panel__clear"
                  @click="clearFiles"
                >
                  {{ t('driverMgmt.clearFiles') }}
                </span>
              </span>
            </div>

            <!-- 上传触发区：无文件时为大尺寸引导态，有文件后收敛为紧凑的"继续添加"入口 -->
            <div
              v-if="!drawerReadonly"
              class="upload-panel__dropzone"
              :class="{ 'upload-panel__dropzone--compact': hasFiles }"
              role="button"
              tabindex="0"
              :aria-label="hasFiles ? t('driverMgmt.addMoreFiles') : t('driverMgmt.dropzoneTitle')"
              @click="triggerFilePick"
              @keydown.enter.prevent="triggerFilePick"
              @keydown.space.prevent="triggerFilePick"
            >
              <template v-if="!hasFiles">
                <q-icon name="sym_r_cloud_upload" size="44px" class="upload-panel__icon" />
                <div class="upload-panel__title">{{ t('driverMgmt.dropzoneTitle') }}</div>
                <div class="upload-panel__subtitle">{{ t('driverMgmt.dropzoneSubtitle') }}</div>
              </template>
              <template v-else>
                <q-icon name="sym_r_add" size="18px" />
                <span>{{ t('driverMgmt.addMoreFiles') }}</span>
              </template>
            </div>

            <!-- 文件列表：与上传触发区同处一个面板，用细分隔线区隔 -->
            <div v-if="hasFiles" class="upload-panel__list">
              <div
                v-for="file in displayFiles"
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
                  v-if="!drawerReadonly"
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

            <!-- 隐藏的文件选择器（由上传区点击触发，可多次追加） -->
            <input
              ref="fileInput"
              :key="fileInputKey"
              type="file"
              multiple
              class="hidden-file-input"
              @change="onFilesChange"
            />
          </div>
        </div>

        <!-- 驱动类（下拉框，仅允许从探测结果中选择，不允许手动输入） -->
        <div class="col-12">
          <q-select
            ref="driverClassSelectRef"
            v-model="form.driverClass"
            :label="t('driverMgmt.driverClass')"
            filled
            square
            :options="driverClassOptions"
            :rules="formRules.driverClass"
            :disable="drawerReadonly"
            hide-bottom-space
            :class="{ 'required-field': !isMongoDb }"
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

/* 隐藏的原生文件选择器 */
.hidden-file-input {
  display: none;
}

/* ── 一体化上传面板：标签 + 上传触发区 + 文件列表共享同一边框与背景 ── */
.upload-panel {
  border: 1.5px dashed #c8cfd8;
  border-radius: 8px;
  background: #fafbfc;
  overflow: hidden;
  transition: border-color 0.2s ease, background 0.2s ease;
}
/* 只读态：实线边框、白底，弱化为信息展示卡片 */
.upload-panel--readonly {
  border-style: solid;
  border-color: #e4e7ec;
  background: #fff;
}
.upload-panel--drag {
  border-color: #1976d2;
  background: #eef4ff;
}

/* 面板头：左侧字段标签，右侧汇总信息 */
.upload-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
}
.upload-panel__head--border {
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}
.upload-panel__label {
  font-size: 12px;
  color: #757575;
  flex-shrink: 0;
}
.upload-panel__summary {
  font-size: 12px;
  color: #9aa3af;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.upload-panel__clear {
  margin-left: 8px;
  cursor: pointer;
  transition: color 0.15s ease;
}
.upload-panel__clear:hover {
  color: var(--q-negative);
}

/* 上传触发区：面板内唯一点击入口，无独立边框 */
.upload-panel__dropzone {
  padding: 26px 16px;
  text-align: center;
  cursor: pointer;
  outline: none;
  transition: background 0.2s ease;
}
.upload-panel__dropzone:hover,
.upload-panel__dropzone:focus-visible {
  background: #f5f9ff;
}
.upload-panel--drag .upload-panel__dropzone {
  background: #eef4ff;
}
.upload-panel__icon {
  color: #1976d2;
  transition: transform 0.2s ease;
}
.upload-panel--drag .upload-panel__icon {
  transform: translateY(-2px);
}
.upload-panel__title {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.8);
}
.upload-panel__subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: #9aa3af;
}

/* 有文件后的紧凑添加入口 */
.upload-panel__dropzone--compact {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 9px 16px;
  font-size: 13px;
  color: #1976d2;
}

/* 文件列表：与触发区共用面板边框，顶部细分隔线 */
.upload-panel__list {
  max-height: 220px;
  overflow-y: auto;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
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
  background: rgba(25, 118, 210, 0.04);
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

/* 一体化上传面板暗色模式 */
.body--dark .upload-panel {
  background: #252525;
  border-color: rgba(255, 255, 255, 0.18);
}
.body--dark .upload-panel--readonly {
  border-color: rgba(255, 255, 255, 0.08);
}
.body--dark .upload-panel--drag {
  border-color: #80cbc4;
  background: #2a2f2e;
}
.body--dark .upload-panel__head--border {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}
.body--dark .upload-panel__label {
  color: rgba(255, 255, 255, 0.55);
}
.body--dark .upload-panel__dropzone:hover,
.body--dark .upload-panel__dropzone:focus-visible,
.body--dark .upload-panel--drag .upload-panel__dropzone {
  background: #2a2f2e;
}
.body--dark .upload-panel__title {
  color: rgba(255, 255, 255, 0.85);
}
.body--dark .upload-panel__dropzone--compact {
  color: #80cbc4;
}
.body--dark .upload-panel__list {
  border-top-color: rgba(255, 255, 255, 0.08);
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
</style>
