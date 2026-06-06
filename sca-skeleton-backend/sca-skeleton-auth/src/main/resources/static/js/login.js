// 登录页面功能
let currentSlide = 0;
const slides = document.querySelectorAll('.carousel-item');
const indicators = document.querySelectorAll('.indicator');
const totalSlides = slides.length;

// 显示指定幻灯片
function showSlide(slideIndex) {
    if (slideIndex >= totalSlides) {
        slideIndex = 0;
    } else if (slideIndex < 0) {
        slideIndex = totalSlides - 1;
    }

    // 移除所有活动状态
    slides.forEach(slide => {
        slide.classList.remove('active');
    });

    indicators.forEach(indicator => {
        indicator.classList.remove('active');
    });

    // 设置当前幻灯片和指示器为活动状态
    slides[slideIndex].classList.add('active');
    indicators[slideIndex].classList.add('active');

    currentSlide = slideIndex;
}

// 移动轮播图
function moveCarousel(direction) {
    showSlide(currentSlide + direction);
}

// 自动轮播
let carouselInterval;

function startAutoCarousel() {
    carouselInterval = setInterval(() => {
        moveCarousel(1);
    }, 5000); // 每5秒切换一次
}

function stopAutoCarousel() {
    clearInterval(carouselInterval);
}

// 初始化轮播控件
function initCarousel() {
    // 直接绑定点击事件到DOM元素
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');

    if (prevBtn) {
        prevBtn.onclick = function() {
            stopAutoCarousel();
            moveCarousel(-1);
            startAutoCarousel();
        };
    }

    if (nextBtn) {
        nextBtn.onclick = function() {
            stopAutoCarousel();
            moveCarousel(1);
            startAutoCarousel();
        };
    }

    // 指示器点击事件
    indicators.forEach((indicator, index) => {
        indicator.onclick = function() {
            stopAutoCarousel();
            showSlide(index);
            startAutoCarousel();
        };
    });

    // 启动自动轮播
    startAutoCarousel();
}

function handleSwipe() {
    const swipeThreshold = 50; // 滑动阈值

    if (touchEndX < touchStartX - swipeThreshold) {
        // 向左滑动，显示下一张
        moveCarousel(1);
    } else if (touchEndX > touchStartX + swipeThreshold) {
        // 向右滑动，显示上一张
        moveCarousel(-1);
    }
}

// 切换密码可见性
function togglePasswordVisibility() {
    const passwordInput = document.getElementById('password');
    const toggleButton = document.querySelector('.toggle-password .material-symbols-rounded');

    if (!passwordInput || !toggleButton) return;

    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        toggleButton.textContent = 'visibility_off';
    } else {
        passwordInput.type = 'password';
        toggleButton.textContent = 'visibility';
    }
}

// 表单验证
function initFormValidation() {
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const usernameError = document.getElementById('username-error');
    const passwordError = document.getElementById('password-error');
    const loginForm = document.getElementById('loginForm');

    if (!usernameInput || !passwordInput || !loginForm) return;

    // 添加输入事件监听器，实时验证
    usernameInput.addEventListener('input', () => validateUsername(usernameInput, usernameError));
    passwordInput.addEventListener('input', () => validatePassword(passwordInput, passwordError));

    // 表单提交处理
    loginForm.addEventListener('submit', (event) => {
        event.preventDefault();

        // 表单验证
        const isUsernameValid = validateUsername(usernameInput, usernameError);
        const isPasswordValid = validatePassword(passwordInput, passwordError);

        // Portal 页面需要额外验证验证码
        const captchaCodeInput = document.getElementById('captchaCode');
        const captchaCodeError = document.getElementById('captchaCode-error');
        let isCaptchaValid = true;
        if (captchaCodeInput && captchaCodeError) {
            isCaptchaValid = validateCaptchaCode(captchaCodeInput, captchaCodeError);
        }

        if (isUsernameValid && isPasswordValid && isCaptchaValid) {
            // 表单验证通过，可以提交
            handleLoginProcess();
        }

        return false;
    });
}

// 用户名验证
function validateUsername(input, errorElement) {
    if (!input || !errorElement) return false;

    const username = input.value.trim();

    if (username === '') {
        setError(input, errorElement, '用户名不能为空');
        return false;
    } else if (username.length < 3) {
        setError(input, errorElement, '用户名长度不能少于3个字符');
        return false;
    } else {
        clearError(input, errorElement);
        return true;
    }
}

// 密码验证
function validatePassword(input, errorElement) {
    if (!input || !errorElement) return false;

    const password = input.value;

    if (password === '') {
        setError(input, errorElement, '密码不能为空');
        return false;
    } else if (password.length < 3) {
        setError(input, errorElement, '密码长度不能少于3个字符');
        return false;
    } else {
        clearError(input, errorElement);
        return true;
    }
}

// 验证码验证（仅 portal 页面）
function validateCaptchaCode(input, errorElement) {
    if (!input || !errorElement) return false;

    const captchaCode = input.value.trim();

    if (captchaCode === '') {
        setError(input, errorElement, '验证码不能为空');
        return false;
    } else if (captchaCode.length !== 4) {
        setError(input, errorElement, '请输入4位验证码');
        return false;
    } else {
        clearError(input, errorElement);
        return true;
    }
}

// 设置错误
function setError(input, errorElement, message) {
    if (!input || !errorElement) return;

    input.classList.add('error');
    const errorText = errorElement.querySelector('.error-text');
    if (errorText) {
        errorText.textContent = message;
    }
    errorElement.style.display = 'flex';
}

// 清除错误
function clearError(input, errorElement) {
    if (!input || !errorElement) return;

    input.classList.remove('error');
    errorElement.style.display = 'none';
}

// 登录处理过程
function handleLoginProcess() {
    const remember = document.getElementById('remember').checked;
    const loginBtn = document.getElementById('loginBtn');
    const loginForm = document.getElementById('loginForm');

    if (!loginForm) return;

    // 按钮切换为“登录中...”并禁用，防止重复提交
    if (loginBtn) {
        loginBtn.textContent = '登录中...';
        loginBtn.disabled = true;
    }

    // 如果勾选了"记住我"，动态追加 remember-me 隐藏字段
    if (remember) {
        let rememberInput = loginForm.querySelector('input[name="remember-me"]');
        if (!rememberInput) {
            rememberInput = document.createElement('input');
            rememberInput.type = 'hidden';
            rememberInput.name = 'remember-me';
            loginForm.appendChild(rememberInput);
        }
        rememberInput.value = 'true';
    }

    // 立即移除 Toast Tips（不用过渡动画，因为 submit 会立即跳转页面）
    const toast = document.querySelector('.login-toast');
    if (toast) toast.remove();
    if (toastTimer) {
        clearTimeout(toastTimer);
        toastTimer = null;
    }

    // 直接提交 HTML 表单，让浏览器自然跟随服务端的 302 重定向
    // 登录成功后 OAuthAuthorizeLoginSuccessHandler 会 302 到 authorize URL，
    // 进而回调到前端的 OAuthCallbackView 完成 token 交换和最终跳转
    loginForm.submit();
}

// 显示页面加载遮罩
function showPageLoader() {
    const loader = document.getElementById('pageLoader');
    if (loader) {
        loader.classList.remove('hidden');
    }
}

// 验证码刷新
function refreshCaptcha() {
    const captchaImg = document.getElementById('captchaImg');
    const captchaKeyInput = document.getElementById('captchaKey');
    if (!captchaImg || !captchaKeyInput) return;

    const newKey = (typeof crypto !== 'undefined' && crypto.randomUUID)
        ? crypto.randomUUID()
        : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
            const r = Math.random() * 16 | 0;
            return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
    captchaKeyInput.value = newKey;

    const baseUrl = captchaImg.getAttribute('data-captcha-url') || '/auth/captcha/generate';
    captchaImg.src = baseUrl + '?key=' + newKey + '&t=' + Date.now();

    const captchaCodeInput = document.getElementById('captchaCode');
    if (captchaCodeInput) {
        captchaCodeInput.value = '';
        captchaCodeInput.focus();
    }
}

// 隐藏 loading 遮罩
function hidePageLoader() {
    const loader = document.getElementById('pageLoader');
    if (loader) {
        setTimeout(() => {
            loader.classList.add('hidden');
        }, 600);
    }
}

// 读取 URL 查询参数并在浏览器中间顶部显示黑色 Toast Tips（5 秒后自动关闭）
function handleServerErrors() {
    const params = new URLSearchParams(window.location.search);

    // 验证码错误优先展示（避免与用户名密码错误重复）
    if (params.has('captcha-error')) {
        showToast('验证码错误，请重新输入');
        return;
    }

    // 用户名密码错误
    if (params.has('error')) {
        showToast('用户名或密码错误，请重试');
        return;
    }
}

/** 当前 toast 的自动关闭定时器 */
let toastTimer = null;

/**
 * 在浏览器中间顶部显示黑色 Toast Tips，5 秒后自动消失。
 * @param {string} message 提示文本
 */
function showToast(message) {
    // 先移除旧 toast
    dismissToast();

    const toast = document.createElement('div');
    toast.className = 'login-toast';

    // 左侧错误/警告图标
    const icon = document.createElement('span');
    icon.className = 'material-symbols-rounded toast-icon';
    icon.textContent = 'error';

    // 文本
    const text = document.createElement('span');
    text.textContent = message;

    // 右侧关闭按钮 — 用 button 确保可点击，区域不小于 24×24
    const closeWrapper = document.createElement('button');
    closeWrapper.className = 'toast-close-btn';
    closeWrapper.type = 'button';
    const closeIcon = document.createElement('span');
    closeIcon.className = 'material-symbols-rounded';
    closeIcon.textContent = 'close';
    closeWrapper.appendChild(closeIcon);
    closeWrapper.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        dismissToast();
    });

    toast.appendChild(icon);
    toast.appendChild(text);
    toast.appendChild(closeWrapper);
    document.body.appendChild(toast);

    // 显示动画
    toast.style.animation = 'toastFadeIn 0.25s ease forwards';

    // 5 秒后自动关闭
    toastTimer = setTimeout(() => dismissToast(), 5000);
}

/**
 * 立即关闭当前 Toast（带淡出过渡）
 */
function dismissToast() {
    if (toastTimer) {
        clearTimeout(toastTimer);
        toastTimer = null;
    }
    const toast = document.querySelector('.login-toast');
    if (toast) {
        toast.style.opacity = '0';
        toast.addEventListener('transitionend', () => toast.remove(), { once: true });
    }
}

// 页面加载完成后初始化所有功能
document.addEventListener('DOMContentLoaded', () => {
    // 初始化轮播图
    initCarousel();

    // 初始化表单验证
    initFormValidation();

    // 绑定密码可见性切换
    const togglePwd = document.getElementById('togglePassword');
    if (togglePwd) {
        togglePwd.addEventListener('click', togglePasswordVisibility);
    }

    // 绑定验证码刷新
    const captchaImg = document.getElementById('captchaImg');
    if (captchaImg) {
        captchaImg.addEventListener('click', refreshCaptcha);
    }

    // 读取 URL 参数并显示服务端错误（如 ?error 或 ?captcha-error）
    handleServerErrors();

    // 隐藏 loading
    hidePageLoader();
});

// 页面完全加载后确保 loading 隐藏
window.addEventListener('load', () => {
    hidePageLoader();
});
