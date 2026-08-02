/** 菜单挂载的页面组件标识（仅叶子节点需要） */
export type MenuComponent = "DashboardView" | "UserCenterView" | "PlaceholderView" | "UserListView" | "PermissionListView" | "RoleListView" | "PostListView" | "DeptListView" | "TenantPackageListView" | "TenantListView" | "OperationLogListView" | "LoginLogListView" | "DriverListView" | "DatasourceListView" | "SqlQueryView";

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

// ── 用户管理相关类型 ──

/** 系统用户实体（对应后端 SysUser） */
export interface SysUser {
  id: string;
  tenantId: string;
  /** 租户名称（通过 tenant_id 关联 sys_tenant 查询，租户不存在时为空） */
  tenantName?: string;
  username: string;
  nickname: string;
  realName: string;
  gender: string;
  avatar: string;
  phone: string;
  email: string;
  /** 用户域：admin 后台用户 / portal 前台用户 */
  realm: string;
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
  /** 乐观锁版本号 */
  version: number;
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
  realm?: string;
  status?: string;
  deptId?: string;
  sortField?: string;
  sortOrder?: "asc" | "desc";
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
  /** 租户名称（通过 tenant_id 关联 sys_tenant 查询，租户不存在时为空） */
  tenantName?: string;
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
  /** 乐观锁版本号 */
  version: number;
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
  sortField?: string;
  sortOrder?: "asc" | "desc";
}

// ── 岗位管理相关类型 ──

/** 系统岗位实体（对应后端 SysPost） */
export interface SysPost {
  id: string;
  tenantId: string;
  /** 租户名称（通过 tenant_id 关联 sys_tenant 查询，租户不存在时为空） */
  tenantName?: string;
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
  sortField?: string;
  sortOrder?: "asc" | "desc";
}

// ── 角色管理相关类型 ──

/** 系统角色实体（对应后端 SysRole） */
export interface SysRole {
  id: string;
  tenantId: string;
  /** 租户名称（通过 tenant_id 关联 sys_tenant 查询，租户不存在时为空） */
  tenantName?: string;
  name: string;
  code: string;
  /** 数据权限范围：all / tenant / dept_and_sub / dept / personal / custom */
  dataScope: string;
  /** 用户域（admin：后台角色；portal：前台角色） */
  realm: string;
  isBuiltin: number;
  sort: number;
  remark: string;
  /** 乐观锁版本号 */
  version: number;
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
  realm?: string;
  sortField?: string;
  sortOrder?: "asc" | "desc";
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
  /** 权限编码，如 sys:user:list */
  code: string;
  /** 权限域（admin：后台权限；portal：前台权限） */
  realm: string;
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
  realm?: string;
  status?: string;
  sortField?: string;
  sortOrder?: "asc" | "desc";
}

/**
 * 权限分配选项响应（最小化字段，对应后端 PermissionAssignOptionResponse）
 *
 * 用于角色/套餐授权面板，仅包含渲染所需字段，剥离 path/component/code/treePath 等敏感字段。
 * 后端已做越权防护：非超管仅返回自身拥有的权限，前端无需再过滤。
 */
export interface PermissionAssignOption {
  id: string;
  /** 父权限ID，顶级为 "0" */
  parentId: string;
  name: string;
  /** 英文名称，用于国际化 */
  nameEn?: string;
  /** 类型：module / folder / menu / button */
  type: string;
  /** 权限域（admin：后台权限；portal：前台权限） */
  realm: string;
  icon: string;
  sort: number;
}

/** 权限树节点（用于 q-tree，排除 button 类型） */
export interface PermissionTreeNode {
  id: string;
  label: string;
  parentId: string;
  type: string;
  icon: string;
  /** 权限域（admin/portal），分类节点和业务节点均携带，用于配色徽章与新建菜单默认域 */
  realm?: string;
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
  sortField?: string;
  sortOrder?: "asc" | "desc";
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
  sortField?: string;
  sortOrder?: "asc" | "desc";
}

// ── 操作日志相关类型 ──

/** 操作日志实体（对应后端 OperationLogResponse） */
export interface SysOperationLog {
  id: string;
  traceId: string;
  tenantId: string;
  /** 租户名称（通过 tenant_id 关联 sys_tenant 查询，租户不存在时为空） */
  tenantName: string;
  userId: string;
  /** 操作用户名 */
  username: string;
  /** 真实姓名（通过 user_id 关联 sys_user 查询，user 不存在时为空） */
  realName: string;
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
  location: string;
  device: string;
  browser: string;
  os: string;
  operationTime: string;
}

/** 操作日志分页查询请求参数 */
export interface OperationLogPageRequest {
  pageNum?: number;
  pageSize?: number;
  /** 搜索关键字（模糊匹配租户名称、操作用户、真实姓名、操作模块、操作动作、请求路径） */
  keyword?: string;
  isSuccess?: number;
  startTime?: string;
  endTime?: string;
  sortField?: string;
  sortOrder?: string;
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
  sortField?: string;
  sortOrder?: string;
}

// ── Options 精简响应类型（对应后端 XxxOptionResponse，用于下拉选择场景） ──

/**
 * 角色选项（最小化字段，对应后端 RoleOptionResponse）
 * 用于用户管理表单下拉选择，剥离 dataScope/tenantId/isBuiltin/审计字段等。
 */
export interface RoleOption {
  id: string;
  name: string;
  code: string;
  sort: number;
  /** 用户域（admin：后台角色；portal：前台角色） */
  realm: string;
}

/**
 * 部门选项（最小化字段，对应后端 DeptOptionResponse）
 * 用于用户管理表单部门树选择，剥离 leader/phone/email/tenantId/treePath/审计字段等。
 */
export interface DeptOption {
  id: string;
  parentId: string;
  name: string;
  sort: number;
}

/**
 * 岗位选项（最小化字段，对应后端 PostOptionResponse）
 * 用于用户管理表单下拉选择，剥离 tenantId/remark/version/审计字段等。
 */
export interface PostOption {
  id: string;
  name: string;
  code: string;
  sort: number;
}

/**
 * 租户套餐选项（最小化字段，对应后端 TenantPackageOptionResponse）
 * 用于租户管理表单下拉选择，剥离 userLimit/apiLimit/storageLimit/expireDays/审计字段等。
 * 仅返回 status=enabled 的套餐。
 */
export interface TenantPackageOption {
  id: string;
  name: string;
  code: string;
  status: string;
  sort: number;
}
