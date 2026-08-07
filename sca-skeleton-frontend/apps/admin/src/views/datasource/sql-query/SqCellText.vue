<script setup lang="ts">
import { ref } from "vue";

const props = defineProps<{
  value: unknown;
}>();

const el = ref<HTMLElement>();
const showTooltip = ref(false);

/** mouseenter 时检测内容是否被 CSS 截断，仅截断时才显示 tooltip */
function onEnter() {
  if (el.value && el.value.scrollWidth > el.value.clientWidth) {
    showTooltip.value = true;
  }
}

function onLeave() {
  showTooltip.value = false;
}
</script>

<template>
  <span
    v-if="value === null || value === undefined"
    class="sq-null"
  >NULL</span>
  <span
    v-else
    ref="el"
    class="sq-cell-text"
    @mouseenter="onEnter"
    @mouseleave="onLeave"
  >{{ String(value) }}<q-tooltip
      no-parent-event
      v-model="showTooltip"
      anchor="top middle"
      self="bottom middle"
      :max-width="600"
      content-style="max-height: 400px; overflow: auto; word-break: break-all; white-space: pre-wrap;"
    >{{ String(value) }}</q-tooltip></span>
</template>

<style scoped>
.sq-cell-text {
  display: inline-block;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.sq-null {
  font-style: italic;
  color: rgba(0, 0, 0, 0.35);
}

.body--dark .sq-null {
  color: rgba(255, 255, 255, 0.35) !important;
}
</style>
