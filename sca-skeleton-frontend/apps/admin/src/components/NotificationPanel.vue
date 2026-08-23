<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { useRouter } from "vue-router";
import { isNotificationHandled, showToast } from "@repo/shared";
import type { NoticeInboxItem } from "../types/auth";
import {
  getNoticeInboxApi,
  markNoticeAsReadApi,
  markAllNoticeAsReadApi
} from "../apis/notice";

const { t } = useI18n({ useScope: "global" });
const router = useRouter();

const props = defineProps<{
  /** 父组件维护的精确未读数（用于头部徽标展示） */
  unreadCount?: number;
}>();

const emit = defineEmits<{
  /** 未读数变化时通知父组件更新铃铛 Badge */
  unreadCountChange: [count: number];
}>();

// ═══════════════════════════════════════════════════════════════
// 状态
// ═══════════════════════════════════════════════════════════════

const loading = ref(false);
const markingAllRead = ref(false);
const notices = ref<NoticeInboxItem[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const totalCount = ref(0);
/** 最近一次首页加载时间戳（毫秒），用于短时间去重防止重复请求 */
let lastRefreshTime = 0;

// ═══════════════════════════════════════════════════════════════
// 计算属性
// ═══════════════════════════════════════════════════════════════

const unreadCount = computed(() => notices.value.filter((n) => !n.isRead).length);

/** 头部徽标展示的未读数：优先使用父组件传入的精确值 */
const unreadBadge = computed(() => props.unreadCount ?? unreadCount.value);

/** 类型 → 图标映射（与 NoticeDetailView 内的同名映射保持一致） */
const typeIconMap: Record<string, string> = {
  notice: "sym_r_campaign",
  announcement: "sym_r_newspaper",
  system: "sym_r_settings",
  other: "sym_r_info"
};

/** 类型 → 颜色映射（与 NoticeDetailView 内的同名映射保持一致） */
const typeColorMap: Record<string, string> = {
  notice: "#1976d2",
  announcement: "#7e57c2",
  system: "#607d8b",
  other: "#78909c"
};

/** 级别 → 标签颜色映射（与 NoticeDetailView 内的同名映射保持一致） */
const levelColorMap: Record<string, string> = {
  normal: "grey-6",
  important: "orange-7",
  urgent: "red-7"
};

// ═══════════════════════════════════════════════════════════════
// 工具方法
// ═══════════════════════════════════════════════════════════════

/** 将 HTML 富文本内容截取为纯文本摘要 */
function getContentSummary(html: string, maxLen = 120): string {
  if (!html) return "";
  // 去除 HTML 标签
  const text = html.replace(/<[^>]+>/g, "").replace(/&nbsp;/g, " ").trim();
  if (text.length <= maxLen) return text;
  return text.substring(0, maxLen) + "...";
}

/** 格式化时间为相对时间描述 */
function formatRelativeTime(timeStr: string | null): string {
  if (!timeStr) return "";
  const date = new Date(timeStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return t("notification.justNow");
  if (minutes < 60) return t("notification.minutesAgo", { count: minutes });
  if (hours < 24) return t("notification.hoursAgo", { count: hours });
  if (days < 7) return t("notification.daysAgo", { count: days });
  // 超过 7 天显示完整日期
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  const h = String(date.getHours()).padStart(2, "0");
  const min = String(date.getMinutes()).padStart(2, "0");
  return `${y}-${m}-${d} ${h}:${min}`;
}

/** 通知父组件当前未读数（基于已加载的数据） */
function notifyUnreadCount() {
  // 未读总数 = 总未读数（如果当前页展示了所有未读则用本地计数）
  // 由于弹窗只加载第一页，更准确的未读数需要单独查询
  // 这里用本地已加载数据中的未读数通知父组件做 UI 更新
  emit("unreadCountChange", unreadCount.value);
}

// ═══════════════════════════════════════════════════════════════
// 数据加载
// ═══════════════════════════════════════════════════════════════

async function loadNotices(page = 1) {
  // 去重防护：加载中直接跳过；首页刷新在 1 秒内只执行一次
  // （QMenu 首开时 onMounted 与父组件 @show → refresh 可能先后触发，避免重复请求）
  if (loading.value) return;
  if (page === 1) {
    if (Date.now() - lastRefreshTime < 1_000) return;
    lastRefreshTime = Date.now();
  }
  loading.value = true;
  try {
    const result = await getNoticeInboxApi(page, pageSize.value);
    if (result.code === 10_000) {
      notices.value = result.data?.records ?? [];
      totalCount.value = Number(result.data?.total) || 0;
      currentPage.value = Number(result.data?.current) || page;
    } else {
      showToast(result.message || t("common.loadFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("common.loadFail"), "negative");
    }
  } finally {
    loading.value = false;
  }
}

// ═══════════════════════════════════════════════════════════════
// 操作
// ═══════════════════════════════════════════════════════════════

async function handleMarkRead(item: NoticeInboxItem) {
  if (item.isRead) return;
  try {
    const result = await markNoticeAsReadApi(item.id);
    if (result.code === 10_000) {
      item.isRead = true;
      item.readTime = new Date().toISOString();
      notifyUnreadCount();
    } else {
      showToast(result.message || t("common.operationFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("common.operationFail"), "negative");
    }
  }
}

async function handleMarkAllRead() {
  if (unreadCount.value === 0 && totalCount.value === 0) return;
  markingAllRead.value = true;
  try {
    const result = await markAllNoticeAsReadApi();
    if (result.code === 10_000) {
      notices.value.forEach((n) => {
        n.isRead = true;
        if (!n.readTime) {
          n.readTime = new Date().toISOString();
        }
      });
      showToast(t("notification.markAllReadSuccess"), "positive");
      emit("unreadCountChange", 0);
    } else {
      showToast(result.message || t("common.operationFail"), "negative");
    }
  } catch (error) {
    if (!isNotificationHandled(error)) {
      showToast(t("common.operationFail"), "negative");
    }
  } finally {
    markingAllRead.value = false;
  }
}

/**
 * 点击整行：等价于「详情」按钮——新开 tab 打开阅读页，未读时顺带标记已读
 */
function handleViewDetail(item: NoticeInboxItem) {
  openNoticeDetail(item);
}

/**
 * 新开浏览器 tab 查看公告详情（独立 HTML 阅读页 /notice-detail/:id）。
 * 同源共享 localStorage，新 tab 自动携带登录态；查看即视为已读。
 */
function openNoticeDetail(item: NoticeInboxItem) {
  if (!item.isRead) {
    handleMarkRead(item);
  }
  const { href } = router.resolve({ name: "NoticeDetail", params: { id: item.id } });
  window.open(href, "_blank", "noopener");
}

// ═══════════════════════════════════════════════════════════════
// 分页
// ═══════════════════════════════════════════════════════════════

function onPageChange(page: number) {
  loadNotices(page);
}

// ═══════════════════════════════════════════════════════════════
// 生命周期
// ═══════════════════════════════════════════════════════════════

// 组件挂载自主加载（兜底）：不依赖父组件 @show 时序，确保弹窗必有数据
// 与父组件 @show → refresh 的重复请求由 loadNotices 的时间戳去重防护
onMounted(() => {
  loadNotices();
});

// 暴露刷新方法供父组件调用（弹窗每次展开时刷新最新数据）
defineExpose({
  refresh: () => loadNotices(1)
});
</script>

<template>
  <div class="notification-panel">
    <!-- 头部工具栏 -->
    <div class="panel-header">
      <span class="panel-title">
        {{ t("notification.panelTitle") }}
        <q-badge
          v-if="unreadBadge > 0"
          color="teal"
          rounded
          dense
          :label="unreadBadge > 99 ? '99+' : unreadBadge"
          class="header-unread-badge"
        />
      </span>
      <q-space />
      <q-btn
        flat
        no-caps
        dense
        :label="t('notification.markAllRead')"
        icon="sym_r_done_all"
        color="teal"
        class="mark-all-btn"
        :disable="unreadBadge === 0"
        :loading="markingAllRead"
        @click="handleMarkAllRead"
      />
    </div>

    <q-separator />

    <!-- 消息列表（有数据时滚动展示） -->
    <q-scroll-area v-if="notices.length > 0" class="panel-scroll">
      <div class="notice-list">
        <div
          v-for="item in notices"
          :key="item.id"
          class="notice-item"
          :class="{ 'notice-item--unread': !item.isRead, 'notice-item--read': item.isRead, 'notice-item--top': item.isTop === 1 }"
          @click="handleViewDetail(item)"
        >
          <!-- 未读标记条 -->
          <div v-if="!item.isRead" class="unread-indicator" />

          <div class="notice-item__icon" :style="{ backgroundColor: typeColorMap[item.type] || typeColorMap.other }">
            <q-icon :name="typeIconMap[item.type] || typeIconMap.other" size="18px" color="white" />
          </div>

          <div class="notice-item__body">
            <div class="notice-item__header">
              <span class="notice-item__title ellipsis">{{ item.title }}</span>
              <q-badge
                v-if="item.isTop === 1"
                color="orange-7"
                rounded
                dense
                :label="t('notification.topBadge')"
                class="top-badge"
              />
              <q-badge
                v-if="item.level !== 'normal'"
                :color="levelColorMap[item.level] || 'grey-6'"
                rounded
                dense
                :label="t(`noticeMgmt.level${item.level.charAt(0).toUpperCase() + item.level.slice(1)}`)"
                class="level-badge"
              />
            </div>
            <p class="notice-item__content">{{ getContentSummary(item.content) }}</p>
            <div class="notice-item__meta">
              <span v-if="item.publisherName" class="notice-item__publisher">{{ item.publisherName }}</span>
              <span class="notice-item__time">{{ formatRelativeTime(item.publishTime) }}</span>
            </div>
          </div>

          <div class="notice-item__action">
            <q-btn
              v-if="!item.isRead"
              flat
              no-caps
              dense
              :label="t('notification.markRead')"
              color="teal"
              size="11px"
              @click.stop="handleMarkRead(item)"
            />
            <!-- 详情：新开浏览器 tab 渲染公告完整内容 -->
            <q-btn
              flat
              no-caps
              dense
              :label="t('notification.detailBtn')"
              color="teal"
              size="11px"
              @click.stop="openNoticeDetail(item)"
            />
          </div>
        </div>
      </div>
    </q-scroll-area>

    <!-- 加载中 / 空状态：撑满面板剩余空间，水平垂直居中 -->
    <div v-else-if="loading" class="panel-state">
      <q-spinner-dots size="32px" color="teal" />
    </div>

    <div v-else class="panel-state panel-empty">
      <q-icon name="sym_r_notifications_off" size="56px" />
      <div class="empty-title">{{ t("common.noData") }}</div>
      <div class="empty-hint">{{ t("notification.emptyHint") }}</div>
    </div>

    <!-- 分页 -->
    <div v-if="totalCount > pageSize" class="panel-footer">
      <q-pagination
        v-model="currentPage"
        :max="Math.ceil(totalCount / pageSize)"
        :max-pages="5"
        direction-links
        boundary-links
        size="sm"
        flat
        color="teal"
        @update:model-value="onPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.notification-panel {
  display: flex;
  flex-direction: column;
  /* 固定高度（矮屏自适应）：q-menu 内容区高度为 auto，height:100% 无法解析，
     且 QScrollArea 内部绝对定位不贡献内容高度，必须给确定高度否则列表塌陷为 0。
     取 64vh 而非更高：外层 q-menu 有核心样式 max-height:65vh 且 overflow 已 hidden，
     面板一旦超过 65vh，底部（分页栏）会被裁掉且无法滚动到 */
  height: min(480px, 64vh);
}

.panel-header {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  min-height: 40px;
  flex-shrink: 0;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  /* 英文等长文案时允许标题收缩省略，避免把头部撑出横向滚动 */
  min-width: 0;
  flex-shrink: 1;
  white-space: nowrap;
  overflow: hidden;
}

.body--dark .panel-title {
  color: rgba(255, 255, 255, 0.92);
}

/* 头部未读数徽标 */
.header-unread-badge {
  font-size: 10px;
  font-weight: 600;
  min-width: 16px;
  height: 16px;
  padding: 0 5px;
}

.mark-all-btn {
  font-size: 12px;
  font-weight: 500;
}

.panel-scroll {
  flex: 1;
  min-height: 0;
}

/*
 * 禁用横向滚动：QScrollArea 的内容容器（__content）会按内容的固有宽度
 * 设置 inline width，英文等更宽的文案会把容器撑出横向滚动条。
 * 强制其宽度恒等于宿主宽度，配合文本断行即可彻底杜绝横向滚动。
 */
.panel-scroll :deep(.q-scrollarea__content) {
  width: 100% !important;
}

/* 加载中 / 空状态：撑满面板剩余空间，水平垂直居中（与列表页空数据效果一致） */
.panel-state {
  flex: 1;
  min-height: 320px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: rgba(0, 0, 0, 0.3);
  padding: 24px;
}

.body--dark .panel-state {
  color: rgba(255, 255, 255, 0.3);
}

.panel-empty .empty-title {
  margin-top: 12px;
  font-size: 15px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.6);
}

.body--dark .panel-empty .empty-title {
  color: rgba(255, 255, 255, 0.6);
}

.panel-empty .empty-hint {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.38);
}

.body--dark .panel-empty .empty-hint {
  color: rgba(255, 255, 255, 0.38);
}

.notice-list {
  padding: 0;
}

.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  position: relative;
  cursor: pointer;
  transition: background 0.2s ease;
}

.body--dark .notice-item {
  border-bottom-color: rgba(255, 255, 255, 0.04);
}

.notice-item:hover {
  background: rgba(0, 150, 136, 0.04);
}

.body--dark .notice-item:hover {
  background: rgba(77, 182, 172, 0.06);
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-item--unread {
  background: rgba(0, 150, 136, 0.03);
}

.body--dark .notice-item--unread {
  background: rgba(77, 182, 172, 0.04);
}

/* 已读项：整体弱化（降低透明度 + 常规字重），与未读项形成清晰视觉层级 */
.notice-item--read {
  opacity: 0.62;
}

.notice-item--read:hover {
  opacity: 0.85;
}

.notice-item--read .notice-item__title {
  font-weight: 400;
}

.unread-indicator {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: #009688;
  border-radius: 0 2px 2px 0;
}

.body--dark .unread-indicator {
  background: #4db6ac;
}

.notice-item__icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.notice-item__body {
  flex: 1;
  min-width: 0;
}

.notice-item__header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 3px;
}

.notice-item__title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  flex: 1;
  min-width: 0;
}

.body--dark .notice-item__title {
  color: rgba(255, 255, 255, 0.92);
}

.top-badge,
.level-badge {
  font-size: 9px;
  padding: 1px 5px;
  flex-shrink: 0;
}

.notice-item__content {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.55);
  line-height: 1.4;
  margin: 0 0 4px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  /* 英文长单词 / URL 强制断行，避免撑出横向滚动 */
  overflow-wrap: anywhere;
}

.body--dark .notice-item__content {
  color: rgba(255, 255, 255, 0.55);
}

.notice-item__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: rgba(0, 0, 0, 0.4);
}

.body--dark .notice-item__meta {
  color: rgba(255, 255, 255, 0.4);
}

.notice-item__publisher::after {
  content: "·";
  margin-left: 8px;
}

.notice-item__action {
  flex-shrink: 0;
  /* 撑满条目高度并垂直居中：未读时「已读/详情」等距堆叠，已读时「详情」单独居中 */
  align-self: stretch;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  gap: 4px;
}

.panel-footer {
  display: flex;
  justify-content: center;
  padding: 8px 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

/*
 * 分页器按钮：与业务功能列表页（.table-bottom）的分页样式保持一致——
 * 30px 圆形按钮、10px 字号，视觉统一。
 */
.panel-footer :deep(.q-pagination__content .q-btn) {
  width: 30px !important;
  height: 30px !important;
  min-width: 30px !important;
  min-height: 30px !important;
  border-radius: 50% !important;
  padding: 0 !important;
  font-size: 10px !important;
}

.panel-footer :deep(.q-pagination__content .q-btn .q-focus-helper) {
  border-radius: 50%;
}

.panel-footer :deep(.q-pagination__content .q-btn .q-icon) {
  font-size: 20px;
}

.panel-footer :deep(.q-pagination__content .q-btn.q-btn--standard) {
  font-weight: 700;
}

.body--dark .panel-footer {
  border-top-color: rgba(255, 255, 255, 0.06);
}
</style>
