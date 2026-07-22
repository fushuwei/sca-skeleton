<script setup lang="ts">
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";

const router = useRouter();
const { t } = useI18n({ useScope: "global" });

function goHome(): void {
  router.push("/portal/home");
}
</script>

<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <div class="not-found-code">404</div>
      <div class="not-found-title">{{ t('error.notFoundTitle') }}</div>
      <div class="not-found-desc">{{ t('error.notFoundDesc') }}</div>
      <q-btn
        unelevated
        no-caps
        color="primary"
        class="not-found-btn"
        :label="t('error.backHome')"
        @click="goHome"
      />
    </div>
  </div>
</template>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.body--dark .not-found-page {
  background: #1a1a1a;
}

.not-found-content {
  text-align: center;
  padding: 40px;
}

.not-found-code {
  font-size: 120px;
  font-weight: 700;
  line-height: 1;
  color: #009688;
  font-family: "JetBrains Mono", monospace;
}

.not-found-title {
  margin-top: 16px;
  font-size: 24px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.body--dark .not-found-title {
  color: rgba(255, 255, 255, 0.92);
}

.not-found-desc {
  margin-top: 8px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.55);
}

.body--dark .not-found-desc {
  color: rgba(255, 255, 255, 0.55);
}

.not-found-btn {
  margin-top: 24px;
  height: 40px;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 0 !important;
}
</style>
