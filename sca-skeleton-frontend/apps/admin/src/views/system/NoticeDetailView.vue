<script setup lang="ts">
/**
 * 通知公告详情页（新浏览器 tab 打开的独立阅读页）。
 *
 * 由铃铛弹窗「详情」按钮通过 window.open 打开：/notice-detail/:id。
 * 走静态路由（非菜单动态路由），登录守卫正常生效——新 tab 与原页面同源共享
 * localStorage，已登录状态直接放行；未登录则被守卫重定向到登录页。
 */
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { isNotificationHandled, showToast } from "@repo/shared";
import { getNoticeByIdApi } from "../../apis/notice";
import type { SysNotice } from "../../types/auth";
import {
  noticeTypeIconMap,
  noticeTypeColorMap,
  noticeLevelColorMap
} from "../../constants/notification-meta";

const route = useRoute();
const { t } = useI18n({ useScope: "global" });

const loading = ref(false);
const loadFailed = ref(false);
const notice = ref<SysNotice | null>(null);

/** 类型标签文案（noticeMgmt.typeXxx） */
const typeLabel = computed(() => {
  const key = notice.value?.type ?? "other";
  const cap = key.charAt(0).toUpperCase() + key.slice(1);
  return t(`noticeMgmt.type${cap}`);
});

/** 级别徽章颜色（q-badge color） */
const levelColor = computed(() => noticeLevelColorMap[notice.value?.level ?? ""] ?? "grey-6");

/** 级别标签文案（noticeMgmt.levelXxx） */
const levelLabel = computed(() => {
  const key = notice.value?.level ?? "normal";
  const cap = key.charAt(0).toUpperCase() + key.slice(1);
  return t(`noticeMgmt.level${cap}`);
});

/** 发布时间（阅读页展示绝对时间，非相对时间） */
const publishTimeText = computed(() => formatDateTime(notice.value?.publishTime ?? null));

function formatDateTime(timeStr: string | null): string {
  if (!timeStr) return "";
  const date = new Date(timeStr);
  if (Number.isNaN(date.getTime())) return timeStr;
  const p = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())} ${p(date.getHours())}:${p(date.getMinutes())}`;
}

async function loadNotice() {
  const id = String(route.params.id ?? "");
  if (!id) {
    loadFailed.value = true;
    return;
  }
  loading.value = true;
  loadFailed.value = false;
  try {
    const result = await getNoticeByIdApi(id);
    if (result.code === 10_000 && result.data) {
      notice.value = result.data;
      // 新 tab 的标题跟随公告标题
      document.title = result.data.title || document.title;
    } else {
      loadFailed.value = true;
      showToast(result.message || t("common.loadFail"), "negative");
    }
  } catch (error) {
    loadFailed.value = true;
    if (!isNotificationHandled(error)) {
      showToast(t("common.loadFail"), "negative");
    }
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadNotice();
});
</script>

<template>
  <div class="notice-detail-page">
    <!-- 加载中 -->
    <div v-if="loading" class="detail-state">
      <q-spinner-dots size="40px" color="teal" />
    </div>

    <!-- 加载失败 / 无数据 -->
    <div v-else-if="loadFailed || !notice" class="detail-state">
      <q-icon name="sym_r_error" size="56px" />
      <div class="state-title">{{ t("common.loadFail") }}</div>
      <q-btn flat no-caps dense :label="t('common.retry')" color="teal" @click="loadNotice" />
    </div>

    <!-- 正文 -->
    <article v-else class="detail-article">
      <header class="article-header">
        <div class="article-badges">
          <span
            class="type-chip"
            :style="{ backgroundColor: noticeTypeColorMap[notice.type] || noticeTypeColorMap.other }"
          >
            <q-icon
              :name="noticeTypeIconMap[notice.type] || noticeTypeIconMap.other"
              size="14px"
              color="white"
            />
            {{ typeLabel }}
          </span>
          <q-badge v-if="notice.isTop === 1" color="orange-7" rounded dense :label="t('notification.topBadge')" />
          <q-badge v-if="notice.level !== 'normal'" rounded dense :color="levelColor" :label="levelLabel" />
        </div>

        <h1 class="article-title">{{ notice.title }}</h1>

        <div class="article-meta">
          <span v-if="notice.publisherName">{{ notice.publisherName }}</span>
          <span v-if="notice.publisherName" class="meta-dot">·</span>
          <span>{{ publishTimeText }}</span>
        </div>
      </header>

      <q-separator />

      <!-- 富文本正文（管理端富文本编辑器产出） -->
      <div class="rich-content" v-html="notice.content" />
    </article>
  </div>
</template>

<style scoped>
.notice-detail-page {
  min-height: 100vh;
  background: #f5f6f8;
  display: flex;
  justify-content: center;
  padding: 32px 16px 48px;
}

.body--dark .notice-detail-page {
  background: #1d1d1d;
}

/* 加载中 / 失败态：水平垂直居中 */
.detail-state {
  margin-top: 18vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: rgba(0, 0, 0, 0.3);
}

.body--dark .detail-state {
  color: rgba(255, 255, 255, 0.3);
}

.state-title {
  font-size: 15px;
  color: rgba(0, 0, 0, 0.55);
}

.body--dark .state-title {
  color: rgba(255, 255, 255, 0.55);
}

/* 阅读卡片：白底圆角阴影，移动端自适应 */
.detail-article {
  width: 100%;
  max-width: 860px;
  align-self: flex-start;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 28px 32px 36px;
}

.body--dark .detail-article {
  background: #2d2d2d;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4);
}

.article-header {
  margin-bottom: 20px;
}

.article-badges {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

/* 类型标签：彩色圆片图标 + 文字 */
.type-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.4;
}

.article-title {
  margin: 0 0 10px;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.4;
  color: rgba(0, 0, 0, 0.87);
  word-break: break-word;
}

.body--dark .article-title {
  color: rgba(255, 255, 255, 0.92);
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.body--dark .article-meta {
  color: rgba(255, 255, 255, 0.45);
}

.meta-dot {
  color: rgba(0, 0, 0, 0.25);
}

/* 富文本正文：v-html 内容不受 scoped 编译影响，需 :deep 穿透约束基础排版 */
.rich-content {
  padding-top: 20px;
  font-size: 15px;
  line-height: 1.75;
  color: rgba(0, 0, 0, 0.78);
  word-break: break-word;
}

.body--dark .rich-content {
  color: rgba(255, 255, 255, 0.78);
}

.rich-content :deep(p) {
  margin: 0 0 12px;
}

.rich-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
}

.rich-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}

.rich-content :deep(th),
.rich-content :deep(td) {
  border: 1px solid rgba(0, 0, 0, 0.12);
  padding: 6px 10px;
}

.body--dark .rich-content :deep(th),
.body--dark .rich-content :deep(td) {
  border-color: rgba(255, 255, 255, 0.16);
}

.rich-content :deep(a) {
  color: #009688;
}

@media (max-width: 600px) {
  .notice-detail-page {
    padding: 16px 8px 32px;
  }

  .detail-article {
    padding: 20px 16px 24px;
    border-radius: 8px;
  }

  .article-title {
    font-size: 19px;
  }
}
</style>
