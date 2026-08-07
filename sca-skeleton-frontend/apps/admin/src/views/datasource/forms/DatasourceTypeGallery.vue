<script setup lang="ts">
/**
 * 数据源类型选择面板（添加数据源第一步）。
 *
 * - 卡片网格按分组展示所有数据源类型；
 * - 未在表单注册表中注册的类型显示为「即将支持」（置灰不可选）；
 * - 支持类型搜索；「上次选择」卡片带角标提示。
 */
import { ref, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { showToast } from "@repo/shared";
import DbTypeIcon from "../../../components/DbTypeIcon.vue";
import { DB_TYPE_GROUPS, LAST_SELECTED_DB_TYPE_KEY, resolveDatasourceForm } from "./DatasourceFormRegistry";
import type { DbTypeCardOption } from "./types";

const { t } = useI18n({ useScope: "global" });

const props = defineProps<{
  options: DbTypeCardOption[];
}>();

const emit = defineEmits<{
  select: [dbType: string];
}>();

const searchKeyword = ref("");
const lastSelectedDbType = ref("");

onMounted(() => {
  try {
    lastSelectedDbType.value = localStorage.getItem(LAST_SELECTED_DB_TYPE_KEY) || "";
  } catch {
    // localStorage 不可用时忽略「上次选择」提示
  }
});

interface DbTypeGroupView {
  labelKey: string;
  cards: DbTypeCardOption[];
}

/** 按分组组织卡片；未归入任何分组的类型进入「其他」组 */
const groups = computed<DbTypeGroupView[]>(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  const match = (card: DbTypeCardOption) =>
    !keyword
    || card.label.toLowerCase().includes(keyword)
    || card.value.toLowerCase().includes(keyword);

  const assigned = new Set<string>();
  const result: DbTypeGroupView[] = [];

  for (const group of DB_TYPE_GROUPS) {
    const cards = group.dbTypes
      .map((dbType) => props.options.find((o) => o.value === dbType))
      .filter((o): o is DbTypeCardOption => !!o)
      .filter(match);
    group.dbTypes.forEach((dbType) => assigned.add(dbType));
    if (cards.length > 0) {
      result.push({ labelKey: group.labelKey, cards });
    }
  }

  const rest = props.options.filter((o) => !assigned.has(o.value)).filter(match);
  if (rest.length > 0) {
    result.push({ labelKey: "datasourceMgmt.groupOther", cards: rest });
  }

  return result;
});

function isSupported(dbType: string): boolean {
  return resolveDatasourceForm(dbType) !== undefined;
}

function handleSelect(dbType: string) {
  if (!isSupported(dbType)) {
    showToast(t("datasourceMgmt.typeUnderDevelopment"), "info");
    return;
  }
  emit("select", dbType);
}
</script>

<template>
  <div class="ds-type-gallery">
    <!-- 类型搜索 -->
    <q-input
      v-model="searchKeyword"
      filled
      square
      dense
      :placeholder="t('datasourceMgmt.searchDbTypePlaceholder')"
      hide-bottom-space
      clearable
      class="ds-type-gallery-search"
    >
      <template #prepend>
        <q-icon name="sym_r_search" size="20px" />
      </template>
    </q-input>

    <!-- 分组卡片 -->
    <div v-if="groups.length > 0" class="ds-type-gallery-groups">
      <div v-for="group in groups" :key="group.labelKey" class="ds-type-gallery-group">
        <div class="ds-type-gallery-group-title">{{ t(group.labelKey) }}</div>
        <div class="ds-type-gallery-grid">
          <button
            v-for="card in group.cards"
            :key="card.value"
            type="button"
            class="ds-type-card"
            :class="{
              'ds-type-card--disabled': !isSupported(card.value),
              'ds-type-card--last': card.value === lastSelectedDbType
            }"
            @click="handleSelect(card.value)"
          >
            <DbTypeIcon :db-type="card.value" :size="28" class="ds-type-card-icon" />
            <span class="ds-type-card-label">{{ card.label }}</span>
            <span
              v-if="card.value === lastSelectedDbType && isSupported(card.value)"
              class="ds-type-card-last-tag"
            >
              {{ t('datasourceMgmt.lastSelected') }}
            </span>
            <span v-if="!isSupported(card.value)" class="ds-type-card-soon-tag">
              {{ t('datasourceMgmt.comingSoon') }}
            </span>
          </button>
        </div>
      </div>
    </div>

    <!-- 搜索无结果 -->
    <div v-else class="ds-type-gallery-empty column items-center justify-center">
      <q-icon name="sym_r_search_off" size="40px" color="grey-5" />
      <div class="text-caption text-grey-6 q-mt-sm">{{ t('common.noData') }}</div>
    </div>
  </div>
</template>

<style scoped>
.ds-type-gallery {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ds-type-gallery-groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ds-type-gallery-group-title {
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.6);
  margin-bottom: 8px;
}

.ds-type-gallery-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

@media (max-width: 560px) {
  .ds-type-gallery-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.ds-type-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 92px;
  padding: 16px 8px;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 6px;
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.ds-type-card:hover:not(.ds-type-card--disabled) {
  border-color: var(--q-primary);
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.1);
}

.ds-type-card:focus-visible {
  outline: 2px solid var(--q-primary);
  outline-offset: 1px;
}

.ds-type-card-label {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.87);
  text-align: center;
  line-height: 1.3;
}

.ds-type-card--disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.ds-type-card--last {
  border-color: var(--q-primary);
}

.ds-type-card-last-tag {
  position: absolute;
  top: 6px;
  right: 6px;
  font-size: 10px;
  line-height: 1;
  padding: 3px 5px;
  border-radius: 3px;
  background: var(--q-primary);
  color: #fff;
}

.ds-type-card-soon-tag {
  position: absolute;
  top: 6px;
  right: 6px;
  font-size: 10px;
  line-height: 1;
  padding: 3px 5px;
  border-radius: 3px;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
}

.ds-type-gallery-empty {
  padding: 40px 0;
}
</style>

<!-- 非 scoped：抽屉经 Teleport 挂载，暗色模式需全局生效 -->
<style>
.body--dark .ds-type-gallery-search .q-field__control {
  background: #2d2d2d;
}

.body--dark .ds-type-gallery-search .q-field__native {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .ds-type-gallery-search .q-field__label {
  color: rgba(255, 255, 255, 0.55);
}

.body--dark .ds-type-gallery-search .q-field__control::before {
  border-color: rgba(255, 255, 255, 0.22);
}

.body--dark .ds-type-gallery-group-title {
  color: rgba(255, 255, 255, 0.6);
}

.body--dark .ds-type-card {
  background: #2d2d2d;
  border-color: rgba(255, 255, 255, 0.16);
}

.body--dark .ds-type-card:hover:not(.ds-type-card--disabled) {
  border-color: var(--q-primary);
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.4);
}

.body--dark .ds-type-card--last {
  border-color: var(--q-primary);
}

.body--dark .ds-type-card-label {
  color: rgba(255, 255, 255, 0.87);
}
</style>
