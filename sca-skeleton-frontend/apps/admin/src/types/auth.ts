/** 获取图形验证码接口返回的业务数据（与后端约定一致后可在 apis/captcha 中做字段映射）。 */
export interface CaptchaData {
  captchaId: string;
  /** 图片：纯 base64、或已带 data:image/... 前缀、或 SVG 原文。 */
  image: string;
}

/** 菜单挂载的页面组件标识（仅叶子节点需要） */
export type MenuComponent = "DashboardView" | "UserCenterView" | "PlaceholderView" | "UserListView";

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

// ── 用户管理相关类型 ──

/** 系统用户实体（对应后端 SysUser） */
export interface SysUser {
  id: string;
  tenantId: string;
  username: string;
  nickname: string;
  realName: string;
  gender: string;
  avatar: string;
  phone: string;
  email: string;
  /** 用户类别：backend / frontend */
  userCategory: string;
  /** 用户类型：superadmin / tenant_admin / dept_admin / normal */
  userType: string;
  /** 状态：active / inactive / locked / frozen / expired / disabled / cancelled */
  status: string;
  statusTime: string;
  statusReason: string;
  loginFailCount: number;
  mustChangePassword: number;
  passwordUpdateTime: string;
  effectiveStartTime: string;
  effectiveEndTime: string;
  lastLoginIp: string;
  lastLoginTime: string;
  /** 是否系统内置：0-否，1-是 */
  isBuiltin: number;
  /** 数据来源：initial / manual / import / sync / sso */
  sourceType: string;
  remark: string;
  createTime: string;
  updateTime: string;
  createBy: string;
  updateBy: string;
}

/** 用户分页查询请求参数 */
export interface UserPageRequest {
  pageNum?: number;
  pageSize?: number;
  username?: string;
  nickname?: string;
  status?: string;
  deptId?: string;
  orderBy?: string;
  orderDirection?: "asc" | "desc";
}

/** MyBatis-Plus 分页响应 */
export interface IPage<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

// ── 部门管理相关类型 ──

/** 系统部门实体（对应后端 SysDept） */
export interface SysDept {
  id: string;
  tenantId: string;
  /** 上级部门ID，顶级为 "0" */
  parentId: string;
  name: string;
  code: string;
  sort: number;
  leader: string;
  phone: string;
  email: string;
  /** 状态：enabled / disabled */
  status: string;
  /** ID 层级路径，逗号分隔 */
  treePath: string;
  createTime: string;
  updateTime: string;
}

/** 部门树节点（用于 q-tree） */
export interface DeptTreeNode {
  id: string;
  label: string;
  parentId: string;
  children?: DeptTreeNode[];
  count?: number;
}

// ── 岗位管理相关类型 ──

/** 系统岗位实体（对应后端 SysPost） */
export interface SysPost {
  id: string;
  tenantId: string;
  name: string;
  code: string;
  sort: number;
  remark: string;
  createTime: string;
  updateTime: string;
}

// ── 角色管理相关类型 ──

/** 系统角色实体（对应后端 SysRole） */
export interface SysRole {
  id: string;
  tenantId: string;
  name: string;
  code: string;
  /** 数据权限范围：all / tenant / dept_and_sub / dept / personal / custom */
  dataScope: string;
  isBuiltin: number;
  sort: number;
  remark: string;
  createTime: string;
  updateTime: string;
}
