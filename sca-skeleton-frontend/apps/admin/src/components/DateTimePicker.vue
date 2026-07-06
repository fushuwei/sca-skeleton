<script setup lang="ts">
import { computed, ref } from "vue";

/** q-input mask: # = 数字位，强制 yyyy-MM-dd HH:mm:ss 格式，只允许输入数字 */
const INPUT_MASK = "####-##-## ##:##:##";
/** q-date / q-time mask，与后端 JavaTimeModule DATETIME_PATTERN 对齐 */
const PICKER_MASK = "YYYY-MM-DD HH:mm:ss";

defineOptions({
  name: "DateTimePicker"
});

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    label?: string;
    disable?: boolean;
    readonly?: boolean;
    clearable?: boolean;
    hideBottomSpace?: boolean;
    /** 最小可选日期时间（同 mask 格式） */
    min?: string;
    /** 最大可选日期时间（同 mask 格式） */
    max?: string;
    rules?: Array<(v: string) => boolean | string>;
  }>(),
  {
    modelValue: "",
    clearable: true,
    hideBottomSpace: true
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: string];
}>();

const popupRef = ref<{ show: () => void }>();

const displayValue = computed({
  get: () => props.modelValue ?? "",
  set: (v: string) => emit("update:modelValue", v ?? "")
});

const pickerDisabled = computed(() => props.disable || props.readonly);

function showPicker() {
  popupRef.value?.show();
}

/** q-date options 限制（dateStr 格式为 YYYY/MM/DD） */
function dateOptions(dateStr: string) {
  const minDate = props.min?.slice(0, 10).replace(/-/g, "/");
  const maxDate = props.max?.slice(0, 10).replace(/-/g, "/");
  if (minDate && dateStr < minDate) return false;
  if (maxDate && dateStr > maxDate) return false;
  return true;
}
</script>

<template>
  <div>
    <q-input
      v-model="displayValue"
      :label="label"
      filled
      square
      :mask="INPUT_MASK"
      :disable="disable"
      :readonly="readonly"
      :clearable="clearable && !pickerDisabled"
      :hide-bottom-space="hideBottomSpace"
      :rules="rules"
    >
      <template v-if="!pickerDisabled" #append>
        <q-icon name="sym_r_event" class="cursor-pointer" @click="showPicker" />
      </template>
    </q-input>
    <!--
      q-popup-proxy 放在外层 div 内（与 q-input 同级），锚定到 div 的 bottom left，
      使弹框上边框与输入框下边框对齐，实现下拉框定位效果。
      anchor/self 是 Quasar q-menu 原生定位属性，无需手动计算。
      no-parent-event: 防止 div 点击触发，通过图标 @click 手动 show()。
      no-focus / no-refocus: 减少 Drawer 内焦点链闪退。
    -->
    <q-popup-proxy
      ref="popupRef"
      anchor="bottom left"
      self="top left"
      no-parent-event
      no-focus
      no-refocus
      transition-show="scale"
      transition-hide="scale"
    >
      <!--
        Firefox 关键：根节点必须 inline-block，禁止 flex column 作为 popup 根，
        否则 Firefox 会算出 0 高度导致弹层不显示。
        用 div 包裹还可规避 QPopupProxy 对 QDate/QTime 强制 cover 的特殊处理。
      -->
      <div class="inline-block admin-datetime-picker">
        <div class="row">
          <q-date
            v-model="displayValue"
            :mask="PICKER_MASK"
            :options="dateOptions"
            today-btn
          />
          <q-time
            v-model="displayValue"
            :mask="PICKER_MASK"
            format24h
            with-seconds
            now-btn
          />
        </div>
      </div>
    </q-popup-proxy>
  </div>
</template>

<style scoped>
.admin-datetime-picker {
  background: var(--q-card-background, #fff);
}

/* 日期选择器：直角，去掉右边框 */
.admin-datetime-picker :deep(.q-date) {
  border-radius: 0;
  border-right: none;
}

/* 时间选择器：直角，去掉左边框 */
.admin-datetime-picker :deep(.q-time) {
  border-radius: 0;
  border-left: none;
}

.body--dark .admin-datetime-picker {
  background: #2d2d2d;
}
</style>
