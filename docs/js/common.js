/* ================================
   氢风官网 · 共用脚本（index / history / community 共用）
   职责：i18n 初始化 + 图片加载失败兜底
   语言数据路径由 <script data-base="data/"> 传入（html/ 下子页为 ../data/）
   ================================ */

// i18n 核心（顶层声明，跨 script 通过全局词法环境共享）
let currentMessages = null;

const localeMap = {
    'zh': 'zh',
    'zh-CN': 'zh',
    'zh-TW': 'zh-TW',
    'zh-HK': 'zh-TW',
    'en': 'en',
    'ja': 'ja',
    'ko': 'ko',
    'ru': 'ru',
    'pt': 'pt',
    'fr': 'fr',
    'de': 'de'
};
const defaultLang = 'zh';

function getBrowserLang() {
    const lang = navigator.language || navigator.userLanguage;
    const baseLang = lang.split('-')[0];
    return localeMap[baseLang] ? baseLang : defaultLang;
}

// 语言文件路径前缀：由 <script data-base> 提供（缺省当前目录）
const DATA_BASE = (document.currentScript && document.currentScript.dataset.base) || '';

function loadMessages(lang) {
    return fetch(`${DATA_BASE}locales/${lang}.json`, { cache: 'no-cache' })
        .then(res => {
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return res.json();
        })
        .catch(err => {
            console.warn(`加载语言文件 ${lang} 失败，回退到 ${defaultLang}`, err);
            if (lang !== defaultLang) return loadMessages(defaultLang);
            throw err;
        });
}

function applyI18n(messages) {
    currentMessages = messages;
    window.currentMessages = messages;
    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (messages[key]) {
            el.textContent = messages[key];
        }
    });
    // 提示卡片中的混合内容（保留 strong 标签）—— 仅首页有此元素，其他页面自动跳过
    const tipWattTextSpan = document.querySelector('#tipCardWatt .tip-text span:last-child');
    if (tipWattTextSpan && messages.tip_watt_text) {
        const text = messages.tip_watt_text;
        const strongMatch = /<strong>(.*?)<\/strong>/.exec(text);
        if (strongMatch) {
            tipWattTextSpan.innerHTML = text;
        } else {
            tipWattTextSpan.textContent = text;
        }
    }
    const tipRepairTextSpan = document.querySelector('#tipCardRepair .tip-text span:last-child');
    if (tipRepairTextSpan && messages.tip_repair_text) {
        tipRepairTextSpan.textContent = messages.tip_repair_text;
    }
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
        const original = (currentMessages && currentMessages.copy_button) || '复制';
        const copied = (currentMessages && currentMessages.copied_feedback) || '已复制';
        label.textContent = copied;
        setTimeout(() => { label.textContent = original; }, 2000);
    });
});

window.getBrowserLang = getBrowserLang;
window.loadMessages = loadMessages;
window.applyI18n = applyI18n;
window.imgErrorSvg = imgErrorSvg;
window.copyToClipboard = copyToClipboard;
