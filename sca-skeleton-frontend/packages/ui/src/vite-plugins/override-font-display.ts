import type { Plugin } from "vite";

/**
 * Vite 插件：将 @quasar/extras 字体 CSS 中的 font-display: block 替换为 swap。
 *
 * 背景：
 *   @quasar/extras 的 material-icons.css 和 material-symbols-rounded.css 使用
 *   font-display: block，导致字体加载完成前图标不可见（block 期 3 秒）。
 *   Material Symbols Rounded 字体 5.2MB，慢网络首屏超过 3 秒时图标全部消失。
 *
 *   最初在 material-symbols-axes.scss 中用空 @font-face（无 src）尝试覆盖，
 *   但 CSS 规范要求 @font-face 必须有 src 才有效，浏览器直接忽略，hack 无效。
 *
 * 方案：
 *   在 Vite transform 阶段直接替换 CSS 文本中的 font-display 值，
 *   不依赖 CSS @font-face 覆盖规则（浏览器实现不一致）。
 */
export function overrideFontDisplay(): Plugin {
  return {
    name: "override-font-display",
    transform(code: string, id: string) {
      // 只处理 @quasar/extras 下的 CSS 文件
      if (id.includes("@quasar/extras") && id.endsWith(".css")) {
        const replaced = code.replace(/font-display:\s*block/g, "font-display: swap");
        if (replaced !== code) {
          return { code: replaced, map: null };
        }
      }
    }
  };
}
