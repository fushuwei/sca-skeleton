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
import "@repo/ui/styles/fonts-web.scss"; // 自托管 JetBrains Mono 的 @font-face（须先于 app-typography）
import "./styles/app-typography.scss"; // 全站字体栈（html 根节点与 Quasar 变量对齐）。
import "./styles/layout-overscroll.scss"; // 全局收紧过度滚动，避免整页橡皮筋
import "@quasar/extras/material-icons/material-icons.css"; // 导入 Material Icons 字体。
// dev 用 @quasar/extras 完整字体（5MB，即写即显）；生产构建由 override-font-display 插件删除其 @font-face，
// 改用下方 material-symbols-axes.scss 的子集字体（~140KB）。须先于 axes.scss，确保子集 @font-face 覆盖完整字体。
import "@quasar/extras/material-symbols-rounded/material-symbols-rounded.css"; // Material Symbols Rounded 完整字体。
import "./styles/material-symbols-axes.scss"; // Material Symbols Rounded：子集 @font-face + 类定义 + 可变轴。
import "./styles/quasar-flat.scss"; // 导入全局直角风格样式覆盖。
import "@repo/ui/styles/quasar-notify.scss"; // Quasar Notify 企业级样式覆盖（admin/portal 共享）。
import "@repo/ui/styles/quasar-dialog.scss"; // Quasar Dialog 企业级样式覆盖。
import "./styles/admin-layout-dark.scss"; // AdminLayout 壳层在 Dark 模式下的颜色修补。
import { registerAdminTokenSync, registerAdminTranslator } from "./apis/http";
import { useAuthStore } from "./stores/auth";
import { setupQuasarNotify } from "@repo/ui";
import { createMaterialSymbolsIconMapFn } from "@repo/ui/setup-icon-map";
import { vMaskClose } from "./directives/maskClose";

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
// 注入 i18n 翻译函数，供 HTTP 错误拦截器国际化提示文案（超时、网络异常等）
registerAdminTranslator((key, params) => i18n.global.t(key, params as Record<string, string | number>));
app.use(router); // 挂载路由到应用实例。
app.use(i18n); // 挂载 vue-i18n。
app.use(Quasar, {
  plugins: { Dialog, Notify }, // 注册 Quasar Dialog 与 Notify 插件（$q.dialog / $q.notify 可用）。
  lang: initialQuasarLang, // 与 i18n locale 一致。
  config: {
    dark: initialDark // `$q.dark` 初始态；头部按钮再 toggle 并持久化。
  }
}); // 挂载 Quasar。

// 全局指令：点击抽屉蒙层关闭（集中处理 Chrome 拖拽选中误关闭，供所有本地抽屉复用）。
app.directive("mask-close", vMaskClose);

// Quasar 运行时只读 $q.config.iconMapFn（quasar.client.js:944），但 QuasarUIConfiguration 类型未声明该字段，
// 放 config 内会 TS 报错。Quasar install 后通过 $q.iconMapFn setter（injectProp 注入）赋值，绕开类型与运行时不一致。
// build 时用 PUA 码位渲染（绕开 ligature）；dev 时映射为空，fallback 到 ligature。
app.config.globalProperties.$q.iconMapFn = createMaterialSymbolsIconMapFn();

// 配置 Quasar Notify 全局默认 + 注入到 @repo/shared 的 showToast（admin/portal 共享实现）
setupQuasarNotify();

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
