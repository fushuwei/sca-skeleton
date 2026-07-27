<script setup lang="ts">
import { computed, ref } from "vue";
import { useI18n } from "vue-i18n";
import { showToast } from "@repo/shared";

const { t } = useI18n({ useScope: "global" });

type ProductType = "table" | "api" | "report";

interface DataProduct {
  id: number;
  name: string;
  icon: string;
  color: string;
  type: ProductType;
  domain: string;
  provider: string;
  description: string;
  usage: number;
  updateTime: string;
}

const products: DataProduct[] = [
  {
    id: 1,
    name: "本科生招生数据分析服务",
    icon: "sym_r_school",
    color: "#009688",
    type: "api",
    domain: "招生域",
    provider: "招生办公室",
    description: "提供历年本科生招生数据查询分析接口，支持按省份、专业、批次等维度统计招生计划与录取结果。",
    usage: 1286,
    updateTime: "2026-07-20"
  },
  {
    id: 2,
    name: "科研项目全周期数据集",
    icon: "sym_r_science",
    color: "#1976d2",
    type: "table",
    domain: "科研域",
    provider: "科研处",
    description: "涵盖项目立项、执行、结题全周期数据，包含经费、人员、成果等多维度明细数据表。",
    usage: 982,
    updateTime: "2026-07-19"
  },
  {
    id: 3,
    name: "教师绩效评估报告",
    icon: "sym_r_analytics",
    color: "#7b1fa2",
    type: "report",
    domain: "人事域",
    provider: "人事处",
    description: "基于教学、科研、服务等维度综合评估教师绩效，输出可视化评估报告及排名。",
    usage: 745,
    updateTime: "2026-07-18"
  },
  {
    id: 4,
    name: "学生成绩分析API",
    icon: "sym_r_grading",
    color: "#e65100",
    type: "api",
    domain: "教务域",
    provider: "教务处",
    description: "提供学生课程成绩查询与分析接口，支持按学年、学期、课程类别统计及趋势分析。",
    usage: 638,
    updateTime: "2026-07-17"
  },
  {
    id: 5,
    name: "财务预算执行数据",
    icon: "sym_r_account_balance",
    color: "#00838f",
    type: "table",
    domain: "财务域",
    provider: "财务处",
    description: "年度预算与执行明细数据集，按院系、科目、季度展示预算分配与实际执行情况。",
    usage: 521,
    updateTime: "2026-07-16"
  },
  {
    id: 6,
    name: "图书借阅趋势分析",
    icon: "sym_r_menu_book",
    color: "#ad1457",
    type: "report",
    domain: "图书域",
    provider: "图书馆",
    description: "图书借阅行为分析报告，包含热门图书 Top10、读者活跃度及借阅趋势可视化图表。",
    usage: 412,
    updateTime: "2026-07-15"
  },
  {
    id: 7,
    name: "一卡通消费行为数据集",
    icon: "sym_r_credit_card",
    color: "#33691e",
    type: "table",
    domain: "一卡通域",
    provider: "信息中心",
    description: "校园一卡通消费明细数据，覆盖食堂、超市、图书馆等场景的消费行为分析数据表。",
    usage: 356,
    updateTime: "2026-07-14"
  },
  {
    id: 8,
    name: "教室资源利用率报告",
    icon: "sym_r_meeting_room",
    color: "#455a64",
    type: "report",
    domain: "资产域",
    provider: "资产管理处",
    description: "教室使用率、空置率及排课冲突分析报告，为教学资源优化配置提供决策依据。",
    usage: 287,
    updateTime: "2026-07-13"
  },
  {
    id: 9,
    name: "毕业生就业质量分析",
    icon: "sym_r_work",
    color: "#bf360c",
    type: "report",
    domain: "教务域",
    provider: "就业指导中心",
    description: "毕业生就业去向、薪资水平及行业分布分析报告，反映人才培养与就业质量情况。",
    usage: 234,
    updateTime: "2026-07-12"
  }
];

const domains = Array.from(new Set(products.map((p) => p.domain)));

const keyword = ref("");
const selectedDomain = ref<string | null>(null);
const activeType = ref<ProductType | "all">("all");

const typeOptions = computed(() => [
  { value: "all", label: t("dataMarket.tabAll") },
  { value: "table", label: t("dataMarket.tabTable") },
  { value: "api", label: t("dataMarket.tabApi") },
  { value: "report", label: t("dataMarket.tabReport") }
]);

const domainOptions = computed(() => [
  { value: null, label: t("common.all") },
  ...domains.map((d) => ({ value: d, label: d }))
]);

const typeIconMap: Record<ProductType, string> = {
  table: "sym_r_table_chart",
  api: "sym_r_api",
  report: "sym_r_description"
};

const typeLabelMap: Record<ProductType, string> = {
  table: "数据表",
  api: "API",
  report: "数据报告"
};

const filteredProducts = computed(() => {
  return products.filter((p) => {
    const matchKeyword =
      !keyword.value.trim() ||
      p.name.toLowerCase().includes(keyword.value.trim().toLowerCase()) ||
      p.description.toLowerCase().includes(keyword.value.trim().toLowerCase());
    const matchDomain = !selectedDomain.value || p.domain === selectedDomain.value;
    const matchType = activeType.value === "all" || p.type === activeType.value;
    return matchKeyword && matchDomain && matchType;
  });
});

function getTypeLabel(type: ProductType): string {
  return typeLabelMap[type] ?? type;
}

function getTypeIcon(type: ProductType): string {
  return typeIconMap[type] ?? "sym_r_dataset";
}

function formatUsage(n: number): string {
  return n.toLocaleString("en-US");
}

function applyProduct(product: DataProduct): void {
  showToast(`已提交「${product.name}」使用申请，请等待审批`, "positive");
}

function viewDetail(product: DataProduct): void {
  showToast(`查看「${product.name}」详情`, "info");
}
</script>

<template>
  <div class="data-market-page">
    <!-- ═══════════════ 页面头部 ═══════════════ -->
    <header class="page-header">
      <h1 class="page-title">{{ t('dataMarket.pageTitle') }}</h1>
      <p class="page-desc">{{ t('dataMarket.pageDesc') }}</p>
    </header>

    <!-- ═══════════════ 筛选栏 ═══════════════ -->
    <div class="filter-bar">
      <q-input
        v-model="keyword"
        dense
        borderless
        clearable
        :placeholder="t('dataMarket.searchPlaceholder')"
        class="filter-search"
        input-class="filter-search-input"
      >
        <template #prepend>
          <q-icon name="sym_r_search" size="20px" class="filter-search-icon" />
        </template>
      </q-input>

      <q-select
        v-model="selectedDomain"
        :options="domainOptions"
        emit-value
        map-options
        dense
        borderless
        :label="t('dataMarket.filterDomain')"
        class="filter-domain"
      />

      <div class="filter-tabs">
        <q-tabs
          v-model="activeType"
          dense
          no-caps
          align="left"
          class="type-tabs"
          active-color="teal"
          indicator-color="teal"
        >
          <q-tab
            v-for="opt in typeOptions"
            :key="opt.value"
            :name="opt.value"
            :label="opt.label"
            class="type-tab"
          />
        </q-tabs>
      </div>
    </div>

    <!-- ═══════════════ 卡片网格 ═══════════════ -->
    <div v-if="filteredProducts.length > 0" class="product-grid">
      <div
        v-for="product in filteredProducts"
        :key="product.id"
        class="product-card"
      >
        <div class="product-card__head">
          <div class="product-card__icon" :style="{ backgroundColor: product.color }">
            <q-icon :name="product.icon" size="26px" color="white" />
          </div>
          <div class="product-card__type">
            <q-icon :name="getTypeIcon(product.type)" size="12px" />
            <span>{{ getTypeLabel(product.type) }}</span>
          </div>
        </div>

        <h3 class="product-card__title">{{ product.name }}</h3>
        <div class="product-card__provider">
          <q-icon name="sym_r_domain" size="14px" />
          <span>{{ product.provider }}</span>
        </div>

        <div class="product-card__tags">
          <q-chip dense square class="domain-chip">
            {{ product.domain }}
          </q-chip>
        </div>

        <p class="product-card__desc">{{ product.description }}</p>

        <div class="product-card__meta">
          <span class="meta-item">
            <q-icon name="sym_r_cloud_download" size="14px" />
            <span class="meta-value">{{ formatUsage(product.usage) }}</span>
            <span class="meta-label">{{ t('dataMarket.cardDownloads') }}</span>
          </span>
          <span class="meta-item">
            <q-icon name="sym_r_schedule" size="14px" />
            <span class="meta-value">{{ product.updateTime }}</span>
          </span>
        </div>

        <div class="product-card__actions">
          <q-btn
            unelevated
            no-caps
            dense
            color="teal"
            :label="t('dataMarket.cardApply')"
            class="action-btn action-btn--primary"
            @click="applyProduct(product)"
          />
          <q-btn
            outline
            no-caps
            dense
            :label="t('dataMarket.cardDetail')"
            class="action-btn action-btn--outline"
            @click="viewDetail(product)"
          />
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <q-icon name="sym_r_search_off" size="64px" class="empty-icon" />
      <p class="empty-text">{{ t('common.noData') }}</p>
    </div>
  </div>
</template>

<style scoped>
.data-market-page {
  padding: 24px 0;
}

/* ═══════════════ 页面头部 ═══════════════ */
.page-header {
  margin-bottom: 20px;
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

/* ═══════════════ 筛选栏 ═══════════════ */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  padding: 12px 16px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.body--dark .filter-bar {
  background: #2a2a2a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.filter-search {
  width: 320px;
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.filter-search:focus-within {
  border-color: #009688;
  background: #fff;
}

.body--dark .filter-search {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.12);
}

.body--dark .filter-search:focus-within {
  border-color: #4db6ac;
  background: rgba(255, 255, 255, 0.06);
}

.filter-search-icon {
  color: rgba(0, 0, 0, 0.4);
}

.body--dark .filter-search-icon {
  color: rgba(255, 255, 255, 0.4);
}

.filter-search :deep(.filter-search-input) {
  font-size: 13px;
  padding: 6px 8px;
  color: rgba(0, 0, 0, 0.87);
  font-family: inherit;
}

.body--dark .filter-search :deep(.filter-search-input) {
  color: rgba(255, 255, 255, 0.92);
}

.filter-domain {
  width: 200px;
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.body--dark .filter-domain {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.12);
}

.filter-tabs {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.type-tabs {
  min-height: 36px;
}

.type-tab {
  font-size: 13px;
  min-height: 36px;
  padding: 0 16px;
  text-transform: none;
  color: rgba(0, 0, 0, 0.55);
  font-weight: 500;
}

.body--dark .type-tab {
  color: rgba(255, 255, 255, 0.55);
}

/* ═══════════════ 卡片网格 ═══════════════ */
.product-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.product-card {
  display: flex;
  flex-direction: column;
  background: #fff;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s ease, transform 0.2s ease;
  position: relative;
}

.body--dark .product-card {
  background: #2a2a2a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.product-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.body--dark .product-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}

.product-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}

.product-card__icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.product-card__type {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  background: rgba(0, 150, 136, 0.08);
  color: #009688;
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.3px;
}

.body--dark .product-card__type {
  background: rgba(77, 182, 172, 0.12);
  color: #4db6ac;
}

.product-card__title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.87);
  margin: 0 0 8px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.body--dark .product-card__title {
  color: rgba(255, 255, 255, 0.92);
}

.product-card__provider {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.55);
  margin-bottom: 10px;
}

.body--dark .product-card__provider {
  color: rgba(255, 255, 255, 0.55);
}

.product-card__tags {
  margin-bottom: 12px;
}

.domain-chip {
  background: rgba(0, 0, 0, 0.04) !important;
  color: rgba(0, 0, 0, 0.65) !important;
  font-size: 11px;
  font-weight: 500;
  min-height: 22px;
  padding: 0 8px;
}

.body--dark .domain-chip {
  background: rgba(255, 255, 255, 0.06) !important;
  color: rgba(255, 255, 255, 0.65) !important;
}

.product-card__desc {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.6);
  line-height: 1.5;
  margin: 0 0 16px 0;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.body--dark .product-card__desc {
  color: rgba(255, 255, 255, 0.6);
}

.product-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  margin-bottom: 14px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.body--dark .product-card__meta {
  border-top-color: rgba(255, 255, 255, 0.06);
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.5);
}

.body--dark .meta-item {
  color: rgba(255, 255, 255, 0.5);
}

.meta-value {
  color: rgba(0, 0, 0, 0.75);
  font-weight: 500;
  font-family: "JetBrains Mono", monospace;
}

.body--dark .meta-value {
  color: rgba(255, 255, 255, 0.8);
}

.meta-label {
  color: rgba(0, 0, 0, 0.4);
}

.body--dark .meta-label {
  color: rgba(255, 255, 255, 0.4);
}

.product-card__actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  min-height: 34px;
}

.action-btn--primary {
  font-weight: 600;
}

.action-btn--outline {
  color: rgba(0, 0, 0, 0.65);
}

.body--dark .action-btn--outline {
  color: rgba(255, 255, 255, 0.65);
}

/* ═══════════════ 空状态 ═══════════════ */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.body--dark .empty-state {
  background: #2a2a2a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.empty-icon {
  color: rgba(0, 0, 0, 0.25);
  margin-bottom: 16px;
}

.body--dark .empty-icon {
  color: rgba(255, 255, 255, 0.25);
}

.empty-text {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.45);
  margin: 0;
}

.body--dark .empty-text {
  color: rgba(255, 255, 255, 0.45);
}

/* ═══════════════ 响应式 ═══════════════ */
@media (max-width: 1200px) {
  .product-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .data-market-page {
    padding: 16px 0;
  }
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  .filter-search,
  .filter-domain {
    width: 100%;
  }
  .filter-tabs {
    justify-content: flex-start;
    overflow-x: auto;
  }
  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
