<script setup lang="ts">
import { ref, reactive, computed, watch } from "vue";
import { useI18n } from "vue-i18n";
import { showToast, isNotificationHandled } from "@repo/shared";
import type { SysDict } from "../../types/auth";
import { createDictApi, updateDictApi } from "../../apis/dict";

const { t } = useI18n({ useScope: "global" });

const props = defineProps<{
  mode: "add" | "edit" | "view";
  dict?: SysDict;
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
  code: "",
  status: "enabled",
  remark: ""
});

// ── 状态选项 ──
const statusOptions = computed(() => [
  { label: t("dictMgmt.statusEnabled"), value: "enabled" },
  { label: t("dictMgmt.statusDisabled"), value: "disabled" }
]);

const formRules = computed(() => ({
  name: [(v: string) => !!v?.trim() || t("dictMgmt.nameRequired")],
  code: [(v: string) => !!v?.trim() || t("dictMgmt.codeRequired")],
  status: [(v: string) => !!v?.trim() || t("dictMgmt.statusRequired")]
}));

function resetForm() {
  form.id = "";
  form.name = "";
  form.code = "";
  form.status = "enabled";
  form.remark = "";
}

function initForm() {
  resetForm();
  if (props.dict) {
    form.id = props.dict.id;
    form.name = props.dict.name;
    form.code = props.dict.code;
    form.status = props.dict.status;
    form.remark = props.dict.remark || "";
  }
}

watch(() => props.dict, initForm, { immediate: true });

function handleClose() {
  emit("close");
}

async function handleSave() {
  if (drawerReadonly.value) return;

  const data: Record<string, unknown> = {
    name: form.name,
    code: form.code,
    status: form.status,
    remark: form.remark || undefined
  };

  try {
    formLoading.value = true;
    let result;
    if (props.mode === "add") {
      result = await createDictApi(data);
    } else {
      data.id = form.id;
      data.version = props.dict?.version;
      result = await updateDictApi(data);
    }

    if (result.code === 10_000) {
      showToast(t("dictMgmt.saveSuccess"), "positive");
      emit("saved");
    } else {
      showToast(result.message || t("dictMgmt.saveFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("dictMgmt.saveFail"), "negative");
    }
  } finally {
    formLoading.value = false;
  }
}
</script>

<template>
  <div class="dict-drawer-content">
    <!-- 中间内容区（唯一滚动区）：表单字段 -->
    <q-form id="dict-drawer-form" class="dict-drawer-form dict-drawer-main" @submit="handleSave">
      <div class="row q-col-gutter-md">
        <!-- 字典名称 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.name"
            :label="t('dictMgmt.name')"
            filled
            square
            :rules="formRules.name"
            :disable="drawerReadonly"
            :readonly="drawerReadonly"
            hide-bottom-space
            class="required-field"
          />
        </div>
        <!-- 字典编码 -->
        <div class="col-12 col-md-6">
          <q-input
            v-model.trim="form.code"
            :label="t('dictMgmt.code')"
            filled
            square
            :rules="formRules.code"
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
            :label="t('dictMgmt.status')"
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
        <!-- 备注 -->
        <div class="col-12">
          <q-input
            v-model="form.remark"
            :label="t('dictMgmt.remark')"
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
    <div v-if="!drawerReadonly" class="dict-drawer-footer row items-center justify-end no-wrap">
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
        form="dict-drawer-form"
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
.dict-drawer-content {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.dict-drawer-main {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 16px 12px;
}

.dict-drawer-footer {
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
.body--dark .dict-drawer-form .q-field__control {
  background: #2d2d2d;
}

.body--dark .dict-drawer-form .q-field__native,
.body--dark .dict-drawer-form .q-field__prefix,
.body--dark .dict-drawer-form .q-field__suffix {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .dict-drawer-form .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .dict-drawer-form .q-field--focused .q-field__label {
  color: #80cbc4;
}

.body--dark .dict-drawer-form .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .dict-drawer-form .q-field--focused .q-field__control::after {
  border-color: #80cbc4;
}

.body--dark .dict-drawer-footer {
  border-top-color: rgba(255, 255, 255, 0.08);
}
</style>
