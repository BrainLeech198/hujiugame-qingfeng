/* ================================
   氢风官网 · 历史版本页专属脚本
   依赖：common.js（i18n / 图片兜底）+ common-modal.js（弹窗）
   ================================ */
(function () {

    // ==================== 版本列表加载 ====================
    const CONFIG = {
        VERSION_DATA_PATH: '../data/versions.json',
        PAGE_SIZE: 5
    };

    const container = document.getElementById('versionsContainer');
    const paginationDiv = document.getElementById('paginationControls');
    const prevBtn = document.getElementById('prevPage');
    const nextBtn = document.getElementById('nextPage');
    const pageInfo = document.getElementById('pageInfo');

    let allVersions = [];
    let currentPage = 1;
    let totalPages = 0;

    // 滚动进入动画：版本卡片动态生成，渲染后调用 observeNewCards
    let revealIO = null;
    if ('IntersectionObserver' in window) {
        revealIO = new IntersectionObserver((entries) => {
            for (const entry of entries) {
                if (!entry.isIntersecting) continue;
                const el = entry.target;
                el.classList.add('in');
                revealIO.unobserve(el);
                setTimeout(() => {
                    el.classList.remove('reveal');
                    el.classList.remove('in');
                    el.style.transitionDelay = '';
                }, 750);
            }
        }, { threshold: 0.08, rootMargin: '0px 0px -6% 0px' });
    }
    function observeNewCards() {
        if (!revealIO) return;
        const cards = container.querySelectorAll('.version-card');
        cards.forEach((el, idx) => {
            el.style.transitionDelay = `${Math.min(idx * 0.05, 0.2)}s`;
            el.classList.add('reveal');
            revealIO.observe(el);
        });
    }

    function showError(message) {
        const errorMsg = currentMessages?.error_load_failed || '加载失败：';
        container.innerHTML = `<div class="error-message">❌ ${errorMsg}${message}</div>`;
        paginationDiv.style.display = 'none';
    }

    function showEmptyState() {
        const msg = currentMessages?.no_versions || '暂无版本信息';
        container.innerHTML = `<div class="no-versions">${msg}</div>`;
        paginationDiv.style.display = 'none';
    }

    function fetchVersions() {
        fetch(CONFIG.VERSION_DATA_PATH, { cache: 'no-cache' })
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status} - ${response.statusText}`);
                return response.json();
            })
            .then(data => {
                if (!data || !data.versions || typeof data.versions !== 'object') throw new Error('版本数据格式错误');
                processVersionData(data);
            })
            .catch(error => showError(error.message));
    }

    function processVersionData(data) {
        const versionsObj = data.versions;
        const rawArray = Object.keys(versionsObj).map(key => ({
            key: key,
            name: versionsObj[key].name || '未知版本',
            log: versionsObj[key].log || '暂无更新日志',
            download: versionsObj[key].download || {}
        }));

        if (rawArray.length === 0) {
            showEmptyState();
            return;
        }

        rawArray.sort((a, b) => {
            const numA = Number(a.key);
            const numB = Number(b.key);
            if (!isNaN(numA) && !isNaN(numB)) return numB - numA;
            return b.name.localeCompare(a.name);
        });

        allVersions = rawArray;
        allVersions[0].isLatest = true;

        totalPages = Math.ceil(allVersions.length / CONFIG.PAGE_SIZE) || 1;
        currentPage = 1;
        renderPage(1);
    }

    function renderPage(page) {
        const start = (page - 1) * CONFIG.PAGE_SIZE;
        const end = Math.min(start + CONFIG.PAGE_SIZE, allVersions.length);
        const pageData = allVersions.slice(start, end);

        let htmlStr = '';
        pageData.forEach(ver => {
            const isLatest = ver.isLatest ? `<span class="latest-tag">${currentMessages?.latest_tag || '最新'}</span>` : '';
            const safeLog = ver.log.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br>');

            // 整份下载对象（四平台）存入 data 属性，点击任意平台按钮都先进入平台选择
            const downloadData = ver.download || {};
            const downloadJson = JSON.stringify(downloadData);

            // 下载按钮图标：加载失败由 common.js 捕获监听兜底（data-fallback* 属性）
            const fallbackAttrs = (label, alt) =>
                `data-fallback="${label}" data-fallback-alt="${alt}" data-fallback-w="130" data-fallback-h="70"` +
                ` data-fallback-font="18" data-fallback-bg="#3b6f8c" data-fallback-color="#ffffff" data-fallback-y="38"`;

            const windowsBtnHtml = `
                <a href="#" class="download-btn" data-platform="windows" data-download='${downloadJson}'>
                    <img src="../resource/image/download-windows.png" alt="Windows下载" class="download-icon-small"
                         ${fallbackAttrs('Win', 'Windows下载')}>
                    <span>${currentMessages?.windows_button || 'Windows版'}</span>
                </a>`;
            const androidBtnHtml = `
                <a href="#" class="download-btn" data-platform="android" data-download='${downloadJson}'>
                    <img src="../resource/image/download-android.png" alt="Android下载" class="download-icon-small"
                         ${fallbackAttrs('Android', 'Android下载')}>
                    <span>${currentMessages?.android_button || 'Android版'}</span>
                </a>`;
            const linuxBtnHtml = `
                <a href="#" class="download-btn" data-platform="linux" data-download='${downloadJson}'>
                    <img src="../resource/image/download-linux.png" alt="Linux下载" class="download-icon-small"
                         ${fallbackAttrs('Linux', 'Linux下载')}>
                    <span>${currentMessages?.linux_button || 'Linux版'}</span>
                </a>`;

            htmlStr += `
                <div class="version-card">
                    <div class="version-header">
                        <span class="version-name">${ver.name}</span>
                        ${isLatest}
                    </div>
                    <div class="version-log">${safeLog}</div>
                    <div class="download-row">
                        <div class="download-item-small">${windowsBtnHtml}</div>
                        <div class="download-item-small">${androidBtnHtml}</div>
                        <div class="download-item-small">${linuxBtnHtml}</div>
                    </div>
                </div>
            `;
        });

        container.innerHTML = htmlStr;
        observeNewCards();
        // 绑定所有下载按钮的点击事件
        document.querySelectorAll('.download-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                const platform = btn.getAttribute('data-platform');
                const downloadStr = btn.getAttribute('data-download');
                let download = null;
                try {
                    download = JSON.parse(downloadStr);
                } catch(e) { download = null; }
                window.showPlatformSelection(platform, download);
            });
        });
        updatePagination(page);
    }

    function updatePagination(page) {
        currentPage = page;
        if (totalPages <= 1) {
            paginationDiv.style.display = 'none';
            return;
        }
        paginationDiv.style.display = 'flex';
        const pageInfoText = (currentMessages?.page_info || '第 {current} / {total} 页')
            .replace('{current}', currentPage)
            .replace('{total}', totalPages);
        pageInfo.textContent = pageInfoText;

        prevBtn.disabled = (currentPage <= 1);
        nextBtn.disabled = (currentPage >= totalPages);

        const newPrev = prevBtn.cloneNode(true);
        const newNext = nextBtn.cloneNode(true);
        prevBtn.replaceWith(newPrev);
        nextBtn.replaceWith(newNext);

        document.getElementById('prevPage').addEventListener('click', (e) => {
            e.preventDefault();
            if (currentPage > 1) {
                renderPage(currentPage - 1);
                window.scrollTo({ top: 0, behavior: 'smooth' });
            }
        });
        document.getElementById('nextPage').addEventListener('click', (e) => {
            e.preventDefault();
            if (currentPage < totalPages) {
                renderPage(currentPage + 1);
                window.scrollTo({ top: 0, behavior: 'smooth' });
            }
        });
    }

    // ==================== 启动流程 ====================
    const userLang = getBrowserLang();
    loadMessages(userLang).then(messages => {
        applyI18n(messages);
        fetchVersions();
    }).catch(err => {
        console.error('i18n 初始化失败', err);
        fetchVersions(); // 使用默认文本
    });
})();
