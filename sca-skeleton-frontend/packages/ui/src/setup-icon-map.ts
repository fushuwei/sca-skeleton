/**
 * 创建 Quasar 的 iconMapFn，用 PUA 码位直接渲染 Material Symbols 图标，
 * 绕开 ligature 机制（子集字体不保留 GSUB ligature 数据，体积可从 5MB 降到 ~140KB）。
 *
 * 工作原理：
 *   Quasar 默认把 sym_r_search 渲染成 <i class="material-symbols-rounded">search</i>，
 *   依赖字体 ligature。本函数返回的 iconMapFn 拦截 sym_r_xxx，返回
 *   { cls: "material-symbols-rounded", content: "\ue8b6" }（PUA 码位），
 *   浏览器用码位直接从字体取字形，无需 ligature。
 *
 *   这是 Google Fonts CSS API 的做法（每个图标名对应一个 CSS 类，content 为 PUA 码位），
 *   也是 fonttools 子集化的标准做法。
 *
 * dev/build 统一调用：
 *   - dev：materialSymbolsCodepoints 为空对象（占位文件），所有图标返回 undefined，
 *     Quasar 走默认 ligature 路径，用 @quasar/extras 完整字体渲染。
 *   - build：Vite 插件生成实际 codepoints 映射，iconMapFn 用 PUA 码位渲染，
 *     配合子集字体（~140KB）绕开 ligature。
 *
 * 用法：在 main.ts 中
 *   app.use(Quasar, { config: { iconMapFn: createMaterialSymbolsIconMapFn() } });
 */
import { materialSymbolsCodepoints } from "./material-symbols-codepoints";

const symRe = /^sym_([ros])_(.+)$/;

const variantMap: Record<string, string> = {
  r: "material-symbols-rounded",
  o: "material-symbols-outlined",
  s: "material-symbols-sharp"
};

// 与 Quasar 的 GlobalQuasarIconMapFn 类型对齐
type IconMapFn = (
  iconName: string
) => { icon: string } | { cls: string; content?: string } | void;

export function createMaterialSymbolsIconMapFn(): IconMapFn {
  return (iconName: string) => {
    const match = symRe.exec(iconName);
    if (!match) return;

    const variant = match[1];
    const name = match[2];

    const codepoint = materialSymbolsCodepoints[name];
    if (codepoint === undefined) return;

    return {
      cls: `notranslate ${variantMap[variant]}`,
      content: String.fromCodePoint(codepoint)
    };
  };
}
