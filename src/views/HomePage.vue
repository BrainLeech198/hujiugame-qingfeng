<template>
    <div class="home-page">
        <!-- HERO 区 -->
        <header class="hero" id="hero" ref="heroRef">
            <div class="hero-bg"></div>
            <div class="hero-spotlight" ref="spotRef"></div>
            <div class="hero-content">
                <img alt="氢风 logo" class="hero-logo" :src="'resource/image/logo.png'">
                <p class="hero-subtitle" :class="{ 'fade-in': reducedMotion }">
                    <span>{{ typewriterText }}</span>
                    <span class="typewriter-cursor">|</span>
                </p>
                <div class="hero-actions">
                    <a href="#download" class="hero-btn hero-btn-primary" @click.prevent="scrollTo('download')">
                        📥 <span>{{ $t('nav_download') }}</span>
                    </a>
                    <router-link to="/history" class="hero-btn hero-btn-secondary">
                        📜 <span>{{ $t('history_button') }}</span> →
                    </router-link>
                </div>
            </div>
        </header>

        <!-- 功能亮点 -->
        <section class="features" id="features">
            <div class="features-grid">
                <div class="feature-card reveal" v-for="(f, i) in features" :key="i" :style="{ transitionDelay: i * 0.08 + 's' }">
                    <div class="feature-icon">{{ f.icon }}</div>
                    <div class="feature-title">{{ $t(f.titleKey) }}</div>
                    <div class="feature-desc">{{ $t(f.descKey) }}</div>
                </div>
            </div>
            <div class="intro-expand">
                <button class="fold-toggle" type="button" :aria-expanded="introExpanded" @click="introExpanded = !introExpanded">
                    <span>{{ introExpanded ? $t('collapse_all') : $t('expand_all') }}</span>
                    <span class="fold-arrow">▾</span>
                </button>
                <div class="intro-fold" :class="{ folded: !introExpanded }">
                    <div class="intro-body">
                        <p>{{ $t('game_intro_desc1') }}</p>
                        <p>{{ $t('game_intro_desc2') }}</p>
                        <p>{{ $t('game_intro_desc3') }}</p>
                        <p>{{ $t('game_intro_desc4') }}</p>
                    </div>
                </div>
            </div>
        </section>

        <!-- 数据亮点 -->
        <section class="stats-bar reveal" id="stats">
            <div class="stats-inner">
                <div class="stat-item">
                    <span class="stat-number">{{ stats[0] }}</span>
                    <span class="stat-label">{{ $t('feature_multilang_title') }}</span>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item">
                    <span class="stat-number">{{ stats[1] }}</span>
                    <span class="stat-label">{{ $t('stat_platforms') }}</span>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item">
                    <span class="stat-number">{{ stats[2] }}</span>
                    <span class="stat-label">{{ $t('stat_formats') }}</span>
                </div>
            </div>
        </section>

        <!-- 资源下载 -->
        <section class="download-section reveal" id="download">
            <div class="download-header">
                <h2><span class="sec-emoji">📥</span> <span>{{ $t('download_section_title') }}</span></h2>
            </div>
            <div class="version-card" id="latestVersionCard">
                <div v-if="loading" class="loading-skeleton" style="text-align:center; padding:40px;">
                    {{ $t('loading_version') }}
                </div>
                <div v-else-if="error" class="error-message">❌ {{ error }}</div>
                <div v-else-if="versionInfo">
                    <div class="version-header">
                        <span class="version-name">{{ versionInfo.name }}</span>
                        <span class="latest-tag">{{ $t('latest_tag') || '最新' }}</span>
                    </div>
                    <div class="version-date" v-if="versionInfo.date">
                        {{ $t('update_time') }}：{{ versionInfo.date }}
                    </div>
                    <div class="log-fold" :class="{ folded: logFolded }">
                        <div class="version-log" v-html="safeLog"></div>
                        <button class="log-toggle" type="button" v-if="logNeedsFold" @click="logFolded = !logFolded">
                            <span>{{ logFolded ? ($t('expand_log') || '展开') : ($t('collapse_log') || '收起') }}</span>
                            <span class="fold-arrow">▾</span>
                        </button>
                    </div>
                    <div class="download-row">
                        <div class="download-item-small" v-for="p in platforms" :key="p.key">
                            <a href="#" class="download-btn" @click.prevent="openDownload(p.key)">
                                <img :src="`resource/image/${p.icon}.png`" :alt="p.alt" class="download-icon-small">
                                <span>{{ $t(p.key + '_button') || p.label + '版' }}</span>
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- 社区分享 -->
        <section class="community-section reveal" id="community">
            <div class="community-inner">
                <h2><span class="sec-emoji">💬</span> <span>{{ $t('community_title') }}</span></h2>
                <p>{{ $t('community_description') }}</p>
                <router-link to="/community" class="btn btn-primary btn-lg">
                    💬 <span>{{ $t('community_button') }}</span> →
                </router-link>
            </div>
        </section>

        <!-- 遇到问题 -->
        <section class="help-section reveal" id="help">
            <div class="help-header">
                <h2><span class="sec-emoji">❓</span> <span>{{ $t('nav_trouble') }}</span></h2>
            </div>
            <p style="color: var(--text-2); margin-bottom: 20px;">{{ $t('tip_watt_text') }}</p>
            <router-link to="/help" class="btn btn-primary">❓ <span>{{ $t('nav_trouble') }}</span> →</router-link>
        </section>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTypewriter } from '../composables/useTypewriter.js'
import { useReveal } from '../composables/useReveal.js'
import { useDownloadModal } from '../composables/useDownloadModal.js'
import '../styles/index.css'

const { t } = useI18n()
const { openPlatformSelection } = useDownloadModal()

// 打字机（副标题复用功能卡描述文案，走 i18n，切换语言时自动用新语言重播）
const subtitlePhrases = computed(() => [
    t('game_intro_desc1_short'),
    t('feature_cross_platform_desc'),
    t('feature_multilang_desc'),
    t('feature_resource_desc')
])
const { text: typewriterText, reducedMotion } = useTypewriter(subtitlePhrases)

// 功能卡片
const features = [
    { icon: '🖥️', titleKey: 'feature_cross_platform_title', descKey: 'feature_cross_platform_desc' },
    { icon: '🌍', titleKey: 'feature_multilang_title', descKey: 'feature_multilang_desc' },
    { icon: '📦', titleKey: 'feature_resource_title', descKey: 'feature_resource_desc' },
    { icon: '🔗', titleKey: 'feature_fileassoc_title', descKey: 'feature_fileassoc_desc' }
]

// 介绍折叠
const introExpanded = ref(false)

// 数据亮点
const stats = ref([0, 0, 0])
const targets = [15, 4, 5]
let statsObserver = null

// 版本信息
const loading = ref(true)
const error = ref(null)
const versionInfo = ref(null)
const logFolded = ref(true)
const logNeedsFold = ref(false)

const safeLog = computed(() => {
    if (!versionInfo.value?.log) return ''
    return versionInfo.value.log.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br>')
})

const platforms = [
    { key: 'windows', label: 'Win', alt: 'Windows下载', icon: 'download-windows' },
    { key: 'android', label: 'Android', alt: 'Android下载', icon: 'download-android' },
    { key: 'linux', label: 'Linux', alt: 'Linux下载', icon: 'download-linux' },
    { key: 'mac', label: 'Mac', alt: 'Mac下载', icon: 'download-mac' }
]


// Hero 聚光灯
const heroRef = ref(null)
const spotRef = ref(null)

function onHeroMouse(e) {
    if (!spotRef.value || !heroRef.value) return
    const rect = heroRef.value.getBoundingClientRect()
    spotRef.value.style.setProperty('--spot-x', (e.clientX - rect.left) + 'px')
    spotRef.value.style.setProperty('--spot-y', (e.clientY - rect.top) + 'px')
}

// 滚动入场
useReveal()

// 数字滚动
onMounted(() => {
    if (!('IntersectionObserver' in window)) return
    statsObserver = new IntersectionObserver((entries) => {
        for (const entry of entries) {
            if (!entry.isIntersecting) continue
            statsObserver.unobserve(entry.target)
            const idx = Number(entry.target.dataset.idx)
            const target = targets[idx]
            const duration = 1200
            const start = performance.now()
            function tick(now) {
                const progress = Math.min((now - start) / duration, 1)
                const eased = 1 - Math.pow(1 - progress, 3)
                stats.value[idx] = Math.round(eased * target)
                if (progress < 1) requestAnimationFrame(tick)
            }
            requestAnimationFrame(tick)
        }
    }, { threshold: 0.3 })

    document.querySelectorAll('.stat-number').forEach((el, i) => {
        el.dataset.idx = i
        statsObserver.observe(el)
    })
})

onUnmounted(() => {
    if (statsObserver) statsObserver.disconnect()
})

// 加载版本数据
async function loadVersion() {
    try {
        const res = await fetch('data/versions.json')
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        const data = await res.json()
        const newestKey = data.newest_version
        if (newestKey === undefined || !data.versions) throw new Error('JSON 缺少字段')
        const info = data.versions[String(newestKey)]
        if (!info || !info.name) throw new Error(`未找到版本 ${newestKey}`)
        versionInfo.value = info
    } catch (err) {
        error.value = err.message
    } finally {
        loading.value = false
    }
}

function openDownload(platform) {
    if (!versionInfo.value?.download) return
    if (!openPlatformSelection(platform, versionInfo.value.download)) {
        alert(t('no_download_options') || '该平台暂无可用下载链接')
    }
}

function scrollTo(id) {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
}

onMounted(() => {
    loadVersion()
    heroRef.value?.addEventListener('mousemove', onHeroMouse)
    // Feature 卡片光晕跟随
    document.querySelectorAll('.feature-card').forEach(card => {
        card.addEventListener('mousemove', (e) => {
            const rect = card.getBoundingClientRect()
            card.style.setProperty('--glow-x', (e.clientX - rect.left) + 'px')
            card.style.setProperty('--glow-y', (e.clientY - rect.top) + 'px')
        })
    })
})

onUnmounted(() => {
    heroRef.value?.removeEventListener('mousemove', onHeroMouse)
})
</script>

<style scoped>
/* 减动效偏好下跳过打字动画，用轻微淡入代替，避免文字生硬闪现 */
.hero-subtitle.fade-in {
    animation: heroFadeIn 0.8s ease-out;
}
@keyframes heroFadeIn {
    from { opacity: 0; transform: translateY(4px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>
