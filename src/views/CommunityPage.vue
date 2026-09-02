<template>
    <div class="main-wrapper" id="main-content">
        <h1 class="page-title">
            <span class="sec-emoji">💬</span>
            <span>{{ $t('community_page_title') }}</span>
        </h1>

        <div class="discussion-card" id="discussionContainer">
            <div v-if="loading" class="loading-skeleton" style="text-align:center; padding:40px;">
                {{ $t('loading_community') }}
            </div>
            <div v-else-if="error" class="error-message">❌ {{ $t('error_load_failed') || '加载失败：' }}{{ error }}</div>
            <div v-else>
                <div class="discussion-title">{{ $t('suggestions_title') || "Feature Suggestions & Improvements – Let's Build Together!" }}</div>
                <div class="discussion-description">{{ $t('suggestions_description') || '我们非常重视每一位用户的反馈。欢迎您在这里提出功能建议、改进意见或分享您的创作。请保持文明、友善的交流。' }}</div>
                <div class="discussion-buttons">
                    <a v-for="(url, platform) in suggestions" :key="platform" :href="url" class="btn btn-primary btn-lg" target="_blank" rel="noopener">
                        {{ getButtonText(platform) }} →
                    </a>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePageSub } from '../composables/usePageSub.js'

usePageSub()

const { t } = useI18n()
const loading = ref(true)
const error = ref(null)
const suggestions = ref({})

function getButtonText(platform) {
    const platformName = t(`discussion_platform_${platform}`) || platform.charAt(0).toUpperCase() + platform.slice(1)
    const template = t('suggestions_button_template')
    if (template && template.includes('{platform}')) {
        return template.replace('{platform}', platformName)
    }
    return `前往 ${platformName} 讨论区`
}

onMounted(() => {
    loadData()
})

async function loadData() {
    try {
        const res = await fetch('/data/community.json', { cache: 'no-cache' })
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        const data = await res.json()
        const raw = data.discussions?.suggestions
        if (!raw) throw new Error('未找到讨论区链接')
        suggestions.value = typeof raw === 'string' ? { github: raw } : raw
    } catch (err) {
        error.value = err.message
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
.discussion-card {
    background: var(--card-bg-strong);
    border-radius: var(--radius-card);
    padding: 32px 36px;
    margin: 20px 0;
    box-shadow: var(--shadow-soft);
    border: 1px solid var(--border-glass);
    transition: transform 0.25s var(--ease-out), box-shadow 0.25s var(--ease-out), background-color 0.25s var(--ease-out);
}
.discussion-card:hover {
    background: rgba(52, 49, 56, 0.9);
    box-shadow: var(--shadow-hover), 0 0 36px -14px var(--glow);
    transform: translateY(-4px);
}
.discussion-title {
    font-size: 2rem;
    font-weight: 700;
    color: var(--text-1);
    margin-bottom: 20px;
    background: var(--grad-text);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
}
.discussion-description {
    font-size: 1.2rem;
    line-height: 1.6;
    color: var(--text-2);
    margin-bottom: 25px;
}
.discussion-buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 15px;
    margin-top: 10px;
}
@media (max-width: 700px) {
    .discussion-card { padding: 20px; }
    .discussion-title { font-size: 1.5rem; }
    .discussion-description { font-size: 0.95rem; }
    .discussion-buttons { gap: 10px; }
    .btn-lg { padding: 10px 20px; font-size: 0.95rem; }
}
</style>
