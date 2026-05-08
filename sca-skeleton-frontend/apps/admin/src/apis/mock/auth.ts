import type { ApiEnvelope, LoginPayload, LoginResponse, MenuItem, UserProfile } from "../../types/auth"; // 导入鉴权相关类型定义。

const USERNAME = "admin"; // 定义演示账号用户名常量。
const PASSWORD = "admin"; // 定义演示账号密码常量。
const TOKEN = "admin-access-token"; // 定义演示登录返回的固定令牌。

/** 演示用树形菜单：贴近数据中台 / 后台常见模块划分，叶子路由由动态注册（工作台走静态路由，不出现在侧栏） */
const DEMO_MENUS: MenuItem[] = [
  {
    name: "ModuleDatasource",
    path: "/datasource",
    title: "数据源中心",
    icon: "sym_r_database",
    children: [
      { name: "DsList", path: "/datasource/list", title: "数据源列表", component: "PlaceholderView" },
      { name: "DsCatalog", path: "/datasource/catalog", title: "元数据目录", component: "PlaceholderView" },
      { name: "DsLineage", path: "/datasource/lineage", title: "数据血缘", component: "PlaceholderView" },
      { name: "DsQuality", path: "/datasource/quality", title: "质量规则", component: "PlaceholderView" },
      { name: "DsStandard", path: "/datasource/standard", title: "数据标准", component: "PlaceholderView" }
    ]
  },
  {
    name: "ModuleDataAsset",
    path: "/asset",
    title: "数据资产",
    icon: "sym_r_inventory_2",
    children: [
      { name: "AssetModel", path: "/asset/model", title: "主题域模型", component: "PlaceholderView" },
      { name: "AssetIndicator", path: "/asset/indicator", title: "指标管理", component: "PlaceholderView" },
      { name: "AssetApi", path: "/asset/api", title: "数据服务 API", component: "PlaceholderView" },
      { name: "AssetSubscribe", path: "/asset/subscribe", title: "订阅与审批", component: "PlaceholderView" }
    ]
  },
  {
    name: "ModuleDataDev",
    path: "/dev",
    title: "数据开发",
    icon: "sym_r_account_tree",
    children: [
      { name: "DevJobFlow", path: "/dev/job/flow", title: "任务编排", component: "PlaceholderView" },
      { name: "DevSchedule", path: "/dev/schedule", title: "调度管理", component: "PlaceholderView" },
      { name: "DevInstance", path: "/dev/instance", title: "实例运维", component: "PlaceholderView" },
      { name: "DevScript", path: "/dev/script", title: "脚本仓库", component: "PlaceholderView" },
      { name: "DevResource", path: "/dev/resource", title: "资源组", component: "PlaceholderView" },
      { name: "DevUdf", path: "/dev/udf", title: "函数与 UDF", component: "PlaceholderView" }
    ]
  },
  {
    name: "ModuleOps",
    path: "/ops",
    title: "运维监控",
    icon: "sym_r_monitor_heart",
    children: [
      { name: "OpsHealth", path: "/ops/health", title: "服务健康", component: "PlaceholderView" },
      { name: "OpsLog", path: "/ops/log", title: "日志检索", component: "PlaceholderView" },
      { name: "OpsAlert", path: "/ops/alert", title: "告警中心", component: "PlaceholderView" },
      { name: "OpsAudit", path: "/ops/audit", title: "安全审计", component: "PlaceholderView" },
      { name: "OpsBackup", path: "/ops/backup", title: "备份与恢复", component: "PlaceholderView" }
    ]
  },
  {
    name: "ModuleSystem",
    path: "/system",
    title: "系统管理",
    icon: "sym_r_settings",
    children: [
      {
        name: "SystemUserGroup",
        path: "/system/iam",
        title: "用户与权限",
        children: [
          { name: "SystemUser", path: "/system/user", title: "用户管理", component: "UserCenterView" },
          { name: "SystemRole", path: "/system/role", title: "角色管理", component: "PlaceholderView" },
          { name: "SystemMenu", path: "/system/menu", title: "菜单管理", component: "PlaceholderView" },
          { name: "SystemPermission", path: "/system/permission", title: "接口权限", component: "PlaceholderView" }
        ]
      },
      {
        name: "SystemOrgGroup",
        path: "/system/org",
        title: "组织与岗位",
        children: [
          { name: "SystemDept", path: "/system/dept", title: "部门管理", component: "PlaceholderView" },
          { name: "SystemPost", path: "/system/post", title: "岗位管理", component: "PlaceholderView" }
        ]
      },
      { name: "SystemDict", path: "/system/dict", title: "字典管理", component: "PlaceholderView" },
      { name: "SystemConfig", path: "/system/config", title: "参数配置", component: "PlaceholderView" },
      { name: "SystemNotice", path: "/system/notice", title: "通知公告", component: "PlaceholderView" },
      { name: "SystemLogOper", path: "/system/log/operation", title: "操作日志", component: "PlaceholderView" },
      { name: "SystemLogLogin", path: "/system/log/login", title: "登录日志", component: "PlaceholderView" }
    ]
  }
];

export async function mockLogin(payload: LoginPayload): Promise<ApiEnvelope<LoginResponse>> { // 定义模拟登录接口函数。
  await new Promise((resolve) => setTimeout(resolve, 300)); // 模拟网络延迟以贴近真实接口体验。

  if (payload.username !== USERNAME || payload.password !== PASSWORD) { // 校验输入账号密码是否与演示账号一致。
    return { code: 401, message: "账号或密码错误", data: { token: "", menus: [] } }; // 返回登录失败结果。
  } // 结束登录失败分支。

  return { // 返回登录成功结果。
    code: 0, // 返回成功业务码。
    message: "登录成功", // 返回成功提示语。
    data: { // 返回业务数据体。
      token: TOKEN, // 返回固定演示令牌。
      menus: DEMO_MENUS // 返回树形菜单。
    } // 结束业务数据体。
  }; // 结束成功返回对象。
} // 结束模拟登录接口函数。

export async function mockGetUserProfile(): Promise<ApiEnvelope<UserProfile>> { // 定义模拟获取当前用户信息函数。
  await new Promise((resolve) => setTimeout(resolve, 200)); // 模拟用户信息接口延迟。
  return { // 返回用户信息成功结果。
    code: 0, // 返回成功业务码。
    message: "获取成功", // 返回成功提示语。
    data: { id: "1", username: USERNAME, nickname: "系统管理员" } // 返回演示用户资料。
  }; // 结束成功返回对象。
} // 结束模拟获取当前用户信息函数。
