<script setup lang="ts">
import { computed, nextTick, ref } from "vue";
import { useI18n } from "vue-i18n";
import { useQuasar } from "quasar";

const { t } = useI18n({ useScope: "global" });
const $q = useQuasar();

interface TableColumn {
  name: string;
  label: string;
  field: string;
  align: "left" | "right" | "center";
}

interface QueryResult {
  columns: TableColumn[];
  rows: Record<string, string | number>[];
}

interface ChatMessage {
  id: string;
  role: "user" | "ai";
  content: string;
  sql?: string;
  result?: QueryResult;
  pending?: boolean;
}

const messages = ref<ChatMessage[]>([]);
const inputText = ref("");
const isThinking = ref(false);
const conversationRef = ref<HTMLElement | null>(null);

const sampleQuestions: string[] = [
  "本校近三年招生人数趋势？",
  "各学院教师职称分布情况",
  "2025年科研项目立项数量统计",
  "图书馆月度借阅量 Top10",
  "本科生毕业率与学位授予率",
  "各院系经费预算执行情况"
];

const isEmpty = computed(() => messages.value.length === 0);

function scrollToBottom(): void {
  nextTick(() => {
    if (conversationRef.value) {
      conversationRef.value.scrollTop = conversationRef.value.scrollHeight;
    }
  });
}

function buildMockReply(question: string): ChatMessage {
  if (question.includes("招生")) {
    return {
      id: `ai-${Date.now()}`,
      role: "ai",
      content:
        "已为您查询到本校近三年（2023-2025）本科生招生人数数据。整体呈稳定增长趋势，三年累计增长 564 人，年均增长率约 4.8%。详细数据如下表所示：",
      sql: `SELECT\n  year AS "年份",\n  student_count AS "招生人数"\nFROM dim_admission_summary\nWHERE year BETWEEN 2023 AND 2025\n  AND student_type = '本科生'\nORDER BY year ASC;`,
      result: {
        columns: [
          { name: "year", label: "年份", field: "year", align: "left" },
          { name: "count", label: "招生人数", field: "count", align: "right" },
          { name: "yoy", label: "同比增长", field: "yoy", align: "right" }
        ],
        rows: [
          { year: "2023年", count: "5,823", yoy: "—" },
          { year: "2024年", count: "6,102", yoy: "+4.8%" },
          { year: "2025年", count: "6,387", yoy: "+4.7%" }
        ]
      }
    };
  }

  if (question.includes("职称")) {
    return {
      id: `ai-${Date.now()}`,
      role: "ai",
      content: "已检索到各学院教师职称分布数据，结果如下表所示，可据此分析各学院师资结构。",
      sql: `SELECT\n  college AS "学院",\n  title AS "职称",\n  COUNT(*) AS "人数"\nFROM fact_teacher\nGROUP BY college, title\nORDER BY college, title;`,
      result: {
        columns: [
          { name: "college", label: "学院", field: "college", align: "left" },
          { name: "title", label: "职称", field: "title", align: "left" },
          { name: "count", label: "人数", field: "count", align: "right" }
        ],
        rows: [
          { college: "计算机学院", title: "教授", count: "42" },
          { college: "计算机学院", title: "副教授", count: "68" },
          { college: "经济管理学院", title: "教授", count: "35" },
          { college: "经济管理学院", title: "副教授", count: "57" }
        ]
      }
    };
  }

  return {
    id: `ai-${Date.now()}`,
    role: "ai",
    content:
      "已理解您的问题，正在从对应业务域检索数据。如需进一步细化查询条件（时间范围、院系、指标维度等），请补充说明，我将为您生成更精确的结果。",
    sql: `-- 根据您的问题生成的基础查询\nSELECT *\nFROM fact_data\nWHERE created_at >= '2025-01-01'\nLIMIT 100;`
  };
}

function sendMessage(text?: string): void {
  const content = (text ?? inputText.value).trim();
  if (!content || isThinking.value) return;

  const userMsg: ChatMessage = {
    id: `user-${Date.now()}`,
    role: "user",
    content
  };
  messages.value.push(userMsg);
  inputText.value = "";
  isThinking.value = true;
  scrollToBottom();

  const pendingId = `ai-pending-${Date.now()}`;
  messages.value.push({
    id: pendingId,
    role: "ai",
    content: t("aiQuery.thinking"),
    pending: true
  });
  scrollToBottom();

  setTimeout(() => {
    const idx = messages.value.findIndex((m) => m.id === pendingId);
    if (idx === -1) {
      isThinking.value = false;
      return;
    }
    const reply = buildMockReply(content);
    reply.id = pendingId;
    messages.value[idx] = reply;
    isThinking.value = false;
    scrollToBottom();
  }, 900);
}

function pickSample(q: string): void {
  sendMessage(q);
}

function clearChat(): void {
  if (messages.value.length === 0) return;
  $q.dialog({
    title: t("aiQuery.clearChat"),
    message: "确定要清空当前对话吗？",
    persistent: false,
    ok: { label: t("common.confirm"), unelevated: true, noCaps: true, color: "teal" },
    cancel: { label: t("common.cancel"), outline: true, noCaps: true }
  }).onOk(() => {
    messages.value = [];
    isThinking.value = false;
  });
}

function exportResult(): void {
  const hasResult = messages.value.some((m) => m.result);
  if (!hasResult) {
    $q.notify({ type: "warning", message: "当前无可导出的查询结果", position: "top" });
    return;
  }
  $q.notify({ type: "positive", message: "结果已导出", position: "top" });
}
</script>

<template>
  <div class="ai-query-page">
    <!-- ═══════════════ 页面头部 ═══════════════ -->
    <header class="page-header">
      <div class="page-header__main">
        <h1 class="page-title">{{ t('aiQuery.pageTitle') }}</h1>
        <p class="page-desc">{{ t('aiQuery.pageDesc') }}</p>
      </div>
      <div class="page-header__actions">
        <q-btn
          flat
          no-caps
          dense
          icon="sym_r_delete_sweep"
          :label="t('aiQuery.clearChat')"
          class="header-btn"
          @click="clearChat"
        />
        <q-btn
          unelevated
          no-caps
          dense
          icon="sym_r_download"
          :label="t('aiQuery.exportResult')"
          color="teal"
          class="header-btn--primary"
          @click="exportResult"
        />
      </div>
    </header>

    <!-- ═══════════════ 主体卡片 ═══════════════ -->
    <div class="ai-card">
      <!-- 对话区 -->
      <div ref="conversationRef" class="conversation">
        <!-- 欢迎语 -->
        <div v-if="isEmpty" class="welcome-block">
          <div class="welcome-avatar">
            <q-icon name="sym_r_smart_toy" size="48px" color="white" />
          </div>
          <p class="welcome-text">{{ t('aiQuery.welcome') }}</p>
          <div class="sample-grid">
            <div
              v-for="(q, idx) in sampleQuestions"
              :key="idx"
              class="sample-card"
              @click="pickSample(q)"
            >
              <q-icon name="sym_r_tips_and_updates" size="18px" class="sample-icon" />
              <span class="sample-text">{{ q }}</span>
              <q-icon name="sym_r_arrow_forward" size="16px" class="sample-arrow" />
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <template v-else>
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="msg-row"
            :class="msg.role === 'user' ? 'msg-row--user' : 'msg-row--ai'"
          >
            <div class="msg-avatar" :class="`msg-avatar--${msg.role}`">
              <q-icon
                :name="msg.role === 'user' ? 'sym_r_person' : 'sym_r_smart_toy'"
                size="20px"
                color="white"
              />
            </div>
            <div class="msg-bubble" :class="`msg-bubble--${msg.role}`">
              <p class="msg-text">{{ msg.content }}</p>

              <template v-if="msg.sql">
                <div class="sql-block">
                  <div class="sql-block__header">
                    <span class="sql-block__label">SQL</span>
                    <q-icon name="sym_r_code" size="14px" class="sql-block__icon" />
                  </div>
                  <pre class="sql-block__code"><code>{{ msg.sql }}</code></pre>
                </div>
              </template>

              <template v-if="msg.result">
                <q-table
                  :rows="msg.result.rows"
                  :columns="msg.result.columns"
                  row-key="year"
                  flat
                  dense
                  square
                  :rows-per-page-options="[0]"
                  hide-pagination
                  class="result-table"
                />
              </template>

              <div v-if="msg.pending" class="msg-thinking">
                <span class="thinking-dot"></span>
                <span class="thinking-dot"></span>
                <span class="thinking-dot"></span>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <q-input
          v-model="inputText"
          type="textarea"
          autogrow
          dense
          borderless
          :placeholder="t('aiQuery.inputPlaceholder')"
          :disable="isThinking"
          class="input-field"
          input-class="input-native"
          @keydown.enter.exact.prevent="sendMessage()"
        />
        <div class="input-actions">
          <q-btn
            flat
            no-caps
            dense
            icon="sym_r_delete_sweep"
            :label="t('aiQuery.clearChat')"
            class="input-clear-btn"
            @click="clearChat"
          />
          <q-btn
            unelevated
            no-caps
            dense
            icon-right="sym_r_send"
            :label="t('aiQuery.send')"
            color="teal"
            :disable="isThinking || !inputText.trim()"
            class="input-send-btn"
            @click="sendMessage()"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-query-page {
  padding: 24px;
  max-width: 1600px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* ═══════════════ 页面头部 ═══════════════ */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
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

.page-header__actions {
  display: flex;
  gap: 8px;
}

.header-btn {
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
}

.body--dark .header-btn {
  color: rgba(255, 255, 255, 0.65);
}

.header-btn--primary {
  font-size: 13px;
  font-weight: 500;
}

/* ═══════════════ 主体卡片 ═══════════════ */
.ai-card {
  flex: 1;
  min-height: 0;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.body--dark .ai-card {
  background: #2a2a2a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

.conversation {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 28px 32px;
}

/* —— 欢迎语 —— */
.welcome-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 48px;
}

.welcome-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #009688 0%, #00796b 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  box-shadow: 0 8px 24px rgba(0, 150, 136, 0.3);
}

.welcome-text {
  font-size: 15px;
  color: rgba(0, 0, 0, 0.7);
  line-height: 1.6;
  text-align: center;
  max-width: 560px;
  margin: 0 0 32px 0;
}

.body--dark .welcome-text {
  color: rgba(255, 255, 255, 0.7);
}

.sample-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  width: 100%;
  max-width: 720px;
}

.sample-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: rgba(0, 150, 136, 0.04);
  border: 1px solid rgba(0, 150, 136, 0.12);
  cursor: pointer;
  transition: all 0.2s ease;
}

.sample-card:hover {
  background: rgba(0, 150, 136, 0.08);
  border-color: #009688;
  transform: translateY(-1px);
}

.body--dark .sample-card {
  background: rgba(77, 182, 172, 0.06);
  border-color: rgba(77, 182, 172, 0.15);
}

.body--dark .sample-card:hover {
  background: rgba(77, 182, 172, 0.12);
  border-color: #4db6ac;
}

.sample-icon {
  color: #009688;
  flex-shrink: 0;
}

.body--dark .sample-icon {
  color: #4db6ac;
}

.sample-text {
  flex: 1;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.75);
  line-height: 1.4;
}

.body--dark .sample-text {
  color: rgba(255, 255, 255, 0.75);
}

.sample-arrow {
  color: rgba(0, 0, 0, 0.3);
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.body--dark .sample-arrow {
  color: rgba(255, 255, 255, 0.3);
}

.sample-card:hover .sample-arrow {
  color: #009688;
  transform: translateX(2px);
}

.body--dark .sample-card:hover .sample-arrow {
  color: #4db6ac;
}

/* —— 消息列表 —— */
.msg-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  align-items: flex-start;
}

.msg-row--user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.msg-avatar--user {
  background: #009688;
}

.msg-avatar--ai {
  background: linear-gradient(135deg, #1976d2 0%, #1565c0 100%);
}

.msg-bubble {
  max-width: 78%;
  padding: 14px 18px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.msg-bubble--user {
  background: #009688;
  color: #fff;
}

.msg-bubble--ai {
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.87);
}

.body--dark .msg-bubble--ai {
  background: #3a3a3a;
  color: rgba(255, 255, 255, 0.92);
}

.msg-text {
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-bubble--user .msg-text {
  color: #fff;
}

/* —— SQL 代码块 —— */
.sql-block {
  margin-top: 12px;
  background: #1e1e1e;
  overflow: hidden;
}

.sql-block__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.04);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.sql-block__label {
  font-size: 11px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 0.5px;
  font-family: "JetBrains Mono", monospace;
}

.sql-block__icon {
  color: rgba(255, 255, 255, 0.4);
}

.sql-block__code {
  margin: 0;
  padding: 14px 16px;
  font-family: "JetBrains Mono", monospace;
  font-size: 12.5px;
  line-height: 1.6;
  color: #4fc3f7;
  white-space: pre;
  overflow-x: auto;
}

/* —— 结果表格 —— */
.result-table {
  margin-top: 12px;
  background: rgba(0, 0, 0, 0.02) !important;
}

.result-table :deep(.q-table) {
  background: transparent;
}

.result-table :deep(.q-table thead th) {
  font-size: 12px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.65);
  background: rgba(0, 0, 0, 0.04);
  padding: 8px 12px;
}

.body--dark .result-table :deep(.q-table thead th) {
  color: rgba(255, 255, 255, 0.65);
  background: rgba(255, 255, 255, 0.04);
}

.result-table :deep(.q-table tbody td) {
  font-size: 13px;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  color: rgba(0, 0, 0, 0.8);
  font-family: "JetBrains Mono", monospace;
}

.body--dark .result-table :deep(.q-table tbody td) {
  border-bottom-color: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.8);
}

.result-table :deep(.q-table tbody tr:hover td) {
  background: rgba(0, 150, 136, 0.05);
}

/* —— 思考动画 —— */
.msg-thinking {
  display: flex;
  gap: 4px;
  align-items: center;
  padding-top: 2px;
}

.thinking-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.5;
  animation: thinking-bounce 1.2s infinite ease-in-out;
}

.thinking-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.thinking-dot:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes thinking-bounce {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-4px);
    opacity: 0.9;
  }
}

/* ═══════════════ 输入区 ═══════════════ */
.input-area {
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  padding: 16px 24px 20px;
  background: #fff;
}

.body--dark .input-area {
  border-top-color: rgba(255, 255, 255, 0.08);
  background: #2a2a2a;
}

.input-field {
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(0, 0, 0, 0.1);
  padding: 4px 12px;
  margin-bottom: 12px;
}

.input-field:focus-within {
  border-color: #009688;
  background: #fff;
}

.body--dark .input-field {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.12);
}

.body--dark .input-field:focus-within {
  border-color: #4db6ac;
  background: rgba(255, 255, 255, 0.06);
}

.input-field :deep(.input-native) {
  font-size: 14px;
  line-height: 1.6;
  padding: 8px 4px;
  color: rgba(0, 0, 0, 0.87);
  font-family: inherit;
  resize: none;
  max-height: 120px;
}

.body--dark .input-field :deep(.input-native) {
  color: rgba(255, 255, 255, 0.92);
}

.input-field :deep(.input-native::placeholder) {
  color: rgba(0, 0, 0, 0.4);
}

.body--dark .input-field :deep(.input-native::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.input-clear-btn {
  color: rgba(0, 0, 0, 0.55);
  font-size: 12px;
}

.body--dark .input-clear-btn {
  color: rgba(255, 255, 255, 0.55);
}

.input-send-btn {
  font-size: 14px;
  font-weight: 600;
  padding: 6px 24px;
  min-height: 36px;
}

/* ═══════════════ 滚动条 ═══════════════ */
.conversation::-webkit-scrollbar {
  width: 8px;
}

.conversation::-webkit-scrollbar-track {
  background: transparent;
}

.conversation::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 4px;
}

.conversation::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.25);
}

.body--dark .conversation::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
}

.body--dark .conversation::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.25);
}

/* ═══════════════ 响应式 ═══════════════ */
@media (max-width: 768px) {
  .ai-query-page {
    padding: 16px;
  }
  .page-header {
    flex-direction: column;
    gap: 12px;
  }
  .sample-grid {
    grid-template-columns: 1fr;
  }
  .msg-bubble {
    max-width: 88%;
  }
  .conversation {
    padding: 20px 16px;
  }
  .input-area {
    padding: 12px 16px 16px;
  }
}
</style>
