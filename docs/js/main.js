/* ================================
   氢风官网 · 首页专属脚本
   依赖：common.js（i18n）+ common-modal.js（弹窗）
   ================================ */
(function () {

    // ==================== Accordion 折叠面板（单开） ====================
    function initAccordion() {
        const items = document.querySelectorAll('.accordion-item');
        items.forEach(item => {
            const trigger = item.querySelector('.accordion-trigger');
            const panel = item.querySelector('.accordion-panel');
            if (!trigger || !panel) return;
            // 关闭时重置 max-height
            panel.addEventListener('transitionend', (e) => {
                if (e.propertyName === 'max-height' && !item.classList.contains('open')) {
                    panel.style.maxHeight = null;
                }
            });
            // Escape 键关闭面板并焦点回到 trigger
            panel.addEventListener('keydown', (e) => {
                if (e.key === 'Escape' && item.classList.contains('open')) {
                    item.classList.remove('open');
                    trigger.setAttribute('aria-expanded', 'false');
                    panel.style.maxHeight = panel.scrollHeight + 'px';
                    panel.offsetHeight;
                    panel.style.maxHeight = null;
                    trigger.focus();
                }
            });
            trigger.addEventListener('click', () => {
                const wasOpen = item.classList.contains('open');
                // 关闭所有（带动画）
                items.forEach(other => {
                    if (other === item) return;
                    if (!other.classList.contains('open')) return;
                    const t = other.querySelector('.accordion-trigger');
                    const p = other.querySelector('.accordion-panel');
                    if (t) t.setAttribute('aria-expanded', 'false');
                    if (p) {
                        p.style.maxHeight = p.scrollHeight + 'px';
                        p.offsetHeight; // force reflow
                        p.style.maxHeight = null;
                    }
                    other.classList.remove('open');
                });
                // 切换当前
                if (wasOpen) {
                    // 先锁定当前高度，强制 reflow 后归零，确保 transition 生效
                    panel.style.maxHeight = panel.scrollHeight + 'px';
                    panel.offsetHeight; // force reflow
                    item.classList.remove('open');
                    trigger.setAttribute('aria-expanded', 'false');
                    panel.style.maxHeight = null;
                } else {
                    item.classList.add('open');
                    trigger.setAttribute('aria-expanded', 'true');
                    panel.style.maxHeight = panel.scrollHeight + 'px';
                }
            });
        });
    }

    // ==================== 功能介绍折叠 ====================
    function initIntroFold() {
        const fold = document.getElementById('introFold');
        const toggle = document.getElementById('introFoldToggle');
        if (!fold || !toggle) return;

        const updateText = () => {
            const span = toggle.querySelector('[data-i18n]');
            if (span && window.currentMessages) {
                const expanded = !fold.classList.contains('folded');
                const key = expanded ? 'collapse_all' : 'expand_all';
                span.textContent = window.currentMessages[key] || (expanded ? '收起' : '了解更多');
            }
        };
        toggle.addEventListener('click', () => {
            fold.classList.toggle('folded');
            const expanded = !fold.classList.contains('folded');
            toggle.setAttribute('aria-expanded', String(expanded));
            updateText();
        });
        toggle.hidden = false;
        updateText();
    }

    // ==================== Lightbox 图片放大 ====================
    function initLightbox() {
        const lightbox = document.getElementById('lightbox');
        if (!lightbox) return;
        const lightboxImg = document.getElementById('lightboxImg');
        const lightboxCaption = document.getElementById('lightboxCaption');
        const closeBtn = document.getElementById('lightboxClose');

        let scale = 1;
        let panX = 0;
        let panY = 0;
        const PAN_STEP = 60;
        const ZOOM_STEP = 0.15;
        const MIN_SCALE = 0.5;
        const MAX_SCALE = 5;

        function applyTransform() {
            lightboxImg.style.transform = `translate(${panX}px, ${panY}px) scale(${scale})`;
        }

        function resetView() {
            scale = 1;
            panX = 0;
            panY = 0;
            applyTransform();
        }

        function open(src, alt) {
            lightboxImg.src = src;
            lightboxImg.alt = alt || '';
            lightboxCaption.textContent = alt || '';
            lightbox.classList.add('open');
            lightbox.setAttribute('aria-hidden', 'false');
            document.body.style.overflow = 'hidden';
            resetView();
        }

        function close() {
            lightbox.classList.remove('open');
            lightbox.setAttribute('aria-hidden', 'true');
            document.body.style.overflow = '';
            lightboxImg.src = '';
            resetView();
        }

        // 点击图片打开
        document.addEventListener('click', (e) => {
            const img = e.target.closest('img');
            if (!img) return;
            if (img.closest('a, button')) return;
            if (!img.offsetParent) return;
            open(img.currentSrc || img.src, img.alt);
        });

        closeBtn.addEventListener('click', close);
        lightbox.addEventListener('click', (e) => {
            if (e.target === lightbox) close();
        });

        // 滚轮缩放（以鼠标位置为中心）
        lightbox.addEventListener('wheel', (e) => {
            if (!lightbox.classList.contains('open')) return;
            e.preventDefault();
            const rect = lightboxImg.getBoundingClientRect();
            const mouseX = e.clientX - rect.left - rect.width / 2;
            const mouseY = e.clientY - rect.top - rect.height / 2;
            const oldScale = scale;
            const delta = e.deltaY < 0 ? ZOOM_STEP : -ZOOM_STEP;
            scale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale + delta * scale));
            const ratio = scale / oldScale;
            panX = mouseX - ratio * (mouseX - panX);
            panY = mouseY - ratio * (mouseY - panY);
            applyTransform();
        }, { passive: false });

        // 键盘控制
        document.addEventListener('keydown', (e) => {
            if (!lightbox.classList.contains('open')) return;
            switch (e.key) {
                case 'Escape': close(); break;
                case 'ArrowLeft':  panX += PAN_STEP; applyTransform(); e.preventDefault(); break;
                case 'ArrowRight': panX -= PAN_STEP; applyTransform(); e.preventDefault(); break;
                case 'ArrowUp':    panY += PAN_STEP; applyTransform(); e.preventDefault(); break;
                case 'ArrowDown':  panY -= PAN_STEP; applyTransform(); e.preventDefault(); break;
                case '+': case '=':
                    scale = Math.min(MAX_SCALE, scale + ZOOM_STEP * scale);
                    applyTransform(); e.preventDefault(); break;
                case '-': case '_':
                    scale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale - ZOOM_STEP * scale));
                    applyTransform(); e.preventDefault(); break;
                case '0': resetView(); e.preventDefault(); break;
            }
        });

        // 拖拽平移（任意缩放级别均可拖动）
        let dragging = false;
        let dragStartX, dragStartY;
        lightboxImg.addEventListener('mousedown', (e) => {
            dragging = true;
            dragStartX = e.clientX - panX;
            dragStartY = e.clientY - panY;
            lightboxImg.style.cursor = 'grabbing';
            e.preventDefault();
        });
        document.addEventListener('mousemove', (e) => {
            if (!dragging) return;
            panX = e.clientX - dragStartX;
            panY = e.clientY - dragStartY;
            applyTransform();
        });
        document.addEventListener('mouseup', () => {
            if (!dragging) return;
            dragging = false;
            lightboxImg.style.cursor = 'grab';
        });

        // 双击重置
        lightboxImg.addEventListener('dblclick', (e) => {
            e.preventDefault();
            resetView();
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
        if (latestCard && window.currentMessages) {
            latestCard.innerHTML = `<div class="loading-skeleton" style="text-align:center; padding:40px;">${window.currentMessages.loading_version}</div>`;
        }
    }

    function showError(message) {
        if (latestCard) {
            latestCard.innerHTML = `<div class="error-message">❌ ${(window.currentMessages?.error_load_failed || '加载失败：')}${message}</div>`;
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
                    <span>${(window.currentMessages && window.currentMessages[p.key + '_button']) || p.label + '版'}</span>
                </a>
            </div>`).join('');
    }

    function renderLatestVersionCard(versionInfo) {
        if (!latestCard) return;
        const isLatest = `<span class="latest-tag">${window.currentMessages?.latest_tag || '最新'}</span>`;
        const verDate = versionInfo.date
            ? `<div class="version-date">${(window.currentMessages?.update_time || '更新时间')}：${versionInfo.date}</div>`
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

        const updateText = (folded) => {
            const span = toggle.querySelector('span');
            const key = folded ? 'expand_log' : 'collapse_log';
            span.textContent = (window.currentMessages && window.currentMessages[key]) || (folded ? '展开全部' : '收起');
        };
        // 折叠状态下阻止滚轮滚动，只允许拖动滚动条
        log.addEventListener('wheel', (e) => {
            if (!foldEl.classList.contains('folded')) return;
            if (log.scrollHeight <= log.clientHeight) return;
            e.preventDefault();
            log.scrollTop += e.deltaY;
            if ((e.deltaY < 0 && log.scrollTop <= 0) || (e.deltaY > 0 && log.scrollTop + log.clientHeight >= log.scrollHeight)) {
                window.scrollBy(0, e.deltaY);
            }
        }, { passive: false });
        toggle.addEventListener('click', () => {
            const folded = foldEl.classList.toggle('folded');
            toggle.setAttribute('aria-expanded', String(!folded));
            updateText(folded);
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

    // ==================== 数字滚动动画 ====================
    function initStatsCounter() {
        const nums = document.querySelectorAll('.stat-number[data-target]');
        if (nums.length === 0 || !('IntersectionObserver' in window)) return;
        const io = new IntersectionObserver((entries) => {
            for (const entry of entries) {
                if (!entry.isIntersecting) continue;
                const el = entry.target;
                io.unobserve(el);
                const target = Number(el.dataset.target);
                const duration = 1200;
                const start = performance.now();
                function tick(now) {
                    const progress = Math.min((now - start) / duration, 1);
                    const eased = 1 - Math.pow(1 - progress, 3);
                    el.textContent = Math.round(eased * target);
                    if (progress < 1) requestAnimationFrame(tick);
                }
                requestAnimationFrame(tick);
            }
        }, { threshold: 0.3 });
        nums.forEach(el => io.observe(el));
    }

    // ==================== 滚动入场动画 ====================
    function initRevealAnimations() {
        if (!('IntersectionObserver' in window)) return;
        const io = new IntersectionObserver((entries) => {
            for (const entry of entries) {
                if (!entry.isIntersecting) continue;
                const el = entry.target;
                el.classList.add('in');
                io.unobserve(el);
                setTimeout(() => {
                    el.classList.remove('reveal', 'in');
                    el.classList.add('done');
                    el.style.transitionDelay = '';
                }, 700);
            }
        }, { threshold: 0.08, rootMargin: '0px 0px -6% 0px' });
        // feature 卡片交错延迟
        document.querySelectorAll('.feature-card.reveal').forEach((el, idx) => {
            el.style.transitionDelay = `${idx * 0.08}s`;
            io.observe(el);
        });
        // 其他 section 整块入场
        document.querySelectorAll('.stats-bar.reveal, .download-section.reveal, .help-section.reveal, .community-section.reveal').forEach(el => {
            io.observe(el);
        });
    }

    // ==================== Hero 打字机效果 ====================
    function initTypewriter() {
        const el = document.getElementById('typewriterText');
        const cursor = document.getElementById('typewriterCursor');
        if (!el || !cursor) {
            console.warn('[typewriter] 元素未找到');
            return;
        }

        const msgs = window.currentMessages || {};
        const phrases = [
            msgs.game_intro_desc1_short || '基于 libGDX 的视觉小说引擎与跨平台游戏启动器',
            msgs.feature_cross_platform_desc || 'Windows / Linux / macOS / Android，桌面与移动端全覆盖',
            msgs.feature_multilang_desc || '界面原生多语言支持，可切换主题系统',
            msgs.feature_resource_desc || '语言包 / 主题包一键导入，版本检测与资源修复'
        ];

        const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        if (reducedMotion) {
            console.log('[typewriter] prefers-reduced-motion 启用，跳过打字动画');
            el.textContent = phrases[0];
            let idx = 0;
            setInterval(() => {
                idx = (idx + 1) % phrases.length;
                el.textContent = phrases[idx];
            }, 4000);
            return;
        }

        let phraseIdx = 0;
        let charIdx = 0;
        const TYPE_SPEED = 65;
        const DELETE_SPEED = 30;
        const PAUSE_AFTER_TYPE = 2200;
        const PAUSE_AFTER_DELETE = 400;

        function typeNext() {
            const current = phrases[phraseIdx];
            if (charIdx < current.length) {
                charIdx++;
                el.textContent = current.substring(0, charIdx);
                setTimeout(typeNext, TYPE_SPEED);
            } else {
                setTimeout(deletePhrase, PAUSE_AFTER_TYPE);
            }
        }

        function deletePhrase() {
            if (charIdx > 0) {
                charIdx--;
                el.textContent = phrases[phraseIdx].substring(0, charIdx);
                setTimeout(deletePhrase, DELETE_SPEED);
            } else {
                phraseIdx = (phraseIdx + 1) % phrases.length;
                setTimeout(typeNext, PAUSE_AFTER_DELETE);
            }
        }

        // 先隐藏光标，清空文字
        el.textContent = '';
        cursor.style.display = 'inline';

        // 等页面入场淡入结束后再启动打字（opacity transition 0.4s）
        function startTyping() {
            setTimeout(typeNext, 300);
        }
        if (document.body.classList.contains('loaded')) {
            startTyping();
        } else {
            document.body.addEventListener('transitionend', function onEnd() {
                document.body.removeEventListener('transitionend', onEnd);
                startTyping();
            });
            // 兜底：如果 transitionend 未触发，1.5s 后启动
            setTimeout(startTyping, 1500);
        }
    }

    // ==================== Hero 光标聚光灯 ====================
    function initHeroSpotlight() {
        const hero = document.getElementById('hero');
        const spot = document.getElementById('heroSpotlight');
        if (!hero || !spot) return;
        hero.addEventListener('mousemove', (e) => {
            const rect = hero.getBoundingClientRect();
            spot.style.setProperty('--spot-x', (e.clientX - rect.left) + 'px');
            spot.style.setProperty('--spot-y', (e.clientY - rect.top) + 'px');
        });
    }

    // ==================== 按钮涟漪效果 ====================
    function initRippleButtons() {
        document.addEventListener('click', (e) => {
            const btn = e.target.closest('.hero-btn-primary, .accordion-btn:not(.accordion-btn-copy), .community-btn');
            if (!btn) return;
            btn.classList.add('ripple-btn');
            const rect = btn.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height) * 2;
            const wave = document.createElement('span');
            wave.className = 'ripple-wave';
            wave.style.width = wave.style.height = size + 'px';
            wave.style.left = (e.clientX - rect.left - size / 2) + 'px';
            wave.style.top = (e.clientY - rect.top - size / 2) + 'px';
            btn.appendChild(wave);
            wave.addEventListener('animationend', () => wave.remove());
        });
    }

    // ==================== Feature 卡片光晕跟随 ====================
    function initCardGlow() {
        const cards = document.querySelectorAll('.feature-card');
        cards.forEach(card => {
            card.addEventListener('mousemove', (e) => {
                const rect = card.getBoundingClientRect();
                card.style.setProperty('--glow-x', (e.clientX - rect.left) + 'px');
                card.style.setProperty('--glow-y', (e.clientY - rect.top) + 'px');
            });
        });
    }

    // ==================== 彩蛋：Brand 连击 ====================
    function initEasterEgg() {
        const brand = document.querySelector('.nav-brand');
        if (!brand) return;
        let clicks = 0;
        let timer = null;
        let raining = false;
        brand.addEventListener('click', (e) => {
            e.preventDefault();
            if (raining) return;
            clicks++;
            clearTimeout(timer);
            timer = setTimeout(() => { clicks = 0; }, 2000);
            if (clicks >= 5) {
                clicks = 0;
                raining = true;
                triggerEmojiRain(() => { raining = false; });
            }
        });
    }

    function triggerEmojiRain(onDone) {
        const emojis = ['◈', '✦', '⚡', '🌍', '📦', '🎮', '💻', '🎉', '✨', '🚀'];
        const count = 25;
        let done = 0;
        for (let i = 0; i < count; i++) {
            setTimeout(() => {
                const el = document.createElement('div');
                el.textContent = emojis[Math.floor(Math.random() * emojis.length)];
                const startX = Math.random() * 100;
                const drift = (Math.random() - 0.5) * 25;
                const duration = (2 + Math.random() * 2) * 1000;
                Object.assign(el.style, {
                    position: 'fixed',
                    left: startX + 'vw',
                    top: '-30px',
                    fontSize: (1.2 + Math.random() * 1.3) + 'rem',
                    pointerEvents: 'none',
                    zIndex: '99999',
                    opacity: '0.85'
                });
                document.body.appendChild(el);

                const anim = el.animate([
                    { top: '-30px', left: startX + 'vw', opacity: 0.85, transform: 'rotate(0deg)' },
                    { top: '105vh', left: (startX + drift) + 'vw', opacity: 0, transform: 'rotate(360deg)' }
                ], { duration: duration, easing: 'ease-in', fill: 'forwards' });

                anim.onfinish = () => {
                    el.remove();
                    done++;
                    if (done >= count && onDone) onDone();
                };
            }, i * 100);
        }
    }

    // ==================== 启动流程 ====================
    const userLang = getBrowserLang();
    initLightbox();
    initTopNav();
    initHeroSpotlight();
    initCardGlow();
    initRippleButtons();
    initEasterEgg();
    initRevealAnimations();
    initScrollSpy();
    loadMessages(userLang).then(messages => {
        applyI18n(messages);
        initAccordion();
        initIntroFold();
        initStatsCounter();
        initTypewriter();
        loadImageConfig();
        fetchVersionAndUpdate();
    }).catch(err => {
        console.error('i18n 初始化失败', err);
        loadImageConfig();
        fetchVersionAndUpdate();
    });
})();
