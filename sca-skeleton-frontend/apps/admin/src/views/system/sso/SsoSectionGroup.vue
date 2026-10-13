<script setup lang="ts">
/**
 * SSO 配置页手风琴卡片容器（对齐「添加 MySQL 数据源」的 .ds-adv-accordion）。
 *
 * 一张白底圆角卡片内嵌多个独立 SsoSection（q-expansion-item），可同时展开多个；
 * 相邻分组之间用发丝线分隔（参照数据源里显式 <q-separator/> 的视觉，这里用 CSS 自动补齐）。
 */
</script>

<template>
  <div class="sso-accordion">
    <slot />
  </div>
</template>

<style scoped>
.sso-accordion {
  border: 1px solid #e4e7ec;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}

/* 相邻分组之间补发丝分隔线（首个分组上方不加） */
.sso-accordion :deep(.q-expansion-item + .q-expansion-item) {
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}
</style>

<style>
.body--dark .sso-accordion {
  background: #252525;
  border-color: rgba(255, 255, 255, 0.08);
}

.body--dark .sso-accordion .q-expansion-item + .q-expansion-item {
  border-top-color: rgba(255, 255, 255, 0.08);
}
</style>
