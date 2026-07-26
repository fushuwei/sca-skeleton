<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import "../../styles/login.scss";
import { usePortalAuthStore } from "../../stores/auth";

const route = useRoute();
const router = useRouter();
const authStore = usePortalAuthStore();

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

/** 生成 UUID（兼容 crypto.randomUUID 和降级方案） */
function generateUuid(): string {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === "x" ? r : (r & 0x3) | 0x8).toString(16);
  });
}

/** 拉取验证码图片（通过网关白名单 /auth/captcha/**） */
async function fetchCaptcha(): Promise<void> {
  captchaLoading.value = true;
  const newKey = generateUuid();
  captchaKey.value = newKey;
  captchaCode.value = "";
  try {
    const response = await fetch(`/auth/captcha/generate?key=${newKey}`, {
      headers: { Accept: "application/json" }
    });
    if (response.ok) {
      const data = (await response.json()) as { captchaKey: string; imageBase64: string };
      captchaImage.value = data.imageBase64;
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

// ── Toast 提示 ──
const toastVisible = ref(false);
const toastMessage = ref("");
let toastTimer: ReturnType<typeof setTimeout> | null = null;

function showToast(message: string): void {
  toastMessage.value = message;
  toastVisible.value = true;
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toastVisible.value = false;
  }, 5000);
}

function dismissToast(): void {
  toastVisible.value = false;
  if (toastTimer) {
    clearTimeout(toastTimer);
    toastTimer = null;
  }
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
  dismissToast();

  try {
    await authStore.login(username.value, password.value, captchaKey.value, captchaCode.value);
    authStore.ensureRoutes(router);
    await authStore.fetchProfile();

    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "";
    await router.push(redirect || "/");
  } catch (error) {
    const message = error instanceof Error ? error.message : "登录失败，请重试";
    showToast(message);
    // 登录失败后刷新验证码
    void fetchCaptcha();
  } finally {
    loading.value = false;
  }
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
  if (toastTimer) clearTimeout(toastTimer);
});
</script>

<template>
  <div class="login-page-root">
    <!-- Toast 提示 -->
    <div
      class="login-toast"
      :class="{ visible: toastVisible }"
      :style="toastVisible ? { animation: 'toastFadeIn 0.25s ease forwards' } : {}"
    >
      <span class="material-symbols-rounded login-icon toast-icon">error</span>
      <span>{{ toastMessage }}</span>
      <button class="toast-close-btn" type="button" @click="dismissToast">
        <span class="material-symbols-rounded login-icon">close</span>
      </button>
    </div>

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
            <span class="material-symbols-rounded login-icon">chevron_left</span>
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
            <span class="material-symbols-rounded login-icon">chevron_right</span>
          </button>
        </div>
      </div>

      <!-- 登录框 -->
      <div class="floating-login-container">
        <div class="login-box md3-surface elevation-3">
          <div class="login-header">
            <div class="login-logo">
              <span class="material-symbols-rounded login-icon logo-icon">admin_panel_settings</span>
            </div>
            <h1 class="md3-display-small login-title">用户登录</h1>
          </div>

          <form @submit.prevent="handleLogin">
            <!-- 用户名 -->
            <div class="md3-text-field-container">
              <div class="md3-text-field-wrapper">
                <span class="material-symbols-rounded login-icon field-icon">person</span>
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
                  <span class="material-symbols-rounded login-icon error-icon">error</span>
                  <span class="error-text">{{ usernameError }}</span>
                </div>
              </div>
            </div>

            <!-- 密码 -->
            <div class="md3-text-field-container">
              <div class="md3-text-field-wrapper">
                <span class="material-symbols-rounded login-icon field-icon">lock</span>
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
                  <span class="material-symbols-rounded login-icon">
                    {{ passwordVisible ? 'visibility_off' : 'visibility' }}
                  </span>
                </button>
                <div class="error-message" :style="{ display: passwordErrorVisible ? 'flex' : 'none' }">
                  <span class="material-symbols-rounded login-icon error-icon">error</span>
                  <span class="error-text">{{ passwordError }}</span>
                </div>
              </div>
            </div>

            <!-- 图形验证码 -->
            <div class="md3-text-field-container captcha-row">
              <div class="md3-text-field-wrapper">
                <span class="material-symbols-rounded login-icon field-icon">verified_user</span>
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
                  <span class="material-symbols-rounded login-icon error-icon">error</span>
                  <span class="error-text">{{ captchaError }}</span>
                </div>
              </div>
              <button
                class="captcha-img-wrapper"
                type="button"
                :disabled="captchaLoading"
                @click="fetchCaptcha"
              >
                <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
                <span v-else class="captcha-loading-placeholder"></span>
              </button>
            </div>

            <!-- 忘记密码 -->
            <div class="form-options">
              <span></span>
              <a href="#" class="md3-link md3-body-medium" tabindex="-1" title="功能开发中" @click.prevent>忘记密码？</a>
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
          </form>

          <!-- 其他登录方式 -->
          <div class="additional-options">
            <p class="md3-body-small">其他登录方式</p>
            <div class="social-login">
              <button class="social-login-button" type="button" tabindex="-1" title="功能开发中">
                <span class="material-symbols-rounded login-icon">smartphone</span>
              </button>
              <button class="social-login-button" type="button" tabindex="-1" title="功能开发中">
                <span class="material-symbols-rounded login-icon">qr_code_scanner</span>
              </button>
              <button class="social-login-button" type="button" tabindex="-1" title="功能开发中">
                <span class="material-symbols-rounded login-icon">fingerprint</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
