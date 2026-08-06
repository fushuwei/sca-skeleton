<script setup lang="ts">
/**
 * Oracle 数据源表单（完整、自包含）。
 *
 * 约定：每种数据源类型一个独立表单组件，不做共用字段抽取，
 * 以便各类型自由排版与演进。契约见 ./types.ts。
 *
 * Oracle 特有差异点：
 * - 连接方式支持「服务名（Service Name）」与「SID」两种形态：
 *   - 服务名：jdbc:oracle:thin:@//host:port/service
 *   - SID：   jdbc:oracle:thin:@host:port:SID
 * - 连接方式通过 connectionParams JSON 的内置键 oracleUrlMode 持久化
 *   （"SID" 表示 SID 形态；缺省为服务名形态），由后端 OracleDialect 解析组装 URL。
 *   该内置键由表单自动维护，不在连接参数输入框中展示给用户。
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
const DB_TYPE = "ORACLE";
const DEFAULT_PORT = 1521;

/** connectionParams 中标记 SID 形态的内置键 */
const ORACLE_URL_MODE_KEY = "oracleUrlMode";
const ORACLE_URL_MODE_SID = "SID";

type OracleConnectionMode = "SERVICE_NAME" | "SID";

const props = defineProps<{
  mode: DatasourceFormMode;
  datasource?: Datasource;
}>();

const readonlyMode = computed(() => props.mode === "view");
const isEdit = computed(() => props.mode === "edit");

// ── 驱动选项（按本类型加载） ──
const driverOptions = ref<{ label: string; value: string }[]>([]);
const driverLoading = ref(false);

async function loadDriverOptions() {
  driverLoading.value = true;
  try {
    const res = await getDriverOptionsApi(DB_TYPE);
    if (res.code === 10_000 && res.data) {
      driverOptions.value = res.data.map((d: DriverOption) => ({
        label: d.name,
        value: d.id
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
  /** 连接方式：服务名 / SID */
  connectionMode: "SERVICE_NAME" as OracleConnectionMode,
  /** 服务名或 SID 值（随连接方式变化） */
  serviceOrSid: "",
  username: "",
  password: "",
  /** 用户可见的连接参数 JSON（不含内置键 oracleUrlMode） */
  connectionParams: "",
  poolConfig: "",
  version: 0 as number | undefined
});

const connectionModeOptions = computed(() => [
  { label: t("datasourceMgmt.oracleServiceName"), value: "SERVICE_NAME" },
  { label: t("datasourceMgmt.oracleSid"), value: "SID" }
]);

const serviceOrSidLabel = computed(() =>
  form.connectionMode === "SID"
    ? t("datasourceMgmt.oracleSid")
    : t("datasourceMgmt.oracleServiceName")
);

const formRules = computed(() => ({
  name: [(v: string) => !!v?.trim() || t("datasourceMgmt.nameRequired")],
  driverId: [(v: string) => !!v || t("datasourceMgmt.driverRequired")],
  host: [(v: string) => !!v?.trim() || t("datasourceMgmt.hostRequired")],
  port: [
    (v: number | null) => !!v || t("datasourceMgmt.portRequired"),
    (v: number | null) => (v != null && v >= 1 && v <= 65535) || t("datasourceMgmt.portRangeError")
  ],
  serviceOrSid: [
    (v: string) =>
      !!v?.trim()
      || (form.connectionMode === "SID"
        ? t("datasourceMgmt.oracleSidRequired")
        : t("datasourceMgmt.oracleServiceNameRequired"))
  ],
  username: [(v: string) => !!v?.trim() || t("datasourceMgmt.usernameRequired")],
  password: props.mode === "add"
    ? [(v: string) => !!v?.trim() || t("datasourceMgmt.passwordRequired")]
    : [],
  // 连接参数必须为空或合法 JSON（保存时需合并内置键 oracleUrlMode）
  connectionParams: [
    (v: string) => {
      if (!v?.trim()) return true;
      try {
        const parsed = JSON.parse(v);
        return (typeof parsed === "object" && parsed !== null && !Array.isArray(parsed))
          || t("datasourceMgmt.oracleParamsMustBeJson");
      } catch {
        return t("datasourceMgmt.oracleParamsMustBeJson");
      }
    }
  ]
}));

function resetForm() {
  form.id = "";
  form.name = "";
  form.driverId = "";
  form.host = "";
  form.port = DEFAULT_PORT;
  form.connectionMode = "SERVICE_NAME";
  form.serviceOrSid = "";
  form.username = "";
  form.password = "";
  form.connectionParams = "";
  form.poolConfig = "";
  form.version = 0;
  showPwd.value = false;
}

// ── 脏检查：初始快照 vs 当前值 ──
let initialSnapshot = "";

function snapshot(): string {
  return JSON.stringify(form);
}

/**
 * 从已保存的 connectionParams 中拆分出连接方式与用户可见参数：
 * - oracleUrlMode === "SID" → SID 形态
 * - 其余键值还原为 JSON 文本展示给用户
 */
function splitStoredConnectionParams(stored: string): {
  connectionMode: OracleConnectionMode;
  userParamsText: string;
} {
  if (!stored?.trim()) {
    return { connectionMode: "SERVICE_NAME", userParamsText: "" };
  }
  try {
    const parsed = JSON.parse(stored);
    if (typeof parsed === "object" && parsed !== null && !Array.isArray(parsed)) {
      const mode: OracleConnectionMode =
        parsed[ORACLE_URL_MODE_KEY] === ORACLE_URL_MODE_SID ? "SID" : "SERVICE_NAME";
      delete parsed[ORACLE_URL_MODE_KEY];
      const rest = Object.keys(parsed).length > 0 ? JSON.stringify(parsed) : "";
      return { connectionMode: mode, userParamsText: rest };
    }
  } catch {
    // 非 JSON（历史 query string 格式）：原样展示，默认服务名形态
  }
  return { connectionMode: "SERVICE_NAME", userParamsText: stored };
}

function initForm() {
  resetForm();
  if (props.datasource) {
    form.id = props.datasource.id;
    form.name = props.datasource.name;
    form.driverId = props.datasource.driverId;
    form.host = props.datasource.host;
    form.port = props.datasource.port ?? null;
    form.serviceOrSid = props.datasource.databaseName || "";
    form.username = props.datasource.username;
    form.password = ""; // 编辑时密码留空，表示不修改
    const split = splitStoredConnectionParams(props.datasource.connectionParams || "");
    form.connectionMode = split.connectionMode;
    form.connectionParams = split.userParamsText;
    form.poolConfig = props.datasource.poolConfig || "";
    form.version = props.datasource.version;
  }
  void loadDriverOptions();
  initialSnapshot = snapshot();
}

watch(() => props.datasource, initForm, { immediate: true });

// ── 端口输入过滤（三层防护） ──
const portInputRef = ref<{ $el: HTMLElement } | null>(null);

function onPortKeydown(e: KeyboardEvent) {
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

// ── 契约方法（defineExpose） ──
async function validate(): Promise<boolean> {
  return formRef.value ? formRef.value.validate() : false;
}

/**
 * 合并连接方式内置键与用户连接参数：
 * - SID 形态：写入 oracleUrlMode = "SID"
 * - 服务名形态：不写入（缺省即服务名）
 */
function buildConnectionParams(): string | undefined {
  const userObj: Record<string, unknown> = form.connectionParams.trim()
    ? JSON.parse(form.connectionParams)
    : {};
  if (form.connectionMode === "SID") {
    userObj[ORACLE_URL_MODE_KEY] = ORACLE_URL_MODE_SID;
  } else {
    delete userObj[ORACLE_URL_MODE_KEY];
  }
  return Object.keys(userObj).length > 0 ? JSON.stringify(userObj) : undefined;
}

function getPayload(): DatasourceFormPayload {
  const data: Record<string, unknown> = {
    name: form.name,
    dbType: DB_TYPE,
    driverId: form.driverId || undefined,
    host: form.host,
    port: form.port ?? undefined,
    // Oracle 的服务名/SID 落在 databaseName 字段，URL 形态由 connectionParams 的
    // 内置键 oracleUrlMode 标记，后端 OracleDialect 据此组装 JDBC URL
    databaseName: form.serviceOrSid || undefined,
    username: form.username,
    connectionParams: buildConnectionParams(),
    poolConfig: form.poolConfig || undefined
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
  <q-form ref="formRef" class="datasource-drawer-form ds-form-oracle">
    <div class="row q-col-gutter-md">
      <!-- 数据源名称 -->
      <div class="col-12">
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
      <!-- 驱动 -->
      <div class="col-12">
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
          @keydown="onPortKeydown"
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
      <!-- 连接方式（Oracle 特有：服务名 / SID） -->
      <div class="col-12">
        <div class="ds-form-oracle-mode-label">
          {{ t('datasourceMgmt.oracleConnectionMode') }}
          <span class="ds-form-oracle-mode-required">*</span>
        </div>
        <q-option-group
          v-model="form.connectionMode"
          :options="connectionModeOptions"
          color="primary"
          inline
          :disable="readonlyMode"
          class="ds-form-oracle-mode-group"
        />
      </div>
      <!-- 服务名 / SID（标签与校验文案随连接方式变化） -->
      <div class="col-12">
        <q-input
          :key="form.connectionMode"
          v-model.trim="form.serviceOrSid"
          :label="serviceOrSidLabel"
          filled
          square
          :rules="formRules.serviceOrSid"
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
      <!-- 连接参数 -->
      <div class="col-12">
        <q-input
          v-model="form.connectionParams"
          :label="t('datasourceMgmt.connectionParams')"
          filled
          square
          type="textarea"
          rows="2"
          :rules="formRules.connectionParams"
          :disable="readonlyMode"
          :readonly="readonlyMode"
          :hint="t('datasourceMgmt.connectionParamsHint')"
          hide-bottom-space
        />
      </div>
      <!-- 连接池配置 -->
      <div class="col-12">
        <q-input
          v-model="form.poolConfig"
          :label="t('datasourceMgmt.poolConfig')"
          filled
          square
          type="textarea"
          rows="2"
          :disable="readonlyMode"
          :readonly="readonlyMode"
          :hint="t('datasourceMgmt.poolConfigHint')"
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

/* 连接方式单选组标签 */
.ds-form-oracle-mode-label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.6);
  margin-bottom: 4px;
}

.ds-form-oracle-mode-required {
  color: var(--q-negative);
}

.ds-form-oracle-mode-group {
  padding: 4px 0 8px;
}
</style>
