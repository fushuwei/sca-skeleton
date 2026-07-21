import { createApp } from "vue"; // 导入创建 Vue 应用的方法。
import { createPinia } from "pinia"; // 导入 Pinia 状态管理创建函数。
import { Quasar, Dialog, Notify } from "quasar"; // 导入 Quasar 主插件与 Dialog、Notify 插件。
import App from "./App.vue"; // 导入应用根组件。
import { router } from "./router"; // 导入路由实例。
import {
  createAdminI18n,
  getInitialLocale,
  quasarLangForLocale,
  readInitialDark
} from "./i18n"; // 应用文案与 Quasar 语言包对齐。
import "quasar/src/css/index.sass"; // 导入 Quasar 基础样式。
import "./styles/fonts-web.scss"; // 自托管 JetBrains Mono / OPPO Sans 的 @font-face（须先于 app-typography）
import "./styles/app-typography.scss"; // 全站字体栈（html 根节点与 Quasar 变量对齐）。
import "./styles/layout-overscroll.scss"; // 全局收紧过度滚动，避免整页橡皮筋
import "@quasar/extras/material-icons/material-icons.css"; // 导入 Material Icons 字体。
import "@quasar/extras/material-symbols-rounded/material-symbols-rounded.css"; // 导入 Material Symbols Rounded 字体。
import "./styles/material-symbols-axes.scss"; // 全站 Material Symbols 默认 FILL=0 等可变轴。
import "./styles/quasar-flat.scss"; // 导入全局直角风格样式覆盖。
import "./styles/quasar-notify.scss"; // Quasar Notify 企业级样式覆盖。
import "@repo/ui/styles/quasar-dialog.scss"; // Quasar Dialog 企业级样式覆盖。
import "./styles/admin-layout-dark.scss"; // AdminLayout 壳层在 Dark 模式下的颜色修补。
import { registerAdminTokenSync } from "./apis/http";
import { useAuthStore } from "./stores/auth";
import { setNotifier } from "@repo/shared";
import type { NotificationType } from "@repo/shared";

const app = createApp(App); // 创建 Vue 应用实例。
const pinia = createPinia(); // 创建 Pinia 状态管理实例。
const i18n = createAdminI18n(); // vue-i18n（与 localStorage 初始语言一致）。

const initialLocale = getInitialLocale(); // 与 createAdminI18n 内部读取同源。
const initialQuasarLang = quasarLangForLocale(initialLocale); // Quasar 组件文案语言包。
const initialDark = readInitialDark(); // 默认 Light（false）；详见 admin-theme-dark。

app.use(pinia); // 挂载 Pinia 到应用实例。
registerAdminTokenSync((accessToken, refreshToken) => {
  useAuthStore().syncOAuthTokens(accessToken, refreshToken);
});
app.use(router); // 挂载路由到应用实例。
app.use(i18n); // 挂载 vue-i18n。
app.use(Quasar, {
  plugins: { Dialog, Notify }, // 注册 Quasar Dialog 与 Notify 插件（$q.dialog / $q.notify 可用）。
  lang: initialQuasarLang, // 与 i18n locale 一致。
  config: {
    dark: initialDark // `$q.dark` 初始态；头部按钮再 toggle 并持久化。
  }
}); // 挂载 Quasar。

// ═══════════════ Quasar Notify 全局默认配置 ═══════════════
// 使用 Notify.setDefaults() 而非 quasar.config.notify，因为前者支持 actions 默认值（后者不支持）
Notify.setDefaults({
  position: "top", // 顶部居中滑入（参照 quasar.dev notify#positioning top 按钮）
  timeout: 0, // 禁用 Quasar 原生自动关闭（由自定义进度条 animationend 触发 dismiss）
  textColor: "white", // 文字+图标颜色统一白色
  progress: false, // 禁用 Quasar 原生进度条（改用自定义 ::after 伪元素，支持鼠标悬停暂停）
  actions: [{ icon: "sym_r_close", color: "white", round: true, dense: true }] // 右侧关闭按钮（Material Symbols Rounded）
});

// ═══════════════ Quasar Notify 注入 shared 包 ═══════════════
// 各类型对应的 Material Symbols Rounded 图标名（sym_r_ 前缀 = Material Symbols Rounded 字体）
// error=圆形感叹号 / check_circle=圆形对勾 / warning=三角感叹号 / info=圆形 i
const NOTIFY_ICONS: Record<NotificationType, string> = {
  negative: "sym_r_error",
  positive: "sym_r_check_circle",
  warning: "sym_r_warning",
  info: "sym_r_info"
};

// 各类型对应的 Quasar 调色板色阶（Light 主题用 Material 700 色阶，Dark 主题由 CSS 覆盖为 400 色阶）
const NOTIFY_COLORS: Record<NotificationType, string> = {
  negative: "red-7", // #d32f2f
  positive: "green-7", // #388e3c
  warning: "orange-7", // #f57c00
  info: "blue-7" // #1976d2
};

// 通知唯一 ID 计数器（用于精准定位 DOM 元素）
let notifyCounter = 0;

// 前一个通知的 dismiss 函数：新通知弹出前先 dismiss 前一个，确保同时只显示一个 toast
// 不使用 Quasar grouping（会显示数字徽章），改为手动 dismiss 实现替换效果
let previousDismiss: (() => void) | null = null;

setNotifier((message: string, type: NotificationType, duration?: number) => {
  // Dismiss 前一个通知，避免堆叠（不分组、不显示累计数字）。
  // 配合 quasar-notify.scss 中 .q-notification--top-move { transition: none } 解决连续触发时的
  // FLIP 晃动问题（仅禁用 move 动画，保留入场/离场滑入滑出动效）。
  if (previousDismiss) {
    previousDismiss();
    previousDismiss = null;
  }

  const actualDuration = duration && duration > 0 ? duration : 3000;
  const notifyId = `sca-notify-${++notifyCounter}`;

  const dismiss = Notify.create({
    message,
    icon: NOTIFY_ICONS[type], // 左侧图标（Material Icons）
    color: NOTIFY_COLORS[type], // 背景色（Quasar bg-{color} class）
    textColor: "white", // 显式覆盖 Quasar 内置 type 的默认 textColor（warning 默认 dark）
    timeout: 0, // 禁用 Quasar 自动关闭（由 animationend 触发）
    group: false, // 禁用 Quasar 分组（手动 dismiss 实现替换）
    classes: `sca-notify sca-notify--${type}`, // 注入类型 class，用于 Dark 主题配色覆盖 + 容器样式
    attrs: {
      "data-sca-notify-id": notifyId, // 用于精准定位 DOM 元素
      style: `--sca-notify-duration: ${actualDuration}ms` // 进度条时长（CSS 变量，::after animation 使用）
    }
  });

  previousDismiss = typeof dismiss === "function" ? dismiss : null;

  // 监听 animationend 事件触发 dismiss（与进度条完成同步）
  // Quasar 2.21.4 不支持 pauseOnHover，:hover 时 CSS animation-play-state: paused
  // 会暂停 animation，animationend 也同步延迟，确保进度条与自动关闭同步暂停/恢复
  requestAnimationFrame(() => {
    const el = document.querySelector(`[data-sca-notify-id="${notifyId}"]`);
    if (!el) return;

    // 为关闭按钮添加 aria-label，提升屏幕阅读器可访问性
    const closeBtn = el.querySelector<HTMLElement>(".q-notification__actions .q-btn");
    if (closeBtn) {
      closeBtn.setAttribute("aria-label", "关闭通知");
    }

    const onAnimationEnd = (event: AnimationEvent) => {
      if (event.animationName !== "sca-notify-progress") return;
      el.removeEventListener("animationend", onAnimationEnd);
      if (typeof dismiss === "function") dismiss();
      if (previousDismiss === dismiss) previousDismiss = null;
    };
    el.addEventListener("animationend", onAnimationEnd);
  });
});

app.mount("#app"); // 将应用挂载到页面根节点。

router.isReady()
  .catch(() => {
    // 导航守卫因 OAuth 登录跳转而中止导航（return false）时，
    // isReady() 的 Promise 会 reject，此处静默吞掉预期的导航中止错误，
    // 避免 Firefox 报告 Uncaught (in promise) 错误。
  })
  .finally(() => {
    document.getElementById("app-loading")?.remove(); // 路由就绪后移除首屏 loading 遮罩。
  });
