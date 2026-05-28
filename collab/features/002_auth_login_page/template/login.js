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
        prevBtn.onclick = function () {
            stopAutoCarousel();
            moveCarousel(-1);
            startAutoCarousel();
        };
    }

    if (nextBtn) {
        nextBtn.onclick = function () {
            stopAutoCarousel();
            moveCarousel(1);
            startAutoCarousel();
        };
    }

    // 指示器点击事件
    indicators.forEach((indicator, index) => {
        indicator.onclick = function () {
            stopAutoCarousel();
            showSlide(index);
            startAutoCarousel();
        };
    });

    // 启动自动轮播
    startAutoCarousel();
}

// 页面加载完成后初始化所有功能
document.addEventListener('DOMContentLoaded', () => {
    // 初始化轮播图
    initCarousel();
});
