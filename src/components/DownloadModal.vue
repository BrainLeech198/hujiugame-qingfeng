<template>
    <div id="downloadModal" ref="modalRef" class="modal" :class="{ show: state.visible }"
         role="dialog" aria-modal="true" :aria-label="titleText" tabindex="-1"
         @keydown="onModalKeydown">
        <div class="modal-content" :class="{ 'modal-content-lg': state.view === 'list' }">
            <div class="modal-header">
                <button class="modal-back" :class="{ show: state.view === 'list' }" @click="goBack">
                    ← <span>{{ $t('back_button') }}</span>
                </button>
                <div class="modal-title">{{ titleText }}</div>
                <button class="modal-close" type="button" :aria-label="$t('close')" @click="close">&times;</button>
            </div>
            <div class="modal-body">
                <!-- 平台来源选择 -->
                <div v-if="state.view === 'grid'" class="platform-select-grid">
                    <button type="button" class="platform-select-btn" v-for="src in state.sourceList" :key="src" @click="openDownloadOptions(src)">
                        {{ sourceName(src) }}
                    </button>
                </div>
                <!-- 下载链接列表 -->
                <template v-else>
                    <a v-for="(item, i) in currentItems" :key="i" class="download-option download-option-row" :href="item.url" target="_blank" rel="noopener">
                        <span class="option-description">{{ item.description || sourceName(state.source) }}</span>
                        <span class="option-arrow">↓</span>
                    </a>
                    <div v-if="currentItems.length === 0" class="modal-error">{{ $t('no_valid_download_source') || '没有有效的下载链接' }}</div>
                </template>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useI18n } from 'vue-i18n'
import { downloadModalState, useDownloadModal } from '../composables/useDownloadModal.js'

const { t } = useI18n()
const { openDownloadOptions, goBack, close } = useDownloadModal()

const state = downloadModalState
const modalRef = ref(null)
let lastFocused = null

// 打开时聚焦关闭按钮，关闭时归还焦点给触发元素
// flush:'post' 确保回调在 DOM patch（.modal 可见）之后执行，focus 才有效
watch(() => state.visible, (visible) => {
    if (visible) {
        lastFocused = document.activeElement
        // opacity 淡入过渡期间（0.24s）元素不可聚焦，等过渡完成后再聚焦
        const modalEl = modalRef.value
        if (modalEl) {
            const focusClose = () => {
                const closeBtn = modalEl.querySelector('.modal-close')
                if (closeBtn) closeBtn.focus()
            }
            modalEl.addEventListener('transitionend', focusClose, { once: true })
            setTimeout(focusClose, 300) // 兜底：reduced-motion 或无过渡时 transitionend 不触发
        }
    } else if (lastFocused) {
        lastFocused.focus()
        lastFocused = null
    }
}, { flush: 'post' })

// Escape 关闭 + Tab 焦点循环（focus trap）
function onModalKeydown(e) {
    if (e.key === 'Escape') {
        close()
        return
    }
    if (e.key !== 'Tab' || !modalRef.value) return
    const focusables = modalRef.value.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])')
    if (focusables.length === 0) return
    const first = focusables[0]
    const last = focusables[focusables.length - 1]
    if (e.shiftKey && document.activeElement === first) {
        last.focus()
        e.preventDefault()
    } else if (!e.shiftKey && document.activeElement === last) {
        first.focus()
        e.preventDefault()
    }
}

onBeforeUnmount(() => {
    if (lastFocused) {
        lastFocused.focus()
        lastFocused = null
    }
})

const currentItems = computed(() => {
    if (!state.options || !state.source) return []
    const data = state.options[state.source]
    return (Array.isArray(data) ? data : [data]).filter(item => item && item.url)
})

const titleText = computed(() => {
    if (state.view === 'list') return `${sourceName(state.source)} - ${platformName(state.platform)}`
    return `${platformName(state.platform)} - ${t('select_platform') || '选择下载平台'}`
})

function platformName (p) {
    return t(`${p}_button`) || { windows: 'Windows', android: 'Android', linux: 'Linux', mac: 'Mac' }[p] || p
}

function sourceName (source) {
    const isIntel = source.endsWith('_intel')
    const base = isIntel ? source.slice(0, -'_intel'.length) : source
    const baseName = t(`source_${base}`) || base
    return isIntel ? `${baseName}（Intel）` : baseName
}
</script>
