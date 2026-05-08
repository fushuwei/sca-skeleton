<script setup lang="ts">
import { PROJECT_NAME } from "@repo/shared";
import { SharedTag } from "@repo/ui";
</script>

<template>
  <main class="p-8">
    <h1 class="text-2xl font-bold">
      Portal App
    </h1>
    <p class="mt-2 text-gray-600">
      {{ PROJECT_NAME }}
    </p>
    <SharedTag text="Portal + Shared UI" />
  </main>
</template>
