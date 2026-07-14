/** 获取图形验证码接口返回的业务数据（与后端约定一致后可在 apis/captcha 中做字段映射）。 */
export interface CaptchaData {
  captchaId: string;
  /** 图片：纯 base64、或已带 data:image/... 前缀、或 SVG 原文。 */
  image: string;
}

/** 菜单挂载的页面组件标识（仅叶子节点需要） */
export type MenuComponent = "DashboardView" | "UserCenterView" | "PlaceholderView" | "UserListView" | "MenuListView" | "RoleListView" | "PostListView" | "DeptListView" | "TenantPackageListView" | "TenantListView" | "OperationLogListView" | "LoginLogListView";

export interface MenuItem {
  /** 主键ID（对应 SQL id 字段），唯一标识 */
  id: string;
  /** 权限名称（对应 SQL name 字段），用于侧边栏显示 */
  name: string;
  /** 英文菜单名称（对应 SQL name_en 字段），英文环境下显示 */
  nameEn?: string;
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
  /** 是否平台超级管理员：0-否，1-是 */
  isSuperadmin: number;
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
  /** 关联的权限数量 */
  permissionCount: number;
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
  /** 英文菜单名称，用于国际化 */
  nameEn?: string;
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

// ── 租户套餐管理相关类型 ──

/** 系统租户套餐实体（对应后端 SysTenantPackage） */
export interface SysTenantPackage {
  id: string;
  name: string;
  code: string;
  /** 套餐状态：enabled / disabled */
  status: string;
  /** 用户数限制，-1 表示不限 */
  userLimit: number;
  /** API 调用限制/日，-1 表示不限 */
  apiLimit: number;
  /** 存储限制(MB)，-1 表示不限 */
  storageLimit: number;
  /** 有效期天数，-1 表示不限 */
  expireDays: number;
  sort: number;
  remark: string;
  version: number;
  /** 关联的权限数量 */
  permissionCount: number;
  createTime: string;
  updateTime: string;
  createBy: string;
  updateBy: string;
}

/** 套餐分页查询请求参数 */
export interface TenantPackagePageRequest {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  orderBy?: string;
  orderDirection?: "asc" | "desc";
}

// ── 租户管理相关类型 ──

/** 系统租户实体（对应后端 SysTenant） */
export interface SysTenant {
  id: string;
  name: string;
  code: string;
  packageId: string;
  /** 套餐名称（关联查询） */
  packageName: string;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  domainName: string;
  /** 生效时间（NULL表示立即生效） */
  effectiveTime: string | null;
  /** 过期时间（NULL表示永不过期） */
  expireTime: string | null;
  /** 租户状态：normal / disabled / expired / cancelled */
  status: string;
  remark: string;
  version: number;
  createTime: string;
  updateTime: string;
  createBy: string;
  updateBy: string;
}

/** 租户分页查询请求参数 */
export interface TenantPageRequest {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  packageId?: string;
  orderBy?: string;
  orderDirection?: "asc" | "desc";
}

// ── 操作日志相关类型 ──

/** 操作日志实体（对应后端 OperationLogResponse） */
export interface SysOperationLog {
  id: string;
  traceId: string;
  userId: string;
  /** 操作人展示名称（格式："real_name (username)"） */
  operator: string;
  module: string;
  action: string;
  httpMethod: string;
  requestUri: string;
  className: string;
  methodName: string;
  requestArgs: string;
  responseResult: string;
  isSuccess: number;
  errorMessage: string;
  costMs: number;
  clientIp: string;
  operationTime: string;
}

/** 操作日志分页查询请求参数 */
export interface OperationLogPageRequest {
  pageNum?: number;
  pageSize?: number;
  module?: string;
  action?: string;
  operator?: string;
  isSuccess?: number;
  startTime?: string;
  endTime?: string;
  orderBy?: string;
  orderDirection?: string;
}

// ── 登录日志相关类型 ──

/** 登录日志实体（对应后端 LoginLogResponse） */
export interface SysLoginLog {
  id: string;
  tenantId: string;
  /** 租户名称（通过 tenant_id 关联 sys_tenant 查询，租户不存在时为空） */
  tenantName: string;
  userId: string;
  /** 登录时输入的用户名（原始输入，无论用户是否存在都记录） */
  username: string;
  /** 真实姓名（通过 user_id 关联 sys_user 查询，user 不存在时为空） */
  realName: string;
  clientIp: string;
  location: string;
  device: string;
  browser: string;
  os: string;
  /** 是否成功：0-失败，1-成功 */
  isSuccess: number;
  errorMessage: string;
  /** 操作耗时（毫秒） */
  costMs: number;
  loginTime: string;
}

/** 登录日志分页查询请求参数 */
export interface LoginLogPageRequest {
  pageNum?: number;
  pageSize?: number;
  /** 搜索关键字（模糊匹配租户名称、登录用户、真实姓名、客户端 IP） */
  keyword?: string;
  /** 是否成功：1-成功，0-失败 */
  isSuccess?: number;
  startTime?: string;
  endTime?: string;
  orderBy?: string;
  orderDirection?: string;
}
