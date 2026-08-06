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
// 而是固定 key/value 表单项：标题为友好中文名，并预填 HikariCP 官方默认值。
interface PoolFieldDef {
  /** 提交给后端的配置键（HikariCP 属性名） */
  key: string;
  /** 标题文案 i18n key */
  labelKey: string;
}

const POOL_FIELDS: PoolFieldDef[] = [
  { key: "maximumPoolSize", labelKey: "datasourceMgmt.poolMaximumPoolSize" },
  { key: "minimumIdle", labelKey: "datasourceMgmt.poolMinimumIdle" },
  { key: "connectionTimeout", labelKey: "datasourceMgmt.poolConnectionTimeout" },
  { key: "idleTimeout", labelKey: "datasourceMgmt.poolIdleTimeout" },
  { key: "maxLifetime", labelKey: "datasourceMgmt.poolMaxLifetime" }
];

/** HikariCP 官方默认值：预填到表单，让用户直观看到实际生效的配置 */
const POOL_DEFAULTS: Record<string, number> = {
  maximumPoolSize: 10,
  minimumIdle: 10,
  connectionTimeout: 30000,
  idleTimeout: 600000,
  maxLifetime: 1800000
};

const poolConfig = reactive<Record<string, number | null>>({
  maximumPoolSize: POOL_DEFAULTS.maximumPoolSize,
  minimumIdle: POOL_DEFAULTS.minimumIdle,
  connectionTimeout: POOL_DEFAULTS.connectionTimeout,
  idleTimeout: POOL_DEFAULTS.idleTimeout,
  maxLifetime: POOL_DEFAULTS.maxLifetime
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

// ── 连接参数：多行 key/value 录入 ──
// 交互模型：末尾始终保留一行空“录入行”——
// - 录入行的参数值只读，输入参数名后解锁（避免产生无名参数）；
// - 录入行参数值一旦填写，自动在末尾追加新的录入行（即“自动新增下一行”）；
// - 任意行的参数名一旦清空，该行自动删除；
// 保存时参数名非空的行序列化为 JSON 对象提交（后端契约不变），
// 并与 JDBC URL 预览保持实时联动。
interface ParamRow {
  key: string;
  value: string;
}

const paramRows = ref<ParamRow[]>([{ key: "", value: "" }]);

/** 高级配置手风琴展开状态（两组独立、可同时展开）：
 *  默认收起以降低高级配置的视觉噪音；已有保存的连接参数时自动展开参数组 */
const paramsExpanded = ref(false);
const poolExpanded = ref(false);

/** 已配置参数个数（不含空录入行），用于手风琴组头摘要徽标 */
const paramRowCount = computed(
  () => paramRows.value.filter((row) => row.key.trim() !== "").length
);

/** 规范化参数行：参数名清空的行自动删除；末尾始终保留唯一一行空录入行 */
function normalizeParamRows() {
  if (readonlyMode.value) return;
  const rows = paramRows.value;
  for (let i = rows.length - 1; i >= 0; i--) {
    const isEmptyKey = rows[i].key.trim() === "";
    const isLast = i === rows.length - 1;
    // 末尾“名空 + 值空”的行是录入行，保留；其余空名行自动删除
    if (isEmptyKey && !(isLast && rows[i].value.trim() === "")) {
      rows.splice(i, 1);
    }
  }
  if (rows.length === 0) {
    rows.push({ key: "", value: "" });
    return;
  }
  // 末尾行名、值均已填写：自动追加新的录入行
  const last = rows[rows.length - 1];
  if (last.key.trim() !== "" && last.value.trim() !== "") {
    rows.push({ key: "", value: "" });
  }
}

/** 参数名规则：禁止破坏 JDBC URL query string 的字符（& = 空白）；行内禁止重名 */
function paramKeyRules(rowIndex: number) {
  return [
    (v: string) =>
      !/[&=\s]/.test(v || "") || t("datasourceMgmt.connectionParamKeyInvalid"),
    (v: string) => {
      const k = (v || "").trim();
      if (!k) return true;
      return (
        paramRows.value.findIndex((r) => r.key.trim() === k) === rowIndex ||
        t("datasourceMgmt.connectionParamKeyDuplicate")
      );
    }
  ];
}

/** 参数是否存在非法（非法字符 / 重名）：校验失败时据此自动展开连接参数组，确保错误提示可见 */
function hasParamErrors(): boolean {
  const seen = new Set<string>();
  for (const row of paramRows.value) {
    if (/[&=\s]/.test(row.key)) return true;
    const k = row.key.trim();
    if (!k) continue;
    if (seen.has(k)) return true;
    seen.add(k);
  }
  return false;
}

/** 解析已保存的连接参数为行：优先 JSON 对象，兜底兼容历史 query string（k1=v1&k2=v2） */
function parseStoredConnectionParams(raw: string): ParamRow[] {
  const trimmed = (raw || "").trim();
  if (!trimmed) return [];
  if (trimmed.startsWith("{")) {
    try {
      const obj = JSON.parse(trimmed) as Record<string, unknown>;
      return Object.entries(obj).map(([k, v]) => ({ key: k, value: String(v ?? "") }));
    } catch {
      // 解析失败落入 query string 解析兜底
    }
  }
  return trimmed
    .split("&")
    .map((pair) => {
      const idx = pair.indexOf("=");
      return idx === -1
        ? { key: pair.trim(), value: "" }
        : { key: pair.slice(0, idx).trim(), value: pair.slice(idx + 1).trim() };
    })
    .filter((r) => r.key !== "");
}

/** 参数行序列化为 JSON 对象字符串；无有效行时返回 undefined（不填即为 null） */
function buildConnectionParamsJson(): string | undefined {
  const entries: Record<string, string> = {};
  for (const row of paramRows.value) {
    const k = row.key.trim();
    if (k) {
      entries[k] = row.value.trim();
    }
  }
  return Object.keys(entries).length > 0 ? JSON.stringify(entries) : undefined;
}

function resetForm() {
  form.id = "";
  form.name = "";
  form.driverId = "";
  form.host = "";
  form.port = DEFAULT_PORT;
  form.databaseName = "";
  form.username = "";
  form.password = "";
  form.version = 0;
  paramRows.value = [{ key: "", value: "" }];
  paramsExpanded.value = false;
  poolExpanded.value = false;
  for (const key of Object.keys(poolConfig)) {
    poolConfig[key] = POOL_DEFAULTS[key];
  }
  showPwd.value = false;
}

// ── 脏检查：初始快照 vs 当前值 ──
let initialSnapshot = "";

function snapshot(): string {
  return JSON.stringify({ form, poolConfig, paramRows: paramRows.value });
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
    // 连接参数：库内 JSON 对象解析回行；编辑态末尾补一行空录入行，查看态仅展示已存行
    const paramDataRows = parseStoredConnectionParams(props.datasource.connectionParams || "");
    paramRows.value = readonlyMode.value
      ? paramDataRows
      : [...paramDataRows, { key: "", value: "" }];
    // 已有保存的参数时自动展开“连接参数”组，便于直观核对配置
    paramsExpanded.value = paramDataRows.length > 0;
    // 连接池配置：库内为 JSON 字符串，解析回固定字段；缺失或非法的键回退默认值，
    // 白名单外的历史配置项不展示（保存时丢弃）
    for (const key of Object.keys(poolConfig)) {
      poolConfig[key] = POOL_DEFAULTS[key];
    }
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
        // 解析失败保持默认值
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

/** 连接池字段输入过滤：仅保留数字；清空时保存会回退默认值 */
function onPoolInput(key: string, v: string | number | null) {
  const digits = String(v ?? "").replace(/\D/g, "");
  poolConfig[key] = digits ? Number(digits) : null;
}

// ── JDBC URL 实时预览（与后端 Dialect.buildJdbcUrl + toQueryString 逻辑保持一致） ──

/** 连接参数 → URL query string：按行顺序拼接，跳过参数名为空的行 */
const paramQueryString = computed(() =>
  paramRows.value
    .filter((row) => row.key.trim() !== "")
    .map((row) => `${row.key.trim()}=${row.value.trim()}`)
    .join("&")
);

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
  const queryString = paramQueryString.value;
  if (queryString) {
    url += `?${queryString}`;
  }
  return url;
});

// ── 契约方法（defineExpose） ──
async function validate(): Promise<boolean> {
  if (!formRef.value) return false;
  const ok = await formRef.value.validate();
  // 参数非法时自动展开“连接参数”组，确保错误提示可见
  if (!ok && hasParamErrors()) {
    paramsExpanded.value = true;
  }
  return ok;
}

function getPayload(): DatasourceFormPayload {
  // 连接池配置：固定白名单字段全量提交；被清空的字段回退默认值（连接池始终生效，配置总有值）
  const poolEntries = POOL_FIELDS.map((field) => [
    field.key,
    poolConfig[field.key] ?? POOL_DEFAULTS[field.key]
  ] as const);

  const data: Record<string, unknown> = {
    name: form.name,
    dbType: DB_TYPE,
    driverId: form.driverId || undefined,
    host: form.host,
    port: form.port ?? undefined,
    databaseName: form.databaseName || undefined,
    username: form.username,
    connectionParams: buildConnectionParamsJson(),
    poolConfig: Object.fromEntries(poolEntries)
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
      <!-- 高级配置手风琴（连接参数 / 连接池配置）：参照左侧菜单导航的 q-expansion-item 交互范式，
           针对表单场景重新调色——白底圆角卡片、图标徽章、标题+副标题、悬停态与展开主题色强调；
           两组相互独立，可同时展开多个。
           连接参数：多行 key/value 录入（无标题占位符引导），参数名清空即删行，末尾常驻一行空录入行 -->
      <div class="col-12">
        <div class="ds-adv-accordion">
          <!-- 连接参数组 -->
          <q-expansion-item
            v-model="paramsExpanded"
            dense-toggle
            class="ds-adv-item"
            expand-icon="sym_r_expand_more"
            expand-icon-class="ds-adv-chevron"
          >
            <template #header>
              <div class="ds-adv-head">
                <span class="ds-adv-head__icon">
                  <q-icon name="sym_r_tune" size="18px" />
                </span>
                <span class="ds-adv-head__text">
                  <span class="ds-adv-head__label">{{ t('datasourceMgmt.connectionParams') }}</span>
                  <span class="ds-adv-head__caption">{{ t('datasourceMgmt.connectionParamsCaption') }}</span>
                </span>
                <span v-if="paramRowCount > 0" class="ds-adv-head__badge">
                  {{ t('datasourceMgmt.paramsConfiguredCount', { n: paramRowCount }) }}
                </span>
              </div>
            </template>
            <div class="ds-adv-body">
              <div v-for="(row, index) in paramRows" :key="index" class="row q-col-gutter-x-md ds-param-row">
                <div class="col-6">
                  <q-input
                    v-model="row.key"
                    filled
                    square
                    :placeholder="t('datasourceMgmt.connectionParamKeyPlaceholder')"
                    :rules="readonlyMode ? [] : paramKeyRules(index)"
                    :readonly="readonlyMode"
                    hide-bottom-space
                    :class="{ 'ds-param-borderless': readonlyMode }"
                    @update:model-value="normalizeParamRows"
                  />
                </div>
                <div class="col-6">
                  <q-input
                    v-model="row.value"
                    filled
                    square
                    :placeholder="t('datasourceMgmt.connectionParamValuePlaceholder')"
                    :readonly="readonlyMode || row.key.trim() === ''"
                    hide-bottom-space
                    :class="{ 'ds-param-borderless': readonlyMode || row.key.trim() === '' }"
                    @update:model-value="normalizeParamRows"
                  />
                </div>
              </div>
            </div>
          </q-expansion-item>
          <q-separator />
          <!-- 连接池配置组：固定白名单字段，预填 HikariCP 官方默认值 -->
          <q-expansion-item
            v-model="poolExpanded"
            dense-toggle
            class="ds-adv-item"
            expand-icon="sym_r_expand_more"
            expand-icon-class="ds-adv-chevron"
          >
            <template #header>
              <div class="ds-adv-head">
                <span class="ds-adv-head__icon">
                  <q-icon name="sym_r_speed" size="18px" />
                </span>
                <span class="ds-adv-head__text">
                  <span class="ds-adv-head__label">{{ t('datasourceMgmt.poolSectionTitle') }}</span>
                  <span class="ds-adv-head__caption">{{ t('datasourceMgmt.poolConfigCaption') }}</span>
                </span>
              </div>
            </template>
            <div class="ds-adv-body">
              <div class="row q-col-gutter-md">
                <div v-for="field in POOL_FIELDS" :key="field.key" class="col-6">
                  <q-input
                    :model-value="poolConfig[field.key]"
                    @update:model-value="(v) => onPoolInput(field.key, v)"
                    @keydown="onNumericKeydown"
                    type="text"
                    inputmode="numeric"
                    :label="t(field.labelKey)"
                    filled
                    square
                    :disable="readonlyMode"
                    :readonly="readonlyMode"
                    hide-bottom-space
                  />
                </div>
              </div>
            </div>
          </q-expansion-item>
        </div>
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

/* 只读态参数输入框：与 JDBC URL 预览一致，只保留 filled 灰色填充，
   移除 Quasar filled+readonly 默认的虚线底边 */
.ds-param-borderless.q-field--filled :deep(.q-field__control::before),
.ds-param-borderless.q-field--filled :deep(.q-field__control::after) {
  border: none;
}

/* 高级配置手风琴：一张白底圆角卡片内嵌两个独立的 q-expansion-item 分组（可同时展开多个）。
   交互范式参照左侧菜单导航，配色适配表单场景：
   图标徽章 + 标题/副标题双行头部 + 悬停淡染 + 展开时主题色强调 */
.ds-adv-accordion {
  border: 1px solid #e4e7ec;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}

/* 组头行：加大点击区域，悬停淡染提供可交互反馈 */
.ds-adv-item :deep(.q-expansion-item__container > .q-item) {
  padding: 12px 16px;
  min-height: 56px;
  transition: background 0.2s;
}

.ds-adv-item :deep(.q-expansion-item__container > .q-item:hover) {
  background: rgba(0, 0, 0, 0.025);
}

.ds-adv-head {
  display: flex;
  align-items: center;
  flex: 1 1 auto;
  min-width: 0;
}

/* 图标徽章：主题色淡底圆角方块，与左侧导航的模块图标同构 */
.ds-adv-head__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 30px;
  height: 30px;
  margin-right: 12px;
  border-radius: 8px;
  background: rgba(0, 150, 136, 0.08);
  color: #00796b;
  transition: background 0.2s;
}

.ds-adv-head__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.ds-adv-head__label {
  font-size: 13.5px;
  font-weight: 500;
  line-height: 1.4;
  color: rgba(0, 0, 0, 0.82);
  transition: color 0.2s;
}

.ds-adv-head__caption {
  font-size: 12px;
  line-height: 1.4;
  color: rgba(0, 0, 0, 0.45);
}

/* 摘要徽标：已配置 N 项，收起时仍可见配置状态 */
.ds-adv-head__badge {
  flex: none;
  margin-left: auto;
  margin-right: 8px;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 12px;
  color: #00796b;
  background: rgba(0, 150, 136, 0.1);
  white-space: nowrap;
}

.ds-adv-chevron {
  color: rgba(0, 0, 0, 0.4);
}

/* 展开态强调：标题与箭头切换为主题色，图标徽章底色加深 */
.ds-adv-item.q-expansion-item--expanded .ds-adv-head__label {
  color: #00796b;
}

.ds-adv-item.q-expansion-item--expanded .ds-adv-head__icon {
  background: rgba(0, 150, 136, 0.16);
}

.ds-adv-item.q-expansion-item--expanded .ds-adv-chevron {
  color: #00796b;
}

/* 内容区：与组头以发丝线分隔 */
.ds-adv-body {
  padding: 14px 16px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

/* 连接参数多行之间的纵向间距（行内横向间距由 q-col-gutter-x-md 提供；
   注意不能用 q-col-gutter-md，其纵向 padding 会让行间距叠加过大） */
.ds-param-row + .ds-param-row {
  margin-top: 10px;
}
</style>

<!-- 非 scoped：预览框与高级配置手风琴暗色模式适配 -->
<style>
.body--dark .ds-form-mysql .ds-jdbc-url-preview .q-field__native {
  color: rgba(255, 255, 255, 0.75);
}

/* 手风琴卡片暗色适配 */
.body--dark .ds-form-mysql .ds-adv-accordion {
  background: #252525;
  border-color: rgba(255, 255, 255, 0.08);
}

.body--dark .ds-form-mysql .ds-adv-item .q-expansion-item__container > .q-item:hover {
  background: rgba(255, 255, 255, 0.04);
}

.body--dark .ds-form-mysql .ds-adv-head__label {
  color: rgba(255, 255, 255, 0.85);
}

.body--dark .ds-form-mysql .ds-adv-head__caption {
  color: rgba(255, 255, 255, 0.45);
}

.body--dark .ds-form-mysql .ds-adv-head__icon {
  background: rgba(0, 150, 136, 0.18);
  color: #4db6ac;
}

.body--dark .ds-form-mysql .ds-adv-head__badge {
  background: rgba(0, 150, 136, 0.2);
  color: #4db6ac;
}

.body--dark .ds-form-mysql .ds-adv-chevron {
  color: rgba(255, 255, 255, 0.45);
}

.body--dark .ds-form-mysql .ds-adv-item.q-expansion-item--expanded .ds-adv-head__label,
.body--dark .ds-form-mysql .ds-adv-item.q-expansion-item--expanded .ds-adv-chevron {
  color: #4db6ac;
}

.body--dark .ds-form-mysql .ds-adv-item.q-expansion-item--expanded .ds-adv-head__icon {
  background: rgba(0, 150, 136, 0.28);
}

.body--dark .ds-form-mysql .ds-adv-body {
  border-top-color: rgba(255, 255, 255, 0.08);
}
</style>
