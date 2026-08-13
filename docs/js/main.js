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

    const versionNameEl = document.getElementById('version-name');
    const versionLogEl = document.getElementById('version-log');
    const windowsLink = document.getElementById('windows-link');
    const androidLink = document.getElementById('android-link');
    const linuxLink = document.getElementById('linux-link');
    const macLink = document.getElementById('mac-link');

    const logoImg = document.getElementById('logoImg');
    const windowsIcon = document.getElementById('windowsIcon');
    const androidIcon = document.getElementById('androidIcon');
    const linuxIcon = document.getElementById('linuxIcon');
    const macIcon = document.getElementById('macIcon');

    let currentVersionInfo = null;

    function loadImageConfig() {
        fetch(CONFIG.IMAGE_CONFIG_PATH, { cache: 'no-cache' })
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                return response.json();
            })
            .then(config => {
                if (logoImg && config.logo) logoImg.src = config.logo;
                if (windowsIcon && config['download-windows']) windowsIcon.src = config['download-windows'];
                if (androidIcon && config['download-android']) androidIcon.src = config['download-android'];
                if (linuxIcon && config['download-linux']) linuxIcon.src = config['download-linux'];
                if (macIcon && config['download-mac']) macIcon.src = config['download-mac'];
            })
            .catch(err => console.warn('图片配置加载失败，使用默认路径', err));
    }

    function setLoadingState() {
        if (versionNameEl && currentMessages) versionNameEl.textContent = currentMessages.loading_version;
        if (versionLogEl && currentMessages) versionLogEl.innerHTML = `<span class="loading-skeleton">${currentMessages.loading_log}</span>`;
    }

    function showError(message) {
        if (versionNameEl) versionNameEl.textContent = '❌ ' + (currentMessages?.error_load_failed || '加载失败：') + message;
        if (versionLogEl) versionLogEl.innerHTML = `<span style="color:#b52b2b;">${message}</span>`;
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

                if (versionNameEl) {
                    versionNameEl.textContent = versionInfo.name;
                    versionNameEl.classList.remove('loading-skeleton');
                }
                if (versionLogEl) {
                    versionLogEl.textContent = versionInfo.log;
                    versionLogEl.classList.remove('loading-skeleton');
                }

                currentVersionInfo = versionInfo;

                if (windowsLink) {
                    windowsLink.onclick = (e) => {
                        e.preventDefault();
                        window.showPlatformSelection('windows', currentVersionInfo.download);
                    };
                }
                if (androidLink) {
                    androidLink.onclick = (e) => {
                        e.preventDefault();
                        window.showPlatformSelection('android', currentVersionInfo.download);
                    };
                }
                if (linuxLink) {
                    linuxLink.onclick = (e) => {
                        e.preventDefault();
                        window.showPlatformSelection('linux', currentVersionInfo.download);
                    };
                }
                if (macLink) {
                    macLink.onclick = (e) => {
                        e.preventDefault();
                        window.showPlatformSelection('mac', currentVersionInfo.download);
                    };
                }
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
        loadImageConfig();
        fetchVersionAndUpdate();
    }).catch(err => {
        console.error('i18n 初始化失败', err);
        loadImageConfig();
        fetchVersionAndUpdate();
    });
})();
