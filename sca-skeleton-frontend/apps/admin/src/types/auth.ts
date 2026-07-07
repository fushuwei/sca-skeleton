/** 获取图形验证码接口返回的业务数据（与后端约定一致后可在 apis/captcha 中做字段映射）。 */
export interface CaptchaData {
  captchaId: string;
  /** 图片：纯 base64、或已带 data:image/... 前缀、或 SVG 原文。 */
  image: string;
}

/** 菜单挂载的页面组件标识（仅叶子节点需要） */
export type MenuComponent = "DashboardView" | "UserCenterView" | "PlaceholderView" | "UserListView" | "MenuListView" | "RoleListView" | "PostListView" | "DeptListView";

export interface MenuItem {
  /** 主键ID（对应 SQL id 字段），唯一标识 */
  id: string;
  /** 权限名称（对应 SQL name 字段），用于侧边栏显示 */
  name: string;
  /** 路由路径（对应 SQL path 字段） */
  path: string;
  /** 组件标识（对应 SQL component 字段），用于 Vue Router name 参数 */
  component?: MenuComponent;
  /** 图标（对应 SQL icon 字段） */
  icon?: string;
  /** 子菜单 */
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
  /** 用户类型：backend 后台用户 / frontend 前台用户 */
  userType: string;
  /** 是否平台超级管理员：0-否，1-是 */
  isSuperadmin: number;
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
  /** 主部门名称 */
  deptName?: string;
  /** 角色名称列表 */
  roleNames?: string;
/** 关联的部门ID列表 */
deptIds?: string[];
/** 关联的岗位ID列表 */
postIds?: string[];
/** 关联的角色ID列表 */
roleIds?: string[];
}

/** 用户分页查询请求参数 */
export interface UserPageRequest {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  username?: string;
  nickname?: string;
  userType?: string;
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

/** 部门分页查询请求参数 */
export interface DeptPageRequest {
  pageNum?: number;
  pageSize?: number;
  parentId?: string;
  keyword?: string;
  status?: string;
  orderBy?: string;
  orderDirection?: "asc" | "desc";
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
  version: number;
  createTime: string;
  updateTime: string;
  createBy: string;
  updateBy: string;
}

/** 岗位分页查询请求参数 */
export interface PostPageRequest {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  orderBy?: string;
  orderDirection?: "asc" | "desc";
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

// ── 角色管理相关类型（扩展） ──

/** 角色分页查询请求参数 */
export interface RolePageRequest {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  dataScope?: string;
  orderBy?: string;
  orderDirection?: "asc" | "desc";
}

// ── 权限（菜单）管理相关类型 ──

/** 系统权限实体（对应后端 SysPermission） */
export interface SysPermission {
  id: string;
  /** 父权限ID，顶级为 "0" */
  parentId: string;
  name: string;
  /** 类型：folder-目录，menu-菜单，button-按钮 */
  type: string;
  /** 权限标识，如 sys:user:list */
  code: string;
  /** 前端路由地址 */
  path: string;
  /** 前端组件路径 */
  component: string;
  icon: string;
  sort: number;
  /** 是否可见：0-否，1-是 */
  isVisible: number;
  /** 是否外链：0-否，1-是 */
  isExternal: number;
  /** 状态：enabled / disabled */
  status: string;
  /** ID 层级路径，逗号分隔 */
  treePath: string;
  remark: string;
  version: number;
  createTime: string;
  updateTime: string;
  createBy: string;
  updateBy: string;
}

/** 权限分页查询请求参数 */
export interface PermissionPageRequest {
  pageNum?: number;
  pageSize?: number;
  parentId?: string;
  keyword?: string;
  type?: string;
  status?: string;
  orderBy?: string;
  orderDirection?: "asc" | "desc";
}

/** 权限树节点（用于 q-tree，排除 button 类型） */
export interface PermissionTreeNode {
  id: string;
  label: string;
  parentId: string;
  type: string;
  icon: string;
  children?: PermissionTreeNode[];
  count?: number;
}
