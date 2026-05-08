import { createI18n } from "vue-i18n";
import langEn from "quasar/lang/en-US";
import langZh from "quasar/lang/zh-CN";
import enUS from "./locales/en-US.json";
import zhCN from "./locales/zh-CN.json";

/** localStorage：界面语言（vue-i18n + Quasar 组件文案） */
export const ADMIN_LOCALE_STORAGE_KEY = "admin-locale";
/** localStorage：是否为 Dark（默认 Light，未写入键时为 false） */
export const ADMIN_THEME_DARK_STORAGE_KEY = "admin-theme-dark";

export type AdminLocale = "en-US" | "zh-CN";

export function getInitialLocale(): AdminLocale {
  try {
    const v = localStorage.getItem(ADMIN_LOCALE_STORAGE_KEY);
    if (v === "en-US" || v === "zh-CN") {
      return v;
    }
  } catch {
    /* ignore */
  }
  return "zh-CN";
}

/** Quasar / `body--dark` 初始态：默认 Light（false） */
export function readInitialDark(): boolean {
  try {
    const v = localStorage.getItem(ADMIN_THEME_DARK_STORAGE_KEY);
    if (v === null) {
      return false;
    }
    return v === "true";
  } catch {
    return false;
  }
}

export function createAdminI18n() {
  return createI18n({
    legacy: false,
    locale: getInitialLocale(),
    fallbackLocale: "zh-CN",
    messages: {
      "en-US": enUS,
      "zh-CN": zhCN
    }
  });
}

export function quasarLangForLocale(code: AdminLocale) {
  return code === "en-US" ? langEn : langZh;
}

export function persistLocale(code: AdminLocale) {
  try {
    localStorage.setItem(ADMIN_LOCALE_STORAGE_KEY, code);
  } catch {
    /* ignore */
  }
}

export function persistDark(isDark: boolean) {
  try {
    localStorage.setItem(ADMIN_THEME_DARK_STORAGE_KEY, String(isDark));
  } catch {
    /* ignore */
  }
}
