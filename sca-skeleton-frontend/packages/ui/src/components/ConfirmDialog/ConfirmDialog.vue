<script lang="ts">
export type ConfirmDialogType = "info" | "warning" | "error" | "success" | "confirm";
</script>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed, onBeforeUnmount } from "vue";
import { useDialogPluginComponent } from "quasar";

const props = withDefaults(defineProps<{
  title: string;
  message: string;
  type?: ConfirmDialogType;
  confirmText: string;
  cancelText: string;
}>(), {
  type: "confirm",
});

defineEmits([...useDialogPluginComponent.emits]);

const { dialogRef, onDialogCancel, onDialogOK, onDialogHide } = useDialogPluginComponent();

const cancelBtnRef = ref<{ $el: HTMLButtonElement } | null>(null);
const confirmBtnRef = ref<{ $el: HTMLButtonElement } | null>(null);

const typeConfig = computed(() => {
  const map: Record<ConfirmDialogType, { icon: string; color: string }> = {
    info: { icon: "sym_r_info", color: "#1976d2" },
    warning: { icon: "sym_r_warning", color: "#f59e0b" },
    error: { icon: "sym_r_error", color: "#ef4444" },
    success: { icon: "sym_r_check_circle", color: "#22c55e" },
    confirm: { icon: "sym_r_help", color: "#f59e0b" },
  };
  return map[props.type] ?? map.confirm;
});

/** 将焦点聚焦到"取消"按钮 */
function focusCancel() {
  nextTick(() => {
    cancelBtnRef.value?.$el?.focus();
  });
}

/** 点击对话框任意区域 — 焦点回到"取消"按钮（点击按钮本身除外） */
function handleCardClick(e: MouseEvent) {
  const confirmEl = confirmBtnRef.value?.$el;
  const cancelEl = cancelBtnRef.value?.$el;
  if (confirmEl?.contains(e.target as Node) || cancelEl?.contains(e.target as Node)) {
    return;
  }
  focusCancel();
}

/** 全局 keydown 拦截 — 回车触发"取消"，ESC 关闭对话框（等同取消） */
function handleKeydown(e: KeyboardEvent) {
  if (e.key === "Escape") {
    e.preventDefault();
    e.stopPropagation();
    onDialogCancel();
    return;
  }
  if (e.key !== "Enter") return;
  const confirmEl = confirmBtnRef.value?.$el;
  const cancelEl = cancelBtnRef.value?.$el;
  // 任一按钮拥有焦点时，让浏览器原生行为自然触发
  if (document.activeElement === confirmEl || document.activeElement === cancelEl) {
    return;
  }
  e.preventDefault();
  e.stopPropagation();
  onDialogCancel();
}

onMounted(() => {
  focusCancel();
  document.addEventListener("keydown", handleKeydown, true);
});

onBeforeUnmount(() => {
  document.removeEventListener("keydown", handleKeydown, true);
});
</script>

<template>
  <q-dialog ref="dialogRef" persistent @hide="onDialogHide">
    <q-card class="confirm-dialog-card" @click="handleCardClick">
      <!-- 标题 -->
      <q-card-section class="confirm-dialog-title">
        {{ title }}
      </q-card-section>

      <!-- 正文：icon + 消息（icon 垂直居中） -->
      <q-card-section class="confirm-dialog-body">
        <q-icon
          :name="typeConfig.icon"
          :style="{ color: typeConfig.color }"
          size="32px"
          class="confirm-dialog-icon"
        />
        <div class="confirm-dialog-message">{{ message }}</div>
      </q-card-section>

      <!-- 按钮 -->
      <q-card-actions class="confirm-dialog-actions">
        <q-btn
          ref="cancelBtnRef"
          unelevated
          no-caps
          class="confirm-dialog-cancel-btn"
          @click="onDialogCancel"
        >
          {{ cancelText }}
        </q-btn>
        <q-btn
          ref="confirmBtnRef"
          unelevated
          no-caps
          class="confirm-dialog-confirm-btn"
          @click="onDialogOK"
        >
          {{ confirmText }}
        </q-btn>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<style>
/* ═══ 标题 — 不可选中 ═══ */
.confirm-dialog-card .confirm-dialog-title {
  user-select: none;
  -webkit-user-select: none;
}

/* ═══ 正文 — flex 布局，icon 垂直居中 ═══ */
.confirm-dialog-card .confirm-dialog-body {
  display: flex !important;
  align-items: center !important;
  gap: 16px;
}

.confirm-dialog-card .confirm-dialog-icon {
  flex-shrink: 0;
}

.confirm-dialog-card .confirm-dialog-message {
  flex: 1 1 auto;
  min-width: 0;
  word-break: break-word;
}

/* ═══ 按钮 — 不可选中 ═══ */
.confirm-dialog-card .confirm-dialog-actions .q-btn {
  user-select: none;
  -webkit-user-select: none;
}

/* ═══ 取消按钮 ═══ */
.confirm-dialog-card .confirm-dialog-cancel-btn {
  background: #e7e7e7 !important;
  color: rgba(0, 0, 0, 0.75) !important;
}

.confirm-dialog-card .confirm-dialog-cancel-btn:hover {
  background: #d7d7d7 !important;
}

/* ═══ 确认按钮 ═══ */
.confirm-dialog-card .confirm-dialog-confirm-btn {
  background: #1976d2 !important;
  color: #ffffff !important;
}

.confirm-dialog-card .confirm-dialog-confirm-btn:hover {
  background: #1565c0 !important;
}

/* hover 时清除全局 unelevated brightness 滤镜，避免叠加 */
.confirm-dialog-card .confirm-dialog-cancel-btn:hover .q-btn__wrapper,
.confirm-dialog-card .confirm-dialog-confirm-btn:hover .q-btn__wrapper {
  filter: none !important;
}

/* ═══ 暗色模式 ═══ */
.body--dark .confirm-dialog-card .confirm-dialog-cancel-btn {
  background: #3a3a3a !important;
  color: rgba(255, 255, 255, 0.87) !important;
}

.body--dark .confirm-dialog-card .confirm-dialog-cancel-btn:hover {
  background: #4a4a4a !important;
}

.body--dark .confirm-dialog-card .confirm-dialog-confirm-btn {
  background: rgba(0, 150, 136, 0.85) !important;
  color: #ffffff !important;
}

.body--dark .confirm-dialog-card .confirm-dialog-confirm-btn:hover {
  background: rgba(0, 150, 136, 1) !important;
}
</style>
