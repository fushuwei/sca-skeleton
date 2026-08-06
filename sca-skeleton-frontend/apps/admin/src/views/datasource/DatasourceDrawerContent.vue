<script setup lang="ts">
/**
 * 数据源抽屉编排器（添加 / 编辑 / 查看）。
 *
 * 两步式添加流程：
 * - 第一步：类型选择面板（DatasourceTypeGallery），先确定数据源类型；
 * - 第二步：按注册表（DatasourceFormRegistry）渲染该类型对应的完整表单。
 * 编辑/查看直接进入第二步，类型只读不可变更（后端更新接口同样不接受 dbType）。
 *
 * 编排器职责：步骤切换、类型徽章、保存请求（调用表单契约方法）。
 * 字段与排版完全由各类型表单组件自行负责。
 */
import { ref, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import { useConfirmDialog } from "@repo/ui";
import type { Datasource, DbTypeOption } from "../../apis/datasource";
import {
  createDatasourceApi,
  updateDatasourceApi,
  getDbTypesApi,
  testDatasourceConfigApi
} from "../../apis/datasource";
import DbTypeIcon from "../../components/DbTypeIcon.vue";
import DatasourceTypeGallery from "./forms/DatasourceTypeGallery.vue";
import { resolveDatasourceForm, LAST_SELECTED_DB_TYPE_KEY } from "./forms/DatasourceFormRegistry";
import type { DbTypeCardOption, DatasourceTypeFormExpose } from "./forms/types";

const { t } = useI18n({ useScope: "global" });
const { confirmDialog } = useConfirmDialog();

const props = defineProps<{
  mode: "add" | "edit" | "view";
  datasource?: Datasource;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");

// ── 两步式状态 ──
type DrawerStep = "select-type" | "form";

const step = ref<DrawerStep>(props.mode === "add" ? "select-type" : "form");
const selectedDbType = ref<string>(
  props.mode === "add" ? "" : (props.datasource?.dbType ?? "")
);

// ── 数据库类型选项（异步加载，含降级） ──
const dbTypeOptions = ref<DbTypeCardOption[]>([]);

async function loadDbTypeOptions() {
  try {
    const res = await getDbTypesApi();
    if (res.code === 10_000 && res.data) {
      dbTypeOptions.value = res.data.map((opt: DbTypeOption) => ({
        label: opt.displayName,
        value: opt.name,
        defaultPort: opt.defaultPort
      }));
      return;
    }
  } catch {
    // 降级处理
  }
  dbTypeOptions.value = [
    { label: "MySQL", value: "MYSQL", defaultPort: 3306 },
    { label: "Oracle", value: "ORACLE", defaultPort: 1521 },
    { label: "PostgreSQL", value: "POSTGRESQL", defaultPort: 5432 },
    { label: "SQLServer", value: "SQLSERVER", defaultPort: 1433 },
    { label: "达梦数据库", value: "DAMENG", defaultPort: 5236 },
    { label: "Kingbase", value: "KINGBASE", defaultPort: 54321 },
    { label: "MongoDB", value: "MONGODB", defaultPort: 27017 },
    { label: "ClickHouse", value: "CLICKHOUSE", defaultPort: 8123 },
    { label: "OceanBase", value: "OCEANBASE", defaultPort: 2881 },
    { label: "GaussDB", value: "GAUSSDB", defaultPort: 8000 }
  ];
}

const selectedDbTypeLabel = computed(() =>
  dbTypeOptions.value.find((o) => o.value === selectedDbType.value)?.label
  ?? selectedDbType.value
);

// ── 类型表单（注册表解析 + 契约调用） ──
const formComponent = computed(() =>
  selectedDbType.value ? resolveDatasourceForm(selectedDbType.value) : undefined
);

// 动态组件 ref 类型宽松，经 formApi() 收敛为契约类型
const formRef = ref<unknown>(null);

function formApi(): DatasourceTypeFormExpose | null {
  return (formRef.value as DatasourceTypeFormExpose | null) ?? null;
}

const formLoading = ref(false);
const testLoading = ref(false);

// ── 第一步 → 第二步 ──
function handleSelectType(dbType: string) {
  selectedDbType.value = dbType;
  step.value = "form";
  try {
    localStorage.setItem(LAST_SELECTED_DB_TYPE_KEY, dbType);
  } catch {
    // localStorage 不可用时不影响主流程
  }
}

// ── 第二步 → 第一步（重新选择类型，脏数据需确认） ──
async function handleChangeType() {
  const api = formApi();
  if (api?.isDirty()) {
    try {
      await confirmDialog(t("datasourceMgmt.changeTypeConfirm"));
    } catch {
      return;
    }
  }
  step.value = "select-type";
}

function handleClose() {
  emit("close");
}

/** 测试当前表单配置的连接（不落库）：先过表单校验，再提交连接相关字段 */
async function handleTestConnection() {
  if (drawerReadonly.value) return;
  const api = formApi();
  if (!api) return;

  const valid = await api.validate();
  if (!valid) return;

  const payload = api.getPayload();
  // 编辑模式密码留空时由后端回退库内已保存的密码
  const testData: Record<string, unknown> = {
    id: payload.id,
    dbType: payload.dbType,
    driverId: payload.driverId,
    host: payload.host,
    port: payload.port,
    databaseName: payload.databaseName,
    username: payload.username,
    password: payload.password,
    connectionParams: payload.connectionParams
  };

  try {
    testLoading.value = true;
    const result = await testDatasourceConfigApi(testData);
    if (result.code === 10_000) {
      showToast(t("datasourceMgmt.testSuccess"), "positive");
    } else {
      showToast(result.message || t("datasourceMgmt.testFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("datasourceMgmt.testFail"), "negative");
    }
  } finally {
    testLoading.value = false;
  }
}

async function handleSave() {
  if (drawerReadonly.value) return;
  const api = formApi();
  if (!api) return;

  const valid = await api.validate();
  if (!valid) return;

  const payload = api.getPayload();
  try {
    formLoading.value = true;
    const result = props.mode === "add"
      ? await createDatasourceApi(payload)
      : await updateDatasourceApi(payload);

    if (result.code === 10_000) {
      showToast(t("datasourceMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("datasourceMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("datasourceMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}

// 编辑/查看场景下 datasource 变化时重置步骤
watch(() => props.datasource, (ds) => {
  if (props.mode === "add") return;
  selectedDbType.value = ds?.dbType ?? "";
  step.value = "form";
});

onMounted(() => {
  loadDbTypeOptions();
});
</script>

<template>
  <div class="datasource-drawer-content">
    <!-- ═══ 第一步：选择数据源类型（仅添加模式） ═══ -->
    <template v-if="step === 'select-type'">
      <div class="ds-select-type-header">
        <div class="ds-select-type-title">{{ t('datasourceMgmt.selectDbTypeTitle') }}</div>
        <div class="ds-select-type-hint">{{ t('datasourceMgmt.selectDbTypeHint') }}</div>
      </div>

      <DatasourceTypeGallery :options="dbTypeOptions" @select="handleSelectType" />

      <div class="datasource-drawer-footer row justify-end q-gutter-sm">
        <q-btn
          color="grey-7"
          outline
          no-caps
          class="drawer-action-btn"
          @click="handleClose"
        >
          {{ t('common.cancel') }}
        </q-btn>
      </div>
    </template>

    <!-- ═══ 第二步：类型专属表单 ═══ -->
    <template v-else>
      <!-- 类型展示条：filled 只读文本框（灰色填充、无边框线），内容 = 图标 + 类型名；添加模式附圆形重选图标按钮 -->
      <q-input
        :model-value="selectedDbTypeLabel"
        filled
        square
        readonly
        class="ds-form-type-field q-mb-md"
      >
        <template #prepend>
          <DbTypeIcon :db-type="selectedDbType" :size="22" />
        </template>
        <template #append>
          <q-badge
            v-if="props.mode !== 'add'"
            outline
            color="grey-7"
            :label="t('datasourceMgmt.typeImmutableHint')"
            class="ds-form-type-immutable-badge"
          />
          <q-btn
            v-if="props.mode === 'add'"
            round
            flat
            dense
            color="grey-7"
            icon="sym_r_swap_horiz"
            icon-size="20px"
            @click="handleChangeType"
          >
            <q-tooltip>{{ t('datasourceMgmt.changeType') }}</q-tooltip>
          </q-btn>
        </template>
      </q-input>

      <component
        :is="formComponent"
        v-if="formComponent"
        ref="formRef"
        :key="selectedDbType"
        :mode="props.mode"
        :datasource="props.datasource"
      />
      <div v-else class="ds-form-not-supported">
        {{ t('datasourceMgmt.formNotSupported') }}
      </div>

      <!-- 底部操作区：左侧向导回退（上一步），右侧动作簇（测试连接→取消→确定，确定置右突出） -->
      <div v-if="!drawerReadonly" class="datasource-drawer-footer row items-center justify-between no-wrap">
        <div>
          <q-btn
            v-if="props.mode === 'add'"
            color="grey-7"
            outline
            no-caps
            class="drawer-action-btn"
            @click="handleChangeType"
          >
            {{ t('datasourceMgmt.prevStep') }}
          </q-btn>
        </div>
        <div class="row q-gutter-sm">
          <q-btn
            color="primary"
            outline
            no-caps
            :loading="testLoading"
            class="drawer-action-btn"
            @click="handleTestConnection"
          >
            {{ t('datasourceMgmt.testConnection') }}
          </q-btn>
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
            color="primary"
            unelevated
            no-caps
            :loading="formLoading"
            class="drawer-action-btn"
            @click="handleSave"
          >
            {{ t('common.confirm') }}
          </q-btn>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.datasource-drawer-content {
  padding: 0;
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

/* 第一步标题区 */
.ds-select-type-header {
  margin-bottom: 16px;
}

.ds-select-type-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  margin-bottom: 4px;
}

.ds-select-type-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.5);
}

/* 第二步类型展示条（filled 只读：灰色填充、无任何边框线） */
.ds-form-type-field {
  margin-top: -4px;
}

.ds-form-type-field.q-field--filled :deep(.q-field__control::before),
.ds-form-type-field.q-field--filled :deep(.q-field__control::after) {
  border: none;
}

/* 类型名加粗突出，作为当前步骤的核心标识 */
.ds-form-type-field :deep(.q-field__native) {
  color: #000;
  font-weight: 700;
}

.ds-form-type-immutable-badge {
  font-weight: 400;
  margin-right: 8px;
}

.ds-form-not-supported {
  padding: 40px 0;
  text-align: center;
  color: rgba(0, 0, 0, 0.5);
  font-size: 13px;
}

.datasource-drawer-footer {
  flex-shrink: 0;
  padding: 12px 0 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  margin-top: 16px;
}

.drawer-action-btn {
  min-width: 72px;
}
</style>

<style>
/* 类型表单暗色模式（表单根元素统一挂 .datasource-drawer-form 类） */
.body--dark .datasource-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .datasource-drawer-form .q-field__native,
.body--dark .datasource-drawer-form .q-field__prefix,
.body--dark .datasource-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .datasource-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .datasource-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .datasource-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .datasource-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

/* 编排器暗色模式 */
.body--dark .ds-select-type-title {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .ds-form-type-field .q-field__control {
  background: #2d2d2d;
}

.body--dark .ds-form-type-field .q-field__native {
  color: rgba(255, 255, 255, 0.87);
  font-weight: 700;
}

.body--dark .ds-select-type-hint,
.body--dark .ds-form-not-supported {
  color: rgba(255, 255, 255, 0.5);
}

/* 抽屉底部按钮区域分隔线 */
.body--dark .datasource-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}
</style>
