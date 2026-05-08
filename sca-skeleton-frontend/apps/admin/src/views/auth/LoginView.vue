<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { fetchCaptcha } from "../../apis/captcha";
import { useAuthStore } from "../../stores/auth";

const slides = [
  {
    image: "https://images.pexels.com/photos/29612112/pexels-photo-29612112.jpeg",
    title: "欢迎使用我们的系统",
    subtitle: "现代化的解决方案"
  },
  {
    image: "https://images.pexels.com/photos/4253272/pexels-photo-4253272.jpeg",
    title: "强大的功能",
    subtitle: "为您提供最佳体验"
  },
  {
    image: "https://images.pexels.com/photos/36752381/pexels-photo-36752381.jpeg",
    title: "安全可靠",
    subtitle: "保护您的数据安全"
  }
];

const currentSlide = ref(0);
const carouselInterval = ref(0);
const username = ref("");
const password = ref("");
const remember = ref(false);
const isPasswordVisible = ref(false);
const usernameError = ref("");
const passwordError = ref("");
const captchaInput = ref("");
const captchaError = ref("");
const captchaImageSrc = ref("");
const captchaId = ref("");
const captchaLoading = ref(false);
const isSubmitting = ref(false);

async function refreshCaptcha(clearMessages = true) {
  if (clearMessages) {
    captchaError.value = "";
  }
  captchaLoading.value = true;
  try {
    const { captchaId: id, imageSrc } = await fetchCaptcha();
    captchaId.value = id;
    captchaImageSrc.value = imageSrc;
    captchaInput.value = "";
  } catch (error) {
    console.error("验证码加载失败:", error);
    captchaId.value = "";
    captchaImageSrc.value = "";
    captchaError.value = "验证码加载失败，请稍后重试";
  } finally {
    captchaLoading.value = false;
  }
}

function validateCaptcha() {
  if (!captchaId.value) {
    captchaError.value = "验证码未就绪，请点击右侧图片刷新";
    return false;
  }
  const value = captchaInput.value.trim();
  if (value === "") {
    captchaError.value = "请输入验证码";
    return false;
  }
  captchaError.value = "";
  return true;
}

function clearCaptchaError() {
  if (captchaError.value) {
    captchaError.value = "";
  }
}
const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const passwordType = computed(() => (isPasswordVisible.value ? "text" : "password"));
const passwordIcon = computed(() => (isPasswordVisible.value ? "visibility_off" : "visibility"));
const loginButtonText = computed(() => (isSubmitting.value ? "登录中..." : "登录"));

function showSlide(slideIndex) {
  if (slideIndex >= slides.length) {
    slideIndex = 0;
  } else if (slideIndex < 0) {
    slideIndex = slides.length - 1;
  }
  currentSlide.value = slideIndex;
  console.log("Changed to slide:", slideIndex);
}

function moveCarousel(direction) {
  showSlide(currentSlide.value + direction);
}

function startAutoCarousel() {
  stopAutoCarousel();
  carouselInterval.value = window.setInterval(() => {
    moveCarousel(1);
  }, 5000);
}

function stopAutoCarousel() {
  if (carouselInterval.value) {
    clearInterval(carouselInterval.value);
    carouselInterval.value = 0;
  }
}

function handlePrev() {
  stopAutoCarousel();
  moveCarousel(-1);
  startAutoCarousel();
}

function handleNext() {
  stopAutoCarousel();
  moveCarousel(1);
  startAutoCarousel();
}

function handleIndicator(index) {
  stopAutoCarousel();
  showSlide(index);
  startAutoCarousel();
}

function togglePasswordVisibility() {
  isPasswordVisible.value = !isPasswordVisible.value;
}

function setError(type, message) {
  if (type === "username") {
    usernameError.value = message;
  } else {
    passwordError.value = message;
  }
}

function clearError(type) {
  if (type === "username") {
    usernameError.value = "";
  } else {
    passwordError.value = "";
  }
}

function validateUsername() {
  const value = username.value.trim();
  if (value === "") {
    setError("username", "用户名不能为空");
    return false;
  }
  if (value.length < 3) {
    setError("username", "用户名长度不能少于3个字符");
    return false;
  }
  clearError("username");
  return true;
}

function validatePassword() {
  if (password.value === "") {
    setError("password", "密码不能为空");
    return false;
  }
  if (password.value.length < 3) {
    setError("password", "密码长度不能少于3个字符");
    return false;
  }
  clearError("password");
  return true;
}

function showGlobalError() {
  const errorElement = document.createElement("div");
  errorElement.className = "md3-snackbar md3-snackbar-error";
  errorElement.textContent = "登录失败，请检查用户名和密码";
  errorElement.style.cssText =
    "position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); z-index: 1000;";
  document.body.appendChild(errorElement);
  setTimeout(() => {
    if (errorElement.parentNode) {
      errorElement.parentNode.removeChild(errorElement);
    }
  }, 3000);
}

async function handleLoginProcess() {
  isSubmitting.value = true;
  try {
    await authStore.login({
      username: username.value,
      password: password.value,
      captchaId: captchaId.value,
      captchaCode: captchaInput.value.trim()
    });
    authStore.ensureRoutes(router);
    await authStore.fetchProfile();
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/dashboard";
    await router.replace(redirect);
  } catch (error) {
    console.error("登录失败:", error);
    showGlobalError();
    void refreshCaptcha();
  } finally {
    isSubmitting.value = false;
  }
}

function handleSubmit() {
  const isUsernameValid = validateUsername();
  const isPasswordValid = validatePassword();
  const isCaptchaValid = validateCaptcha();
  if (isUsernameValid && isPasswordValid && isCaptchaValid) {
    handleLoginProcess();
  }
}

onMounted(() => {
  startAutoCarousel();
  void refreshCaptcha();
});

onBeforeUnmount(() => {
  stopAutoCarousel();
});
</script>

<template>
  <div class="fullscreen-login md3">
    <div class="fullscreen-carousel">
      <div
        v-for="(slide, index) in slides"
        :key="slide.image"
        class="carousel-item"
        :class="{ active: index === currentSlide }"
      >
        <img :src="slide.image" :alt="`轮播图${index + 1}`">
        <div class="carousel-caption">
          <h2 class="md3-headline-large">{{ slide.title }}</h2>
          <p class="md3-body-large">{{ slide.subtitle }}</p>
        </div>
      </div>

      <div class="carousel-controls">
        <button class="md3-icon-button carousel-prev" type="button" @click="handlePrev">
          <span class="material-symbols-rounded">chevron_left</span>
        </button>
        <div class="carousel-indicators">
          <span
            v-for="(_, index) in slides"
            :key="index"
            class="indicator"
            :class="{ active: index === currentSlide }"
            @click="handleIndicator(index)"
          />
        </div>
        <button class="md3-icon-button carousel-next" type="button" @click="handleNext">
          <span class="material-symbols-rounded">chevron_right</span>
        </button>
      </div>
    </div>

    <div class="floating-login-container">
      <div class="login-box md3-surface elevation-3">
        <div class="login-header">
          <div class="login-logo">
            <span class="material-symbols-rounded logo-icon">admin_panel_settings</span>
          </div>
          <h1 class="md3-display-small login-title" style="font-weight: 700 !important;">用户登录</h1>
        </div>

        <form id="loginForm" @submit.prevent="handleSubmit">
          <div class="md3-text-field-container">
            <div class="md3-text-field-wrapper">
              <span class="material-symbols-rounded field-icon">person</span>
              <input
                v-model="username"
                class="md3-text-field with-icon"
                :class="{ error: usernameError }"
                type="text"
                id="username"
                placeholder="请输入用户名/邮箱/手机号"
                @input="validateUsername"
              >
              <div id="username-error" class="error-message" :style="{ display: usernameError ? 'flex' : 'none' }">
                <span class="material-symbols-rounded error-icon">error</span>
                <span class="error-text">{{ usernameError }}</span>
              </div>
            </div>
          </div>

          <div class="md3-text-field-container">
            <div class="md3-text-field-wrapper">
              <span class="material-symbols-rounded field-icon">lock</span>
              <input
                v-model="password"
                class="md3-text-field with-icon"
                :class="{ error: passwordError }"
                :type="passwordType"
                id="password"
                placeholder="请输入密码"
                @input="validatePassword"
              >
              <button type="button" class="toggle-password" @click="togglePasswordVisibility">
                <span class="material-symbols-rounded">{{ passwordIcon }}</span>
              </button>
              <div id="password-error" class="error-message" :style="{ display: passwordError ? 'flex' : 'none' }">
                <span class="material-symbols-rounded error-icon">error</span>
                <span class="error-text">{{ passwordError }}</span>
              </div>
            </div>
          </div>

          <div class="md3-text-field-container">
            <div class="captcha-row">
              <div class="md3-text-field-wrapper captcha-input-wrapper">
                <span class="material-symbols-rounded field-icon">shield</span>
                <input
                  v-model="captchaInput"
                  class="md3-text-field with-icon captcha-input"
                  :class="{ error: captchaError }"
                  type="text"
                  id="captcha"
                  maxlength="32"
                  autocomplete="off"
                  placeholder="请输入验证码"
                  @input="clearCaptchaError"
                >
              </div>
              <button
                type="button"
                class="captcha-image-button"
                title="看不清？点击换一张"
                :disabled="captchaLoading"
                @click="refreshCaptcha()"
              >
                <img v-if="captchaImageSrc" :src="captchaImageSrc" alt="验证码" class="captcha-image" width="120" height="44">
                <span v-else class="captcha-placeholder md3-body-small">{{ captchaLoading ? "加载中…" : "点击加载" }}</span>
              </button>
            </div>
            <div id="captcha-error" class="error-message captcha-error-message" :style="{ display: captchaError ? 'flex' : 'none' }">
              <span class="material-symbols-rounded error-icon">error</span>
              <span class="error-text">{{ captchaError }}</span>
            </div>
          </div>

          <div class="form-options">
            <label class="md3-checkbox-container">
              <input id="remember" v-model="remember" type="checkbox">
              <span class="md3-checkbox enhanced"></span>
              <span class="md3-checkbox-label md3-body-medium">记住我</span>
            </label>
            <a href="#" class="md3-link md3-body-medium">忘记密码？</a>
          </div>

          <button id="loginBtn" type="submit" class="md3-button md3-filled-button login-button" style="font-weight: 700 !important;" :class="{ loading: isSubmitting }" :disabled="isSubmitting">
            {{ loginButtonText }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<style>
@import url("https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;700&display=swap");
@import url("https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@24,400,1,0");

:root {
  --md-sys-color-primary: #006494;
  --md-sys-color-on-primary: #ffffff;
  --md-sys-color-primary-container: #c8e6ff;
  --md-sys-color-on-primary-container: #001e30;
  --md-sys-color-secondary: #50606e;
  --md-sys-color-on-secondary: #ffffff;
  --md-sys-color-secondary-container: #d3e5f5;
  --md-sys-color-on-secondary-container: #0c1d29;
  --md-sys-color-tertiary: #66587b;
  --md-sys-color-on-tertiary: #ffffff;
  --md-sys-color-tertiary-container: #ecdcff;
  --md-sys-color-on-tertiary-container: #221533;
  --md-sys-color-error: #ba1a1a;
  --md-sys-color-on-error: #ffffff;
  --md-sys-color-error-container: #ffdad6;
  --md-sys-color-on-error-container: #410002;
  --md-sys-color-success: #006e1c;
  --md-sys-color-on-success: #ffffff;
  --md-sys-color-success-container: #98f990;
  --md-sys-color-on-success-container: #002204;
  --md-sys-color-background: #fcfcff;
  --md-sys-color-on-background: #1a1c1e;
  --md-sys-color-surface: #fcfcff;
  --md-sys-color-on-surface: #1a1c1e;
  --md-sys-color-surface-variant: #dee3eb;
  --md-sys-color-on-surface-variant: #42474e;
  --md-sys-color-outline: #72777f;
  --md-sys-color-outline-variant: #c2c7cf;
  --md-shadow-1: 0 1px 3px 0 rgba(0, 0, 0, 0.15), 0 1px 2px 0 rgba(0, 0, 0, 0.3);
  --md-shadow-2: 0 2px 6px 2px rgba(0, 0, 0, 0.15), 0 1px 2px 0 rgba(0, 0, 0, 0.3);
  --md-shadow-3: 0 4px 8px 3px rgba(0, 0, 0, 0.15), 0 1px 3px 0 rgba(0, 0, 0, 0.3);
  --md-shadow-4: 0 6px 10px 4px rgba(0, 0, 0, 0.15), 0 2px 3px 0 rgba(0, 0, 0, 0.3);
  --md-shadow-5: 0 8px 12px 6px rgba(0, 0, 0, 0.15), 0 4px 4px 0 rgba(0, 0, 0, 0.3);
  --md-sys-shape-corner-small: 4px;
  --md-sys-shape-corner-medium: 8px;
  --md-sys-shape-corner-large: 16px;
  --md-sys-shape-corner-extra-large: 28px;
  --md-state-hover-opacity: 0.08;
  --md-state-focus-opacity: 0.12;
  --md-state-pressed-opacity: 0.12;
  --md-state-dragged-opacity: 0.16;
  --md-ref-typeface-brand: "JetBrains Mono", "OPPO Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif !important;
  --md-ref-typeface-plain: "JetBrains Mono", "OPPO Sans", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif !important;
  --md-spacing-unit: 8px;
}

.fullscreen-login {
  margin: 0;
  padding: 0;
  font-family: var(--md-ref-typeface-plain);
  background-color: var(--md-sys-color-background);
  color: var(--md-sys-color-on-background);
  position: relative;
  min-height: 100vh;
  width: 100%;
  overflow: hidden;
}

.fullscreen-login :is(
  h1,
  h2,
  h3,
  h4,
  h5,
  h6,
  p,
  a,
  label,
  button,
  input,
  textarea,
  select,
  span
):not(.material-symbols-rounded) {
  font-weight: 500 !important;
  letter-spacing: 0 !important;
}

.md3-display-large { font-family: var(--md-ref-typeface-brand); font-size: 57px; line-height: 64px; font-weight: 400; }
.md3-display-medium { font-family: var(--md-ref-typeface-brand); font-size: 45px; line-height: 52px; font-weight: 400; }
.md3-display-small { font-family: var(--md-ref-typeface-brand); font-size: 36px; line-height: 44px; font-weight: 400; }
.md3-headline-large { font-family: var(--md-ref-typeface-brand); font-size: 32px; line-height: 40px; font-weight: 400; }
.md3-headline-medium { font-family: var(--md-ref-typeface-brand); font-size: 28px; line-height: 36px; font-weight: 400; }
.md3-headline-small { font-family: var(--md-ref-typeface-brand); font-size: 24px; line-height: 32px; font-weight: 400; }
.md3-title-large { font-family: var(--md-ref-typeface-brand); font-size: 22px; line-height: 28px; font-weight: 400; }
.md3-title-medium { font-family: var(--md-ref-typeface-plain); font-size: 16px; line-height: 24px; font-weight: 500; letter-spacing: 0.15px; }
.md3-title-small { font-family: var(--md-ref-typeface-plain); font-size: 14px; line-height: 20px; font-weight: 500; letter-spacing: 0.1px; }
.md3-body-large { font-family: var(--md-ref-typeface-plain); font-size: 16px; line-height: 24px; font-weight: 400; letter-spacing: 0.5px; }
.md3-body-medium { font-family: var(--md-ref-typeface-plain); font-size: 14px; line-height: 20px; font-weight: 400; letter-spacing: 0.25px; }
.md3-body-small { font-family: var(--md-ref-typeface-plain); font-size: 12px; line-height: 16px; font-weight: 400; letter-spacing: 0.4px; }
.md3-label-large { font-family: var(--md-ref-typeface-plain); font-size: 14px; line-height: 20px; font-weight: 500; letter-spacing: 0.1px; }
.md3-label-medium { font-family: var(--md-ref-typeface-plain); font-size: 12px; line-height: 16px; font-weight: 500; letter-spacing: 0.5px; }
.md3-label-small { font-family: var(--md-ref-typeface-plain); font-size: 11px; line-height: 16px; font-weight: 500; letter-spacing: 0.5px; }

.md3-surface { background-color: var(--md-sys-color-surface); color: var(--md-sys-color-on-surface); border-radius: var(--md-sys-shape-corner-medium); }
.elevation-3 { box-shadow: var(--md-shadow-3); }
.md3-button { font-family: var(--md-ref-typeface-plain); font-size: 14px; font-weight: 500; text-transform: uppercase; letter-spacing: 0.1px; height: 48px; padding: 0 24px; border-radius: 24px; display: inline-flex; align-items: center; justify-content: center; position: relative; border: none; cursor: pointer; outline: none; overflow: hidden; transition: all 0.2s ease-in-out; }
.md3-filled-button { background-color: var(--md-sys-color-primary); color: var(--md-sys-color-on-primary); }
.md3-filled-button:hover { background-color: var(--md-sys-color-primary); opacity: 0.92; box-shadow: var(--md-shadow-2); }
.md3-filled-button:active { background-color: var(--md-sys-color-primary); opacity: 0.85; box-shadow: var(--md-shadow-1); }

.md3-icon-button {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.2);
  color: white;
  border: none;
  cursor: pointer;
  outline: none;
  transition: background-color 0.2s, transform 0.2s;
}

.md3-icon-button:hover {
  background-color: rgba(255, 255, 255, 0.3);
  transform: scale(1.05);
}

.md3-icon-button:active {
  background-color: rgba(255, 255, 255, 0.4);
  transform: scale(0.95);
}

.md3-text-field-container {
  margin-bottom: calc(var(--md-spacing-unit) * 4);
  position: relative;
}

.md3-text-field-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.md3-text-field {
  width: 100%;
  height: 56px;
  padding: 0 16px;
  font-family: var(--md-ref-typeface-plain);
  font-size: 16px;
  background-color: var(--md-sys-color-surface-variant);
  color: var(--md-sys-color-on-surface);
  border: none;
  border-radius: 12px;
  outline: none;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.md3-text-field::placeholder { color: var(--md-sys-color-on-surface-variant); opacity: 0.7; }
.md3-text-field.with-icon { padding-left: 56px; padding-right: 56px; }
.md3-text-field.captcha-input { padding-right: 16px; }

.captcha-row {
  display: flex;
  align-items: stretch;
  gap: calc(var(--md-spacing-unit) * 2);
}

.captcha-image-button {
  flex-shrink: 0;
  min-width: 120px;
  min-height: 44px;
  padding: 0;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  background: var(--md-sys-color-surface-variant);
  line-height: 0;
  transition: box-shadow 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.captcha-image-button:hover:not(:disabled) {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.12);
}

.captcha-image-button:focus-visible {
  outline: 2px solid var(--md-sys-color-primary);
  outline-offset: 2px;
}

.captcha-image-button:disabled {
  cursor: wait;
  opacity: 0.85;
}

.captcha-placeholder {
  padding: 0 8px;
  text-align: center;
  color: var(--md-sys-color-on-surface-variant);
  line-height: 1.3;
  max-width: 112px;
}

.captcha-image {
  display: block;
  vertical-align: top;
}

.captcha-input-wrapper {
  flex: 1;
  min-width: 0;
}

.captcha-error-message {
  position: static;
  margin-top: 4px;
  left: auto;
  bottom: auto;
}
.field-icon { position: absolute; left: 16px; color: var(--md-sys-color-on-surface-variant); pointer-events: none; font-size: 24px; }
.toggle-password { position: absolute; right: 16px; background: none; border: none; color: var(--md-sys-color-on-surface-variant); cursor: pointer; display: flex; align-items: center; justify-content: center; padding: 0; z-index: 2; }
.md3-text-field:focus { background-color: var(--md-sys-color-surface); box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1), 0 0 0 2px var(--md-sys-color-primary); }
.md3-text-field.error { box-shadow: 0 4px 8px rgba(186, 26, 26, 0.1), 0 0 0 2px var(--md-sys-color-error); background-color: var(--md-sys-color-error-container); }

.error-message {
  color: var(--md-sys-color-error);
  font-size: 12px;
  margin-top: 4px;
  display: none;
  position: absolute;
  left: 16px;
  bottom: -22px;
  font-weight: 500;
  align-items: center;
  gap: 4px;
}

.error-icon { font-size: 14px; color: var(--md-sys-color-error); }

.md3-checkbox-container {
  display: inline-flex;
  align-items: center;
  position: relative;
  cursor: pointer;
  user-select: none;
}

.md3-checkbox-container input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
  height: 0;
  width: 0;
}

.md3-checkbox { position: relative; height: 18px; width: 18px; border: 2px solid var(--md-sys-color-outline); border-radius: 2px; margin-right: 6px; transition: all 0.2s ease-in-out; background-color: transparent; }
.md3-checkbox.enhanced { height: 16px; width: 16px; border-radius: 3px; border: 2px solid var(--md-sys-color-primary); box-shadow: 0 1px 2px rgba(0, 100, 148, 0.1); background-color: rgba(255, 255, 255, 0.8); }
.md3-checkbox-container:hover .md3-checkbox { border-color: var(--md-sys-color-primary); background-color: rgba(0, 100, 148, 0.04); }
.md3-checkbox-container:hover .md3-checkbox.enhanced { background-color: rgba(0, 100, 148, 0.08); transform: scale(1.05); }
.md3-checkbox-container input:checked ~ .md3-checkbox { background-color: var(--md-sys-color-primary); border-color: var(--md-sys-color-primary); }
.md3-checkbox-container input:checked ~ .md3-checkbox.enhanced { background-color: var(--md-sys-color-primary); box-shadow: 0 2px 8px rgba(0, 100, 148, 0.3); }

.md3-checkbox-container input:checked ~ .md3-checkbox::after {
  content: "";
  position: absolute;
  left: 6px;
  top: 2px;
  width: 5px;
  height: 10px;
  border: solid var(--md-sys-color-on-primary);
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
  animation: checkmark 0.2s ease-in-out forwards;
}

.md3-checkbox-container input:checked ~ .md3-checkbox.enhanced::after {
  left: 5px;
  top: 2px;
  width: 4px;
  height: 8px;
  border-width: 0 2px 2px 0;
}

@keyframes checkmark {
  0% { opacity: 0; transform: rotate(45deg) scale(0.5); }
  100% { opacity: 1; transform: rotate(45deg) scale(1); }
}

.fullscreen-login .form-options .md3-checkbox-label { font-family: var(--md-ref-typeface-plain); color: var(--md-sys-color-on-surface-variant); font-weight: 700 !important; }
.fullscreen-login .form-options .md3-link { color: var(--md-sys-color-on-surface-variant); text-decoration: none; transition: opacity 0.2s; font-weight: 700 !important; }
.md3-link:hover { opacity: 0.8; text-decoration: none; }
.material-symbols-rounded {
  font-family: "Material Symbols Rounded" !important;
  font-weight: normal;
  font-style: normal;
  font-size: 24px !important;
  line-height: 1;
  letter-spacing: normal;
  text-transform: none;
  display: inline-block;
  white-space: nowrap;
  word-wrap: normal;
  direction: ltr;
  font-feature-settings: "liga";
  -webkit-font-feature-settings: "liga";
  -webkit-font-smoothing: antialiased;
  font-variation-settings: "FILL" 1, "wght" 400, "GRAD" 0, "opsz" 24;
}

.fullscreen-carousel {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.carousel-item {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  transition: opacity 0.8s ease-in-out;
}

.carousel-item.active { opacity: 1; }
.carousel-item img { width: 100%; height: 100%; object-fit: cover; filter: brightness(0.7); }

.carousel-caption {
  position: absolute;
  bottom: 15%;
  left: 10%;
  max-width: 50%;
  text-align: left;
  color: white;
  padding: calc(var(--md-spacing-unit) * 3);
  background: rgba(0, 0, 0, 0.3);
  border-radius: var(--md-sys-shape-corner-large);
  backdrop-filter: blur(10px);
  box-shadow: var(--md-shadow-2);
  animation: fadeIn 0.5s ease-in-out 0.3s forwards;
  opacity: 0;
  transform: translateY(20px);
}

@keyframes fadeIn { to { opacity: 1; transform: translateY(0); } }

.carousel-controls {
  position: absolute;
  bottom: 40px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: calc(var(--md-spacing-unit) * 2);
  z-index: 3;
}

.carousel-prev, .carousel-next { color: white; background-color: rgba(0, 0, 0, 0.2); backdrop-filter: blur(4px); box-shadow: var(--md-shadow-2); }
.carousel-indicators { display: flex; justify-content: center; gap: 8px; }
.indicator { width: 8px; height: 8px; border-radius: 50%; background: rgba(255, 255, 255, 0.5); cursor: pointer; transition: all 0.3s ease; }
.indicator.active { width: 24px; border-radius: 4px; background: white; }

.floating-login-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: calc(var(--md-spacing-unit) * 4);
  padding-right: calc(var(--md-spacing-unit) * 15);
  z-index: 2;
  box-sizing: border-box;
  pointer-events: none;
}

.login-box {
  width: 100%;
  max-width: 420px;
  box-sizing: content-box;
  padding: calc(var(--md-spacing-unit) * 5);
  border-radius: var(--md-sys-shape-corner-large);
  backdrop-filter: blur(16px);
  background-color: rgba(255, 255, 255, 0.9);
  box-shadow: var(--md-shadow-5);
  transform: translateX(0);
  animation: floatIn 0.5s ease-out;
  pointer-events: auto;
}

@keyframes floatIn {
  from { opacity: 0; transform: translateY(30px); }
  to { opacity: 1; transform: translateY(0); }
}

.login-header { text-align: center; margin-bottom: calc(var(--md-spacing-unit) * 5); }
.login-logo { width: 80px; height: 80px; margin: 0 auto calc(var(--md-spacing-unit) * 3); background-color: var(--md-sys-color-primary-container); color: var(--md-sys-color-on-primary-container); border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: var(--md-shadow-2); }
.login-logo .material-symbols-rounded.logo-icon {
  font-family: "Material Symbols Rounded" !important;
  font-size: 42px !important;
  line-height: 1 !important;
  font-variation-settings: "FILL" 1, "wght" 400, "GRAD" 0, "opsz" 24;
}

.carousel-prev .material-symbols-rounded,
.carousel-next .material-symbols-rounded,
.toggle-password .material-symbols-rounded,
.field-icon.material-symbols-rounded {
  font-family: "Material Symbols Rounded" !important;
  font-size: 24px !important;
  line-height: 1 !important;
  font-variation-settings: "FILL" 1, "wght" 400, "GRAD" 0, "opsz" 24;
}

.error-icon.material-symbols-rounded {
  font-family: "Material Symbols Rounded" !important;
  font-size: 14px !important;
  line-height: 1 !important;
  font-variation-settings: "FILL" 1, "wght" 400, "GRAD" 0, "opsz" 24;
}
.login-title { color: var(--md-sys-color-on-surface-variant); margin-top: calc(var(--md-spacing-unit) * 1.5); font-weight: 600; letter-spacing: -0.5px; }
.fullscreen-login .login-button { width: 100%; margin-top: calc(var(--md-spacing-unit) * 4); font-size: 18px; font-weight: 700 !important; letter-spacing: 1px; height: 52px; text-transform: none; border-radius: 26px; background: linear-gradient(135deg, var(--md-sys-color-primary), #3a89c9); box-shadow: 0 4px 12px rgba(0, 100, 148, 0.3); transition: all 0.3s ease; }
.login-button { box-sizing: border-box; }
.login-button:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0, 100, 148, 0.4); }
.login-button:active { transform: translateY(0); box-shadow: 0 2px 8px rgba(0, 100, 148, 0.3); }
.form-options { display: flex; justify-content: space-between; align-items: center; margin: calc(var(--md-spacing-unit) * 2) 5px; }
.login-button.loading { position: relative; pointer-events: none; overflow: hidden; }

.login-button.loading::before {
  content: "";
  position: absolute;
  left: -100%;
  top: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  animation: loading 1.5s infinite;
}

@keyframes loading { 100% { left: 100%; } }

@media (max-width: 768px) {
  .floating-login-container {
    justify-content: center;
    padding: calc(var(--md-spacing-unit) * 2);
  }

  .carousel-caption {
    left: 5%;
    right: 5%;
    max-width: 90%;
    bottom: auto;
    top: 15%;
  }
}
</style>
