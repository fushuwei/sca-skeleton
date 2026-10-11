import js from "@eslint/js";
import vue from "eslint-plugin-vue";
import tseslint from "typescript-eslint";
import globals from "globals";
import prettier from "eslint-config-prettier";

export default [
  {
    ignores: ["**/dist/**", "**/node_modules/**", "**/.turbo/**", "**/coverage/**"]
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...vue.configs["flat/recommended"],
  {
    // `.vue` 文件的 `<script lang="ts">` 脚本段使用 TS parser 解析
    // （vue.configs["flat/recommended"] 已把 languageOptions.parser 设为 vue-eslint-parser，
    //  这里再为脚本段指定 parserOptions.parser，使其能解析 import type 等 TS 语法）。
    files: ["**/*.vue"],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser
      }
    }
  },
  {
    files: ["**/*.{ts,tsx,vue}"],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node
      }
    },
    rules: {
      "vue/multi-word-component-names": "off"
    }
  },
  // 必须放在最后：关闭所有与 Prettier 冲突的格式化类规则
  // （如 vue/max-attributes-per-line、vue/html-indent 等），
  // 否则 ESLint 要求"每属性换行"而 Prettier 按 printWidth=100 折行，两边互相打架。
  prettier
];
