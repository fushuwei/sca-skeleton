<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { consumePkceSession, exchangeAuthorizationCode } from "@repo/shared";
import { getPortalOAuthConfig } from "../../config/oauth";
import { usePortalAuthStore } from "../../stores/auth";

const route = useRoute();
const router = useRouter();
const authStore = usePortalAuthStore();
const errorMessage = ref("");
const processing = ref(true);

onMounted(async () => {
  const oauthConfig = getPortalOAuthConfig();
  const code = typeof route.query.code === "string" ? route.query.code : "";
  const state = typeof route.query.state === "string" ? route.query.state : "";
  const pkceSession = consumePkceSession(oauthConfig.clientId);

  if (!code || !pkceSession) {
    errorMessage.value = "授权回调参数无效，请重新登录。";
    processing.value = false;
    return;
  }
  if (state !== pkceSession.state) {
    errorMessage.value = "state 校验失败。";
    processing.value = false;
    return;
  }

  try {
    const tokenResponse = await exchangeAuthorizationCode(oauthConfig, code, pkceSession.codeVerifier);
    await authStore.applyOAuthTokens(tokenResponse.access_token, tokenResponse.refresh_token);
    await authStore.fetchProfile();
    await router.replace(pkceSession.returnUrl || "/");
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "登录失败";
    processing.value = false;
  }
});
</script>

<template>
  <main class="p-8 text-center">
    <p v-if="processing">正在完成门户登录…</p>
    <p v-else class="text-red-600">{{ errorMessage }}</p>
  </main>
</template>
