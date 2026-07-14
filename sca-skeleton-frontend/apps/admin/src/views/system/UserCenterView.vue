<script setup lang="ts">
import { computed } from "vue"; // 导入计算属性函数。
import { useAuthStore } from "../../stores/auth"; // 导入鉴权仓库。

const authStore = useAuthStore(); // 获取鉴权仓库实例。
const profile = computed(() => authStore.profile); // 计算当前用户基本信息对象。
</script>

<template>
  <!-- 渲染用户中心容器。 -->
  <q-card flat bordered>
    <!-- 用户中心内容区域。 -->
    <q-card-section>
      <!-- 页面标题文本。 -->
      <div class="text-h6">用户中心</div>
      <!-- 用户 ID 展示。 -->
      <div class="text-body2 q-mt-sm">用户 ID：{{ profile?.id ?? "-" }}</div>
      <!-- 用户名展示。 -->
      <div class="text-body2 q-mt-sm">用户名：{{ profile?.username ?? "-" }}</div>
      <!-- 昵称展示。 -->
      <div class="text-body2 q-mt-sm">昵称：{{ profile?.nickname ?? "-" }}</div>
    </q-card-section>
  </q-card>
</template>
