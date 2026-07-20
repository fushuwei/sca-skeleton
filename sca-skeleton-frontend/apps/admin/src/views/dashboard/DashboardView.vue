<script setup lang="ts">
import { computed } from "vue"; // 导入计算属性函数。
import { useAuthStore } from "../../stores/auth"; // 导入鉴权仓库。
import { showToast } from "@repo/shared"; // 全局 Toast 提示（与项目实际按钮调用的底层方法一致）。

const authStore = useAuthStore(); // 获取鉴权仓库实例。

const nickname = computed(() => authStore.profile?.nickname ?? "管理员"); // 展示昵称。
const loginHint = computed(() => authStore.profile?.username ?? "admin"); // 登录名提示。

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
    label: "错误 · 超长消息",
    color: "red-7",
    hint: "negative 类型，超长文本，触发 max-height 300px 滚动",
    trigger: () => showToast(VERY_LONG_MSG, "negative")
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
    label: "自定义时长 3 秒",
    color: "deep-purple-7",
    hint: "info 类型，duration=3000ms，验证自定义时长",
    trigger: () => showToast("这条通知 3 秒后自动关闭", "info", 3000)
  },
  {
    label: "连续弹出替换测试",
    color: "grey-8",
    hint: "连续触发 3 次，验证「前一个消失、展示最新」不堆叠",
    trigger: () => {
      showToast("第一条通知", "info");
      setTimeout(() => showToast("第二条通知", "warning"), 300);
      setTimeout(() => showToast("第三条通知", "negative"), 600);
    }
  }
];
</script>

<template>
  <div class="workbench-page column q-gutter-y-lg">
    <!-- 欢迎区 -->
    <q-card flat bordered class="workbench-hero overflow-hidden">
      <q-card-section class="workbench-hero__inner row items-center no-wrap">
        <q-avatar rounded color="white" text-color="primary" size="56px" class="workbench-hero__avatar">
          <span class="text-h6 text-weight-medium">{{ nickname.slice(0, 1) || "U" }}</span>
        </q-avatar>
        <div class="column q-ml-md min-w-0">
          <div class="text-h6 text-white text-weight-medium ellipsis">
            {{ nickname }}，欢迎使用工作台
          </div>
          <div class="text-body2 workbench-hero__sub q-mt-xs ellipsis">
            数据中台管理系统 · 登录账号 {{ loginHint }}
          </div>
        </div>
        <q-space />
        <div class="gt-xs text-body2 text-white text-weight-medium workbench-hero__date">
          {{ new Date().toLocaleDateString("zh-CN", { weekday: "long", year: "numeric", month: "long", day: "numeric" }) }}
        </div>
      </q-card-section>
    </q-card>

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

    <!-- 快捷说明 -->
    <q-banner rounded class="workbench-tip bg-grey-2 text-grey-9">
      <template #avatar>
        <q-icon name="sym_r_lightbulb" color="primary" size="28px" />
      </template>
      <div class="text-body2">
        工作台为默认首页，可通过左侧<strong class="text-weight-medium">系统导航</strong>打开各业务模块；打开的页面会以标签页形式保留在主区域顶部，便于来回切换。
      </div>
    </q-banner>
  </div>
</template>

<style scoped>
.workbench-page {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

.workbench-hero {
  border: none;
  background: linear-gradient(120deg, #00796b 0%, #009688 55%, #26a69a 100%);
}

.workbench-hero__inner {
  padding: 20px 24px;
}

.workbench-hero__sub {
  opacity: 0.92;
}

.workbench-hero__date {
  opacity: 0.95;
}

.workbench-hero__avatar {
  border: 2px solid rgba(255, 255, 255, 0.35);
}

.workbench-metric {
  transition: box-shadow 0.2s ease;
}

.workbench-metric:hover {
  box-shadow: 0 2px 8px rgba(0, 121, 107, 0.12);
}

.workbench-tip {
  border: 1px solid rgba(0, 0, 0, 0.06);
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
</style>
