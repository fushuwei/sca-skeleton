<script setup lang="ts">
import { computed } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { usePortalAuthStore } from "../../stores/auth";

const router = useRouter();
const { t } = useI18n({ useScope: "global" });
const authStore = usePortalAuthStore();

const username = computed(() => authStore.profile?.nickname ?? t("layout.defaultNickname"));

interface StatCard {
  key: string;
  value: string;
  icon: string;
  color: string;
}

const stats = computed<StatCard[]>(() => [
  { key: "statDataAssets", value: "1,286", icon: "sym_r_dataset", color: "#009688" },
  { key: "statDataTables", value: "8,432", icon: "sym_r_table_chart", color: "#1976d2" },
  { key: "statApiServices", value: "156", icon: "sym_r_api", color: "#7b1fa2" },
  { key: "statDataDownloads", value: "3,847", icon: "sym_r_cloud_download", color: "#e65100" }
]);

interface QuickEntry {
  titleKey: string;
  descKey: string;
  icon: string;
  path: string;
  color: string;
}

const quickEntries: QuickEntry[] = [
  { titleKey: "quickEntryDataMap", descKey: "quickEntryDataMapDesc", icon: "sym_r_map", path: "/portal/data/map", color: "#009688" },
  { titleKey: "quickEntryAiQuery", descKey: "quickEntryAiQueryDesc", icon: "sym_r_smart_toy", path: "/portal/service/ai-query", color: "#1976d2" },
  { titleKey: "quickEntryDataMarket", descKey: "quickEntryDataMarketDesc", icon: "sym_r_store", path: "/portal/service/data-market", color: "#7b1fa2" },
  { titleKey: "quickEntryDataSubmit", descKey: "quickEntryDataSubmitDesc", icon: "sym_r_edit_note", path: "/portal/service/data-submit", color: "#e65100" }
];

interface ActivityItem {
  user: string;
  action: string;
  target: string;
  time: string;
  icon: string;
  color: string;
}

const recentActivities: ActivityItem[] = [
  { user: "张教授", action: "下载了", target: "2025级本科生招生数据", time: "5分钟前", icon: "sym_r_cloud_download", color: "#009688" },
  { user: "李老师", action: "申请了", target: "科研项目经费明细表", time: "23分钟前", icon: "sym_r_description", color: "#1976d2" },
  { user: "王主任", action: "发布了", target: "教务管理系统 API v2.1", time: "1小时前", icon: "sym_r_publish", color: "#7b1fa2" },
  { user: "赵老师", action: "收藏了", target: "学生成绩分析数据集", time: "2小时前", icon: "sym_r_star", color: "#e65100" },
  { user: "陈院长", action: "查看了", target: "学院科研产出统计", time: "3小时前", icon: "sym_r_visibility", color: "#00838f" },
  { user: "孙老师", action: "更新了", target: "教师基本信息表", time: "5小时前", icon: "sym_r_edit", color: "#5d4037" }
];

interface HotDataItem {
  name: string;
  domain: string;
  owner: string;
  downloads: number;
  updateTime: string;
}

const hotData: HotDataItem[] = [
  { name: "本科生招生数据（2015-2025）", domain: "招生域", owner: "招生办公室", downloads: 1286, updateTime: "2026-07-20" },
  { name: "科研项目立项明细", domain: "科研域", owner: "科研处", downloads: 982, updateTime: "2026-07-19" },
  { name: "教职工薪酬统计表", domain: "人事域", owner: "人事处", downloads: 745, updateTime: "2026-07-18" },
  { name: "学生成绩分析数据集", domain: "教务域", owner: "教务处", downloads: 638, updateTime: "2026-07-17" },
  { name: "财务预算执行情况", domain: "财务域", owner: "财务处", downloads: 521, updateTime: "2026-07-16" },
  { name: "图书借阅统计分析", domain: "图书域", owner: "图书馆", downloads: 412, updateTime: "2026-07-15" }
];

function goTo(path: string): void {
  router.push(path);
}
</script>

<template>
  <div class="home-page">
    <!-- ═══════════════ Hero 欢迎区 ═══════════════ -->
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">{{ t('home.welcomeTitle') }}</h1>
        <p class="hero-subtitle">{{ t('home.welcomeSubtitle') }}</p>
        <div class="hero-search row items-center no-wrap">
          <q-icon name="sym_r_search" size="24px" class="hero-search-icon" />
          <input
            type="text"
            class="hero-search-input"
            :placeholder="t('layout.searchPlaceholder')"
            @keyup.enter="goTo('/portal/data/resources')"
          />
          <q-btn
            unelevated
            no-caps
            label="搜索"
            class="hero-search-btn"
            @click="goTo('/portal/data/resources')"
          />
        </div>
      </div>
      <div class="hero-decoration">
        <q-icon name="sym_r_dataset" size="120px" class="hero-decoration-icon" />
      </div>
    </section>

    <!-- ═══════════════ 指标卡片区 ═══════════════ -->
    <section class="stats-section">
      <div class="stats-grid">
        <div
          v-for="stat in stats"
          :key="stat.key"
          class="stat-card"
        >
          <div class="stat-card__icon" :style="{ backgroundColor: stat.color }">
            <q-icon :name="stat.icon" size="32px" color="white" />
          </div>
          <div class="stat-card__body">
            <div class="stat-card__value">{{ stat.value }}</div>
            <div class="stat-card__label">{{ t(`home.${stat.key}`) }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══════════════ 快捷入口区 ═══════════════ -->
    <section class="quick-section">
      <div class="section-header">
        <h2 class="section-title">{{ t('home.quickEntryTitle') }}</h2>
      </div>
      <div class="quick-grid">
        <div
          v-for="entry in quickEntries"
          :key="entry.path"
          class="quick-card"
          @click="goTo(entry.path)"
        >
          <div class="quick-card__icon" :style="{ color: entry.color }">
            <q-icon :name="entry.icon" size="40px" />
          </div>
          <div class="quick-card__body">
            <div class="quick-card__title">{{ t(`home.${entry.titleKey}`) }}</div>
            <div class="quick-card__desc">{{ t(`home.${entry.descKey}`) }}</div>
          </div>
          <q-icon name="sym_r_arrow_forward" size="20px" class="quick-card__arrow" />
        </div>
      </div>
    </section>

    <!-- ═══════════════ 最近动态 + 热门数据 ═══════════════ -->
    <section class="bottom-section">
      <!-- 最近动态 -->
      <div class="bottom-card bottom-card--activity">
        <div class="section-header">
          <h2 class="section-title">{{ t('home.recentActivityTitle') }}</h2>
          <q-btn flat no-caps dense :label="t('home.viewAll')" class="view-all-btn" @click="goTo('/portal/profile/notifications')" />
        </div>
        <div class="activity-list">
          <div
            v-for="(item, idx) in recentActivities"
            :key="idx"
            class="activity-item"
          >
            <div class="activity-item__icon" :style="{ backgroundColor: item.color }">
              <q-icon :name="item.icon" size="18px" color="white" />
            </div>
            <div class="activity-item__body">
              <span class="activity-item__text">
                <strong>{{ item.user }}</strong> {{ item.action }}
                <span class="activity-item__target">{{ item.target }}</span>
              </span>
              <span class="activity-item__time">{{ item.time }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 热门数据 -->
      <div class="bottom-card bottom-card--hot">
        <div class="section-header">
          <h2 class="section-title">{{ t('home.hotDataTitle') }}</h2>
          <q-btn flat no-caps dense :label="t('home.viewAll')" class="view-all-btn" @click="goTo('/portal/data/resources')" />
        </div>
        <q-table
          :rows="hotData"
          :columns="[
            { name: 'name', label: t('home.hotDataCol'), field: 'name', align: 'left' as const, sortable: true },
            { name: 'domain', label: t('home.hotDataColDomain'), field: 'domain', align: 'left' as const, sortable: true },
            { name: 'owner', label: t('home.hotDataColOwner'), field: 'owner', align: 'left' as const },
            { name: 'downloads', label: t('home.hotDataColDownloads'), field: 'downloads', align: 'right' as const, sortable: true },
            { name: 'updateTime', label: t('home.hotDataColUpdateTime'), field: 'updateTime', align: 'left' as const, sortable: true }
          ]"
          row-key="name"
          flat
          dense
          :rows-per-page-options="[0]"
          hide-pagination
          class="hot-table"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
}

/* ═══════════════ Hero ═══════════════ */
.hero-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #009688 0%, #00796b 100%);
  border-radius: 0;
  padding: 40px 48px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}

.hero-content {
  flex: 1;
  z-index: 1;
}

.hero-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px 0;
  line-height: 1.3;
}

.hero-subtitle {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 24px 0;
  font-weight: 400;
}

.hero-search {
  background: #fff;
  border-radius: 0;
  padding: 4px 4px 4px 16px;
  width: 560px;
  max-width: 100%;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.hero-search-icon {
  color: #009688;
  flex-shrink: 0;
}

.hero-search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  padding: 10px 12px;
  background: transparent;
  font-family: inherit;
  color: rgba(0, 0, 0, 0.87);
}

.hero-search-input::placeholder {
  color: rgba(0, 0, 0, 0.4);
}

.hero-search-btn {
  background: #009688 !important;
  color: #fff !important;
  height: 40px;
  padding: 0 24px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 0 !important;
}

.hero-decoration {
  flex-shrink: 0;
  margin-left: 48px;
}

.hero-decoration-icon {
  color: rgba(255, 255, 255, 0.15);
}

/* ═══════════════ 指标卡片 ═══════════════ */
.stats-section {
  margin-bottom: 24px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  padding: 20px 24px;
  border-radius: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.body--dark .stat-card {
  background: #2a2a2a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.stat-card__icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0;
  flex-shrink: 0;
}

.stat-card__value {
  font-size: 28px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.87);
  font-family: "JetBrains Mono", monospace;
  line-height: 1.2;
}

.body--dark .stat-card__value {
  color: rgba(255, 255, 255, 0.95);
}

.stat-card__label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  margin-top: 4px;
  font-weight: 500;
}

.body--dark .stat-card__label {
  color: rgba(255, 255, 255, 0.55);
}

/* ═══════════════ 快捷入口 ═══════════════ */
.quick-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  margin: 0;
}

.body--dark .section-title {
  color: rgba(255, 255, 255, 0.92);
}

.view-all-btn {
  color: #009688;
  font-size: 13px;
  font-weight: 500;
}

.body--dark .view-all-btn {
  color: #4db6ac;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  padding: 20px 24px;
  border-radius: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
  border-left: 4px solid transparent;
}

.body--dark .quick-card {
  background: #2a2a2a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.quick-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
  border-left-color: #009688;
}

.quick-card__icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-card__body {
  flex: 1;
  min-width: 0;
}

.quick-card__title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  margin-bottom: 4px;
}

.body--dark .quick-card__title {
  color: rgba(255, 255, 255, 0.92);
}

.quick-card__desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  line-height: 1.4;
}

.body--dark .quick-card__desc {
  color: rgba(255, 255, 255, 0.55);
}

.quick-card__arrow {
  color: rgba(0, 0, 0, 0.3);
  flex-shrink: 0;
  transition: transform 0.2s ease, color 0.2s ease;
}

.body--dark .quick-card__arrow {
  color: rgba(255, 255, 255, 0.3);
}

.quick-card:hover .quick-card__arrow {
  color: #009688;
  transform: translateX(4px);
}

.body--dark .quick-card:hover .quick-card__arrow {
  color: #4db6ac;
}

/* ═══════════════ 底部双栏 ═══════════════ */
.bottom-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.bottom-card {
  background: #fff;
  padding: 20px 24px;
  border-radius: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.body--dark .bottom-card {
  background: #2a2a2a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

/* —— 活动列表 —— */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 0;
}

.activity-item__icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  flex-shrink: 0;
}

.activity-item__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.activity-item__text {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.75);
  line-height: 1.5;
}

.body--dark .activity-item__text {
  color: rgba(255, 255, 255, 0.75);
}

.activity-item__text strong {
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.body--dark .activity-item__text strong {
  color: rgba(255, 255, 255, 0.92);
}

.activity-item__target {
  color: #009688;
  font-weight: 500;
}

.body--dark .activity-item__target {
  color: #4db6ac;
}

.activity-item__time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.4);
}

.body--dark .activity-item__time {
  color: rgba(255, 255, 255, 0.4);
}

/* —— 热门数据表 —— */
.hot-table {
  background: transparent !important;
}

.hot-table :deep(.q-table) {
  background: transparent;
}

.hot-table :deep(.q-table thead th) {
  font-size: 12px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.55);
  background: rgba(0, 0, 0, 0.02);
  padding: 8px 12px;
}

.body--dark .hot-table :deep(.q-table thead th) {
  color: rgba(255, 255, 255, 0.55);
  background: rgba(255, 255, 255, 0.03);
}

.hot-table :deep(.q-table tbody td) {
  font-size: 13px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  color: rgba(0, 0, 0, 0.75);
}

.body--dark .hot-table :deep(.q-table tbody td) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.75);
}

.hot-table :deep(.q-table tbody tr:hover td) {
  background: rgba(0, 150, 136, 0.04);
}

.body--dark .hot-table :deep(.q-table tbody tr:hover td) {
  background: rgba(77, 182, 172, 0.06);
}

/* ═══════════════ 响应式 ═══════════════ */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .bottom-section {
    grid-template-columns: 1fr;
  }
  .hero-decoration {
    display: none;
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 16px;
  }
  .hero-section {
    padding: 24px;
  }
  .hero-title {
    font-size: 22px;
  }
  .hero-search {
    width: 100%;
  }
  .stats-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }
}
</style>
