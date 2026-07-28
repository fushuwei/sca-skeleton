<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import "../../styles/login.scss";
import { useAuthStore } from "../../stores/auth";
import { showToast } from "@repo/shared";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// ── DOM 引用 ──
const usernameInput = ref<HTMLInputElement>();
const passwordInput = ref<HTMLInputElement>();
const captchaInput = ref<HTMLInputElement>();

// ── 表单状态 ──
const username = ref("");
const password = ref("");
const passwordVisible = ref(false);
const loading = ref(false);

// ── 验证码状态 ──
const captchaKey = ref("");
const captchaImage = ref("");
const captchaCode = ref("");
const captchaLoading = ref(false);

/** 拉取验证码图片（走 /api 前缀，经网关白名单 /auth/captcha/** 转发到 auth 服务）
 *  captchaKey 由后端生成并返回，前端无需（也不应）自行指定。
 *  focusInput 为 true 时（点击验证码图片刷新），刷新成功后清空输入框并自动聚焦。 */
async function fetchCaptcha(focusInput = false): Promise<void> {
  captchaLoading.value = true;
  try {
    const response = await fetch(`/api/auth/captcha/generate`, {
      headers: { Accept: "application/json" }
    });
    if (response.ok) {
      const data = (await response.json()) as { captchaKey: string; imageBase64: string };
      captchaKey.value = data.captchaKey;
      captchaImage.value = data.imageBase64;
      // 刷新成功后清空验证码输入框
      captchaCode.value = "";
      captchaErrorVisible.value = false;
      if (focusInput) {
        captchaInput.value?.focus();
      }
    }
  } catch {
    // 网络错误时保持空白图片，不阻断登录
  } finally {
    captchaLoading.value = false;
  }
}

// ── 表单验证 ──
const usernameError = ref("");
const passwordError = ref("");
const captchaError = ref("");
const usernameErrorVisible = ref(false);
const passwordErrorVisible = ref(false);
const captchaErrorVisible = ref(false);

function validateUsername(): boolean {
  if (!username.value.trim()) {
    usernameError.value = "用户名不能为空";
    usernameErrorVisible.value = true;
    return false;
  }
  usernameErrorVisible.value = false;
  return true;
}

function validatePassword(): boolean {
  if (!password.value) {
    passwordError.value = "密码不能为空";
    passwordErrorVisible.value = true;
    return false;
  }
  passwordErrorVisible.value = false;
  return true;
}

function validateCaptcha(): boolean {
  if (!captchaCode.value.trim()) {
    captchaError.value = "验证码不能为空";
    captchaErrorVisible.value = true;
    return false;
  }
  captchaErrorVisible.value = false;
  return true;
}

// ── 轮播图 ──
const slides = [
  { src: "/images/login/carousel1.jpg", title: "欢迎使用我们的系统", subtitle: "现代化的解决方案" },
  { src: "/images/login/carousel2.jpg", title: "强大的功能", subtitle: "为您提供最佳体验" },
  { src: "/images/login/carousel3.jpg", title: "安全可靠", subtitle: "保护您的数据安全" }
];
const currentSlide = ref(0);
let carouselInterval: ReturnType<typeof setInterval> | null = null;

function showSlide(index: number): void {
  currentSlide.value = (index + slides.length) % slides.length;
}

function moveCarousel(direction: number): void {
  showSlide(currentSlide.value + direction);
  restartCarousel();
}

function startCarousel(): void {
  carouselInterval = setInterval(() => moveCarousel(1), 5000);
}

function stopCarousel(): void {
  if (carouselInterval) {
    clearInterval(carouselInterval);
    carouselInterval = null;
  }
}

function restartCarousel(): void {
  stopCarousel();
  startCarousel();
}

// ── 登录处理 ──
async function handleLogin(): Promise<void> {
  if (loading.value) return;

  const isUsernameValid = validateUsername();
  const isPasswordValid = validatePassword();
  const isCaptchaValid = validateCaptcha();
  if (!isUsernameValid || !isPasswordValid || !isCaptchaValid) return;

  loading.value = true;

  try {
    await authStore.login(username.value, password.value, captchaKey.value, captchaCode.value);
    authStore.ensureRoutes(router);
    await authStore.fetchProfile();

    // 登录成功后重定向到原始页面或首页
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "";
    await router.push(redirect || "/dashboard");
  } catch (error) {
    const message = error instanceof Error ? error.message : "登录失败，请重试";
    showToast(message, "negative");
    // 登录失败后刷新验证码
    void fetchCaptcha();
  } finally {
    loading.value = false;
  }
}

// ── 统一身份认证登录 ──
function handleSsoLogin(): void {
  showToast("统一身份认证功能开发中", "info");
}

// ── 密码可见性切换 ──
function togglePassword(): void {
  passwordVisible.value = !passwordVisible.value;
}

// ── 键盘交互 ──
/** 全局 Enter 提交：焦点不在输入框/按钮上时，按 Enter 触发登录 */
function onKeydown(e: KeyboardEvent): void {
  if (e.key !== "Enter") return;
  const tag = document.activeElement?.tagName;
  if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT" || tag === "BUTTON") return;
  e.preventDefault();
  void handleLogin();
}

/** 按键自动聚焦用户名框：焦点不在输入框时，按任意字母键直接聚焦并输入 */
function onFirstKeyFocus(e: KeyboardEvent): void {
  const tag = document.activeElement?.tagName;
  if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return;
  if (e.key.length !== 1 || e.ctrlKey || e.altKey || e.metaKey) return;
  e.preventDefault();
  // 通过 Vue 响应式更新 username，v-model 自动同步到 DOM
  username.value += e.key;
  usernameInput.value?.focus();
}

/** Tab 循环：在用户名框、密码框、验证码框之间循环，不跳出至浏览器地址栏 */
function onTabCycle(e: KeyboardEvent): void {
  if (e.key !== "Tab") return;
  const inputs = [usernameInput.value, passwordInput.value, captchaInput.value].filter(Boolean) as HTMLInputElement[];
  if (inputs.length < 2) return;
  const first = inputs[0];
  const last = inputs[inputs.length - 1];
  if (e.shiftKey && document.activeElement === first) {
    e.preventDefault();
    last.focus();
  } else if (!e.shiftKey && document.activeElement === last) {
    e.preventDefault();
    first.focus();
  }
}

onMounted(() => {
  startCarousel();
  void fetchCaptcha();
  window.addEventListener("keydown", onKeydown);
  window.addEventListener("keydown", onFirstKeyFocus);
  window.addEventListener("keydown", onTabCycle);
});

onUnmounted(() => {
  stopCarousel();
  window.removeEventListener("keydown", onKeydown);
  window.removeEventListener("keydown", onFirstKeyFocus);
  window.removeEventListener("keydown", onTabCycle);
});
</script>

<template>
  <div class="login-page-root">
    <div class="fullscreen-login md3">
      <!-- 轮播图背景 -->
      <div class="fullscreen-carousel">
        <div
          v-for="(slide, index) in slides"
          :key="index"
          class="carousel-item"
          :class="{ active: currentSlide === index }"
        >
          <img :src="slide.src" :alt="`轮播图${index + 1}`" />
          <div class="carousel-caption">
            <h2 class="md3-headline-large">{{ slide.title }}</h2>
            <p class="md3-body-large">{{ slide.subtitle }}</p>
          </div>
        </div>

        <div class="carousel-controls">
          <button class="md3-icon-button carousel-prev" type="button" tabindex="-1" @click="moveCarousel(-1)">
            <q-icon name="sym_r_chevron_left" class="login-icon" />
          </button>
          <div class="carousel-indicators">
            <span
              v-for="(_, index) in slides"
              :key="index"
              class="indicator"
              :class="{ active: currentSlide === index }"
              @click="showSlide(index); restartCarousel()"
            />
          </div>
          <button class="md3-icon-button carousel-next" type="button" tabindex="-1" @click="moveCarousel(1)">
            <q-icon name="sym_r_chevron_right" class="login-icon" />
          </button>
        </div>
      </div>

      <!-- 登录框 -->
      <div class="floating-login-container">
        <div class="login-box md3-surface elevation-3">
          <div class="login-header">
            <div class="login-logo">
              <q-icon name="sym_r_admin_panel_settings" class="login-icon logo-icon" />
            </div>
            <h1 class="md3-display-small login-title">用户登录</h1>
          </div>

          <form @submit.prevent="handleLogin">
            <!-- 用户名 -->
            <div class="md3-text-field-container">
              <div class="md3-text-field-wrapper">
                <q-icon name="sym_r_person" class="login-icon field-icon" />
                <input
                  v-model="username"
                  ref="usernameInput"
                  class="md3-text-field with-icon"
                  :class="{ error: usernameErrorVisible }"
                  type="text"
                  placeholder="请输入用户名/邮箱/手机号"
                  @input="validateUsername"
                  @focus="($event.target as HTMLInputElement).select()"
                />
                <div class="error-message" :style="{ display: usernameErrorVisible ? 'flex' : 'none' }">
                  <q-icon name="sym_r_error" class="login-icon error-icon" />
                  <span class="error-text">{{ usernameError }}</span>
                </div>
              </div>
            </div>

            <!-- 密码 -->
            <div class="md3-text-field-container">
              <div class="md3-text-field-wrapper">
                <q-icon name="sym_r_lock" class="login-icon field-icon" />
                <input
                  v-model="password"
                  ref="passwordInput"
                  class="md3-text-field with-icon"
                  :class="{ error: passwordErrorVisible }"
                  :type="passwordVisible ? 'text' : 'password'"
                  placeholder="请输入密码"
                  @input="validatePassword"
                />
                <button type="button" class="toggle-password" tabindex="-1" @click="togglePassword">
                  <q-icon :name="passwordVisible ? 'sym_r_visibility_off' : 'sym_r_visibility'" class="login-icon" />
                </button>
                <div class="error-message" :style="{ display: passwordErrorVisible ? 'flex' : 'none' }">
                  <q-icon name="sym_r_error" class="login-icon error-icon" />
                  <span class="error-text">{{ passwordError }}</span>
                </div>
              </div>
            </div>

            <!-- 图形验证码 -->
            <div class="md3-text-field-container captcha-row">
              <div class="md3-text-field-wrapper">
                <q-icon name="sym_r_verified_user" class="login-icon field-icon" />
                <input
                  v-model="captchaCode"
                  ref="captchaInput"
                  class="md3-text-field with-icon captcha-input"
                  :class="{ error: captchaErrorVisible }"
                  type="text"
                  placeholder="请输入验证码"
                  maxlength="6"
                  autocomplete="off"
                  @input="validateCaptcha"
                />
                <div class="error-message" :style="{ display: captchaErrorVisible ? 'flex' : 'none' }">
                  <q-icon name="sym_r_error" class="login-icon error-icon" />
                  <span class="error-text">{{ captchaError }}</span>
                </div>
              </div>
              <button
                class="captcha-img-wrapper"
                type="button"
                :disabled="captchaLoading"
                @click="fetchCaptcha(true)"
              >
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                <span v-else class="captcha-loading-placeholder"></span>
              </button>
            </div>

            <!-- 登录按钮 -->
            <button
              type="submit"
              class="md3-button md3-filled-button login-button"
              :class="{ loading }"
              :disabled="loading"
            >
              {{ loading ? '登录中...' : '登录' }}
            </button>

            <!-- 统一身份认证登录 -->
            <button
              type="button"
              class="md3-button md3-outlined-button sso-button"
              :disabled="loading"
              @click="handleSsoLogin"
            >
              统一身份认证登录
            </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>
