import { request } from "./http";
import type { CaptchaData } from "../types/auth";

/** 经网关转发至 auth 的验证码接口（与 gateway 白名单 /auth/captcha/** 一致） */
const CAPTCHA_PATH = "/auth/captcha";

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

/** 拉取图形验证码（后端生成并校验）。 */
export async function fetchCaptcha(): Promise<{ captchaId: string; imageSrc: string }> {
  const envelope = await request<CaptchaData>({ method: "GET", url: CAPTCHA_PATH });
  return {
    captchaId: envelope.data.captchaId,
    imageSrc: normalizeCaptchaImage(envelope.data.image)
  };
}
