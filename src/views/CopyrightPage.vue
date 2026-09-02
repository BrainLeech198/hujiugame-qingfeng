<template>
    <div class="main-wrapper" id="main-content">
        <h1 class="page-title">
            <span class="sec-emoji">⚖️</span>
            <span>{{ $t('copyright_page_title') }}</span>
        </h1>

        <div class="copyright-list">
            <div class="copyright-card" v-for="item in cards" :key="item.to">
                <router-link :to="item.to">
                    <span>{{ $t(item.titleKey) || item.fallbackTitle }}</span>
                    <span class="arrow">→</span>
                </router-link>
                <div class="card-desc">{{ $t(item.descKey) || item.fallbackDesc }}</div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { usePageSub } from '../composables/usePageSub.js'

const cards = [
    {
        to: '/copyright/license',
        titleKey: 'copyright_license_title',
        fallbackTitle: '项目许可证',
        descKey: 'copyright_license_desc',
        fallbackDesc: '氢风项目源代码的开源许可证条款'
    },
    {
        to: '/copyright/project-thirdparty',
        titleKey: 'copyright_project_thirdparty_title',
        fallbackTitle: '项目素材第三方版权声明',
        descKey: 'copyright_project_thirdparty_desc',
        fallbackDesc: '项目中使用的第三方素材（音效、字体等）的版权信息'
    },
    {
        to: '/copyright/thirdparty',
        titleKey: 'copyright_website_thirdparty_title',
        fallbackTitle: '网站素材第三方版权声明',
        descKey: 'copyright_website_thirdparty_desc',
        fallbackDesc: '官方网站中使用的第三方素材的版权信息'
    }
]

usePageSub()
</script>

<style scoped>
.copyright-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
}
.copyright-card {
    background: var(--card-bg);
    border: 1px solid var(--border-glass);
    border-left: 3px solid var(--accent);
    border-radius: var(--radius-card);
    padding: 24px 32px;
    transition: transform 0.3s var(--ease-out), box-shadow 0.3s var(--ease-out), border-color 0.3s var(--ease-out);
}
.copyright-card:hover {
    transform: translateY(-3px);
    box-shadow: 0 0 0 1px var(--border-glass-strong), 0 12px 28px -8px rgba(0, 0, 0, 0.6), 0 0 20px -8px var(--glow);
    border-color: var(--accent-2);
}
.copyright-card a {
    color: var(--text-1);
    text-decoration: none;
    font-size: 1.1rem;
    font-weight: 600;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.copyright-card a:hover {
    color: var(--accent-2);
}
.card-desc {
    font-size: 0.9rem;
    color: var(--text-3);
    margin-top: 6px;
}
.arrow {
    color: var(--text-3);
    font-size: 1.2rem;
}
</style>
