/* ================================
   氢风官网 · 首页专属脚本
   依赖：common.js（i18n）+ common-modal.js（弹窗）
   ================================ */
(function () {

    // ==================== 修复步骤面板 ====================
    function bindRepairButton() {
        const repairBtn = document.querySelector('#tipCardRepair .tip-btn');
        const repairSteps = document.getElementById('repairSteps');
        if (repairBtn && repairSteps) {
            // 只更新按钮内文案 span，保留 🔧 emoji
            const updateRepairBtnText = (key) => {
                const span = repairBtn.querySelector('[data-i18n]');
                if (span && currentMessages && currentMessages[key]) {
                    span.textContent = currentMessages[key];
                }
            };
            repairBtn.onclick = () => {
                const isOpen = repairSteps.classList.toggle('open');
                updateRepairBtnText(isOpen ? 'tip_repair_button_hide' : 'tip_repair_button');
            };
            // 点击面板外部关闭（lightbox 内的点击不触发：图片放大再关闭时不应收起面板）
            document.addEventListener('click', (e) => {
                const tipCard = repairBtn.closest('.tip-card');
                if (repairSteps.classList.contains('open') &&
                    !repairSteps.contains(e.target) &&
                    !tipCard.contains(e.target) &&
                    !e.target.closest('.lightbox')) {
                    repairSteps.classList.remove('open');
                    updateRepairBtnText('tip_repair_button');
                }
            });
        }
    }

    // ==================== 游戏介绍折叠 ====================
    function initIntroFold() {
        const fold = document.getElementById('introFold');
        if (!fold) return;
        const toggle = document.getElementById('introFoldToggle');
        const body = fold.querySelector('.intro-body');
        if (!toggle || !body) return;

        const paras = body.querySelectorAll('p');
        if (paras.length <= 2) return;
        // 第 3、4 段有实际内容时才折叠（单语言内容过短不显示按钮）
        const overflow = paras.length > 2 &&
            (paras[2].textContent.trim() !== '' || (paras[3] && paras[3].textContent.trim() !== ''));

        const updateText = () => {
            const span = toggle.querySelector('[data-i18n]');
            if (span && currentMessages) {
                const key = fold.classList.contains('folded') ? 'expand_all' : 'collapse_all';
                span.textContent = currentMessages[key] || (fold.classList.contains('folded') ? '展开全部' : '收起全部');
            }
        };
        toggle.addEventListener('click', () => {
            fold.classList.toggle('folded');
            toggle.setAttribute('aria-expanded', String(!fold.classList.contains('folded')));
            updateText();
        });
        if (overflow) {
            fold.classList.add('folded');
            toggle.hidden = false;
        }
    }

    // ==================== Lightbox 图片放大 ====================
    function initLightbox() {
        const lightbox = document.getElementById('lightbox');
        if (!lightbox) return;
        const lightboxImg = document.getElementById('lightboxImg');
        const lightboxCaption = document.getElementById('lightboxCaption');
        const closeBtn = document.getElementById('lightboxClose');

        function open(src, alt) {
            lightboxImg.src = src;
            lightboxImg.alt = alt || '';
            lightboxCaption.textContent = alt || '';
            lightbox.classList.add('open');
            lightbox.setAttribute('aria-hidden', 'false');
            document.body.style.overflow = 'hidden';
        }
        function close() {
            lightbox.classList.remove('open');
            lightbox.setAttribute('aria-hidden', 'true');
            document.body.style.overflow = '';
            lightboxImg.src = '';
        }

        // 事件委托：仅放大非交互容器内的图片（排除下载按钮等）
        document.addEventListener('click', (e) => {
            const img = e.target.closest('img');
            if (!img) return;
            if (img.closest('a, button')) return;   // 交互元素内的图片不放大
            if (!img.offsetParent) return;          // 已隐藏（兜底）的图片不放大
            open(img.currentSrc || img.src, img.alt);
        });

        closeBtn.addEventListener('click', close);
        lightbox.addEventListener('click', (e) => {
            if (e.target === lightbox) close();
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') close();
        });
    }

    // ==================== 版本信息与下载 ====================
    const CONFIG = {
        VERSION_DATA_PATH: 'data/versions.json',
        IMAGE_CONFIG_PATH: 'data/image.json'
    };

    const latestCard = document.getElementById('latestVersionCard');

    const logoImg = document.getElementById('logoImg');

    function loadImageConfig() {
        fetch(CONFIG.IMAGE_CONFIG_PATH, { cache: 'no-cache' })
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                return response.json();
            })
            .then(config => {
                if (logoImg && config.logo) logoImg.src = config.logo;
            })
            .catch(err => console.warn('图片配置加载失败，使用默认路径', err));
    }

    function setLoadingState() {
        if (latestCard && currentMessages) {
            latestCard.innerHTML = `<div class="loading-skeleton" style="text-align:center; padding:40px;">${currentMessages.loading_version}</div>`;
        }
    }

    function showError(message) {
        if (latestCard) {
            latestCard.innerHTML = `<div class="error-message">❌ ${(currentMessages?.error_load_failed || '加载失败：')}${message}</div>`;
        }
    }

    function buildDownloadBtns(downloadData) {
        const downloadJson = JSON.stringify(downloadData || {});
        const platforms = [
            { key: 'windows', label: 'Win', alt: 'Windows下载', icon: 'download-windows' },
            { key: 'android', label: 'Android', alt: 'Android下载', icon: 'download-android' },
            { key: 'linux', label: 'Linux', alt: 'Linux下载', icon: 'download-linux' },
            { key: 'mac', label: 'Mac', alt: 'Mac下载', icon: 'download-mac' }
        ];
        const fallbackAttrs = (label, alt) =>
            `data-fallback="${label}" data-fallback-alt="${alt}" data-fallback-w="130" data-fallback-h="70"` +
            ` data-fallback-font="18" data-fallback-bg="#3b6f8c" data-fallback-color="#ffffff" data-fallback-y="38"`;
        return platforms.map(p => `
            <div class="download-item-small">
                <a href="#" class="download-btn" data-platform="${p.key}" data-download='${downloadJson}'>
                    <img src="resource/image/${p.icon}.png" alt="${p.alt}" class="download-icon-small"
                         ${fallbackAttrs(p.label, p.alt)}>
                    <span>${(currentMessages && currentMessages[p.key + '_button']) || p.label + '版'}</span>
                </a>
            </div>`).join('');
    }

    function renderLatestVersionCard(versionInfo) {
        if (!latestCard) return;
        const isLatest = `<span class="latest-tag">${currentMessages?.latest_tag || '最新'}</span>`;
        const verDate = versionInfo.date
            ? `<div class="version-date">${(currentMessages?.update_time || '更新时间')}：${versionInfo.date}</div>`
            : '';
        const safeLog = (versionInfo.log || '暂无更新日志').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br>');

        latestCard.innerHTML = `
            <div class="version-header">
                <span class="version-name">${versionInfo.name}</span>
                ${isLatest}
            </div>
            ${verDate}
            <div class="log-fold">
                <div class="version-log">${safeLog}</div>
                <button class="log-toggle" type="button" hidden>
                    <span>展开全部</span><span class="fold-arrow">▾</span>
                </button>
            </div>
            <div class="download-row">
                ${buildDownloadBtns(versionInfo.download)}
            </div>
        `;

        latestCard.querySelectorAll('.download-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const platform = btn.getAttribute('data-platform');
                let download = null;
                try { download = JSON.parse(btn.getAttribute('data-download')); } catch (err) { download = null; }
                window.showPlatformSelection(platform, download);
            });
        });
        initLatestLogFold();
    }

    // ==================== 主页最新版本日志折叠（固定像素超阈值才折叠） ====================
    const LOG_FOLD_MAX_HEIGHT = 110;

    function initLatestLogFold() {
        const foldEl = latestCard.querySelector('.log-fold');
        if (!foldEl) return;
        const log = foldEl.querySelector('.version-log');
        const toggle = foldEl.querySelector('.log-toggle');
        if (!log || !toggle) return;
        if (log.scrollHeight <= LOG_FOLD_MAX_HEIGHT) return;

        const updateText = (open) => {
            const span = toggle.querySelector('span');
            const key = open ? 'expand_log' : 'collapse_log';
            span.textContent = (currentMessages && currentMessages[key]) || (open ? '展开全部' : '收起');
        };
        toggle.addEventListener('click', () => {
            const open = foldEl.classList.toggle('folded');
            toggle.setAttribute('aria-expanded', String(open));
            updateText(open);
        });
        foldEl.classList.add('folded');
        toggle.setAttribute('aria-expanded', 'false');
        updateText(true);
        toggle.hidden = false;
    }

    function fetchVersionAndUpdate() {
        setLoadingState();

        fetch(CONFIG.VERSION_DATA_PATH, { cache: 'no-cache' })
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status} - ${response.statusText}`);
                return response.json();
            })
            .then(data => {
                const newestKey = data.newest_version;
                if (newestKey === undefined || !data.versions) throw new Error('JSON 缺少 newest_version 或 versions 字段');
                const versionInfo = data.versions[String(newestKey)];
                if (!versionInfo || !versionInfo.name) throw new Error(`未找到键 ${newestKey} 对应的版本信息`);

                renderLatestVersionCard(versionInfo);
            })
            .catch(error => showError(error.message));
    }

    // ==================== 吸顶导航（滚动毛玻璃 + 汉堡菜单） ====================
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
            navLinks.addEventListener('click', (e) => {
                if (e.target.tagName === 'A') {
                    navLinks.classList.remove('open');
                    navToggle.setAttribute('aria-expanded', 'false');
                }
            });
        }
    }

    // ==================== 滚动进入动画 ====================
    function initScrollReveal() {
        const els = document.querySelectorAll('.hero-card, .tip-card, .card, .download-item');
        if (!els.length) return;
        if (!('IntersectionObserver' in window)) return;
        const io = new IntersectionObserver((entries) => {
            for (const entry of entries) {
                if (!entry.isIntersecting) continue;
                const el = entry.target;
                el.classList.add('in');
                io.unobserve(el);
                // 动画结束后摘除 reveal，恢复元素自身的 hover 过渡（避免 delay 影响悬浮）
                setTimeout(() => {
                    el.classList.remove('reveal');
                    el.classList.remove('in');
                    el.style.transitionDelay = '';
                }, 750);
            }
        }, { threshold: 0.08, rootMargin: '0px 0px -6% 0px' });
        els.forEach((el) => {
            const parent = el.parentElement;
            const siblings = parent ? Array.from(parent.children).filter(c => c.matches('.hero-card, .tip-card, .card, .download-item')) : [el];
            const idx = siblings.indexOf(el);
            el.style.transitionDelay = `${Math.min(idx * 0.05, 0.2)}s`;
            el.classList.add('reveal');
            io.observe(el);
        });
    }

    // ==================== 启动流程 ====================
    const userLang = getBrowserLang();
    initLightbox();
    initTopNav();
    initScrollReveal();
    loadMessages(userLang).then(messages => {
        applyI18n(messages);
        bindRepairButton();
        initIntroFold();
        loadImageConfig();
        fetchVersionAndUpdate();
    }).catch(err => {
        console.error('i18n 初始化失败', err);
        loadImageConfig();
        fetchVersionAndUpdate();
    });
})();
