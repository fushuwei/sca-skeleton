import { createApp } from "vue";
import { createPinia } from "pinia";
import { Quasar, Dialog, Notify } from "quasar";
import App from "./App.vue";
import { router } from "./router";
import {
  createPortalI18n,
  getInitialLocale,
  quasarLangForLocale,
  readInitialDark
} from "./i18n";
import "quasar/src/css/index.sass";
import "./styles/fonts-web.scss";
import "./styles/app-typography.scss";
import "./styles/layout-overscroll.scss";
import "@quasar/extras/material-icons/material-icons.css";
import "@quasar/extras/material-symbols-rounded/material-symbols-rounded.css";
import "./styles/material-symbols-axes.scss";
import "./styles/quasar-flat.scss";
import "./styles/quasar-notify.scss";
import "@repo/ui/styles/quasar-dialog.scss";
import { registerPortalTokenSync } from "./apis/http";
import { usePortalAuthStore } from "./stores/auth";
import { setNotifier } from "@repo/shared";
import type { NotificationType } from "@repo/shared";

const app = createApp(App);
const pinia = createPinia();
const i18n = createPortalI18n();

const initialLocale = getInitialLocale();
const initialQuasarLang = quasarLangForLocale(initialLocale);
const initialDark = readInitialDark();

app.use(pinia);
registerPortalTokenSync((accessToken, refreshToken) => {
  usePortalAuthStore().syncOAuthTokens(accessToken, refreshToken);
});
app.use(router);
app.use(i18n);
app.use(Quasar, {
  plugins: { Dialog, Notify },
  lang: initialQuasarLang,
  config: {
    dark: initialDark
  }
});

Notify.setDefaults({
  position: "top",
  timeout: 0,
  textColor: "white",
  progress: false,
  actions: [{ icon: "sym_r_close", color: "white", round: true, dense: true }]
});

const NOTIFY_ICONS: Record<NotificationType, string> = {
  negative: "sym_r_error",
  positive: "sym_r_check_circle",
  warning: "sym_r_warning",
  info: "sym_r_info"
};

const NOTIFY_COLORS: Record<NotificationType, string> = {
  negative: "red-7",
  positive: "green-7",
  warning: "orange-7",
  info: "blue-7"
};

let notifyCounter = 0;
let previousDismiss: (() => void) | null = null;

setNotifier((message: string, type: NotificationType, duration?: number) => {
  if (previousDismiss) {
    previousDismiss();
    previousDismiss = null;
  }

  const actualDuration = duration && duration > 0 ? duration : 3000;
  const notifyId = `sca-notify-${++notifyCounter}`;

  const dismiss = Notify.create({
    message,
    icon: NOTIFY_ICONS[type],
    color: NOTIFY_COLORS[type],
    textColor: "white",
    timeout: 0,
    group: false,
    classes: `sca-notify sca-notify--${type}`,
    attrs: {
      "data-sca-notify-id": notifyId,
      style: `--sca-notify-duration: ${actualDuration}ms`
    }
  });

  previousDismiss = typeof dismiss === "function" ? dismiss : null;

  requestAnimationFrame(() => {
    const el = document.querySelector(`[data-sca-notify-id="${notifyId}"]`);
    if (!el) return;

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

app.mount("#app");

router.isReady()
  .catch(() => {
    // 导航守卫因 OAuth 登录跳转而中止导航（return false）时，
    // isReady() 的 Promise 会 reject，此处静默吞掉预期的导航中止错误。
  })
  .finally(() => {
    document.getElementById("app-loading")?.remove();
  });
