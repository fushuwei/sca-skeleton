import { createApp } from "vue"; // 导入创建 Vue 应用的方法。
import { createPinia } from "pinia"; // 导入 Pinia 状态管理创建函数。
import { Quasar, Dialog } from "quasar"; // 导入 Quasar 主插件与 Dialog 插件。
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
import "./styles/admin-layout-dark.scss"; // AdminLayout 壳层在 Dark 模式下的颜色修补。
import { registerAdminTokenSync } from "./apis/http";
import { useAuthStore } from "./stores/auth";

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
  plugins: { Dialog }, // 注册 Quasar Dialog 插件（$q.dialog 可用）。
  lang: initialQuasarLang, // 与 i18n locale 一致。
  config: {
    dark: initialDark // `$q.dark` 初始态；头部按钮再 toggle 并持久化。
  }
}); // 挂载 Quasar。

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
