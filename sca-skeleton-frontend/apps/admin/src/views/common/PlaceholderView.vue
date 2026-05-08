<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const title = computed(() => String(route.meta?.title ?? "页面"));
</script>

<template>
  <div class="placeholder-page column flex-center q-pa-lg text-center">
    <div class="text-h6 text-weight-medium">
      {{ title }}
    </div>
    <div class="text-body2 text-grey-7 q-mt-sm">
      该页面为布局骨架占位，后续可替换为实际业务实现。
    </div>
  </div>
</template>

<style scoped>
.placeholder-page {
  min-height: 240px;
}
</style>
