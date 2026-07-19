/**
 * 全局 Toast 提示组件
 *
 * 布局与 auth 登录页的 Toast Tips 完全一致：
 * - 无圆角矩形，固定在浏览器顶部居中
 * - 左侧图标 + 文字 + 右侧关闭按钮
 * - 5 秒后自动消失，支持手动关闭
 *
 * 支持 4 种类型：negative（红）、positive（绿）、warning（琥珀）、info（蓝）
 */

const ICONS = {
  negative: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>`,
  positive: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>`,
  warning: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg>`,
  info: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/></svg>`
} as const;

const CLOSE_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>`;

const TYPE_COLORS: Record<string, { bg: string; color: string; icon: string }> = {
    negative: { bg: '#000', color: '#fff', icon: '#ef4444' },
    positive: { bg: '#000', color: '#fff', icon: '#22c55e' },
    warning: { bg: '#000', color: '#fff', icon: '#f59e0b' },
    info: { bg: '#000', color: '#fff', icon: '#60a5fa' }
};

const BASE_STYLE = `
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10000;
  display: flex;
  align-items: center;
  min-height: 64px;
  max-height: 300px;
  max-width: 600px;
  box-sizing: border-box;
  padding: 12px 16px;
  border-radius: 0;
  white-space: normal;
  word-break: break-word;
  overflow: hidden;
  opacity: 0;
  transition: opacity 0.25s ease;
  pointer-events: auto;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.5;
  font-family: inherit;
`;

const ICON_STYLE = `
  font-size: 18px;
  flex-shrink: 0;
  margin-right: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
`;

const CLOSE_BTN_STYLE = `
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  margin-left: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  opacity: 0.7;
  padding: 0;
`;

const TEXT_STYLE = `
  flex: 1 1 auto;
  min-width: 0;
  white-space: normal;
  word-break: break-word;
  overflow: hidden;
`;

const CLOSE_ICON_STYLE = `
  font-size: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
`;

const FADE_IN_KEYFRAMES = `
  @keyframes scaToastFadeIn {
    from {
      opacity: 0;
      transform: translateX(-50%) translateY(-12px);
    }
    to {
      opacity: 1;
      transform: translateX(-50%) translateY(0);
    }
  }
`;

export type NotificationType = "negative" | "positive" | "warning" | "info";

let toastTimer: ReturnType<typeof setTimeout> | null = null;

function dismissToast(): void {
  if (toastTimer) {
    clearTimeout(toastTimer);
    toastTimer = null;
  }
  const existing = document.querySelector(".sca-toast");
  if (existing) {
    (existing as HTMLElement).style.opacity = "0";
    existing.remove();
  }
}

/** 各类型默认自动关闭时长（毫秒）：成功类提示较短，警告/错误类提示需更长时间供用户阅读 */
const DEFAULT_DURATION: Record<NotificationType, number> = {
  positive: 5000,
  info: 5000,
  warning: 10000,
  negative: 10000
};

/**
 * 在浏览器顶部居中显示 Toast 提示，与 auth 登录页的 Toast Tips 布局完全一致。
 *
 * @param message 提示文本
 * @param type 通知类型：negative（红）、positive（绿）、warning（琥珀）、info（蓝），默认 negative
 * @param duration 自动关闭时间（毫秒），不传则按类型取默认值：positive/info=5000ms，warning/negative=10000ms
 */
export function showToast(message: string, type: NotificationType = "negative", duration?: number): void {
  dismissToast();

  // 注入 keyframes（只注入一次）
  if (!document.getElementById("sca-toast-keyframes")) {
    const style = document.createElement("style");
    style.id = "sca-toast-keyframes";
    style.textContent = FADE_IN_KEYFRAMES;
    document.head.appendChild(style);
  }

  const colors = TYPE_COLORS[type] || TYPE_COLORS.negative;
  const autoCloseMs = duration ?? DEFAULT_DURATION[type];

  // 容器
  const toast = document.createElement("div");
  toast.className = "sca-toast";
  toast.setAttribute("style", `${BASE_STYLE} background-color: ${colors.bg}; color: ${colors.color};`);

  // 左侧图标
  const icon = document.createElement("span");
  icon.setAttribute("style", `${ICON_STYLE} color: ${colors.icon};`);
  icon.innerHTML = ICONS[type] || ICONS.negative;

  // 文本
  const text = document.createElement("span");
  text.setAttribute("style", TEXT_STYLE);
  text.textContent = message;

  // 右侧关闭按钮
  const closeBtn = document.createElement("button");
  closeBtn.type = "button";
  closeBtn.setAttribute("style", `${CLOSE_BTN_STYLE} color: ${colors.color};`);
  closeBtn.addEventListener("click", (e) => {
    e.preventDefault();
    e.stopPropagation();
    dismissToast();
  });

  const closeIcon = document.createElement("span");
  closeIcon.setAttribute("style", CLOSE_ICON_STYLE);
  closeIcon.innerHTML = CLOSE_SVG;
  closeBtn.appendChild(closeIcon);

  toast.appendChild(icon);
  toast.appendChild(text);
  toast.appendChild(closeBtn);
  document.body.appendChild(toast);

  // 显示动画
  toast.style.animation = "scaToastFadeIn 0.25s ease forwards";

  // 自动关闭
  toastTimer = setTimeout(() => dismissToast(), autoCloseMs);
}
