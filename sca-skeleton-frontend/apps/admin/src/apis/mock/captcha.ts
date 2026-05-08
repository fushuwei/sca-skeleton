import type { ApiEnvelope, CaptchaData } from "../../types/auth";

const MOCK_CAPTCHA_SVG_BAK = `<svg xmlns="http://www.w3.org/2000/svg" width="120" height="44" viewBox="0 0 120 44">
  <rect width="120" height="44" fill="#eceff4"/>
  <text x="60" y="28" text-anchor="middle" font-family="system-ui,sans-serif" font-size="12" fill="#50606e">图片加载中...</text>
</svg>`;

/** 与 `index.html` 首屏 `#app-loading .loader` 同款的渐变扫光效果，缩小字号以放入验证码区域。 */
const MOCK_CAPTCHA_SVG = `<svg xmlns="http://www.w3.org/2000/svg" width="120" height="44" viewBox="0 0 120 44">
<foreignObject width="120" height="44">
<div xmlns="http://www.w3.org/1999/xhtml" style="margin:0;width:120px;height:44px;background:#eceff4;display:flex;align-items:center;justify-content:center;">
<style type="text/css">
.app-loader-mock {
  width: fit-content;
  font-size: 15px;
  font-family: monospace, sans-serif;
  font-weight: bold;
  text-transform: uppercase;
  color: transparent;
  -webkit-text-stroke: 1px #000;
  background: linear-gradient(90deg, transparent 33%, #000 0 67%, transparent 0) 100% / 300% 100% no-repeat text;
  -webkit-background-clip: text;
  background-clip: text;
  animation: app-loader-mock-l12 4s steps(14) infinite;
}
.app-loader-mock::before {
  content: "Loading";
}
@keyframes app-loader-mock-l12 {
  to {
    background-position: 0;
  }
}
</style>
<div class="app-loader-mock"></div>
</div>
</foreignObject>
</svg>`;

export async function mockGetCaptcha(): Promise<ApiEnvelope<CaptchaData>> {
  await new Promise((resolve) => setTimeout(resolve, 120));
  return {
    code: 0,
    message: "ok",
    data: {
      captchaId: `mock-${Date.now()}`,
      image: MOCK_CAPTCHA_SVG
    }
  };
}
