<script setup lang="ts">
/**
 * SSO 配置页可折叠分组项（对齐「添加 MySQL 数据源」高级配置手风琴风格）。
 *
 * - 独立项（不放进 group），展开态由父级 v-model:open 受控，
 *   方便「进入编辑态一键全展开、退出后回到默认收起」；
 * - 头部用自定义 #header：图标徽章 + 标题/副标题双行 + 可选计数徽标；
 * - 主题色为 teal（#00796b / rgba(0,150,136,x)），展开时标题与箭头强调；
 * - 暗色态用非 scoped 的 .body--dark 覆盖（与数据源表单同策略，靠 sso-acc-* 命名隔离）。
 */
defineProps<{
  title: string;
  caption?: string;
  icon?: string;
  badge?: string;
  open?: boolean;
}>();

const emit = defineEmits<{
  "update:open": [value: boolean];
}>();
</script>

<template>
  <q-expansion-item
    :model-value="open"
    dense-toggle
    class="sso-acc-item"
    expand-icon="sym_r_expand_more"
    expand-icon-class="sso-acc-chevron"
    @update:model-value="emit('update:open', Boolean($event))"
  >
    <template #header>
      <div class="sso-acc-head">
        <span v-if="icon" class="sso-acc-head__icon">
          <q-icon :name="icon" size="18px" />
        </span>
        <span class="sso-acc-head__text">
          <span class="sso-acc-head__label">{{ title }}</span>
          <span v-if="caption" class="sso-acc-head__caption">{{ caption }}</span>
        </span>
        <span v-if="badge" class="sso-acc-head__badge">{{ badge }}</span>
      </div>
    </template>
    <div class="sso-acc-body">
      <slot />
    </div>
  </q-expansion-item>
</template>

<style scoped>
/* 组头行：加大点击区域，悬停淡染提供可交互反馈 */
.sso-acc-item :deep(.q-expansion-item__container > .q-item) {
  padding: 12px 16px;
  min-height: 56px;
  transition: background 0.2s;
}

.sso-acc-item :deep(.q-expansion-item__container > .q-item:hover) {
  background: rgba(0, 0, 0, 0.025);
}

.sso-acc-head {
  display: flex;
  align-items: center;
  flex: 1 1 auto;
  min-width: 0;
}

/* 图标徽章：主题色淡底圆角方块 */
.sso-acc-head__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 30px;
  height: 30px;
  margin-right: 12px;
  border-radius: 8px;
  background: rgba(0, 150, 136, 0.08);
  color: #00796b;
  transition: background 0.2s;
}

.sso-acc-head__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.sso-acc-head__label {
  font-size: 13.5px;
  font-weight: 500;
  line-height: 1.4;
  color: rgba(0, 0, 0, 0.82);
  transition: color 0.2s;
}

.sso-acc-head__caption {
  font-size: 12px;
  line-height: 1.4;
  color: rgba(0, 0, 0, 0.45);
}

/* 计数徽标：收起时也能看到配置状态 */
.sso-acc-head__badge {
  flex: none;
  margin-left: auto;
  margin-right: 8px;
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 12px;
  color: #00796b;
  background: rgba(0, 150, 136, 0.1);
  white-space: nowrap;
}

.sso-acc-chevron {
  color: rgba(0, 0, 0, 0.4);
}

/* 展开态强调：标题与箭头切主题色，图标徽章底色加深 */
.sso-acc-item.q-expansion-item--expanded .sso-acc-head__label {
  color: #00796b;
}

.sso-acc-item.q-expansion-item--expanded .sso-acc-head__icon {
  background: rgba(0, 150, 136, 0.16);
}

.sso-acc-item.q-expansion-item--expanded .sso-acc-chevron {
  color: #00796b;
}

/* 内容区：与组头以发丝线分隔 */
.sso-acc-body {
  padding: 14px 16px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}
</style>

<style>
/* 暗色态（非 scoped，靠 sso-acc-* 命名隔离避免全局污染） */
.body--dark .sso-acc-item :deep(.q-expansion-item__container > .q-item),
.body--dark .sso-acc-item .q-expansion-item__container > .q-item {
  transition: background 0.2s;
}

.body--dark .sso-acc-item .q-expansion-item__container > .q-item:hover {
  background: rgba(255, 255, 255, 0.04);
}

.body--dark .sso-acc-head__icon {
  background: rgba(0, 150, 136, 0.18);
  color: #4db6ac;
}

.body--dark .sso-acc-head__label {
  color: rgba(255, 255, 255, 0.85);
}

.body--dark .sso-acc-head__caption {
  color: rgba(255, 255, 255, 0.45);
}

.body--dark .sso-acc-head__badge {
  background: rgba(0, 150, 136, 0.2);
  color: #4db6ac;
}

.body--dark .sso-acc-chevron {
  color: rgba(255, 255, 255, 0.45);
}

.body--dark .sso-acc-item.q-expansion-item--expanded .sso-acc-head__label {
  color: #4db6ac;
}

.body--dark .sso-acc-item.q-expansion-item--expanded .sso-acc-head__icon {
  background: rgba(0, 150, 136, 0.28);
}

.body--dark .sso-acc-item.q-expansion-item--expanded .sso-acc-chevron {
  color: #4db6ac;
}

.body--dark .sso-acc-body {
  border-top-color: rgba(255, 255, 255, 0.08);
}
</style>
