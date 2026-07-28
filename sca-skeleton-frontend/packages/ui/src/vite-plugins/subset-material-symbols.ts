import type { Plugin } from "vite";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import { dirname, join, resolve } from "node:path";
import { readFileSync, readdirSync, writeFileSync, mkdirSync } from "node:fs";
import { create as createFont } from "fontkit";
import subsetFont from "subset-font";

/**
 * Vite 插件：扫描代码中实际使用的 Material Symbols 图标，子集化字体（5MB → ~140KB）。
 *
 * 渲染机制（关键背景）：
 *   Quasar 的 q-icon 把 sym_r_search 渲染成 <i class="material-symbols-rounded">search</i>，
 *   依赖字体的 GSUB ligature 把 "search" 字母序列替换成图标字形。但 subset-font 按字符
 *   匹配 ligature，无法精确裁剪——只保留 3 个图标也会带入 128 个 ligature，109 个图标
 *   会保留全部 ligature 导致体积回到 5MB。
 *
 *   因此本方案绕开 ligature：子集字体只保留 PUA 码位对应的字形（每个图标 6KB），
 *   同时生成 icon-name → PUA codepoint 映射文件，由 iconMapFn 在运行时把
 *   sym_r_search 映射成 { cls: "material-symbols-rounded", content: "\ue8b6" }，
 *   浏览器用 PUA 码位直接从字体取字形，无需 ligature。
 *
 *   这是 Google Fonts CSS API 的做法（每个图标名对应一个 CSS 类，content 为 PUA 码位），
 *   也是 fonttools 子集化的标准做法。
 *
 * 设计要点（dev / build 分流，与 override-font-display 插件配合）：
 * - dev：不执行（apply: "build"），由 @quasar/extras 完整字体提供图标，即写即显。
 * - build：在 buildStart 钩子内扫描 sym_r_xxx 图标名，用 fontkit 解析源字体 cmap 获取
 *   码位，用 subset-font（harfbuzzjs/WASM）子集化（仅 PUA 码位），产物写入
 *   packages/ui/fonts/Material_Symbols/，同时生成 icon-codepoints.ts 供 iconMapFn 使用。
 *
 * 纯 Node.js 实现（fontkit + subset-font/harfbuzzjs），无外部系统依赖，与 Vite/turbo 构建管道对齐。
 */
export function subsetMaterialSymbols(): Plugin {
  return {
    name: "subset-material-symbols",
    apply: "build",
    async buildStart() {
      const t0 = Date.now();

      // 1. 定位 @quasar/extras 的 Material Symbols Rounded woff2 源字体
      // Vite 启动时 cwd 为各 app 目录（apps/admin 或 apps/portal），从该处解析 @quasar/extras
      const require = createRequire(process.cwd() + "/package.json");
      const extrasPkgPath = require.resolve("@quasar/extras/package.json");
      const webFontDir = join(
        dirname(extrasPkgPath),
        "material-symbols-rounded",
        "web-font"
      );
      const woff2Name = readdirSync(webFontDir).find((f) =>
        f.endsWith(".woff2")
      );
      if (!woff2Name) {
        throw new Error(
          `[subset] 未在 ${webFontDir} 下找到 Material Symbols woff2 源字体`
        );
      }
      const fontSrcPath = join(webFontDir, woff2Name);
      const fontBuffer = readFileSync(fontSrcPath);

      // 2. 扫描代码中的图标名（sym_r_xxx / sym_o_xxx / sym_s_xxx → 去前缀得到图标名）
      // 扫描范围：项目根目录下所有 .vue/.ts/.tsx/.scss/.sql 文件
      // SQL 文件必须纳入扫描：菜单图标存储在数据库中，运行时由后端返回给前端，
      // 前端源码中不会硬编码这些图标名。若不扫描 SQL，这些图标不会被纳入子集字体，
      // 导致 iconMapFn 查不到 codepoint，Quasar fallback 到 ligature 渲染，
      // 而子集字体不含 GSUB ligature 数据，最终显示为乱码。
      const frontendRoot = resolve(
        dirname(fileURLToPath(import.meta.url)),
        "..",
        "..",
        "..",
        ".."
      );
      const projectRoot = resolve(frontendRoot, "..");
      const icons = scanIcons(projectRoot);
      console.log(`[subset] 扫描到 ${icons.size} 个图标`);

      // 3. fontkit 解析 cmap，构建 name → codepoint 映射
      const font = createFont(fontBuffer) as any;
      const nameToCodepoint = new Map<string, number>();
      for (const cp of font.characterSet) {
        const glyph = font.glyphForCodePoint(cp);
        if (glyph && glyph.name) {
          nameToCodepoint.set(glyph.name, cp);
        }
      }

      // 4. 匹配码位，同时构建 icon-name → codepoint 映射（供 iconMapFn 使用）
      const codepoints: number[] = [];
      const iconCodepoints: Record<string, number> = {};
      const missing: string[] = [];
      for (const name of icons) {
        const cp = nameToCodepoint.get(name);
        if (cp !== undefined) {
          codepoints.push(cp);
          iconCodepoints[name] = cp;
        } else {
          missing.push(name);
        }
      }
      if (missing.length > 0) {
        console.warn(
          `[subset] 警告: ${missing.length} 个图标未在 cmap 中找到: ${missing.slice(0, 5).join(", ")}${missing.length > 5 ? "..." : ""}`
        );
      }
      console.log(`[subset] 匹配到 ${codepoints.length} 个码位`);

      // 5. subset-font 子集化：只传 PUA 码位，不传 ligature 文本
      // 这样字体只保留图标字形（每个 ~1.3KB），不保留字母 glyph 和 GSUB ligature 数据。
      // 渲染由 iconMapFn 用 PUA 码位直接取字形，绕开 ligature 机制。
      const puaChars = codepoints
        .map((cp) => String.fromCodePoint(cp))
        .join("");
      const subsetBuffer = await subsetFont(fontBuffer, puaChars, {
        targetFormat: "woff2"
      });

      // 6. 写入产物（路径与 material-symbols-axes.scss 的 @font-face url() 对齐）
      const outDir = join(
        frontendRoot,
        "packages/ui/fonts/Material_Symbols"
      );
      mkdirSync(outDir, { recursive: true });
      const outPath = join(outDir, "material-symbols-rounded-subset.woff2");
      writeFileSync(outPath, subsetBuffer);

      // 7. 生成 icon-codepoints.ts（供 iconMapFn 在运行时把图标名映射成 PUA 码位）
      // 产物路径：packages/ui/src/material-symbols-codepoints.ts
      // iconMapFn 注册逻辑见 packages/ui/src/setup-icon-map.ts
      const codepointsTsPath = join(
        frontendRoot,
        "packages/ui/src/material-symbols-codepoints.ts"
      );
      const codepointsTs = `// 由 Vite 插件 subset-material-symbols 在 build 时自动生成，请勿手动编辑。
// 图标名 → Material Symbols PUA 码位映射，供 iconMapFn 绕过 ligature 直接用码位渲染。
export const materialSymbolsCodepoints: Record<string, number> = ${JSON.stringify(iconCodepoints, null, 2)};
`;
      writeFileSync(codepointsTsPath, codepointsTs);

      console.log(
        `[subset] 输出: ${outPath} (${(subsetBuffer.length / 1024).toFixed(0)} KB, ${Date.now() - t0}ms)`
      );
    }
  };
}

/**
 * 从项目根目录递归扫描 .vue/.ts/.tsx/.scss/.sql 文件，
 * 收集 sym_r_xxx / sym_o_xxx / sym_s_xxx 形式的图标引用，去前缀后返回图标名集合
 *
 * SQL 文件必须纳入扫描：菜单图标存储在数据库中，运行时由后端返回给前端，
 * 前端源码中不会硬编码这些图标名。若不扫描 SQL，这些图标不会被纳入子集字体，
 * 导致 iconMapFn 查不到 codepoint，Quasar fallback 到 ligature 渲染，
 * 而子集字体不含 GSUB ligature 数据，最终显示为乱码。
 *
 * sca-skeleton-frontend 和 sca-skeleton-backend 是工程目录，不含生产 SQL 脚本，
 * 进入这两个目录后不再扫描 .sql 文件（仍扫描 .vue/.ts/.tsx/.scss）。
 */
function scanIcons(projectRoot: string): Set<string> {
  const icons = new Set<string>();
  const iconRe = /sym_[ros]_[a-z0-9_]*/g;
  const prefixRe = /^sym_[ros]_/;
  // 跳过依赖、构建产物、IDE 配置等非源码目录
  const skipDirs = new Set([
    "node_modules",
    "dist",
    ".git",
    ".turbo",
    "coverage",
    "target",      // Java/Maven 编译产物
    "build",       // Gradle 构建产物
    ".gradle",     // Gradle 缓存
    ".idea",       // IntelliJ IDEA 配置
    ".vscode"      // VS Code 配置
  ]);
  // 进入这些目录后不再扫描 .sql（工程目录不含生产 SQL 脚本）
  const noSqlDirs = new Set([
    "sca-skeleton-frontend",
    "sca-skeleton-backend"
  ]);
  // 跳过本插件及 iconMapFn 自身文件：注释里的 sym_r_xxx 是说明文字，不是真实图标引用
  const skipFiles = new Set([
    "subset-material-symbols.ts",
    "setup-icon-map.ts",
    "material-symbols-codepoints.ts"
  ]);
  const sourceExtRe = /\.(vue|ts|tsx|scss)$/;

  const walk = (dir: string, allowSql: boolean) => {
    let entries: ReturnType<typeof readdirSync>;
    try {
      entries = readdirSync(dir, { withFileTypes: true });
    } catch {
      return;
    }
    for (const entry of entries) {
      if (entry.isDirectory()) {
        if (!skipDirs.has(entry.name)) {
          // 进入工程目录后关闭 SQL 扫描
          walk(join(dir, entry.name), !noSqlDirs.has(entry.name));
        }
      } else if (!skipFiles.has(entry.name)) {
        const isSource = sourceExtRe.test(entry.name);
        const isSql = entry.name.endsWith(".sql");
        if (isSource || (allowSql && isSql)) {
          const content = readFileSync(join(dir, entry.name), "utf8");
          const matches = content.match(iconRe);
          if (matches) {
            for (const m of matches) {
              const name = m.replace(prefixRe, "");
              if (name) icons.add(name);
            }
          }
        }
      }
    }
  };

  walk(projectRoot, true);
  return icons;
}
