<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { consumePkceSession, exchangeAuthorizationCode, startOAuthLogin } from "@repo/shared";
import { getPortalOAuthConfig } from "../../config/oauth";
import { usePortalAuthStore } from "../../stores/auth";

const route = useRoute();
const router = useRouter();
const authStore = usePortalAuthStore();
const errorMessage = ref("");
const processing = ref(true);
const retryCountdown = ref(0);
let countdownTimer: ReturnType<typeof setInterval> | null = null;

/**
 * 防御性剥离 SPA HTML base 前缀，避免 Vue Router 产生双重 base 路径。
 */
function stripBasePrefix(path: string, basePath?: string): string {
  if (!basePath || basePath === "/" || !path) {
    return path;
  }
  const normalizedBase = basePath.endsWith("/") ? basePath : basePath + "/";
  let result = path;
  while (result.startsWith(normalizedBase)) {
    result = result.slice(normalizedBase.length - 1);
  }
  if (!result.startsWith("/")) {
    result = "/" + result;
  }
  return result;
}

async function attemptLogin() {
  errorMessage.value = "";
  processing.value = true;

  const oauthConfig = getPortalOAuthConfig();
  const code = typeof route.query.code === "string" ? route.query.code : "";
  const state = typeof route.query.state === "string" ? route.query.state : "";
  // 按 state 查找 PKCE 会话，state 天然唯一，不需要额外的 state 校验
  const pkceSession = state ? consumePkceSession(state) : null;

  if (!code || !pkceSession) {
    errorMessage.value = "授权回调参数无效，正在重新登录…";
    processing.value = false;
    startRetryCountdown();
    return;
  }

  try {
    const tokenResponse = await exchangeAuthorizationCode(oauthConfig, code, pkceSession.codeVerifier);
    await authStore.applyOAuthTokens(tokenResponse.access_token, tokenResponse.refresh_token);
    await authStore.fetchProfile();
    // 防御性剥离 base 前缀后再传给 router.replace()
    const safeReturnUrl = stripBasePrefix(pkceSession.returnUrl, oauthConfig.basePath) || "/";
    await router.replace(safeReturnUrl);
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
      void startOAuthLogin(getPortalOAuthConfig(), "/");
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
  <main class="callback-page">
    <template v-if="processing">
      <div class="loading-box">
        <div class="spinner"></div>
        <p class="loading-text">正在登录，请稍等...</p>
      </div>
    </template>
    <template v-else>
      <div class="error-box">
        <p class="error-text">{{ errorMessage }}</p>
        <p class="countdown-text">{{ retryCountdown }} 秒后自动重试…</p>
        <button class="retry-btn" @click="() => startOAuthLogin(getPortalOAuthConfig(), '/')">立即重试</button>
      </div>
    </template>
  </main>
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
  box-sizing: content-box;
  width: 40px;
  height: 40px;
  border: 3px solid #e0e0e0;
  border-top-color: #1976d2;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  font-size: 16px;
  color: #666;
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
  background: #059669;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.retry-btn:hover {
  background: #047857;
}
</style>
