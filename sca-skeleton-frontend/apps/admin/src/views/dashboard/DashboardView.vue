<script setup lang="ts">
import { useI18n } from "vue-i18n";

const { t } = useI18n({ useScope: "global" });
</script>

<template>
  <div class="workbench-empty" role="status" aria-live="polite">
    <div class="workbench-empty__inner column items-center text-center">
      <!-- 图标舞台：双层方框错位叠放，营造"正在搭建"的层次感 -->
      <div class="workbench-empty__stage" aria-hidden="true">
        <span class="workbench-empty__ghost workbench-empty__ghost--back"></span>
        <span class="workbench-empty__ghost workbench-empty__ghost--front"></span>
        <span class="workbench-empty__tile">
          <q-icon name="sym_r_rocket_launch" size="44px" />
        </span>
        <span class="workbench-empty__spark workbench-empty__spark--tl"></span>
        <span class="workbench-empty__spark workbench-empty__spark--br"></span>
      </div>

      <!-- 状态徽章：呼吸圆点 + 状态文案 -->
      <div class="workbench-empty__badge">
        <span class="workbench-empty__dot"></span>
        <span>{{ t("dashboard.comingSoonStatus") }}</span>
      </div>

      <!-- 主标题与说明 -->
      <h2 class="workbench-empty__title">{{ t("dashboard.comingSoonTitle") }}</h2>
      <p class="workbench-empty__desc">{{ t("dashboard.comingSoonDesc") }}</p>

      <!-- 装饰分隔线：弱化收尾，避免文案"戛然而止" -->
      <div class="workbench-empty__divider" aria-hidden="true">
        <span></span>
        <span></span>
        <span></span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ═══ 容器：水平垂直居中，填满内容区可视高度 ═══ */
.workbench-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: calc(100vh - 64px - 40px - 16px);
  min-height: 360px;
  padding: 24px;
}

.workbench-empty__inner {
  max-width: 480px;
}

/* ═══ 图标舞台 ═══ */
.workbench-empty__stage {
  position: relative;
  width: 112px;
  height: 112px;
  margin-bottom: 28px;
  animation: workbench-empty-float 4.5s ease-in-out infinite;
}

/* 主体方块：品牌色淡底 + 品牌色图标 */
.workbench-empty__tile {
  position: absolute;
  inset: 12px;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--q-primary);
  background: linear-gradient(135deg, color-mix(in srgb, var(--q-primary) 10%, transparent), color-mix(in srgb, var(--q-primary) 22%, transparent));
  border: 1px solid color-mix(in srgb, var(--q-primary) 24%, transparent);
}

/* 错位叠放的描边方框，营造"搭建中"的层次 */
.workbench-empty__ghost {
  position: absolute;
  inset: 12px;
  border: 1px solid color-mix(in srgb, var(--q-primary) 16%, transparent);
}

.workbench-empty__ghost--back {
  transform: translate(-8px, -8px);
}

.workbench-empty__ghost--front {
  transform: translate(8px, 8px);
  border-style: dashed;
}

/* 点缀火花：两个小方块，一实一虚，呼应主色 */
.workbench-empty__spark {
  position: absolute;
  z-index: 3;
  width: 7px;
  height: 7px;
  background: color-mix(in srgb, var(--q-primary) 55%, transparent);
  animation: workbench-empty-twinkle 2.4s ease-in-out infinite;
}

.workbench-empty__spark--tl {
  top: 2px;
  right: 14px;
}

.workbench-empty__spark--br {
  bottom: 4px;
  left: 8px;
  background: color-mix(in srgb, var(--q-primary) 32%, transparent);
  animation-delay: 1.2s;
}

/* ═══ 状态徽章 ═══ */
.workbench-empty__badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 28px;
  padding: 0 12px;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--q-primary);
  background: color-mix(in srgb, var(--q-primary) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--q-primary) 20%, transparent);
}

/* 呼吸圆点 */
.workbench-empty__dot {
  position: relative;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--q-primary);
}

.workbench-empty__dot::after {
  content: "";
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--q-primary) 35%, transparent);
  animation: workbench-empty-pulse 2s ease-out infinite;
}

/* ═══ 文案 ═══ */
.workbench-empty__title {
  margin: 20px 0 0;
  font-size: 20px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.workbench-empty__desc {
  margin: 12px auto 0;
  max-width: 400px;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(0, 0, 0, 0.54);
}

/* ═══ 装饰分隔线 ═══ */
.workbench-empty__divider {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 28px;
}

.workbench-empty__divider span {
  width: 5px;
  height: 5px;
  background: rgba(0, 0, 0, 0.12);
}

.workbench-empty__divider span:nth-child(2) {
  background: color-mix(in srgb, var(--q-primary) 45%, transparent);
}

/* ═══ 动效 ═══ */
@keyframes workbench-empty-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

@keyframes workbench-empty-pulse {
  0% {
    transform: scale(0.6);
    opacity: 0.8;
  }
  70%,
  100% {
    transform: scale(1.8);
    opacity: 0;
  }
}

@keyframes workbench-empty-twinkle {
  0%,
  100% {
    opacity: 0.35;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .workbench-empty__stage,
  .workbench-empty__spark,
  .workbench-empty__dot::after {
    animation: none;
  }
}

/* ═══ 暗色模式 ═══ */
.body--dark .workbench-empty__title {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .workbench-empty__desc {
  color: rgba(255, 255, 255, 0.6);
}

.body--dark .workbench-empty__divider span {
  background: rgba(255, 255, 255, 0.16);
}
</style>
