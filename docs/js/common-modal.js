/* ================================
   氢风官网 · 两阶段下载弹窗（index / history 共用）
   依赖：common.js 已先加载（currentMessages / applyI18n）
   暴露 window.showPlatformSelection
   ================================ */
(function () {
    const modal = document.getElementById('downloadModal');
    const modalContent = document.querySelector('#downloadModal .modal-content');
    const modalTitle = document.getElementById('modalTitle');
    const modalBody = document.getElementById('modalBody');
    const modalClose = document.getElementById('modalClose');
    const modalBack = document.getElementById('modalBack');
    const modalBackText = document.getElementById('modalBackText');

    const PLATFORM_FALLBACK_NAMES = { windows: 'Windows', android: 'Android', linux: 'Linux', mac: 'Mac' };
    function platformName(p) {
        return currentMessages?.[`${p}_button`] || PLATFORM_FALLBACK_NAMES[p] || p;
    }

    // 当前正在浏览的系统平台 + 版本下载对象，供「返回」回到来源选择
    let currentPlatform = null;
    let currentDownload = null;

    // 某下载来源（蓝奏云/GitHub/Gitee...）的数据是否包含至少一个有效链接
    function hasDownloadOption(data) {
        if (!data) return false;
        const items = Array.isArray(data) ? data : [data];
        return items.some(item => item && item.url);
    }

    // 下载来源显示名（蓝奏云/GitHub/Gitee；_intel 后缀标 Intel 版）
    function sourceName(source) {
        const isIntel = source.endsWith('_intel');
        const base = isIntel ? source.slice(0, -'_intel'.length) : source;
        const baseName = currentMessages?.[`source_${base}`] || base;
        return isIntel ? `${baseName}（Intel）` : baseName;
    }

    // 阶段一：选择下载来源（只显示来源按钮，不显示具体文件）
    function showPlatformSelection(platform, download) {
        const options = download ? download[platform] : null;
        if (!options || typeof options !== 'object') {
            alert(currentMessages?.no_download_options || '该平台暂无可用下载链接，请联系管理员。');
            return;
        }
        const sources = Object.keys(options).filter(src => hasDownloadOption(options[src]));
        if (sources.length === 0) {
            alert(currentMessages?.no_download_options || '该平台暂无可用下载链接，请联系管理员。');
            return;
        }
        currentPlatform = platform;
        currentDownload = download;
        modalContent.classList.remove('modal-content-lg');
        modalBack.classList.remove('show');
        modalTitle.textContent = `${platformName(platform)} - ${currentMessages?.select_platform || '选择下载平台'}`;
        modalBody.innerHTML = '';
        const grid = document.createElement('div');
        grid.className = 'platform-select-grid';
        for (const src of sources) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'platform-select-btn';
            btn.textContent = sourceName(src);
            btn.addEventListener('click', () => showDownloadOptions(platform, src, options[src]));
            grid.appendChild(btn);
        }
        modalBody.appendChild(grid);
        modal.classList.add('show');
    }

    // 阶段二：展示所选下载来源的具体文件（大弹窗 + 返回/关闭）
    function showDownloadOptions(platform, source, options) {
        if (!options || Object.keys(options).length === 0) {
            alert(currentMessages?.no_download_options || '该来源暂无可用下载链接，请联系管理员。');
            return;
        }
        modalContent.classList.add('modal-content-lg');
        modalBack.classList.add('show');
        if (modalBackText) modalBackText.textContent = currentMessages?.back_button || '返回';
        modalTitle.textContent = `${sourceName(source)} - ${platformName(platform)}`;
        modalBody.innerHTML = '';
        // 单个下载项或下载项数组统一展开（同一来源可配置多个具体文件）
        const items = Array.isArray(options) ? options : [options];
        for (const item of items) {
            const description = item.description || sourceName(source);
            const url = item.url;
            if (!url) continue;
            const optionRow = document.createElement('a');
            optionRow.className = 'download-option download-option-row';
            optionRow.href = url;
            optionRow.target = '_blank';
            optionRow.rel = 'noopener';
            optionRow.innerHTML = `
                    <span class="option-description">${description}</span>
                    <span class="option-arrow">↓</span>
                `;
            modalBody.appendChild(optionRow);
        }
        if (modalBody.children.length === 0) {
            modalBody.innerHTML = `<div class="modal-error">${currentMessages?.no_valid_download_source || '没有有效的下载链接'}</div>`;
        }
        modal.classList.add('show');
    }

    modalClose.onclick = () => { modal.classList.remove('show'); };
    modalBack.onclick = () => { showPlatformSelection(currentPlatform, currentDownload); };
    window.onclick = (event) => {
        if (event.target === modal) { modal.classList.remove('show'); }
    };

    window.showPlatformSelection = showPlatformSelection;
})();
