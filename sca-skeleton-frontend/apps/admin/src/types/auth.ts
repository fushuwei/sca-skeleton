/** 获取图形验证码接口返回的业务数据（与后端约定一致后可在 apis/captcha 中做字段映射）。 */
export interface CaptchaData {
  captchaId: string;
  /** 图片：纯 base64、或已带 data:image/... 前缀、或 SVG 原文。 */
  image: string;
}

/** 菜单挂载的页面组件标识（仅叶子节点需要） */
export type MenuComponent = "DashboardView" | "UserCenterView" | "PlaceholderView";

export interface MenuItem {
  name: string;
  path: string;
  title: string;
  /** 侧栏手风琴标题左侧图标，Material Symbols Rounded，如 sym_r_settings */
  icon?: string;
  component?: MenuComponent;
  children?: MenuItem[];
}

export interface UserProfile {
  id: string;
  username: string;
  nickname: string;
}

export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
}

/** OAuth2 PKCE 回调写入的令牌对（Pinia / localStorage）。 */
export interface OAuthTokenPair {
  accessToken: string;
  refreshToken?: string;
}
