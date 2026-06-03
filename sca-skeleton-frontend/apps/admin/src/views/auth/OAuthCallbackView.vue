<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { consumePkceSession, exchangeAuthorizationCode, startOAuthLogin } from "@repo/shared";
import { getAdminOAuthConfig } from "../../config/oauth";
import { useAuthStore } from "../../stores/auth";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const errorMessage = ref("");
const processing = ref(true);
const retryCountdown = ref(0);
let countdownTimer: ReturnType<typeof setInterval> | null = null;

async function attemptLogin() {
  errorMessage.value = "";
  processing.value = true;

  const oauthConfig = getAdminOAuthConfig();
  const code = typeof route.query.code === "string" ? route.query.code : "";
  const state = typeof route.query.state === "string" ? route.query.state : "";
  const pkceSession = consumePkceSession(oauthConfig.clientId);

  if (!code || !pkceSession) {
    errorMessage.value = "授权回调参数无效，正在重新登录…";
    processing.value = false;
    startRetryCountdown();
    return;
  }
  if (state !== pkceSession.state) {
    errorMessage.value = "state 校验失败，可能存在 CSRF 风险。正在重新登录…";
    processing.value = false;
    startRetryCountdown();
    return;
  }

  try {
    const tokenResponse = await exchangeAuthorizationCode(oauthConfig, code, pkceSession.codeVerifier);
    await authStore.applyOAuthTokens(tokenResponse.access_token, tokenResponse.refresh_token);
    authStore.ensureRoutes(router);
    await authStore.fetchProfile();
    await router.replace(pkceSession.returnUrl || "/dashboard");
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "登录失败，正在重试…";
    processing.value = false;
    startRetryCountdown();
  }
}

function startRetryCountdown() {
  retryCountdown.value = 3;
  if (countdownTimer) clearInterval(countdownTimer);
  countdownTimer = setInterval(() => {
    retryCountdown.value--;
    if (retryCountdown.value <= 0) {
      clearInterval(countdownTimer!);
      countdownTimer = null;
      void startOAuthLogin(getAdminOAuthConfig(), "/");
    }
  }, 1000);
}

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer);
});

onMounted(() => {
  void attemptLogin();
});
</script>

<template>
  <div class="callback-page">
    <!-- 加载中：全屏居中 loading -->
    <template v-if="processing">
      <div class="loading-box">
        <div class="spinner"></div>
      </div>
    </template>
    <!-- 错误并倒计时重试 -->
    <template v-else>
      <div class="error-box">
        <p class="error-text">{{ errorMessage }}</p>
        <p class="countdown-text">{{ retryCountdown }} 秒后自动重试…</p>
        <button class="retry-btn" @click="() => startOAuthLogin(getAdminOAuthConfig(), '/')">立即重试</button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.callback-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}

.loading-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #e0e0e0;
  border-top-color: #1976d2;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  text-align: center;
}

.error-text {
  font-size: 16px;
  color: #b91c1c;
}

.countdown-text {
  font-size: 14px;
  color: #999;
}

.retry-btn {
  padding: 8px 20px;
  background: #1976d2;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: #1565c0;
}
</style>
