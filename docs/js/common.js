/* ================================
   氢风官网 · 共用脚本（index / history / community 共用）
   职责：i18n 初始化 + 图片加载失败兜底
   语言数据路径由 <script data-base="data/"> 传入（html/ 下子页为 ../data/）
   ================================ */

// ================================
// Google Fonts 加载（preconnect + link 标签，避免 @import 渲染阻塞）
// ================================
(function () {
    if (document.querySelector('link[href*="fonts.googleapis.com"]')) return;
    const head = document.head;
    const preconnect1 = document.createElement('link');
    preconnect1.rel = 'preconnect';
    preconnect1.href = 'https://fonts.googleapis.com';
    const preconnect2 = document.createElement('link');
    preconnect2.rel = 'preconnect';
    preconnect2.href = 'https://fonts.gstatic.com';
    preconnect2.crossOrigin = 'anonymous';
    const stylesheet = document.createElement('link');
    stylesheet.rel = 'stylesheet';
    stylesheet.href = 'https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;600;700&display=swap';
    head.prepend(preconnect1, preconnect2, stylesheet);
})();

// i18n 核心（通过 window 全局共享，main.js / common-modal.js 均读取此变量）
window.currentMessages = null;

// key 统一小写；value 为实际语言文件名（zh-TW 存在独立的繁体语言包）
const localeMap = {
    'zh': 'zh',
    'zh-cn': 'zh',
    'zh-tw': 'zh-TW',
    'zh-hk': 'zh-TW',
    'zh-mo': 'zh-TW',
    'en': 'en',
    'en-us': 'en',
    'en-gb': 'en',
    'ja': 'ja',
    'ko': 'ko',
    'ru': 'ru',
    'pt': 'pt',
    'pt-br': 'pt',
    'pt-pt': 'pt',
    'fr': 'fr',
    'fr-fr': 'fr',
    'fr-ca': 'fr',
    'de': 'de',
    'es': 'es',
    'es-es': 'es',
    'es-mx': 'es'
};
const defaultLang = 'zh';

function getBrowserLang() {
    const lang = (navigator.language || navigator.userLanguage || defaultLang).toLowerCase();
    if (localeMap[lang]) return localeMap[lang];
    const baseLang = lang.split('-')[0];
    return localeMap[baseLang] || defaultLang;
}

// 语言文件路径前缀：由 <script data-base> 提供（缺省当前目录）
const DATA_BASE = (document.currentScript && document.currentScript.dataset.base) || '';

function loadMessages(lang) {
    return fetch(`${DATA_BASE}locales/${lang}.json`, { cache: 'no-cache' })
        .then(res => {
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return res.json();
        })
        .then(messages => {
            messages._lang = lang;
            return messages;
        })
        .catch(err => {
            console.warn(`加载语言文件 ${lang} 失败，回退到 ${defaultLang}`, err);
            if (lang !== defaultLang) return loadMessages(defaultLang);
            throw err;
        });
}

function applyI18n(messages) {
    window.currentMessages = messages;
    // 更新 <html lang> 以匹配当前语言
    if (messages._lang) {
        document.documentElement.lang = messages._lang;
    }
    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (messages[key]) {
            el.textContent = messages[key];
        }
    });
    // aria-label 国际化：<nav data-i18n-aria="nav_label">
    document.querySelectorAll('[data-i18n-aria]').forEach(el => {
        const key = el.getAttribute('data-i18n-aria');
        if (messages[key]) {
            el.setAttribute('aria-label', messages[key]);
        }
    });
    // 页面标题：key 由 <title data-i18n> 提供（site_title / history_title / community_page_title）
    const titleEl = document.querySelector('title[data-i18n]');
    const titleKey = titleEl && titleEl.getAttribute('data-i18n');
    if (titleKey && messages[titleKey]) document.title = messages[titleKey];
}

// ================================
// 图片加载失败兜底（error 事件不冒泡，用捕获阶段监听）
// ================================

// 生成 SVG data-URI 兜底图（替代内联 onerror 里手写 6 段长 URI）
function imgErrorSvg(label, w, h, opts) {
    opts = opts || {};
    const bg = opts.bg || '#b8d4e3';
    const color = opts.color || '#1d4d6b';
    const fontSize = opts.fontSize != null ? opts.fontSize : Math.round(h * 0.24);
    const y = opts.y != null ? opts.y : Math.round(h * 0.55);
    const svg =
        '<svg xmlns="http://www.w3.org/2000/svg" width="' + w + '" height="' + h +
        '" viewBox="0 0 ' + w + ' ' + h + '">' +
        '<rect width="' + w + '" height="' + h + '" fill="' + bg + '"/>' +
        '<text x="' + (w / 2) + '" y="' + y + '" font-size="' + fontSize + '" fill="' + color +
        '" text-anchor="middle" dominant-baseline="middle">' + label + '</text></svg>';
    return 'data:image/svg+xml,' + encodeURIComponent(svg);
}

document.addEventListener('error', function (e) {
    const el = e.target;
    if (!el || el.tagName !== 'IMG') return;
    if (el.dataset.fallback === undefined && el.dataset.fallbackNext === undefined) return;
    if (el.src && el.src.indexOf('data:') === 0) return; // 已兜底过
    // 修复步骤图：隐藏自身，显示兄弟占位
    if (el.dataset.fallbackNext !== undefined) {
        el.style.display = 'none';
        const ph = el.nextElementSibling;
        if (ph) ph.style.display = 'block';
        return;
    }
    const label = el.dataset.fallback;
    const w = Number(el.dataset.fallbackW || 200);
    const h = Number(el.dataset.fallbackH || 100);
    if (el.dataset.fallbackLogo !== undefined) {
        // 品牌 logo：保留 grayscale 效果，不加 error 类
        el.style.filter = 'grayscale(0.3)';
    } else {
        el.classList.add('error');
        if (el.dataset.fallbackAlt) el.alt = el.dataset.fallbackAlt;
    }
    el.src = imgErrorSvg(label, w, h, {
        bg: el.dataset.fallbackBg,
        color: el.dataset.fallbackColor,
        fontSize: el.dataset.fallbackFont,
        y: el.dataset.fallbackY,
    });
}, true);

// ================================
// 页脚邮箱复制
// ================================

function copyToClipboard(text) {
    if (navigator.clipboard && window.isSecureContext) {
        return navigator.clipboard.writeText(text);
    }
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.opacity = '0';
    document.body.appendChild(ta);
    ta.select();
    try { document.execCommand('copy'); } catch (e) {}
    document.body.removeChild(ta);
    return Promise.resolve();
}

document.addEventListener('click', (e) => {
    const btn = e.target.closest('.footer-copy-btn');
    if (!btn) return;
    copyToClipboard(btn.dataset.copy).then(() => {
        const label = btn.querySelector('[data-i18n]');
        if (!label) return;
        const original = (window.currentMessages && window.currentMessages.copy_button) || '复制';
        const copied = (window.currentMessages && window.currentMessages.copied_feedback) || '已复制';
        label.textContent = copied;
        setTimeout(() => { label.textContent = original; }, 2000);
    });
});

// ==================== 吸顶导航（全站共享） ====================
function initTopNav() {
    const topNav = document.getElementById('topNav');
    const navToggle = document.getElementById('navToggle');
    const navLinks = document.getElementById('navLinks');
    if (topNav) {
        window.addEventListener('scroll', () => {
            topNav.classList.toggle('scrolled', window.scrollY > 10);
        }, { passive: true });
    }
    if (navToggle && navLinks) {
        navToggle.addEventListener('click', () => {
            const open = navLinks.classList.toggle('open');
            navToggle.setAttribute('aria-expanded', String(open));
        });
        // 点击菜单外部关闭
        document.addEventListener('click', (e) => {
            if (!navLinks.classList.contains('open')) return;
            if (e.target.closest('.top-nav')) return;
            navLinks.classList.remove('open');
            navToggle.setAttribute('aria-expanded', 'false');
        });
        navLinks.addEventListener('click', (e) => {
            if (e.target.closest('a')) {
                navLinks.classList.remove('open');
                navToggle.setAttribute('aria-expanded', 'false');
                navToggle.focus();
            }
        });
    }
}

// ==================== 导航滚动高亮（ScrollSpy） ====================
function initScrollSpy() {
    if (!('IntersectionObserver' in window)) return;
    const navLinks = document.querySelectorAll('.nav-links a[href^="#"]');
    if (navLinks.length === 0) return;
    const sections = [];
    navLinks.forEach(link => {
        const id = link.getAttribute('href').slice(1);
        const section = document.getElementById(id);
        if (section) sections.push({ id, el: section, link });
    });
    if (sections.length === 0) return;
    const io = new IntersectionObserver((entries) => {
        for (const entry of entries) {
            const item = sections.find(s => s.el === entry.target);
            if (!item) continue;
            if (entry.isIntersecting) {
                sections.forEach(s => s.link.classList.remove('active'));
                item.link.classList.add('active');
            }
        }
    }, { threshold: 0.2, rootMargin: '-80px 0px -50% 0px' });
    sections.forEach(s => io.observe(s.el));
}

// ==================== 页面入场淡入 ====================
function initPageEntrance() {
    requestAnimationFrame(() => {
        document.body.classList.add('loaded');
    });
}
// 自动执行：所有页面加载后立即淡入
initPageEntrance();

window.getBrowserLang = getBrowserLang;
window.loadMessages = loadMessages;
window.applyI18n = applyI18n;
window.imgErrorSvg = imgErrorSvg;
window.copyToClipboard = copyToClipboard;
window.initTopNav = initTopNav;
window.initScrollSpy = initScrollSpy;
