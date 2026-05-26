<script setup lang="ts">
import { onMounted } from "vue";
import { PROJECT_NAME } from "@repo/shared";
import { SharedTag } from "@repo/ui";
import { usePortalAuthStore } from "../stores/auth";

const authStore = usePortalAuthStore();

onMounted(async () => {
  if (authStore.isLoggedIn && !authStore.profile) {
    await authStore.fetchProfile();
  }
});
</script>

<template>
  <main class="p-8">
    <h1 class="text-2xl font-bold text-emerald-800">
      Portal App
    </h1>
    <p class="mt-2 text-gray-600">
      {{ PROJECT_NAME }} · OAuth2 PKCE
    </p>
    <SharedTag text="Portal + Shared UI" />
    <section v-if="authStore.profile" class="mt-6 rounded-lg border border-emerald-200 bg-emerald-50 p-4">
      <p class="text-sm text-emerald-900">
        已登录：<strong>{{ authStore.profile.nickname }}</strong>（{{ authStore.profile.username }}）
      </p>
      <button
        type="button"
        class="mt-3 rounded bg-emerald-600 px-4 py-2 text-sm text-white"
        @click="authStore.logout()"
      >
        退出登录
      </button>
    </section>
  </main>
</template>
