// 引入 Vite 客户端环境类型声明。
/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 图形验证码 GET 路径，相对 `VITE_API_BASE_URL`。 */
  readonly VITE_AUTH_CAPTCHA_PATH?: string;
}

declare module "*.vue" {
  import type { DefineComponent } from "vue";
  const component: DefineComponent<object, object, unknown>;
  export default component;
}
