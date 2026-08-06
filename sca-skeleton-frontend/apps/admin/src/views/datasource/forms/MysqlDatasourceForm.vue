<script setup lang="ts">
/**
 * MySQL 数据源表单（完整、自包含）。
 *
 * 约定：每种数据源类型一个独立表单组件，不做共用字段抽取，
 * 以便各类型自由排版与演进。契约见 ./types.ts。
 */
import { ref, reactive, computed, watch, nextTick } from "vue";
import { useI18n } from "vue-i18n";
import type { QForm } from "quasar";
import type { Datasource, DriverOption } from "../../../apis/datasource";
import { getDriverOptionsApi } from "../../../apis/datasource";
import DbTypeIcon from "../../../components/DbTypeIcon.vue";
import type { DatasourceFormMode, DatasourceFormPayload } from "./types";

const { t } = useI18n({ useScope: "global" });

// ── 本表单固定的数据库类型与默认端口 ──
const DB_TYPE = "MYSQL";
const DEFAULT_PORT = 3306;
/** JDBC URL 前缀默认值（驱动未返回 urlTemplate 时使用） */
const URL_PREFIX_FALLBACK = "jdbc:mysql://";

// ── 连接池配置：固定白名单字段（与后端 HikariCP 白名单一一对应）──
// 普通用户不了解连接池底层配置，故不提供自由 JSON 输入，
// 而是固定 key/value 表单项：标题为友好中文名，留空即使用系统默认值。
interface PoolFieldDef {
  /** 提交给后端的配置键（HikariCP 属性名） */
  key: string;
  /** 标题文案 i18n key */
  labelKey: string;
  /** 占位符：默认值（数字直显或 i18n key） */
  placeholder?: string;
  placeholderKey?: string;
}

const POOL_FIELDS: PoolFieldDef[] = [
  { key: "maximumPoolSize", labelKey: "datasourceMgmt.poolMaximumPoolSize", placeholder: "10" },
  { key: "minimumIdle", labelKey: "datasourceMgmt.poolMinimumIdle", placeholderKey: "datasourceMgmt.poolMinimumIdlePlaceholder" },
  { key: "connectionTimeout", labelKey: "datasourceMgmt.poolConnectionTimeout", placeholder: "30000" },
  { key: "idleTimeout", labelKey: "datasourceMgmt.poolIdleTimeout", placeholder: "600000" },
  { key: "maxLifetime", labelKey: "datasourceMgmt.poolMaxLifetime", placeholder: "1800000" }
];

const poolConfig = reactive<Record<string, number | null>>({
  maximumPoolSize: null,
  minimumIdle: null,
  connectionTimeout: null,
  idleTimeout: null,
  maxLifetime: null
});

const props = defineProps<{
  mode: DatasourceFormMode;
  datasource?: Datasource;
}>();

const readonlyMode = computed(() => props.mode === "view");
const isEdit = computed(() => props.mode === "edit");

// ── 驱动选项（按本类型加载，携带 urlTemplate 供 JDBC URL 预览） ──
const driverOptions = ref<{ label: string; value: string; urlTemplate?: string }[]>([]);
const driverLoading = ref(false);

async function loadDriverOptions() {
  driverLoading.value = true;
  try {
    const res = await getDriverOptionsApi(DB_TYPE);
    if (res.code === 10_000 && res.data) {
      driverOptions.value = res.data.map((d: DriverOption) => ({
        label: d.name,
        value: d.id,
        urlTemplate: d.urlTemplate
      }));
    }
  } catch {
    // 静默失败，下拉为空
  } finally {
    driverLoading.value = false;
  }
}

// ── 表单数据 ──
const formRef = ref<QForm | null>(null);
const showPwd = ref(false);
const form = reactive({
  id: "",
  name: "",
  driverId: "",
  host: "",
  port: DEFAULT_PORT as number | null,
  databaseName: "",
  username: "",
  password: "",
  connectionParams: "",
  version: 0 as number | undefined
});

const formRules = computed(() => ({
  name: [(v: string) => !!v?.trim() || t("datasourceMgmt.nameRequired")],
  driverId: [(v: string) => !!v || t("datasourceMgmt.driverRequired")],
  host: [(v: string) => !!v?.trim() || t("datasourceMgmt.hostRequired")],
  port: [
    (v: number | null) => !!v || t("datasourceMgmt.portRequired"),
    (v: number | null) => (v != null && v >= 1 && v <= 65535) || t("datasourceMgmt.portRangeError")
  ],
  databaseName: [(v: string) => !!v?.trim() || t("datasourceMgmt.databaseNameRequired")],
  username: [(v: string) => !!v?.trim() || t("datasourceMgmt.usernameRequired")],
  password: props.mode === "add"
    ? [(v: string) => !!v?.trim() || t("datasourceMgmt.passwordRequired")]
    : []
}));

function resetForm() {
  form.id = "";
  form.name = "";
  form.driverId = "";
  form.host = "";
  form.port = DEFAULT_PORT;
  form.databaseName = "";
  form.username = "";
  form.password = "";
  form.connectionParams = "";
  form.version = 0;
  for (const key of Object.keys(poolConfig)) {
    poolConfig[key] = null;
  }
  showPwd.value = false;
}

// ── 脏检查：初始快照 vs 当前值 ──
let initialSnapshot = "";

function snapshot(): string {
  return JSON.stringify({ form, poolConfig });
}

function initForm() {
  resetForm();
  if (props.datasource) {
    form.id = props.datasource.id;
    form.name = props.datasource.name;
    form.driverId = props.datasource.driverId;
    form.host = props.datasource.host;
    form.port = props.datasource.port ?? null;
    form.databaseName = props.datasource.databaseName || "";
    form.username = props.datasource.username;
    form.password = ""; // 编辑时密码留空，表示不修改
    form.connectionParams = props.datasource.connectionParams || "";
    // 连接池配置：库内为 JSON 字符串，解析回固定字段；白名单外的历史配置项不展示（保存时丢弃）
    if (props.datasource.poolConfig) {
      try {
        const parsed = JSON.parse(props.datasource.poolConfig) as Record<string, unknown>;
        for (const key of Object.keys(poolConfig)) {
          const v = parsed[key];
          if (typeof v === "number" && v > 0) {
            poolConfig[key] = v;
          }
        }
      } catch {
        // 解析失败视为未配置
      }
    }
    form.version = props.datasource.version;
  }
  void loadDriverOptions();
  initialSnapshot = snapshot();
}

watch(() => props.datasource, initForm, { immediate: true });

// ── 端口 / 连接池数字输入过滤 ──
const portInputRef = ref<{ $el: HTMLElement } | null>(null);

// 第 1 层：keydown 拦截 IME 输入法和非数字字符（端口与各连接池字段共用）
function onNumericKeydown(e: KeyboardEvent) {
  if (e.isComposing || e.keyCode === 229 || e.key === "Process") {
    e.preventDefault();
    return;
  }
  const controlKeys = ["Backspace", "Delete", "Tab", "Escape", "Enter", "Home", "End", "ArrowLeft", "ArrowRight"];
  if (controlKeys.includes(e.key)) return;
  if ((e.ctrlKey || e.metaKey) && /^[acvxzy]$/i.test(e.key)) return;
  if (!/^\d$/.test(e.key)) {
    e.preventDefault();
  }
}

// 第 2 层：@update:model-value 过滤非数字字符
// 第 3 层：当检测到非数字字符时，通过 nextTick 同步原生 input DOM 值
function onPortInput(v: string | number | null) {
  const raw = String(v ?? "");
  const digits = raw.replace(/\D/g, "");
  form.port = digits ? Number(digits) : null;
  if (raw !== digits) {
    nextTick(() => {
      const el = portInputRef.value?.$el?.querySelector?.("input") as HTMLInputElement | null;
      if (el && el.value !== digits) {
        el.value = digits;
      }
    });
  }
}

/** 连接池字段输入过滤：仅保留数字，空值表示不配置（使用系统默认值） */
function onPoolInput(key: string, v: string | number | null) {
  const digits = String(v ?? "").replace(/\D/g, "");
  poolConfig[key] = digits ? Number(digits) : null;
}

// ── JDBC URL 实时预览（与后端 Dialect.buildJdbcUrl + toQueryString 逻辑保持一致） ──

/** 连接参数转 URL query string：JSON 对象逐键拼接，非 JSON 原样使用（同后端 toQueryString） */
function toQueryString(params: string): string {
  const trimmed = params.trim();
  if (!trimmed) return "";
  if (!trimmed.startsWith("{")) return trimmed;
  try {
    const entries = Object.entries(JSON.parse(trimmed) as Record<string, unknown>);
    if (entries.length === 0) return "";
    return entries.map(([k, v]) => `${k}=${v}`).join("&");
  } catch {
    // JSON 解析失败时按原始内容展示，交由保存时后端校验提示
    return trimmed;
  }
}

const jdbcUrlPreview = computed(() => {
  // 未选驱动：显示 MySQL 官方默认 JDBC URL 前缀；选了驱动则以驱动配置的 urlTemplate 为准
  // （同一类型可能存在多种 URL 写法，以所选驱动的配置为准）
  const selected = driverOptions.value.find((d) => d.value === form.driverId);
  let url = selected?.urlTemplate || URL_PREFIX_FALLBACK;
  const host = form.host.trim();
  if (host) {
    url += host;
    if (form.port != null) {
      url += `:${form.port}`;
    }
  }
  const dbName = form.databaseName.trim();
  if (dbName) {
    url += `/${dbName}`;
  }
  const queryString = toQueryString(form.connectionParams || "");
  if (queryString) {
    url += `?${queryString}`;
  }
  return url;
});

// ── 契约方法（defineExpose） ──
async function validate(): Promise<boolean> {
  return formRef.value ? formRef.value.validate() : false;
}

function getPayload(): DatasourceFormPayload {
  // 连接池配置：仅提交已填写的白名单字段，全部留空则不提交（后端用连接池默认值）
  const poolEntries = POOL_FIELDS
    .map((field) => [field.key, poolConfig[field.key]] as const)
    .filter(([, value]) => value != null);

  const data: Record<string, unknown> = {
    name: form.name,
    dbType: DB_TYPE,
    driverId: form.driverId || undefined,
    host: form.host,
    port: form.port ?? undefined,
    databaseName: form.databaseName || undefined,
    username: form.username,
    connectionParams: form.connectionParams || undefined,
    poolConfig: poolEntries.length > 0 ? Object.fromEntries(poolEntries) : undefined
  };
  if (props.mode === "add") {
    data.password = form.password;
  } else {
    data.id = form.id;
    data.version = form.version;
    // 编辑时密码留空表示不修改
    if (form.password) {
      data.password = form.password;
    }
  }
  return data;
}

function isDirty(): boolean {
  return snapshot() !== initialSnapshot;
}

defineExpose({ validate, getPayload, isDirty });
</script>

<template>
  <q-form ref="formRef" class="datasource-drawer-form ds-form-mysql">
    <div class="row q-col-gutter-md">
      <!-- 数据源名称 / 驱动：同行等分 -->
      <div class="col-6">
        <q-input
          v-model.trim="form.name"
          :label="t('datasourceMgmt.name')"
          filled
          square
          :rules="formRules.name"
          :disable="readonlyMode"
          :readonly="readonlyMode"
          hide-bottom-space
          class="required-field"
        />
      </div>
      <div class="col-6">
        <q-select
          v-model="form.driverId"
          :label="t('datasourceMgmt.driverId')"
          filled
          square
          :options="driverOptions"
          emit-value
          map-options
          :rules="formRules.driverId"
          :loading="driverLoading"
          :disable="readonlyMode"
          :readonly="readonlyMode"
          hide-bottom-space
          class="required-field"
        >
          <template v-slot:selected-item="scope">
            <div v-if="scope.opt" class="row items-center no-wrap">
              <DbTypeIcon :db-type="DB_TYPE" :size="20" class="q-mr-xs" />
              <span>{{ scope.opt.label }}</span>
            </div>
          </template>
        </q-select>
      </div>
      <!-- 主机地址 -->
      <div class="col-12 col-md-6">
        <q-input
          v-model.trim="form.host"
          :label="t('datasourceMgmt.host')"
          filled
          square
          :rules="formRules.host"
          :disable="readonlyMode"
          :readonly="readonlyMode"
          hide-bottom-space
          class="required-field"
        />
      </div>
      <!-- 端口 -->
      <div class="col-12 col-md-6">
        <q-input
          ref="portInputRef"
          :model-value="form.port"
          @update:model-value="onPortInput"
          @keydown="onNumericKeydown"
          type="text"
          inputmode="numeric"
          :label="t('datasourceMgmt.port')"
          filled
          square
          :rules="formRules.port"
          :disable="readonlyMode"
          :readonly="readonlyMode"
          hide-bottom-space
          class="required-field"
        />
      </div>
      <!-- 数据库名 -->
      <div class="col-12">
        <q-input
          v-model.trim="form.databaseName"
          :label="t('datasourceMgmt.databaseName')"
          filled
          square
          :rules="formRules.databaseName"
          :disable="readonlyMode"
          :readonly="readonlyMode"
          hide-bottom-space
          class="required-field"
        />
      </div>
      <!-- 用户名 -->
      <div class="col-12 col-md-6">
        <q-input
          v-model.trim="form.username"
          :label="t('datasourceMgmt.username')"
          filled
          square
          :rules="formRules.username"
          :disable="readonlyMode"
          :readonly="readonlyMode"
          hide-bottom-space
          class="required-field"
        />
      </div>
      <!-- 密码 -->
      <div class="col-12 col-md-6">
        <q-input
          v-model="form.password"
          :label="isEdit ? t('datasourceMgmt.password') + '（' + t('datasourceMgmt.passwordEditHint') + '）' : t('datasourceMgmt.password')"
          filled
          square
          :type="showPwd ? 'text' : 'password'"
          :rules="formRules.password"
          :disable="readonlyMode"
          :readonly="readonlyMode"
          hide-bottom-space
          :class="{ 'required-field': props.mode === 'add' }"
        >
          <template #append>
            <q-icon
              :name="showPwd ? 'sym_r_visibility' : 'sym_r_visibility_off'"
              class="cursor-pointer"
              color="grey-7"
              @click="showPwd = !showPwd"
            />
          </template>
        </q-input>
      </div>
      <!-- JDBC URL 预览（filled 只读、不可编辑、无标题无占位符）：
           默认展示 MySQL 官方 URL 前缀，选择驱动后切换为驱动配置的 URL 写法，
           并随主机/端口/库名/连接参数实时拼装 -->
      <div class="col-12">
        <q-input
          :model-value="jdbcUrlPreview"
          filled
          square
          readonly
          autogrow
          hide-bottom-space
          class="ds-jdbc-url-preview"
        />
      </div>
      <!-- 连接参数 -->
      <div class="col-12">
        <q-input
          v-model="form.connectionParams"
          :label="t('datasourceMgmt.connectionParams')"
          filled
          square
          type="textarea"
          rows="2"
          :disable="readonlyMode"
          :readonly="readonlyMode"
          :hint="t('datasourceMgmt.connectionParamsHint')"
          hide-bottom-space
        />
      </div>
      <!-- 连接池配置：固定白名单 key/value，留空使用系统默认值 -->
      <div class="col-12">
        <div class="ds-pool-section row items-center justify-between">
          <div class="ds-pool-section-title">{{ t('datasourceMgmt.poolSectionTitle') }}</div>
          <div class="ds-pool-section-hint">{{ t('datasourceMgmt.poolSectionHint') }}</div>
        </div>
      </div>
      <div v-for="field in POOL_FIELDS" :key="field.key" class="col-6">
        <q-input
          :model-value="poolConfig[field.key]"
          @update:model-value="(v) => onPoolInput(field.key, v)"
          @keydown="onNumericKeydown"
          type="text"
          inputmode="numeric"
          :label="t(field.labelKey)"
          :placeholder="field.placeholderKey ? t(field.placeholderKey) : field.placeholder"
          filled
          square
          :disable="readonlyMode"
          :readonly="readonlyMode"
          hide-bottom-space
        />
      </div>
    </div>
  </q-form>
</template>

<style scoped>
/* 必填项星号红色高亮 */
.required-field :deep(.q-field__label::after) {
  content: " *";
  color: var(--q-negative);
}

:deep(.q-field__append > .q-icon:not(.text-negative)) {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

/* JDBC URL 预览：等宽字体，突出 URL 可读性 */
.ds-jdbc-url-preview :deep(.q-field__native) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12.5px;
}

/* JDBC URL 预览框只保留 filled 灰色填充背景，完全去掉底边线
   （Quasar 默认 filled 有实线底边、filled+readonly 为虚线，均需移除） */
.ds-jdbc-url-preview.q-field--filled :deep(.q-field__control::before),
.ds-jdbc-url-preview.q-field--filled :deep(.q-field__control::after) {
  border: none;
}

/* 连接池配置分区标题行：左侧标题 + 右侧默认值提示 */
.ds-pool-section {
  padding: 4px 0 12px;
}

.ds-pool-section-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.75);
}

.ds-pool-section-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>

<!-- 非 scoped：预览框与连接池分区暗色模式文字颜色 -->
<style>
.body--dark .ds-form-mysql .ds-jdbc-url-preview .q-field__native {
  color: rgba(255, 255, 255, 0.75);
}

.body--dark .ds-form-mysql .ds-pool-section-title {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .ds-form-mysql .ds-pool-section-hint {
  color: rgba(255, 255, 255, 0.45);
}
</style>
