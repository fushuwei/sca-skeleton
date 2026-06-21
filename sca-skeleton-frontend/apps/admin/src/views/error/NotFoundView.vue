<script setup lang="ts">
import { useRouter } from "vue-router"; // 导入路由实例访问函数。

const router = useRouter(); // 获取路由实例用于页面跳转。

function goDashboard() { // 定义返回控制台处理函数。
  router.replace("/dashboard"); // 跳转到控制台页面。
} // 结束返回控制台处理函数。
</script>

<template>
  <!-- 渲染 404 页面容器。 -->
  <div class="column items-center justify-center q-gutter-md" style="min-height: 100vh">
    <!-- 404 标题。 -->
    <div class="text-h4">404</div>
    <!-- 404 描述文案。 -->
    <div class="text-body1">页面不存在或已被移除</div>
    <!-- 返回按钮。 -->
    <q-btn color="primary" no-caps unelevated label="返回首页" @click="goDashboard" />
  </div>
</template>
