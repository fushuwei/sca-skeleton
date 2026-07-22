import { portalRequest } from "./http";
import type { ApiEnvelope, PortalProfile, SysPermission } from "../types/auth";

/** 获取当前登录用户基本信息 */
export async function getUserProfileApi(): Promise<ApiEnvelope<PortalProfile>> {
  return portalRequest<PortalProfile>({
    method: "GET",
    url: "/sys/user/profile"
  });
}

/** 查询当前用户菜单列表（扁平列表，前端负责转树形） */
export async function getUserMenusApi(): Promise<ApiEnvelope<SysPermission[]>> {
  return portalRequest<SysPermission[]>({
    method: "GET",
    url: "/sys/permission/menus"
  });
}
