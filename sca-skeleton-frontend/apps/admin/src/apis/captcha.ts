import { request } from "./http";
import { mockGetCaptcha } from "./mock/captcha";
import type { CaptchaData } from "../types/auth";

const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

const rawPath = import.meta.env.VITE_AUTH_CAPTCHA_PATH;
const CAPTCHA_PATH = typeof rawPath === "string" && rawPath.length > 0 ? rawPath : "/auth/captcha";

function normalizeCaptchaImage(image: string): string {
  const trimmed = image.trim();
  if (!trimmed) {
    return "";
  }
  if (trimmed.startsWith("data:")) {
    return trimmed;
  }
  if (trimmed.startsWith("<svg")) {
    return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(trimmed)}`;
  }
  return `data:image/png;base64,${trimmed}`;
}

/** 拉取图形验证码；后端路径默认 `VITE_AUTH_CAPTCHA_PATH` 或 `/auth/captcha`。 */
export async function fetchCaptcha(): Promise<{ captchaId: string; imageSrc: string }> {
  const envelope = USE_MOCK
    ? await mockGetCaptcha()
    : await request<CaptchaData>({ method: "GET", url: CAPTCHA_PATH });
  return {
    captchaId: envelope.data.captchaId,
    imageSrc: normalizeCaptchaImage(envelope.data.image)
  };
}
