import { Notify } from "quasar";
import { setNotifier, type NotificationType } from "@repo/shared";

/**
 * 各类型对应的 Material Symbols Rounded 图标名（sym_r_ 前缀 = Material Symbols Rounded 字体）
 * error=圆形感叹号 / check_circle=圆形对勾 / warning=三角感叹号 / info=圆形 i
 */
const NOTIFY_ICONS: Record<NotificationType, string> = {
  negative: "sym_r_error",
  positive: "sym_r_check_circle",
  warning: "sym_r_warning",
  info: "sym_r_info"
};

/**
 * 各类型对应的 Quasar 调色板色阶（Light 主题用 Material 700 色阶，Dark 主题由 quasar-notify.scss 覆盖为 400 色阶）
 */
const NOTIFY_COLORS: Record<NotificationType, string> = {
  negative: "red-7", // #d32f2f（Light 主题由 Quasar bg-red-7 控制，Dark 主题由 quasar-notify.scss 覆盖为 red-4）
  positive: "green-7", // #388e3c（Light + Dark 均由 quasar-notify.scss 覆盖为品牌绿 #21BA45）
  warning: "orange-7", // #f57c00
  info: "blue-7" // #1976d2
};

/**
 * 配置 Quasar Notify 全局默认 + 注入到 @repo/shared 的 showToast
 *
 * 应在各 app 的 main.ts 启动阶段调用一次（在 `app.use(Quasar, { plugins: { Notify } })` 之后）：
 * 1. 调用 Notify.setDefaults() 设置全局默认（position/timeout/textColor/progress/actions）
 * 2. 调用 setNotifier() 注入 Quasar Notify 实现到 shared 包，使 showToast 委托给 Quasar Notify
 *
 * 配合 `@repo/ui/styles/quasar-notify.scss` 样式覆盖使用，该 SCSS 提供容器尺寸、入场/离场动画、
 * Dark 主题配色、底部进度条等企业级样式。
 *
 * 实现要点：
 * - 使用 Notify.setDefaults() 而非 quasar.config.notify，因为前者支持 actions 默认值（后者不支持）
 * - timeout: 0 禁用 Quasar 原生自动关闭，由自定义进度条 animationend 触发 dismiss（支持 hover 暂停）
 * - progress: false 禁用 Quasar 原生进度条，改用 ::after 伪元素 + CSS animation 自管理
 * - group: false 禁用 Quasar 分组（手动 dismiss 前一个实现替换效果，不显示累计数字徽章）
 * - classes: `sca-notify sca-notify--${type}` 注入类型 class，用于 Dark 主题配色覆盖 + 容器样式
 */
export function setupQuasarNotify(): void {
  // 使用 Notify.setDefaults() 而非 quasar.config.notify，因为前者支持 actions 默认值（后者不支持）
  Notify.setDefaults({
    position: "top", // 顶部居中滑入（参照 quasar.dev notify#positioning top 按钮）
    timeout: 0, // 禁用 Quasar 原生自动关闭（由自定义进度条 animationend 触发 dismiss）
    textColor: "white", // 文字+图标颜色统一白色
    progress: false, // 禁用 Quasar 原生进度条（改用自定义 ::after 伪元素，支持鼠标悬停暂停）
    actions: [{ icon: "sym_r_close", color: "white", round: true, dense: true }] // 右侧关闭按钮（Material Symbols Rounded）
  });

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
      icon: NOTIFY_ICONS[type], // 左侧图标（Material Symbols Rounded）
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

      const onAnimationEnd = ((event: Event) => {
        const animEvent = event as AnimationEvent;
        if (animEvent.animationName !== "sca-notify-progress") return;
        el.removeEventListener("animationend", onAnimationEnd);
        if (typeof dismiss === "function") dismiss();
        if (previousDismiss === dismiss) previousDismiss = null;
      }) as EventListener;
      el.addEventListener("animationend", onAnimationEnd);
    });
  });
}
