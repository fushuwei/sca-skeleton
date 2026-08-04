<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { Datasource, DbTypeOption, DriverOption } from "../../apis/datasource";
import {
  createDatasourceApi,
  updateDatasourceApi,
  getDriverOptionsApi,
  getDbTypesApi
} from "../../apis/datasource";
import DbTypeIcon from "../../components/DbTypeIcon.vue";

const { t } = useI18n({ useScope: "global" });

const props = defineProps<{
  mode: "add" | "edit" | "view";
  datasource?: Datasource;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");
const isEdit = computed(() => props.mode === "edit");

// ── 数据库类型选项（异步加载，含降级） ──
const dbTypeOptions = ref<{ label: string; value: string; defaultPort: number }[]>([]);

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
    { label: "OceanBase", value: "OCEANBASE", defaultPort: 2883 },
    { label: "GaussDB", value: "GAUSSDB", defaultPort: 5432 }
  ];
}

// ── 驱动选项（根据 dbType 异步加载） ──
const driverOptions = ref<{ label: string; value: string }[]>([]);
const driverLoading = ref(false);

async function loadDriverOptions(dbType?: string) {
  if (!dbType) {
    driverOptions.value = [];
    return;
  }
  driverLoading.value = true;
  try {
    const res = await getDriverOptionsApi(dbType);
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

const formLoading = ref(false);
const showPwd = ref(false);
const form = reactive({
  id: "",
  name: "",
  dbType: "",
  driverId: "",
  host: "",
  port: 3306 as number,
  databaseName: "",
  username: "",
  password: "",
  connectionParams: "",
  poolConfig: "",
  version: 0 as number | undefined
});

const formRules = computed(() => ({
  name: [(v: string) => !!v?.trim() || t("datasourceMgmt.nameRequired")],
  dbType: [(v: string) => !!v || t("datasourceMgmt.dbTypeRequired")],
  host: [(v: string) => !!v?.trim() || t("datasourceMgmt.hostRequired")],
  port: [(v: number) => !!v || t("datasourceMgmt.portRequired")],
  username: [(v: string) => !!v?.trim() || t("datasourceMgmt.usernameRequired")],
  password: props.mode === "add"
    ? [(v: string) => !!v?.trim() || t("datasourceMgmt.passwordRequired")]
    : []
}));

function resetForm() {
  form.id = "";
  form.name = "";
  form.dbType = "";
  form.driverId = "";
  form.host = "";
  form.port = 3306;
  form.databaseName = "";
  form.username = "";
  form.password = "";
  form.connectionParams = "";
  form.poolConfig = "";
  form.version = 0;
  showPwd.value = false;
}

function initForm() {
  resetForm();
  if (props.datasource) {
    form.id = props.datasource.id;
    form.name = props.datasource.name;
    form.dbType = props.datasource.dbType;
    form.driverId = props.datasource.driverId;
    form.host = props.datasource.host;
    form.port = props.datasource.port ?? 3306;
    form.databaseName = props.datasource.databaseName || "";
    form.username = props.datasource.username;
    form.password = ""; // 编辑时密码留空，表示不修改
    form.connectionParams = props.datasource.connectionParams || "";
    form.poolConfig = props.datasource.poolConfig || "";
    form.version = props.datasource.version;
    // 编辑/查看时加载对应 dbType 的驱动选项
    if (props.datasource.dbType) {
      loadDriverOptions(props.datasource.dbType);
    }
  } else {
    driverOptions.value = [];
  }
}

watch(() => props.datasource, initForm, { immediate: true });

// ── 数据库类型变更：自动填充默认端口 + 重新加载驱动选项 ──
async function onDbTypeChange(dbType: string) {
  if (drawerReadonly.value) return;
  // 自动填充默认端口（仅当端口为空或为旧默认值时）
  const option = dbTypeOptions.value.find((o) => o.value === dbType);
  if (option) {
    form.port = option.defaultPort;
  }
  // 清空已选驱动（不同 dbType 的驱动不通用）
  form.driverId = "";
  await loadDriverOptions(dbType);
}

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  const data: Record<string, unknown> = {
    name: form.name,
    dbType: form.dbType,
    driverId: form.driverId || undefined,
    host: form.host,
    port: form.port,
    databaseName: form.databaseName || undefined,
    username: form.username,
    connectionParams: form.connectionParams || undefined,
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

  try {
    formLoading.value = true;
    const result = props.mode === "add"
      ? await createDatasourceApi(data)
      : await updateDatasourceApi(data);

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

onMounted(() => {
  loadDbTypeOptions();
});
</script>

<template>
  <div class="datasource-drawer-content">
    <q-form class="datasource-drawer-form" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 数据源名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.name"
            :label="t('datasourceMgmt.name')"
            filled
            square
            :rules="formRules.name"
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
            :label="t('datasourceMgmt.dbType')"
            filled
            square
            :options="dbTypeOptions"
            emit-value
            map-options
            :rules="formRules.dbType"
            :disable="isEdit || drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
            @update:model-value="onDbTypeChange"
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
        <!-- 关联驱动 -->
        <div class="col-12">
          <q-select
            v-model="form.driverId"
            :label="t('datasourceMgmt.driverId')"
            filled
            square
            :options="driverOptions"
            emit-value
            map-options
            :loading="driverLoading"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 主机地址 -->
        <div class="col-12 col-md-8">
          <q-input
            v-model.trim="form.host"
            :label="t('datasourceMgmt.host')"
            filled
            square
            :rules="formRules.host"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 端口 -->
        <div class="col-12 col-md-4">
          <q-input
            v-model.number="form.port"
            :label="t('datasourceMgmt.port')"
            filled
            square
            type="number"
            :rules="formRules.port"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 数据库名 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.databaseName"
            :label="t('datasourceMgmt.databaseName')"
            filled
            square
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
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
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 密码 -->
        <div class="col-12">
          <q-input
            v-model="form.password"
            :label="isEdit ? t('datasourceMgmt.password') + '（' + t('datasourceMgmt.passwordEditHint') + '）' : t('datasourceMgmt.password')"
            filled
            square
            :type="showPwd ? 'text' : 'password'"
            :rules="formRules.password"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
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
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
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
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            :hint="t('datasourceMgmt.poolConfigHint')"
            hide-bottom-space
          />
        </div>
      </div>

      <!-- 底部操作按钮 -->
      <div v-if="!drawerReadonly" class="datasource-drawer-footer row justify-end q-gutter-sm">
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
.datasource-drawer-content {
  padding: 0;
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

/* 必填项星号红色高亮 */
.required-field :deep(.q-field__label::after) {
  content: " *";
  color: var(--q-negative);
}

:deep(.q-field__append > .q-icon:not(.text-negative)) {
  transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 修复 prefix 右侧多余间距 */
:deep(.q-field__prefix) {
  padding-right: 0 !important;
}
</style>

<style>
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

/* 抽屉底部按钮区域分隔线 */
.body--dark .datasource-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}
</style>
