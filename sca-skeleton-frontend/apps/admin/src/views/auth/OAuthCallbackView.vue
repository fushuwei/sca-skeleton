<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { consumePkceSession, exchangeAuthorizationCode } from "@repo/shared";
import { getAdminOAuthConfig } from "../../config/oauth";
import { useAuthStore } from "../../stores/auth";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const errorMessage = ref("");
const processing = ref(true);

onMounted(async () => {
  const oauthConfig = getAdminOAuthConfig();
  const code = typeof route.query.code === "string" ? route.query.code : "";
  const state = typeof route.query.state === "string" ? route.query.state : "";
  const pkceSession = consumePkceSession(oauthConfig.clientId);

  if (!code || !pkceSession) {
    errorMessage.value = "授权回调参数无效，请重新登录。";
    processing.value = false;
    return;
  }
  if (state !== pkceSession.state) {
    errorMessage.value = "state 校验失败，可能存在 CSRF 风险。";
    processing.value = false;
    return;
  }

  try {
    const tokenResponse = await exchangeAuthorizationCode(oauthConfig, code, pkceSession.codeVerifier);
    await authStore.applyOAuthTokens(tokenResponse.access_token, tokenResponse.refresh_token);
    authStore.ensureRoutes(router);
    await authStore.fetchProfile();
    await router.replace(pkceSession.returnUrl || "/dashboard");
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "登录失败";
    processing.value = false;
  }
});
</script>

<template>
  <div class="callback-page">
    <p v-if="processing">正在完成登录，请稍候…</p>
    <p v-else class="error">{{ errorMessage }}</p>
  </div>
</template>

<style scoped>
.callback-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.error {
  color: #b91c1c;
}
</style>
