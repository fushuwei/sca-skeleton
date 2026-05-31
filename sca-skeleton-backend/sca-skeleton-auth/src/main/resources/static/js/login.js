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

    console.log('Changed to slide:', slideIndex);
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
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const remember = document.getElementById('remember').checked;
    const loginBtn = document.getElementById('loginBtn');
    const loginForm = document.getElementById('loginForm');

    // 检查是否是 portal 页面，如果是则添加验证码字段
    const captchaCodeInput = document.getElementById('captchaCode');
    const captchaKeyInput = document.getElementById('captchaKey');

    // 显示登录中状态
    if (loginBtn) {
        loginBtn.classList.add('loading');
        loginBtn.textContent = '登录中...';
        loginBtn.disabled = true;
    }

    // 创建表单数据
    const formData = new FormData();
    formData.append('username', username);
    formData.append('password', password);
    if (remember) {
        formData.append('remember-me', 'true');
    }

    // 如果存在验证码字段，添加到表单数据
    if (captchaCodeInput && captchaKeyInput) {
        formData.append('captchaCode', captchaCodeInput.value);
        formData.append('captchaKey', captchaKeyInput.value);
    }

    // 获取表单的 action URL
    const actionUrl = loginForm ? loginForm.getAttribute('action') : '/login';

    // 使用Fetch API提交登录请求
    fetch(actionUrl, {
        method: 'POST',
        body: formData,
        credentials: 'same-origin' // 确保Cookie随请求发送
    })
    .then(response => {
        if (response.ok) {
            // 登录成功，跳转到首页
            window.location.href = '/';
        } else {
            // 登录失败，显示错误信息
            return response.text().then(errorMessage => {
                throw new Error(errorMessage);
            });
        }
    })
    .catch(error => {
        console.error('登录失败:', error);
        // 显示错误提示
        const errorElement = document.createElement('div');
        errorElement.className = 'md3-snackbar md3-snackbar-error';
        errorElement.textContent = '登录失败，请检查用户名和密码';
        errorElement.style.cssText = 'position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); z-index: 1000;';
        document.body.appendChild(errorElement);

        // 3秒后自动移除错误提示
        setTimeout(() => {
            document.body.removeChild(errorElement);
        }, 3000);

        // 刷新验证码（如果是 portal 页面）
        if (captchaCodeInput) {
            refreshCaptcha();
        }

        // 重置登录按钮状态
        if (loginBtn) {
            loginBtn.classList.remove('loading');
            loginBtn.textContent = '登录';
            loginBtn.disabled = false;
        }
    });
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

// 页面加载完成后初始化所有功能
document.addEventListener('DOMContentLoaded', () => {
    // 初始化轮播图
    initCarousel();

    // 初始化表单验证
    initFormValidation();

    // 绑定验证码刷新
    const captchaImg = document.getElementById('captchaImg');
    if (captchaImg) {
        captchaImg.addEventListener('click', refreshCaptcha);
    }

    // 隐藏 loading
    hidePageLoader();
});

// 页面完全加载后确保 loading 隐藏
window.addEventListener('load', () => {
    hidePageLoader();
});
