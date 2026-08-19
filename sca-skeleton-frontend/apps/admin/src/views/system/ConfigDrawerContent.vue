<script setup lang="ts">
import { ref, reactive, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysConfig } from "../../types/auth";
import { createConfigApi, updateConfigApi } from "../../apis/config";

const { t } = useI18n({ useScope: "global" });

const props = defineProps<{
  mode: "add" | "edit" | "view";
  config?: SysConfig;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");

const formLoading = ref(false);
const form = reactive({
  id: "",
  name: "",
  key: "",
  value: "",
  type: "string",
  status: "enabled",
  remark: ""
});

const statusOptions = computed(() => [
  { label: t("configMgmt.statusEnabled"), value: "enabled" },
  { label: t("configMgmt.statusDisabled"), value: "disabled" }
]);

const typeOptions = computed(() => [
  { label: t("configMgmt.typeString"), value: "string" },
  { label: t("configMgmt.typeNumber"), value: "number" },
  { label: t("configMgmt.typeBoolean"), value: "boolean" },
  { label: t("configMgmt.typeDatetime"), value: "datetime" },
  { label: t("configMgmt.typeJson"), value: "json" }
]);

const formRules = computed(() => ({
  name: [(v: string) => !!v?.trim() || t("configMgmt.nameRequired")],
  key: [(v: string) => !!v?.trim() || t("configMgmt.keyRequired")],
  type: [(v: string) => !!v?.trim() || t("configMgmt.typeRequired")],
  status: [(v: string) => !!v?.trim() || t("configMgmt.statusRequired")]
}));

function resetForm() {
  form.id = "";
  form.name = "";
  form.key = "";
  form.value = "";
  form.type = "string";
  form.status = "enabled";
  form.remark = "";
}

function initForm() {
  resetForm();
  if (props.config) {
    form.id = props.config.id;
    form.name = props.config.name;
    form.key = props.config.key;
    form.value = props.config.value || "";
    form.type = props.config.type || "string";
    form.status = props.config.status || "enabled";
    form.remark = props.config.remark || "";
  }
}

watch(() => props.config, initForm, { immediate: true });

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  const data: Record<string, unknown> = {
    name: form.name,
    key: form.key,
    value: form.value || undefined,
    type: form.type,
    status: form.status,
    remark: form.remark || undefined
  };

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createConfigApi(data);
    } else {
      data.id = form.id;
      data.version = props.config?.version;
      result = await updateConfigApi(data);
    }

    if (result.code === 10_000) {
      showToast(t("configMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("configMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("configMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}
</script>

<template>
  <div class="config-drawer-content">
    <q-form id="config-drawer-form" class="config-drawer-form config-drawer-main" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 配置名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.name"
            :label="t('configMgmt.name')"
            filled
            square
            :rules="formRules.name"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 配置键 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.key"
            :label="t('configMgmt.key')"
            filled
            square
            :rules="formRules.key"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 类型 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.type"
            :label="t('configMgmt.type')"
            :options="typeOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            filled
            square
            :rules="formRules.type"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 状态 -->
        <div class="col-12 col-md-6">
          <q-select
            v-model="form.status"
            :label="t('configMgmt.status')"
            :options="statusOptions"
            option-label="label"
            option-value="value"
            emit-value
            map-options
            filled
            square
            :rules="formRules.status"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 配置值 -->
        <div class="col-12">
          <q-input
            v-model="form.value"
            :label="t('configMgmt.value')"
            filled
            square
            type="textarea"
            rows="3"
            :placeholder="t('configMgmt.valuePlaceholder')"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 备注 -->
        <div class="col-12">
          <q-input
            v-model="form.remark"
            :label="t('configMgmt.remark')"
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
    </q-form>

    <div v-if="!drawerReadonly" class="config-drawer-footer row items-center justify-end no-wrap">
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
        form="config-drawer-form"
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
</template>

<style scoped>
.config-drawer-content {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.config-drawer-main {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 16px 12px;
}

.config-drawer-footer {
  flex-shrink: 0;
  gap: 8px;
  padding: 14px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.drawer-action-btn {
  min-width: 72px;
}

.required-field :deep(.q-field__label::after) {
  content: " *";
  color: var(--q-negative);
}
</style>

<style>
/* 抽屉表单暗色模式 */
.body--dark .config-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .config-drawer-form .q-field__native,
.body--dark .config-drawer-form .q-field__prefix,
.body--dark .config-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .config-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .config-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .config-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .config-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

.body--dark .config-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}

/* 抽屉中 q-select 下拉弹出层暗色 */
.body--dark .config-drawer-form .q-menu .q-item {
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .config-drawer-form .q-menu .q-item.q-item--active {
  background: rgba(0, 121, 107, 0.15) !important;
  color: #80cbc4 !important;
}

.body--dark .config-drawer-form .q-menu {
  background: #2d2d2d !important;
}

.body--dark .config-drawer-form .q-menu .q-item:hover {
  background: rgba(255, 255, 255, 0.04) !important;
}
</style>
