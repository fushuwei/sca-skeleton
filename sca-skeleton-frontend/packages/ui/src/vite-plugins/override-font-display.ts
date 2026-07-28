import type { Plugin } from "vite";

/**
 * Vite 插件：处理 @quasar/extras 字体 CSS，按构建模式分流 Material Symbols 字体来源。
 *
 * 设计要点（dev / build 自动分流，无需手动传 mode）：
 *
 * - 所有模式：font-display: block → swap（避免慢网络下图标隐藏 3 秒）
 * - serve（dev）：完整字体 @font-face 原样保留。字体来源 = @quasar/extras 的 5MB 完整字体。
 *   subset-material-symbols 插件在 dev 下不执行（apply: "build"），新增图标即写即显。
 * - build（production）：删除 @quasar/extras 的完整字体 @font-face（5MB）。字体来源改由
 *   material-symbols-axes.scss 里的子集 @font-face（~140KB）提供，该字体由
 *   subset-material-symbols Vite 插件在 buildStart 时自动生成。
 *
 * 字体 @font-face 的 url() 路径由各 app 的 material-symbols-axes.scss 管理（相对路径，天然正确），
 * 本插件只负责"完整字体 @font-face 删不删"这一件事，职责单一。
 */
export function overrideFontDisplay(): Plugin {
  let isBuild = false;

  return {
    name: "override-font-display",
    enforce: "pre",
    config(_, { command }) {
      isBuild = command === "build";
    },
    transform(code: string, id: string) {
      if (!id.includes("@quasar/extras") || !id.endsWith(".css")) return;

      let result = code;

      // Material Symbols Rounded：仅 build 时删除完整字体 @font-face
      if (id.includes("material-symbols-rounded") && isBuild) {
        result = result.replace(/@font-face\s*\{[^}]*\}/g, "");
      }

      // 所有 @quasar/extras CSS：font-display block → swap
      result = result.replace(/font-display:\s*block/g, "font-display: swap");

      if (result !== code) {
        return { code: result, map: null };
      }
    }
  };
}
