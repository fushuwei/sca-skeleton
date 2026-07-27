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
import "@repo/ui/styles/fonts-web.scss";
import "./styles/app-typography.scss";
import "./styles/layout-overscroll.scss";
import "@quasar/extras/material-icons/material-icons.css";
import "@quasar/extras/material-symbols-rounded/material-symbols-rounded.css";
import "./styles/material-symbols-axes.scss";
import "./styles/quasar-flat.scss";
import "@repo/ui/styles/quasar-notify.scss";
import "@repo/ui/styles/quasar-dialog.scss";
import { registerPortalTokenSync } from "./apis/http";
import { usePortalAuthStore } from "./stores/auth";
import { setupQuasarNotify } from "@repo/ui";

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

// 配置 Quasar Notify 全局默认 + 注入到 @repo/shared 的 showToast（admin/portal 共享实现）
setupQuasarNotify();

app.mount("#app");

router.isReady()
  .catch(() => {
    // 导航守卫因 OAuth 登录跳转而中止导航（return false）时，
    // isReady() 的 Promise 会 reject，此处静默吞掉预期的导航中止错误。
  })
  .finally(() => {
    document.getElementById("app-loading")?.remove();
  });
