<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { SsoExtraParam } from "../../../types/sso";

/**
 * 统一身份认证扩展参数编辑器（受控组件）。
 * 各校非标准实现常有额外参数，此处提供键值对增删，随表单整体保存。
 */
const props = defineProps<{
  modelValue: SsoExtraParam[];
  readonly?: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: SsoExtraParam[]];
}>();

const { t } = useI18n({ useScope: "global" });

function addRow() {
  emit("update:modelValue", [...props.modelValue, { key: "", value: "" }]);
}

function removeRow(index: number) {
  const next = props.modelValue.filter((_, i) => i !== index);
  emit("update:modelValue", next);
}

function patchRow(index: number, field: keyof SsoExtraParam, value: string) {
  const next = props.modelValue.map((row, i) =>
    i === index ? { ...row, [field]: value } : row
  );
  emit("update:modelValue", next);
}
</script>

<template>
  <div class="extra-params-editor">
    <div
      v-for="(row, index) in modelValue"
      :key="index"
      class="row q-col-gutter-md items-center q-mb-md"
    >
      <div class="col-12 col-sm-5">
        <q-input
          :model-value="row.key"
          :label="t('ssoConfig.extraParamKey')"
          filled
          square
          :disable="readonly"
          :readonly="readonly"
          hide-bottom-space
          @update:model-value="(v: unknown) => patchRow(index, 'key', String(v ?? ''))"
        />
      </div>
      <div class="col-12 col-sm-5">
        <q-input
          :model-value="row.value"
          :label="t('ssoConfig.extraParamValue')"
          filled
          square
          :disable="readonly"
          :readonly="readonly"
          hide-bottom-space
          @update:model-value="(v: unknown) => patchRow(index, 'value', String(v ?? ''))"
        />
      </div>
      <div class="col-12 col-sm-2 flex justify-end">
        <q-btn
          flat
          dense
          round
          size="md"
          color="negative"
          icon="sym_r_delete_outline"
          :disable="readonly"
          @click="removeRow(index)"
        >
          <q-tooltip>{{ t("common.delete") }}</q-tooltip>
        </q-btn>
      </div>
    </div>

    <q-btn
      v-if="!readonly"
      flat
      dense
      no-caps
      color="primary"
      icon="sym_r_add"
      :label="t('ssoConfig.addExtraParam')"
      class="q-mt-xs"
      @click="addRow"
    />
    <div v-else-if="!modelValue.length" class="text-grey-6 q-mt-xs">
      {{ t("ssoConfig.noExtraParam") }}
    </div>
  </div>
</template>

<style scoped>
.extra-params-editor {
  width: 100%;
}
</style>
