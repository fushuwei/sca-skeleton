<script setup lang="ts">
import { ref, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useQuasar } from "quasar";
import ProfileSidebar from "../../components/ProfileSidebar.vue";

const { t } = useI18n({ useScope: "global" });
const $q = useQuasar();

type NotificationType = "system" | "approval" | "data";
type NotificationStatus = "unread" | "read";

interface NotificationItem {
  id: number;
  type: NotificationType;
  title: string;
  content: string;
  time: string;
  status: NotificationStatus;
}

const notifications = ref<NotificationItem[]>([
  { id: 1, type: "approval", title: "数据访问申请已通过", content: "您申请的「本科生招生数据（2015-2025）」访问权限已审批通过，可前往资源目录查看使用。", time: "2026-07-22 09:30", status: "unread" },
  { id: 2, type: "data", title: "本科生招生数据已更新", content: "「本科生招生数据」已于 2026-07-22 08:00 完成数据更新，新增 2025 年度招生记录，请及时查阅。", time: "2026-07-22 08:15", status: "unread" },
  { id: 3, type: "system", title: "系统维护通知", content: "数据中台将于今晚 22:00-23:30 进行例行维护升级，期间部分服务可能不可用，请提前安排相关工作。", time: "2026-07-22 07:00", status: "unread" },
  { id: 4, type: "approval", title: "数据下载申请已通过", content: "您申请的「科研项目经费明细表」下载权限已审批通过，文件有效期 7 天，请及时下载。", time: "2026-07-21 16:20", status: "read" },
  { id: 5, type: "data", title: "科研项目立项明细已更新", content: "「科研项目立项明细」数据集已同步最新立项信息，共更新 128 条记录。", time: "2026-07-21 14:00", status: "unread" },
  { id: 6, type: "system", title: "密码安全提醒", content: "检测到您的账户已超过 90 天未修改密码，建议尽快前往个人中心更新密码以保障账户安全。", time: "2026-07-20 10:00", status: "read" },
  { id: 7, type: "approval", title: "API 接入申请已驳回", content: "您申请的「教务管理系统 API v2.1」接入权限已被驳回，原因：授权范围不明确，请补充说明后重新申请。", time: "2026-07-20 09:15", status: "read" },
  { id: 8, type: "data", title: "教职工信息表数据预警", content: "「教职工信息表」检测到 3 条数据质量异常（手机号格式错误），已通知数据负责人处理。", time: "2026-07-19 17:30", status: "read" }
]);

type TabName = "all" | "unread" | NotificationType;

const activeTab = ref<TabName>("all");

const filteredNotifications = computed(() => {
  switch (activeTab.value) {
    case "unread":
      return notifications.value.filter((n) => n.status === "unread");
    case "system":
    case "approval":
    case "data":
      return notifications.value.filter((n) => n.type === activeTab.value);
    default:
      return notifications.value;
  }
});

const unreadCount = computed(() => notifications.value.filter((n) => n.status === "unread").length);

const typeIconMap: Record<NotificationType, string> = {
  system: "sym_r_settings",
  approval: "sym_r_task_alt",
  data: "sym_r_sync"
};

const typeColorMap: Record<NotificationType, string> = {
  system: "#607d8b",
  approval: "#009688",
  data: "#1976d2"
};

function markRead(item: NotificationItem): void {
  if (item.status === "read") return;
  item.status = "read";
}

function deleteNotification(item: NotificationItem): void {
  notifications.value = notifications.value.filter((n) => n.id !== item.id);
  $q.notify({ type: "info", message: t("notification.delete"), position: "top" });
}

function markAllRead(): void {
  notifications.value.forEach((n) => {
    n.status = "read";
  });
  $q.notify({ type: "positive", message: t("notification.markAllRead"), position: "top" });
}
</script>

<template>
  <div class="notification-page">
    <div class="profile-layout">
      <ProfileSidebar />
      <div class="profile-main">
    <header class="page-header">
      <h1 class="page-title">{{ t("notification.pageTitle") }}</h1>
      <p class="page-desc">{{ t("notification.pageDesc") }}</p>
    </header>

    <q-card flat class="page-card">
      <!-- 工具栏：Tab + 全部标记已读 -->
      <div class="toolbar">
        <q-tabs
          v-model="activeTab"
          dense
          no-caps
          align="left"
          class="filter-tabs"
          active-color="teal"
          indicator-color="teal"
        >
          <q-tab name="all" :label="t('notification.tabAll')" />
          <q-tab name="unread">
            <span class="tab-label">
              {{ t("notification.tabUnread") }}
              <q-badge v-if="unreadCount > 0" color="teal" rounded class="tab-badge" :label="unreadCount" />
            </span>
          </q-tab>
          <q-tab name="system" :label="t('notification.tabSystem')" />
          <q-tab name="approval" :label="t('notification.tabApproval')" />
          <q-tab name="data" :label="t('notification.tabData')" />
        </q-tabs>
        <q-btn
          flat
          no-caps
          dense
          :label="t('notification.markAllRead')"
          icon="sym_r_done_all"
          color="teal"
          class="mark-all-btn"
          :disable="unreadCount === 0"
          @click="markAllRead"
        />
      </div>

      <!-- 通知列表 -->
      <div class="notification-list">
        <div
          v-for="item in filteredNotifications"
          :key="item.id"
          class="notification-item"
          :class="{ 'notification-item--unread': item.status === 'unread' }"
        >
          <div class="notification-item__icon" :style="{ backgroundColor: typeColorMap[item.type] }">
            <q-icon :name="typeIconMap[item.type]" size="20px" color="white" />
          </div>
          <div class="notification-item__body">
            <div class="notification-item__header">
              <span class="notification-item__title">{{ item.title }}</span>
              <span v-if="item.status === 'unread'" class="unread-dot" />
            </div>
            <p class="notification-item__content">{{ item.content }}</p>
            <span class="notification-item__time">{{ item.time }}</span>
          </div>
          <div class="notification-item__actions">
            <q-btn
              v-if="item.status === 'unread'"
              flat
              no-caps
              dense
              :label="t('notification.markRead')"
              color="teal"
              size="12px"
              @click="markRead(item)"
            />
            <q-btn
              flat
              no-caps
              dense
              :label="t('notification.delete')"
              color="red"
              icon="sym_r_delete_outline"
              size="12px"
              @click="deleteNotification(item)"
            />
          </div>
        </div>

        <div v-if="filteredNotifications.length === 0" class="empty-state">
          <q-icon name="sym_r_notifications_off" size="48px" />
          <p>{{ t("layout.noNotifications") }}</p>
        </div>
      </div>
    </q-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notification-page {
  padding: 24px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 24px;
  align-items: start;
}

.profile-main {
  min-width: 0;
}

@media (max-width: 1024px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  margin: 0 0 4px 0;
  line-height: 1.4;
}

.body--dark .page-title {
  color: rgba(255, 255, 255, 0.92);
}

.page-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  margin: 0;
}

.body--dark .page-desc {
  color: rgba(255, 255, 255, 0.55);
}

.page-card {
  background: #fff !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.body--dark .page-card {
  background: #2a2a2a !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: 0 16px;
}

.body--dark .toolbar {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.filter-tabs {
  flex: 1;
}

.filter-tabs :deep(.q-tab) {
  min-height: 48px;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 500;
  text-transform: none;
}

.filter-tabs :deep(.q-tab__indicator) {
  height: 3px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-badge {
  font-size: 10px;
  padding: 1px 6px;
}

.mark-all-btn {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

.notification-list {
  padding: 8px 0;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  position: relative;
  transition: background 0.2s ease;
}

.body--dark .notification-item {
  border-bottom-color: rgba(255, 255, 255, 0.04);
}

.notification-item:hover {
  background: rgba(0, 150, 136, 0.04);
}

.body--dark .notification-item:hover {
  background: rgba(77, 182, 172, 0.06);
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item--unread {
  background: rgba(0, 150, 136, 0.03);
}

.body--dark .notification-item--unread {
  background: rgba(77, 182, 172, 0.04);
}

.notification-item--unread::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: #009688;
}

.body--dark .notification-item--unread::before {
  background: #4db6ac;
}

.notification-item__icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notification-item__body {
  flex: 1;
  min-width: 0;
}

.notification-item__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.notification-item__title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.body--dark .notification-item__title {
  color: rgba(255, 255, 255, 0.92);
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #009688;
  flex-shrink: 0;
}

.body--dark .unread-dot {
  background: #4db6ac;
}

.notification-item__content {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  line-height: 1.5;
  margin: 0 0 6px 0;
}

.body--dark .notification-item__content {
  color: rgba(255, 255, 255, 0.65);
}

.notification-item__time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.4);
}

.body--dark .notification-item__time {
  color: rgba(255, 255, 255, 0.4);
}

.notification-item__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.notification-item__actions :deep(.q-btn) {
  font-size: 12px;
  font-weight: 500;
  padding: 4px 8px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  color: rgba(0, 0, 0, 0.3);
}

.body--dark .empty-state {
  color: rgba(255, 255, 255, 0.3);
}

.empty-state p {
  margin: 12px 0 0 0;
  font-size: 14px;
}

@media (max-width: 768px) {
  .notification-page {
    padding: 16px;
  }
  .notification-item {
    padding: 16px;
    flex-wrap: wrap;
  }
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .mark-all-btn {
    align-self: flex-end;
  }
}
</style>
