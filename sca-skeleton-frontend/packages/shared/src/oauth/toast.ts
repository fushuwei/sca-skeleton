/**
 * 全局 Toast 提示组件
 *
 * 通过 {@link setNotifier} 注入框架原生实现（如 Quasar Notify、Ant Design Vue message），
 * 注入后 showToast 将委托给原生实现，享受原生 pauseOnHover / 动画 / 无障碍等能力。
 *
 * 支持 4 种类型：negative（红）、positive（绿）、warning（琥珀）、info（蓝）
 *
 * 注意：已移除内置的基于原生 DOM 的 fallback 实现。各 app 必须在 main.ts 启动阶段
 * 调用 setNotifier 注入框架原生 notifier，否则 showToast 将打印 console.warn 且不显示通知。
 * （auth 服务后端登录页 login.js 有独立的 toast 实现，不依赖此模块）
 */

export type NotificationType = "negative" | "positive" | "warning" | "info";

/**
 * Notifier 接口：由各 app 启动时注入实现
 * <p>
 * 返回值用于支持手动关闭场景（如 Quasar Notify 返回的 dismiss 函数），无此需求时可忽略
 */
export type Notifier = (message: string, type: NotificationType, duration?: number) => (() => void) | void;

let injectedNotifier: Notifier | null = null;

/**
 * 注入框架原生 notifier 实现
 * <p>
 * 应在各 app 的 main.ts 启动阶段调用一次，注入后所有 showToast 调用都将委托给该实现。
 *
 * @param notifier notifier 实现，传 null 可清除注入
 */
export function setNotifier(notifier: Notifier | null): void {
  injectedNotifier = notifier;
}

/**
 * 显示 Toast 提示
 * <p>
 * 委托给通过 {@link setNotifier} 注入的框架原生实现（如 Quasar Notify）。
 * 未注入时打印 console.warn 且不显示通知。
 *
 * @param message 提示文本
 * @param type 通知类型：negative（红）、positive（绿）、warning（琥珀）、info（蓝），默认 negative
 * @param duration 自动关闭时间（毫秒），不传由 notifier 默认值决定
 */
export function showToast(message: string, type: NotificationType = "negative", duration?: number): void {
  if (injectedNotifier) {
    injectedNotifier(message, type, duration);
    return;
  }
  console.warn("[showToast] 未注入 notifier 实现，通知将被忽略。请在 app 启动阶段调用 setNotifier 注入框架原生 notifier。", { message, type, duration });
}
