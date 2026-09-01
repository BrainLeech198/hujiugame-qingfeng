/* ================================
   氢风官网 · 社区分享页专属脚本
   依赖：common.js（i18n）
   ================================ */
(function () {

    // ==================== 加载社区数据 ====================
    const container = document.getElementById('discussionContainer');

    function showError(message) {
        const errorMsg = window.currentMessages?.error_load_failed || '加载失败：';
        container.innerHTML = `<div class="error-message">❌ ${errorMsg}${message}</div>`;
    }

    function getPlatformDisplayName(platformKey) {
        // 尝试从语言文件中获取平台名称，否则简单处理
        const key = `discussion_platform_${platformKey}`;
        if (window.currentMessages && window.currentMessages[key]) {
            return window.currentMessages[key];
        }
        // 默认将首字母大写
        return platformKey.charAt(0).toUpperCase() + platformKey.slice(1);
    }

    function renderDiscussion(data) {
        // 兼容旧格式（如果直接是字符串）和新格式（对象）
        let suggestions = data.discussions?.suggestions;
        if (!suggestions) {
            showError(window.currentMessages?.no_discussion_link || '未找到讨论区链接');
            return;
        }
        // 如果 suggestions 是字符串（旧格式），转换为单对象
        if (typeof suggestions === 'string') {
            suggestions = { github: suggestions };
        }
        const platforms = Object.entries(suggestions);
        if (platforms.length === 0) {
            showError(window.currentMessages?.no_discussion_link || '未找到讨论区链接');
            return;
        }

        const title = window.currentMessages?.suggestions_title || "Feature Suggestions & Improvements – Let's Build Together!";
        const description = window.currentMessages?.suggestions_description || "我们非常重视每一位用户的反馈。欢迎您在这里提出功能建议、改进意见或分享您的创作。请保持文明、友善的交流。";

        let buttonsHtml = '';
        for (const [platform, url] of platforms) {
            const platformName = getPlatformDisplayName(platform);
            const buttonText = window.currentMessages?.suggestions_button_template
                ? window.currentMessages.suggestions_button_template.replace('{platform}', platformName)
                : `前往 ${platformName} 讨论区`;
            buttonsHtml += `<a href="${url}" class="discussion-button" target="_blank" rel="noopener">${buttonText} →</a>`;
        }

        container.innerHTML = `
            <div class="discussion-title">${title}</div>
            <div class="discussion-description">${description}</div>
            <div class="discussion-buttons">${buttonsHtml}</div>
        `;
    }

    function fetchCommunityData() {
        fetch('../data/community.json', { cache: 'no-cache' })
            .then(response => {
                if (!response.ok) throw new Error(`HTTP ${response.status} - ${response.statusText}`);
                return response.json();
            })
            .then(data => renderDiscussion(data))
            .catch(error => showError(error.message));
    }

    // ==================== 启动流程 ====================
    const userLang = getBrowserLang();
    initTopNav();
    loadMessages(userLang).then(messages => {
        applyI18n(messages);
        fetchCommunityData();
    }).catch(err => {
        console.error('i18n 初始化失败', err);
        fetchCommunityData(); // 使用硬编码中文继续
    });
})();
