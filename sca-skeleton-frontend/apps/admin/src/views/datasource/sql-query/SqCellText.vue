<script setup lang="ts">
import { ref } from "vue";
import { useI18n } from "vue-i18n";
import { showToast } from "@repo/shared";

const props = defineProps<{
  value: unknown;
}>();

const { t } = useI18n({ useScope: "global" });

const el = ref<HTMLElement>();
const showTooltip = ref(false);
const copied = ref(false);
let restoreTimer: ReturnType<typeof setTimeout> | undefined;

/** mouseenter 时检测内容是否被 CSS 截断，仅截断时才显示 tooltip */
function onEnter() {
  if (el.value && el.value.scrollWidth > el.value.clientWidth) {
    showTooltip.value = true;
  }
}

function onLeave() {
  showTooltip.value = false;
}

async function copyValue() {
  try {
    await navigator.clipboard.writeText(String(props.value));
    copied.value = true;
    if (restoreTimer) clearTimeout(restoreTimer);
    restoreTimer = setTimeout(() => {
      copied.value = false;
      restoreTimer = undefined;
    }, 2000);
  } catch {
    showToast(t("sqlQuery.copyFail"), "negative");
  }
}
</script>

<template>
  <span
    v-if="value === null || value === undefined"
    class="sq-null"
  >NULL</span>
  <span v-else class="sq-cell-wrap">
    <span
      ref="el"
      class="sq-cell-text"
      @mouseenter="onEnter"
      @mouseleave="onLeave"
    >{{ String(value) }}<q-tooltip
        no-parent-event
        v-model="showTooltip"
        anchor="top middle"
        self="bottom middle"
        max-width="600px"
        content-style="max-height: 400px; overflow: auto; word-break: break-all; white-space: pre-wrap;"
      >{{ String(value) }}</q-tooltip></span>
    <q-btn
      flat
      dense
      round
      size="8px"
      :icon="copied ? 'sym_r_check' : 'sym_r_content_copy'"
      :class="['sq-cell-copy', { 'sq-cell-copy--done': copied }]"
      @click.stop="copyValue"
    >
      <q-tooltip>{{ copied ? t('sqlQuery.copySuccess') : t('common.copy') }}</q-tooltip>
    </q-btn>
  </span>
</template>

<style scoped>
.sq-cell-wrap {
  display: inline-flex;
  align-items: center;
  max-width: 300px;
}

.sq-cell-text {
  display: inline-block;
  max-width: 270px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.sq-cell-copy {
  flex-shrink: 0;
  margin-left: 2px;
  color: rgba(0, 0, 0, 0.35);
}

.sq-cell-copy:hover {
  color: rgba(0, 0, 0, 0.7);
}

.sq-cell-copy--done {
  color: #2e7d32 !important;
}

.sq-null {
  font-style: italic;
  color: rgba(0, 0, 0, 0.35);
}

.body--dark .sq-null {
  color: rgba(255, 255, 255, 0.35) !important;
}

.body--dark .sq-cell-copy {
  color: rgba(255, 255, 255, 0.35);
}

.body--dark .sq-cell-copy:hover {
  color: rgba(255, 255, 255, 0.7);
}

.body--dark .sq-cell-copy--done {
  color: #66bb6a !important;
}
</style>
