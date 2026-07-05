<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { useI18n } from "vue-i18n";

const props = defineProps<{
  modelValue: string;
  label?: string;
  disable?: boolean;
  readonly?: boolean;
  rules?: unknown[];
}>();

const emit = defineEmits<{
  "update:modelValue": [value: string];
}>();

const { t } = useI18n({ useScope: "global" });

/** QDate v-model（mask="YYYY-MM-DD"，格式 yyyy-MM-dd） */
const dateModel = ref("");
/** QTime v-model（格式 HH:mm） */
const timeModel = ref("");

/** 是否禁用交互（查看模式或禁用模式） */
const isDisabled = computed(() => props.disable || props.readonly);

/** 仅在可交互时为原生 input 添加自定义 class（隐藏光标、指针手型） */
const nativeInputClass = computed(() =>
  isDisabled.value ? "" : "datetime-picker-native-input"
);

/**
 * 从外部 modelValue（yyyy-MM-dd HH:mm:ss）同步到内部 date/time。
 * 兼容 "yyyy-MM-dd HH:mm:ss" / "yyyy-MM-dd HH:mm" / "yyyy-MM-ddTHH:mm" 等历史格式。
 */
watch(
  () => props.modelValue,
  (val) => {
    if (!val) {
      dateModel.value = "";
      timeModel.value = "";
      return;
    }
    const normalized = val.replace("T", " ");
    const [d, tm] = normalized.split(" ");
    dateModel.value = d || "";
    timeModel.value = tm ? tm.substring(0, 5) : "00:00";
  },
  { immediate: true }
);

/** 合并 date + time 并 emit（格式 yyyy-MM-dd HH:mm:ss） */
function emitValue(date: string, time: string) {
  if (!date) {
    emit("update:modelValue", "");
    return;
  }
  const timeStr = time || "00:00";
  // QTime 输出 HH:mm，补全秒位为 HH:mm:ss 以匹配后端 LocalDateTime 格式
  const fullTime = timeStr.length === 5 ? `${timeStr}:00` : timeStr;
  emit("update:modelValue", `${date} ${fullTime}`);
}

function onDateUpdate(val: string | null) {
  dateModel.value = val || "";
  emitValue(dateModel.value, timeModel.value);
}

function onTimeUpdate(val: string | null) {
  timeModel.value = val || "00:00";
  emitValue(dateModel.value, timeModel.value);
}

function onClear() {
  dateModel.value = "";
  timeModel.value = "";
  emit("update:modelValue", "");
}

/** 拦截键盘输入，仅允许 Tab / Escape 透传 */
function onKeydown(e: KeyboardEvent) {
  if (e.key === "Tab" || e.key === "Escape") return;
  e.preventDefault();
}
</script>

<template>
  <q-input
    :model-value="modelValue || ''"
    :label="label"
    filled
    square
    hide-bottom-space
    :disable="isDisabled"
    :rules="rules"
    :input-class="nativeInputClass"
    clearable
    @clear="onClear"
    @keydown="onKeydown"
  >
    <template #append>
      <q-icon
        name="sym_r_event"
        :class="{ 'cursor-pointer': !isDisabled }"
      />
    </template>
    <q-menu
      v-if="!isDisabled"
      anchor="bottom left"
      self="top left"
      :offset="[0, 4]"
      no-focus
    >
      <div class="datetime-picker-body">
        <div class="row no-wrap">
          <q-date
            :model-value="dateModel"
            mask="YYYY-MM-DD"
            flat
            today-btn
            @update:model-value="onDateUpdate"
          />
          <q-time
            :model-value="timeModel"
            format24h
            flat
            now-btn
            @update:model-value="onTimeUpdate"
          />
        </div>
      </div>
    </q-menu>
  </q-input>
</template>

<style scoped>
/* 隐藏原生 input 的文本光标，并改为指针手型 */
:deep(.datetime-picker-native-input) {
  caret-color: transparent;
  cursor: pointer;
}

/* QDate 与 QTime 之间的分隔线，使并排布局更有层次 */
.datetime-picker-body :deep(.q-date + .q-time) {
  border-left: 1px solid rgba(0, 0, 0, 0.08);
}
</style>

<style>
/* 暗色模式：QDate 与 QTime 分隔线颜色调整 */
.body--dark .datetime-picker-body .q-date + .q-time {
  border-left-color: rgba(255, 255, 255, 0.08);
}
</style>
