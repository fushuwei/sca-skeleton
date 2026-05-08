export interface LoginPayload { // 定义登录请求参数类型。
  username: string; // 定义用户名字段。
  password: string; // 定义密码字段。
  /** 图形验证码会话标识，由 GET 验证码接口返回；字段名需与后端一致时可再调整映射。 */
  captchaId?: string;
  /** 用户输入的验证码文本。 */
  captchaCode?: string;
} // 结束登录请求参数类型定义。

/** 获取图形验证码接口返回的业务数据（与后端约定一致后可在 apis/captcha 中做字段映射）。 */
export interface CaptchaData {
  captchaId: string;
  /** 图片：纯 base64、或已带 data:image/... 前缀、或 SVG 原文。 */
  image: string;
}

/** 菜单挂载的页面组件标识（仅叶子节点需要） */
export type MenuComponent = "DashboardView" | "UserCenterView" | "PlaceholderView";

export interface MenuItem { // 定义菜单节点类型（支持树形；无 component 的为分组节点）。
  name: string; // 定义路由名称字段（全树唯一；叶子与路由 name 一致）。
  path: string; // 定义节点路径（叶子为可访问路径；分组可为逻辑前缀）。
  title: string; // 定义菜单展示标题字段。
  /** 侧栏手风琴标题左侧图标，Material Symbols Rounded，如 sym_r_settings */
  icon?: string;
  component?: MenuComponent; // 定义页面组件标识字段，仅叶子节点必填。
  children?: MenuItem[]; // 定义子菜单集合。
} // 结束菜单节点类型定义。

export interface LoginResponse { // 定义登录返回结果类型。
  token: string; // 定义登录成功后的访问令牌字段。
  menus: MenuItem[]; // 定义登录后可访问菜单列表字段。
} // 结束登录返回结果类型定义。

export interface UserProfile { // 定义用户资料类型。
  id: string; // 定义用户唯一标识字段。
  username: string; // 定义用户名字段。
  nickname: string; // 定义用户昵称字段。
} // 结束用户资料类型定义。

export interface ApiEnvelope<T> { // 定义通用接口包裹类型。
  code: number; // 定义业务状态码字段。
  message: string; // 定义业务状态描述字段。
  data: T; // 定义业务数据字段。
} // 结束通用接口包裹类型定义。
