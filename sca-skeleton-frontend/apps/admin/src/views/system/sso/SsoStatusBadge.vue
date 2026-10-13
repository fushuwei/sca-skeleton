<script setup lang="ts">
/**
 * 统一认证配置页头部启用状态徽标。
 *
 * 比纯色 q-badge 更有辨识度：软底药丸 + 前置状态圆点，
 * 已启用为绿色实心点并带扩散脉冲（暗示「正在生效」），未启用为灰色空心点。
 */
import { useI18n } from "vue-i18n";

defineProps<{ enabled: boolean }>();

const { t } = useI18n({ useScope: "global" });
</script>

<template>
  <span
    class="sso-status-pill"
    :class="enabled ? 'sso-status-pill--on' : 'sso-status-pill--off'"
  >
    <span class="sso-status-pill__dot" />
    {{ enabled ? t("ssoConfig.enabledBadge") : t("ssoConfig.disabledBadge") }}
  </span>
</template>

<style scoped>
.sso-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.7;
  white-space: nowrap;
}

.sso-status-pill__dot {
  position: relative;
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

/* 已启用：绿色软底 + 实心点 + 扩散脉冲 */
.sso-status-pill--on {
  color: #1b8a3b;
  background: rgba(33, 186, 69, 0.12);
}

.sso-status-pill--on .sso-status-pill__dot {
  background: #21ba45;
}

.sso-status-pill--on .sso-status-pill__dot::after {
  content: "";
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: #21ba45;
  animation: sso-status-pulse 1.8s ease-out infinite;
}

@keyframes sso-status-pulse {
  0% {
    transform: scale(1);
    opacity: 0.55;
  }
  70%,
  100% {
    transform: scale(2.6);
    opacity: 0;
  }
}

/* 未启用：中性灰软底 + 空心点 */
.sso-status-pill--off {
  color: rgba(0, 0, 0, 0.55);
  background: rgba(0, 0, 0, 0.06);
}

.sso-status-pill--off .sso-status-pill__dot {
  background: transparent;
  box-shadow: inset 0 0 0 1.5px rgba(0, 0, 0, 0.3);
}
</style>

<style>
.body--dark .sso-status-pill--on {
  color: #66bb6a;
  background: rgba(33, 186, 69, 0.18);
}

.body--dark .sso-status-pill--off {
  color: rgba(255, 255, 255, 0.6);
  background: rgba(255, 255, 255, 0.08);
}

.body--dark .sso-status-pill--off .sso-status-pill__dot {
  box-shadow: inset 0 0 0 1.5px rgba(255, 255, 255, 0.4);
}
</style>
