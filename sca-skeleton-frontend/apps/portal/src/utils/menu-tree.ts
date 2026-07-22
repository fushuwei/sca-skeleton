import type { MenuItem } from "../types/auth";

/** 自菜单树中取出所有可路由叶子（含 component），用于注册动态路由 */
export function flattenRoutableMenus(items: MenuItem[]): MenuItem[] {
  const out: MenuItem[] = [];
  for (const item of items) {
    if (item.children?.length) {
      out.push(...flattenRoutableMenus(item.children));
    } else if (item.component) {
      out.push(item);
    }
  }
  return out;
}

/** 叶子菜单路径集合，用于路由守卫判断是否为已授权动态路径 */
export function collectLeafMenuPaths(items: MenuItem[]): string[] {
  return flattenRoutableMenus(items).map((m) => m.path);
}

const MENU_ICON_FALLBACK = "sym_r_nest_eco_leaf";

/**
 * 按菜单 ID 在菜单树中解析图标：仅使用**该菜单节点自身**的 `icon`。
 */
export function getIconForMenuRouteName(items: MenuItem[], menuId: string): string {
  function walk(nodes: MenuItem[]): string | null {
    for (const n of nodes) {
      const selfIcon = typeof n.icon === "string" && n.icon.trim() !== "" ? n.icon : undefined;
      if (n.id === menuId) {
        return selfIcon ?? MENU_ICON_FALLBACK;
      }
      if (n.children?.length) {
        const hit = walk(n.children);
        if (hit !== null) {
          return hit;
        }
      }
    }
    return null;
  }
  return walk(items) ?? MENU_ICON_FALLBACK;
}
