<script setup lang="ts">
import { ref, reactive, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysDictData } from "../../types/auth";
import { createDictDataApi, updateDictDataApi } from "../../apis/dict";

const { t } = useI18n({ useScope: "global" });

const props = defineProps<{
  mode: "add" | "edit" | "view";
  dictData?: SysDictData;
  dictId?: string;
}>();

const emit = defineEmits<{
  close: [];
  saved: [];
}>();

const drawerReadonly = computed(() => props.mode === "view");

const formLoading = ref(false);
const form = reactive({
  id: "",
  dictId: "",
  label: "",
  value: "",
  status: "enabled",
  sort: 100,
  remark: ""
});

// ── 状态选项 ──
const statusOptions = computed(() => [
  { label: t("dictDataMgmt.statusEnabled"), value: "enabled" },
  { label: t("dictDataMgmt.statusDisabled"), value: "disabled" }
]);

const formRules = computed(() => ({
  label: [(v: string) => !!v?.trim() || t("dictDataMgmt.labelRequired")],
  value: [(v: string) => !!v?.trim() || t("dictDataMgmt.valueRequired")],
  status: [(v: string) => !!v?.trim() || t("dictDataMgmt.statusRequired")]
}));

function resetForm() {
  form.id = "";
  form.dictId = "";
  form.label = "";
  form.value = "";
  form.status = "enabled";
  form.sort = 100;
  form.remark = "";
}

function initForm() {
  resetForm();
  if (props.dictData) {
    form.id = props.dictData.id;
    form.dictId = props.dictData.dictId;
    form.label = props.dictData.label;
    form.value = props.dictData.value;
    form.status = props.dictData.status;
    form.sort = props.dictData.sort ?? 100;
    form.remark = props.dictData.remark || "";
  } else if (props.dictId) {
    form.dictId = props.dictId;
  }
}

watch(() => [props.dictData, props.dictId], initForm, { immediate: true });

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  const data: Record<string, unknown> = {
    dictId: form.dictId,
    label: form.label,
    value: form.value,
    status: form.status,
    sort: form.sort,
    remark: form.remark || undefined
  };

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createDictDataApi(data);
    } else {
      data.id = form.id;
      result = await updateDictDataApi(data);
    }

    if (result.code === 10_000) {
      showToast(t("dictDataMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("dictDataMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("dictDataMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}
</script>

<template>
  <div class="dict-data-drawer-content">
    <!-- 中间内容区（唯一滚动区）：表单字段 -->
    <q-form id="dict-data-drawer-form" class="dict-data-drawer-form dict-data-drawer-main" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 字典标签 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.label"
            :label="t('dictDataMgmt.label')"
            filled
            square
            :rules="formRules.label"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 字典值 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.value"
            :label="t('dictDataMgmt.value')"
            filled
            square
            :rules="formRules.value"
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
            :label="t('dictDataMgmt.status')"
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
            transition-show="jump-up"
            transition-hide="jump-down"
            popup-content-class="status-select-popup"
          />
        </div>
        <!-- 排序 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.number="form.sort"
            :label="t('dictDataMgmt.sort')"
            filled
            square
            type="number"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
          />
        </div>
        <!-- 备注 -->
        <div class="col-12">
          <q-input
            v-model="form.remark"
            :label="t('dictDataMgmt.remark')"
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

    <!-- 底部操作栏 -->
    <div v-if="!drawerReadonly" class="dict-data-drawer-footer row items-center justify-end no-wrap">
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
        form="dict-data-drawer-form"
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
.dict-data-drawer-content {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.dict-data-drawer-main {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 16px 12px;
}

.dict-data-drawer-footer {
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
.body--dark .dict-data-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .dict-data-drawer-form .q-field__native,
.body--dark .dict-data-drawer-form .q-field__prefix,
.body--dark .dict-data-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .dict-data-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .dict-data-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .dict-data-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .dict-data-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

.body--dark .dict-data-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}
</style>
