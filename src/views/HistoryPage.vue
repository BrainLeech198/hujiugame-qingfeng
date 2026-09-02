<template>
    <div class="main-wrapper" id="main-content">
        <h1 class="page-title">
            <span class="sec-emoji">📜</span>
            <span>{{ $t('history_title') }}</span>
        </h1>
        <div class="versions-container" id="versionsContainer">
            <div v-if="loading" class="loading-skeleton" style="text-align:center; padding:40px;" aria-live="polite">
                {{ $t('loading_history') }}
            </div>
            <div v-else-if="error" class="error-message">❌ {{ $t('error_load_failed') || '加载失败：' }}{{ error }}</div>
            <div v-else-if="pagedVersions.length === 0" class="no-versions">{{ $t('no_versions') || '暂无版本信息' }}</div>
            <div v-else>
                <div class="version-card" v-for="ver in pagedVersions" :key="ver.key">
                    <div class="version-header">
                        <span class="version-name">{{ ver.name }}</span>
                        <span v-if="ver.isLatest" class="latest-tag">{{ $t('latest_tag') || '最新' }}</span>
                    </div>
                    <div class="version-date" v-if="ver.date">
                        {{ $t('update_time') }}：{{ ver.date }}
                    </div>
                    <div class="log-fold">
                        <div class="version-log" v-html="formatLog(ver.log)"></div>
                    </div>
                    <div class="download-row">
                        <div class="download-item-small" v-for="p in platforms" :key="p.key">
                            <a href="#" class="download-btn" @click.prevent="openDownload(ver.download, p.key)">
                                <img :src="`resource/image/${p.icon}.png`" :alt="p.alt" class="download-icon-small">
                                <span>{{ $t(p.key + '_button') || p.label + '版' }}</span>
                            </a>
                        </div>
                    </div>
                </div>

                <!-- 分页 -->
                <div class="pagination-controls" v-if="totalPages > 1">
                    <button class="btn btn-secondary btn-sm" :disabled="currentPage <= 1" :aria-label="$t('prev_page') || '上一页'" @click="goPage(currentPage - 1)">← {{ $t('prev_page') || '上一页' }}</button>
                    <span class="page-info" aria-live="polite" aria-current="page">{{ pageInfoText }}</span>
                    <button class="btn btn-secondary btn-sm" :disabled="currentPage >= totalPages" :aria-label="$t('next_page') || '下一页'" @click="goPage(currentPage + 1)">{{ $t('next_page') || '下一页' }} →</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePageSub } from '../composables/usePageSub.js'
import { useDownloadModal } from '../composables/useDownloadModal.js'

usePageSub()

const { t } = useI18n()
const { openPlatformSelection } = useDownloadModal()
const loading = ref(true)
const error = ref(null)
const allVersions = ref([])
const currentPage = ref(1)
const PAGE_SIZE = 5

const platforms = [
    { key: 'windows', label: 'Win', alt: 'Windows下载', icon: 'download-windows' },
    { key: 'android', label: 'Android', alt: 'Android下载', icon: 'download-android' },
    { key: 'linux', label: 'Linux', alt: 'Linux下载', icon: 'download-linux' },
    { key: 'mac', label: 'Mac', alt: 'Mac下载', icon: 'download-mac' }
]

const totalPages = computed(() => Math.ceil(allVersions.value.length / PAGE_SIZE) || 1)

const pagedVersions = computed(() => {
    const start = (currentPage.value - 1) * PAGE_SIZE
    return allVersions.value.slice(start, start + PAGE_SIZE)
})

const pageInfoText = computed(() => {
    const template = t('page_info') || '第 {current} / {total} 页'
    return template.replace('{current}', currentPage.value).replace('{total}', totalPages.value)
})

function formatLog(log) {
    if (!log) return ''
    return log.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br>')
}

function goPage(page) {
    if (page < 1 || page > totalPages.value) return
    currentPage.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
}

function openDownload(download, platform) {
    if (!download) return
    if (!openPlatformSelection(platform, download)) {
        alert(t('no_download_options') || '该平台暂无可用下载链接')
    }
}

onMounted(() => {
    loadData()
})

async function loadData() {
    try {
        const res = await fetch('data/versions.json')
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        const data = await res.json()
        if (!data.versions) throw new Error('版本数据格式错误')
        const rawArray = Object.keys(data.versions).map(key => ({
            key,
            name: data.versions[key].name || '未知版本',
            date: data.versions[key].date || '',
            log: data.versions[key].log || '暂无更新日志',
            download: data.versions[key].download || {},
            isLatest: String(key) === String(data.newest_version)
        }))
        rawArray.sort((a, b) => Number(b.key) - Number(a.key))
        allVersions.value = rawArray
    } catch (err) {
        error.value = err.message
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
.versions-container {
    margin: 30px 0 20px;
    display: flex;
    flex-direction: column;
    gap: 25px;
}
.pagination-controls {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    margin-top: 24px;
    padding: 16px 0;
}
.page-info {
    color: var(--text-3);
    font-size: 0.9rem;
}
</style>
