import js from "@eslint/js";
import vue from "eslint-plugin-vue";
import tseslint from "typescript-eslint";
import globals from "globals";

export default [
  {
    ignores: ["**/dist/**", "**/node_modules/**", "**/.turbo/**", "**/coverage/**"]
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...vue.configs["flat/recommended"],
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
  }
];
