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
// dev 用 @quasar/extras 完整字体（5MB，即写即显）；生产构建由 override-font-display 插件删除其 @font-face，
// 改用下方 material-symbols-axes.scss 的子集字体（~140KB）。须先于 axes.scss，确保子集 @font-face 覆盖完整字体。
import "@quasar/extras/material-symbols-rounded/material-symbols-rounded.css"; // Material Symbols Rounded 完整字体。
import "./styles/material-symbols-axes.scss"; // Material Symbols Rounded：子集 @font-face + 类定义 + 可变轴。
import "./styles/quasar-flat.scss";
import "@repo/ui/styles/quasar-notify.scss";
import "@repo/ui/styles/quasar-dialog.scss";
import { registerPortalTokenSync } from "./apis/http";
import { usePortalAuthStore } from "./stores/auth";
import { setupQuasarNotify } from "@repo/ui";
import { createMaterialSymbolsIconMapFn } from "@repo/ui/setup-icon-map";

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

// Quasar 运行时只读 $q.config.iconMapFn（quasar.client.js:944），但 QuasarUIConfiguration 类型未声明该字段，
// 放 config 内会 TS 报错。Quasar install 后通过 $q.iconMapFn setter（injectProp 注入）赋值，绕开类型与运行时不一致。
// build 时用 PUA 码位渲染（绕开 ligature）；dev 时映射为空，fallback 到 ligature。
app.config.globalProperties.$q.iconMapFn = createMaterialSymbolsIconMapFn();

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
