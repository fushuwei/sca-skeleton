<script setup lang="ts">
import { computed, ref, watch } from "vue";

/**
 * 搜索栏专用日期时间选择器。
 *
 * 与 DateTimePicker 的区别：
 * 1. 使用 placeholder 而非 label，与搜索栏其他 q-input 样式一致
 * 2. 使用 dense 紧凑模式
 * 3. 校验失败时不在输入框下方显示红色文字，而是利用 Quasar 原生的 error 图标（感叹号），
 *    悬停时通过 q-tooltip 显示错误信息
 */

/** q-input mask: # = 数字位，强制 yyyy-MM-dd HH:mm:ss 格式，只允许输入数字 */
const INPUT_MASK = "####-##-## ##:##:##";
/** q-date / q-time mask，与后端 JavaTimeModule DATETIME_PATTERN 对齐 */
const PICKER_MASK = "YYYY-MM-DD HH:mm:ss";

defineOptions({
  name: "SearchDateTimePicker"
});

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    placeholder?: string;
    disable?: boolean;
    readonly?: boolean;
    clearable?: boolean;
    /** 最小可选日期时间（同 mask 格式） */
    min?: string;
    /** 最大可选日期时间（同 mask 格式） */
    max?: string;
  }>(),
  {
    modelValue: "",
    clearable: true
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: string];
}>();

const DATE_TIME_REGEX = /^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/;

const popupRef = ref<{ show: () => void }>();
/** 当前校验错误信息（空字符串表示无错误） */
const errorMessage = ref("");

const displayValue = computed({
  get: () => props.modelValue ?? "",
  set: (v: string) => {
    emit("update:modelValue", v ?? "");
    errorMessage.value = validate(v ?? "");
  }
});

/**
 * 监听外部 modelValue 变化（如父组件点击「重置」按钮清空搜索条件），
 * 同步重新校验并刷新 errorMessage，避免输入框已清空但错误图标（感叹号）残留。
 */
watch(
  () => props.modelValue,
  (v) => {
    errorMessage.value = validate(v ?? "");
  }
);

const pickerDisabled = computed(() => props.disable || props.readonly);
const hasError = computed(() => Boolean(errorMessage.value));

/**
 * 暴露校验状态给父组件，便于父组件在触发搜索前拦截非法输入，
 * 避免把已知非法值提交到后端导致类型转换异常。
 */
defineExpose({ hasError, errorMessage });

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

/** 校验日期时间是否合法，返回错误信息字符串（空表示合法） */
function validate(v: string): string {
  if (!v) return "";

  const match = v.match(DATE_TIME_REGEX);
  if (!match) return "格式错误，应为 yyyy-MM-dd HH:mm:ss";

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const hour = Number(match[4]);
  const minute = Number(match[5]);
  const second = Number(match[6]);

  const date = new Date(year, month - 1, day, hour, minute, second);

  if (
    date.getFullYear() !== year ||
    date.getMonth() !== month - 1 ||
    date.getDate() !== day ||
    date.getHours() !== hour ||
    date.getMinutes() !== minute ||
    date.getSeconds() !== second
  ) {
    return "日期时间无效";
  }

  return "";
}
</script>

<template>
  <div>
    <q-input
      v-model="displayValue"
      :placeholder="placeholder"
      filled
      square
      dense
      :mask="INPUT_MASK"
      :disable="disable"
      :readonly="readonly"
      :clearable="clearable && !pickerDisabled"
      hide-bottom-space
      :error="hasError"
      error-message=""
    >
      <template v-if="!pickerDisabled" #append>
        <q-icon name="sym_r_event" class="cursor-pointer" @click="showPicker" />
      </template>
    </q-input>
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

.admin-datetime-picker :deep(.q-date) {
  border-radius: 0;
  border-right: none !important;
  box-shadow: none !important;
}

.admin-datetime-picker :deep(.q-date__calendar-item .q-btn) {
  border-radius: 50% !important;
}

.admin-datetime-picker :deep(.q-time) {
  border-radius: 0;
  border-left: none !important;
  box-shadow: none !important;
}

.body--dark .admin-datetime-picker {
  background: #2d2d2d;
}

/* 隐藏 q-input 底部错误文字区域，仅保留 error 图标 */
:deep(.q-field__bottom) {
  display: none !important;
}
</style>
