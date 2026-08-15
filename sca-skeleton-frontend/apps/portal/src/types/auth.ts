export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
}

export interface PortalProfile {
  id: string;
  username: string;
  nickname: string;
  realName?: string;
  avatar?: string;
  /** 用户类型：SUPER_ADMIN 超级管理员 / TENANT_ADMIN 租户管理员 / DEPT_ADMIN 部门管理员 / NORMAL 普通用户 */
  userType?: string;
}

export interface SysPermission {
  id: string;
  parentId: string;
  name: string;
  nameEn?: string;
  type: string;
  code: string;
  realm: string;
  path: string;
  component: string;
  icon: string;
  sort: number;
  isVisible: number;
  isExternal: number;
  status: string;
  treePath: string;
}

export type MenuComponent =
  | "HomeView"
  | "DataMapView"
  | "ResourceCatalogView"
  | "DataStandardView"
  | "AiQueryView"
  | "DataMarketView"
  | "DataSubmitView"
  | "ProfileView"
  | "MyRequestView"
  | "MyDownloadView"
  | "MyFavoriteView"
  | "NotificationView"
  | "AppIntegrationView";

export interface MenuItem {
  id: string;
  name: string;
  nameEn?: string;
  path: string;
  component?: MenuComponent;
  icon?: string;
  children?: MenuItem[];
}
