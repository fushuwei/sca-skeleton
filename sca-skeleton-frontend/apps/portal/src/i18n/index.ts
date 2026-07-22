import { createI18n } from "vue-i18n";
import langEn from "quasar/lang/en-US";
import langZh from "quasar/lang/zh-CN";
import enUS from "./locales/en-US.json";
import zhCN from "./locales/zh-CN.json";

export const PORTAL_LOCALE_STORAGE_KEY = "portal-locale";
export const PORTAL_THEME_DARK_STORAGE_KEY = "portal-theme-dark";

export type PortalLocale = "en-US" | "zh-CN";

export function getInitialLocale(): PortalLocale {
  try {
    const v = localStorage.getItem(PORTAL_LOCALE_STORAGE_KEY);
    if (v === "en-US" || v === "zh-CN") {
      return v;
    }
  } catch {
    /* ignore */
  }
  return "zh-CN";
}

export function readInitialDark(): boolean {
  try {
    const v = localStorage.getItem(PORTAL_THEME_DARK_STORAGE_KEY);
    if (v === null) {
      return false;
    }
    return v === "true";
  } catch {
    return false;
  }
}

export function createPortalI18n() {
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

export function quasarLangForLocale(code: PortalLocale) {
  return code === "en-US" ? langEn : langZh;
}

export function persistLocale(code: PortalLocale) {
  try {
    localStorage.setItem(PORTAL_LOCALE_STORAGE_KEY, code);
  } catch {
    /* ignore */
  }
}

export function persistDark(isDark: boolean) {
  try {
    localStorage.setItem(PORTAL_THEME_DARK_STORAGE_KEY, String(isDark));
  } catch {
    /* ignore */
  }
}
