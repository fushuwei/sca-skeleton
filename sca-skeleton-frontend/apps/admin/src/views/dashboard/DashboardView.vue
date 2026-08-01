<script setup lang="ts">
import { ref } from "vue";
import { showToast } from "@repo/shared"; // 全局 Toast 提示（与项目实际按钮调用的底层方法一致）。

/** 工作台指标占位（骨架演示，非真实业务统计） */
const metricCards = [
  { label: "已接入数据源", value: "12", unit: "个", icon: "sym_r_database", hint: "含测试环境 3 个" },
  { label: "注册数据资产", value: "1.8k", unit: "张表", icon: "sym_r_inventory_2", hint: "近 7 日新增 42" },
  { label: "今日调度实例", value: "326", unit: "次", icon: "sym_r_schedule", hint: "成功率 98.2%" },
  { label: "待处理告警", value: "3", unit: "条", icon: "sym_r_notifications_active", hint: "含 1 条严重" }
] as const;

// ═══════════════════════════════════════════════════════════════
// 通知组件测试用例
// ═══════════════════════════════════════════════════════════════
// 与项目实际按钮调用的底层方法（showToast）完全一致，用于验证不同类型、
// 不同内容长度下的样式表现（宽度自适应 300-600px、高度自适应 64-300px、
// 超长内容滚动、连续弹出替换等）。

interface NotifyTestCase {
  /** 按钮显示文案 */
  label: string;
  /** 按钮颜色（Quasar color 调色板） */
  color: string;
  /** 触发函数 */
  trigger: () => void;
  /** 用例说明 */
  hint: string;
}

/** 短消息（测试最小宽度 300px） */
const SHORT_MSG = "操作成功";
/** 中等消息（测试中等宽度，单行不换行） */
const MEDIUM_MSG = "用户角色已更新，新权限将在下次登录时生效。";
/** 长消息（测试最大宽度 600px，触发换行） */
const LONG_MSG = "数据源连接失败：无法连接到主机 192.168.1.100:3306。请检查网络配置、防火墙规则以及数据库服务是否正常运行，并在确认后重试。如问题持续存在，请联系系统管理员。";
/** 超长消息（测试 max-height 300px 滚动） */
const VERY_LONG_MSG = Array.from({ length: 12 }, (_, i) =>
  `第 ${i + 1} 行：这是一段用于测试通知框最大高度（300px）与垂直滚动效果的模拟文本内容，包含足够的字符量以触发高度上限。`
).join("\n");

const notifyTestCases: NotifyTestCase[] = [
  {
    label: "信息 · 短消息",
    color: "blue-7",
    hint: "info 类型，最短内容，触发 min-width 300px",
    trigger: () => showToast(SHORT_MSG, "info")
  },
  {
    label: "成功 · 中等消息",
    color: "green-7",
    hint: "positive 类型，单行中等长度",
    trigger: () => showToast(MEDIUM_MSG, "positive")
  },
  {
    label: "警告 · 长消息",
    color: "orange-7",
    hint: "warning 类型，长文本，触发换行接近 max-width 600px",
    trigger: () => showToast(LONG_MSG, "warning")
  },
  {
    label: "错误 · 超长消息 · 时长 60 秒",
    color: "red-7",
    hint: "negative 类型，超长文本，触发 max-height 300px 滚动",
    trigger: () => showToast(VERY_LONG_MSG, "negative", 60000)
  },
  {
    label: "信息 · 中等消息",
    color: "blue-7",
    hint: "info 类型，中等长度，对比类型配色",
    trigger: () => showToast(MEDIUM_MSG, "info")
  },
  {
    label: "成功 · 长消息",
    color: "green-7",
    hint: "positive 类型，长文本，对比类型配色",
    trigger: () => showToast(LONG_MSG, "positive")
  },
  {
    label: "自定义时长 10 秒",
    color: "deep-purple-7",
    hint: "info 类型，duration=3000ms，验证自定义时长",
    trigger: () => showToast("这条通知 10 秒后自动关闭", "info", 10000)
  },
  {
    label: "连续弹出替换测试",
    color: "grey-8",
    hint: "连续触发 4 次，验证「前一个消失、展示最新」不堆叠",
    trigger: () => {
      showToast("第一条通知", "info");
      setTimeout(() => showToast("第二条通知", "positive"), 1000);
      setTimeout(() => showToast("第三条通知", "warning"), 2000);
      setTimeout(() => showToast("第四条通知", "negative"), 3000);
    }
  }
];

// ═══════════════════════════════════════════════════════════════
// Loading 效果对比（临时验证用，确定后删除）
// ═══════════════════════════════════════════════════════════════

const loadingAVisible = ref(false);
const loadingBVisible = ref(false);
const loadingCVisible = ref(false);
const loadingDVisible = ref(false);
</script>

<template>
  <div class="workbench-page column q-gutter-y-lg">
    <!-- 指标卡 -->
    <div class="row q-col-gutter-md">
      <div v-for="m in metricCards" :key="m.label" class="col-12 col-sm-6 col-lg-3">
        <q-card flat bordered class="workbench-metric full-height">
          <q-card-section class="row items-start no-wrap">
            <q-avatar rounded color="primary" text-color="white" icon-size="28px" size="48px">
              <q-icon :name="m.icon" size="28px" />
            </q-avatar>
            <div class="column q-ml-md min-w-0">
              <div class="text-caption text-grey-7">{{ m.label }}</div>
              <div class="row items-baseline q-mt-xs">
                <span class="text-h5 text-weight-medium text-primary">{{ m.value }}</span>
                <span class="text-body2 text-grey-7 q-ml-xs">{{ m.unit }}</span>
              </div>
              <div class="text-caption text-grey-6 q-mt-xs">{{ m.hint }}</div>
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <!-- 通知组件测试 -->
    <q-card flat bordered class="workbench-notify-test">
      <q-card-section class="row items-center q-pb-none">
        <q-icon name="sym_r_notifications_active" size="22px" color="primary" class="q-mr-sm" />
        <span class="text-subtitle1 text-weight-medium">通知组件测试</span>
        <q-space />
        <span class="text-caption text-grey-6">
          调用底层 showToast 方法，与项目实际按钮弹框效果一致
        </span>
      </q-card-section>
      <q-card-section>
        <div class="row q-col-gutter-md">
          <div v-for="tc in notifyTestCases" :key="tc.label" class="col-12 col-sm-6 col-md-4 col-lg-3">
            <q-btn
              unelevated
              no-caps
              :color="tc.color"
              class="full-width notify-test-btn"
              padding="sm md"
              :label="tc.label"
              @click="tc.trigger()"
            >
              <q-tooltip class="bg-grey-9" max-width="280px">
                {{ tc.hint }}
              </q-tooltip>
            </q-btn>
          </div>
        </div>
      </q-card-section>
    </q-card>

    <!-- Loading 效果对比（临时验证用，确定后删除） -->
    <q-card flat bordered class="workbench-notify-test">
      <q-card-section class="row items-center q-pb-none">
        <q-icon name="sym_r_autorenew" size="22px" color="primary" class="q-mr-sm" />
        <span class="text-subtitle1 text-weight-medium">Loading 效果对比</span>
        <q-space />
        <span class="text-caption text-grey-6">
          点击按钮查看全屏 loading 效果，点击遮罩关闭
        </span>
      </q-card-section>
      <q-card-section>
        <div class="row q-col-gutter-md">
          <div class="col-12 col-sm-6 col-md-3">
            <q-btn
              unelevated
              no-caps
              color="blue-7"
              class="full-width notify-test-btn"
              padding="sm md"
              label="效果A：单环旋转（当前全局）"
              @click="loadingAVisible = true"
            />
          </div>
          <div class="col-12 col-sm-6 col-md-3">
            <q-btn
              unelevated
              no-caps
              color="deep-orange-7"
              class="full-width notify-test-btn"
              padding="sm md"
              label="效果B：双环叠放（对比方案）"
              @click="loadingBVisible = true"
            />
          </div>
          <div class="col-12 col-sm-6 col-md-3">
            <q-btn
              unelevated
              no-caps
              color="teal-7"
              class="full-width notify-test-btn"
              padding="sm md"
              label="效果C：A+B 同屏对比"
              @click="loadingCVisible = true"
            />
          </div>
          <div class="col-12 col-sm-6 col-md-3">
            <q-btn
              unelevated
              no-caps
              color="purple-7"
              class="full-width notify-test-btn"
              padding="sm md"
              label="效果D：A+B 同位叠放"
              @click="loadingDVisible = true"
            />
          </div>
        </div>
      </q-card-section>
    </q-card>
  </div>

  <!-- Loading 效果A：单环旋转（当前全局 loading） -->
  <div v-if="loadingAVisible" class="loading-overlay" @click="loadingAVisible = false">
    <div class="loading-overlay__spinner-a"></div>
    <div class="loading-overlay__text">正在加载...</div>
  </div>

  <!-- Loading 效果B：双环叠放（对比方案） -->
  <div v-if="loadingBVisible" class="loading-overlay" @click="loadingBVisible = false">
    <div class="loading-overlay__spinner-b"></div>
    <div class="loading-overlay__text">正在加载...</div>
  </div>

  <!-- Loading 效果C：A+B 同屏对比 -->
  <div v-if="loadingCVisible" class="loading-overlay" @click="loadingCVisible = false">
    <div class="loading-overlay__compare">
      <div class="loading-overlay__compare-item">
        <div class="loading-overlay__spinner-a"></div>
        <div class="loading-overlay__text">正在加载...</div>
      </div>
      <div class="loading-overlay__compare-item">
        <div class="loading-overlay__spinner-b"></div>
        <div class="loading-overlay__text">正在加载...</div>
      </div>
    </div>
  </div>

  <!-- Loading 效果D：A+B 同位叠放（按实际坐标显示，可能重叠） -->
  <div v-if="loadingDVisible" class="loading-overlay" @click="loadingDVisible = false">
    <div class="loading-overlay__stack">
      <div class="loading-overlay__spinner-a"></div>
      <div class="loading-overlay__spinner-b"></div>
    </div>
    <div class="loading-overlay__text">正在加载...</div>
  </div>
</template>

<style scoped>
.workbench-page {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

.workbench-metric {
  transition: box-shadow 0.2s ease;
}

.workbench-metric:hover {
  box-shadow: 0 2px 8px rgba(0, 121, 107, 0.12);
}

.workbench-notify-test {
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.notify-test-btn {
  border-radius: 0;
  font-weight: 500;
}

.notify-test-btn :deep(.q-btn__content) {
  justify-content: flex-start;
}

/* ═══ Loading 效果对比（临时验证用，确定后删除）═══ */

.loading-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 16px;
  background-color: #fff;
  cursor: pointer;
}

.loading-overlay__text {
  font-size: 16px;
  color: #666;
  font-family: system-ui, -apple-system, sans-serif;
}

/* 效果A：单环旋转（当前全局 loading） */
.loading-overlay__spinner-a {
  box-sizing: content-box;
  width: 40px;
  height: 40px;
  border: 3px solid #e0e0e0;
  border-top-color: #1976d2;
  border-radius: 50%;
  animation: loading-a-spin 0.8s linear infinite;
}

@keyframes loading-a-spin {
  to {
    transform: rotate(360deg);
  }
}

/* 效果B：双环叠放（对比方案） */
.loading-overlay__spinner-b {
  --clr: #1976d2;
  box-sizing: border-box;
  width: 46px;
  height: 46px;
  position: relative;
}

.loading-overlay__spinner-b:before,
.loading-overlay__spinner-b:after {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 100%;
  border: 3px solid transparent;
  border-top-color: var(--clr);
}

.loading-overlay__spinner-b:before {
  z-index: 100;
  animation: loading-b-spin 1s linear infinite;
}

.loading-overlay__spinner-b:after {
  border: 3px solid #ccc;
}

@keyframes loading-b-spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 效果C：A+B 同屏对比 */
.loading-overlay__compare {
  display: flex;
  gap: 80px;
  align-items: center;
}

.loading-overlay__compare-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

/* 效果D：A+B 同位叠放（按实际坐标居中重叠，不加区分标签） */
.loading-overlay__stack {
  position: relative;
  width: 50px;
  height: 50px;
}

.loading-overlay__stack > * {
  position: absolute;
  inset: 0;
  margin: auto;
}
</style>
