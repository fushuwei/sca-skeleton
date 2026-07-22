<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";

const { t } = useI18n({ useScope: "global" });

interface DomainCard {
  key: string;
  name: string;
  icon: string;
  color: string;
  assetCount: number;
  tableCount: number;
  apiCount: number;
  owner: string;
  updateTime: string;
}

const domains: DomainCard[] = [
  { key: "enrollment", name: "招生域", icon: "sym_r_how_to_reg", color: "#009688", assetCount: 186, tableCount: 1232, apiCount: 24, owner: "招生办公室", updateTime: "2026-07-20" },
  { key: "academic", name: "教务域", icon: "sym_r_school", color: "#1976d2", assetCount: 342, tableCount: 2156, apiCount: 38, owner: "教务处", updateTime: "2026-07-21" },
  { key: "research", name: "科研域", icon: "sym_r_science", color: "#7b1fa2", assetCount: 218, tableCount: 1486, apiCount: 19, owner: "科研处", updateTime: "2026-07-19" },
  { key: "hr", name: "人事域", icon: "sym_r_badge", color: "#e65100", assetCount: 156, tableCount: 892, apiCount: 12, owner: "人事处", updateTime: "2026-07-18" },
  { key: "finance", name: "财务域", icon: "sym_r_account_balance_wallet", color: "#388e3c", assetCount: 134, tableCount: 768, apiCount: 15, owner: "财务处", updateTime: "2026-07-17" },
  { key: "asset", name: "资产域", icon: "sym_r_inventory_2", color: "#5d4037", assetCount: 98, tableCount: 562, apiCount: 8, owner: "资产管理处", updateTime: "2026-07-16" },
  { key: "library", name: "图书域", icon: "sym_r_menu_book", color: "#00838f", assetCount: 76, tableCount: 432, apiCount: 6, owner: "图书馆", updateTime: "2026-07-15" },
  { key: "card", name: "一卡通域", icon: "sym_r_credit_card", color: "#c62828", assetCount: 76, tableCount: 904, apiCount: 34, owner: "信息中心", updateTime: "2026-07-21" }
];

const summary = computed(() => ({
  domains: domains.length,
  assets: domains.reduce((s, d) => s + d.assetCount, 0),
  tables: domains.reduce((s, d) => s + d.tableCount, 0),
  apis: domains.reduce((s, d) => s + d.apiCount, 0)
}));

const summaryCards = computed(() => [
  { key: "domainCol", value: summary.value.domains, icon: "sym_r_domain", color: "#009688" },
  { key: "assetCountCol", value: summary.value.assets, icon: "sym_r_dataset", color: "#1976d2" },
  { key: "tableCountCol", value: summary.value.tables, icon: "sym_r_table_chart", color: "#7b1fa2" },
  { key: "apiCountCol", value: summary.value.apis, icon: "sym_r_api", color: "#e65100" }
]);

function formatNum(n: number): string {
  return n.toLocaleString("en-US");
}
</script>

<template>
  <div class="page-data-map">
    <!-- ═══════════════ 页面头部 ═══════════════ -->
    <header class="page-header">
      <div class="page-header__main">
        <h1 class="page-title">{{ t('dataMap.pageTitle') }}</h1>
        <p class="page-desc">{{ t('dataMap.pageDesc') }}</p>
      </div>
    </header>

    <!-- ═══════════════ 汇总指标 ═══════════════ -->
    <section class="summary-grid">
      <div
        v-for="card in summaryCards"
        :key="card.key"
        class="summary-card"
      >
        <div class="summary-card__icon" :style="{ backgroundColor: card.color }">
          <q-icon :name="card.icon" size="24px" color="white" />
        </div>
        <div class="summary-card__body">
          <div class="summary-card__value">{{ formatNum(card.value) }}</div>
          <div class="summary-card__label">{{ t(`dataMap.${card.key}`) }}</div>
        </div>
      </div>
    </section>

    <!-- ═══════════════ 业务域卡片网格 ═══════════════ -->
    <section class="domain-grid">
      <div
        v-for="domain in domains"
        :key="domain.key"
        class="domain-card"
        :style="{ borderLeftColor: domain.color }"
      >
        <!-- 卡片头部 -->
        <div class="domain-card__header">
          <div class="domain-card__icon" :style="{ backgroundColor: domain.color }">
            <q-icon :name="domain.icon" size="22px" color="white" />
          </div>
          <div class="domain-card__title">{{ domain.name }}</div>
        </div>

        <!-- 卡片统计区 -->
        <div class="domain-card__stats">
          <div class="domain-stat">
            <div class="domain-stat__value">{{ formatNum(domain.assetCount) }}</div>
            <div class="domain-stat__label">{{ t('dataMap.assetCountCol') }}</div>
          </div>
          <div class="domain-stat">
            <div class="domain-stat__value">{{ formatNum(domain.tableCount) }}</div>
            <div class="domain-stat__label">{{ t('dataMap.tableCountCol') }}</div>
          </div>
          <div class="domain-stat">
            <div class="domain-stat__value">{{ formatNum(domain.apiCount) }}</div>
            <div class="domain-stat__label">{{ t('dataMap.apiCountCol') }}</div>
          </div>
        </div>

        <!-- 卡片元信息 -->
        <div class="domain-card__meta">
          <div class="meta-row">
            <q-icon name="sym_r_person" size="16px" class="meta-icon" />
            <span class="meta-label">{{ t('dataMap.ownerCol') }}</span>
            <span class="meta-value">{{ domain.owner }}</span>
          </div>
          <div class="meta-row">
            <q-icon name="sym_r_schedule" size="16px" class="meta-icon" />
            <span class="meta-label">{{ t('dataMap.updateTimeCol') }}</span>
            <span class="meta-value">{{ domain.updateTime }}</span>
          </div>
        </div>

        <!-- 卡片操作 -->
        <div class="domain-card__footer">
          <q-btn
            flat
            no-caps
            dense
            :label="t('dataMap.viewDetail')"
            icon-right="sym_r_arrow_forward"
            class="detail-btn"
            :style="{ color: domain.color }"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page-data-map {
  padding: 24px;
}

/* ═══════════════ 页面头部 ═══════════════ */
.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  margin: 0 0 6px 0;
  line-height: 1.4;
}

.body--dark .page-title {
  color: rgba(255, 255, 255, 0.92);
}

.page-desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.55);
  margin: 0;
  line-height: 1.5;
}

.body--dark .page-desc {
  color: rgba(255, 255, 255, 0.55);
}

/* ═══════════════ 汇总指标 ═══════════════ */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.body--dark .summary-card {
  background: #2a2a2a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card__icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.summary-card__value {
  font-size: 24px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.87);
  font-family: "JetBrains Mono", monospace;
  line-height: 1.2;
}

.body--dark .summary-card__value {
  color: rgba(255, 255, 255, 0.95);
}

.summary-card__label {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.55);
  margin-top: 4px;
  font-weight: 500;
}

.body--dark .summary-card__label {
  color: rgba(255, 255, 255, 0.55);
}

/* ═══════════════ 业务域卡片 ═══════════════ */
.domain-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.domain-card {
  background: #fff;
  border-left: 4px solid transparent;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s ease, transform 0.2s ease;
  display: flex;
  flex-direction: column;
}

.body--dark .domain-card {
  background: #2a2a2a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.domain-card:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-3px);
}

/* —— 卡片头部 —— */
.domain-card__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px 12px;
}

.domain-card__icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.domain-card__title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
}

.body--dark .domain-card__title {
  color: rgba(255, 255, 255, 0.92);
}

/* —— 统计区 —— */
.domain-card__stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  padding: 8px 20px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.body--dark .domain-card__stats {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.domain-stat {
  text-align: center;
}

.domain-stat__value {
  font-size: 20px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.87);
  font-family: "JetBrains Mono", monospace;
  line-height: 1.2;
}

.body--dark .domain-stat__value {
  color: rgba(255, 255, 255, 0.95);
}

.domain-stat__label {
  font-size: 11px;
  color: rgba(0, 0, 0, 0.5);
  margin-top: 4px;
  font-weight: 500;
}

.body--dark .domain-stat__label {
  color: rgba(255, 255, 255, 0.5);
}

/* —— 元信息 —— */
.domain-card__meta {
  padding: 14px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.meta-icon {
  color: rgba(0, 0, 0, 0.35);
  flex-shrink: 0;
}

.body--dark .meta-icon {
  color: rgba(255, 255, 255, 0.35);
}

.meta-label {
  color: rgba(0, 0, 0, 0.5);
  flex-shrink: 0;
}

.body--dark .meta-label {
  color: rgba(255, 255, 255, 0.5);
}

.meta-value {
  color: rgba(0, 0, 0, 0.75);
  font-weight: 500;
}

.body--dark .meta-value {
  color: rgba(255, 255, 255, 0.75);
}

/* —— 卡片底部 —— */
.domain-card__footer {
  padding: 0 20px 14px;
  display: flex;
  justify-content: flex-end;
}

.detail-btn {
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0;
}

.detail-btn :deep(.q-icon) {
  transition: transform 0.2s ease;
}

.domain-card:hover .detail-btn :deep(.q-icon) {
  transform: translateX(4px);
}

/* ═══════════════ 响应式 ═══════════════ */
@media (max-width: 1200px) {
  .summary-grid,
  .domain-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .page-data-map {
    padding: 16px;
  }
  .summary-grid,
  .domain-grid {
    grid-template-columns: 1fr;
  }
}
</style>
