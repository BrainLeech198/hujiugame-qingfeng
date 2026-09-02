<template>
    <div class="main-wrapper" id="main-content">
        <h1 class="page-title">
            <span class="sec-emoji">📜</span>
            <span>{{ $t('license_page_title') }}</span>
            <a class="cc-license-btn" href="https://creativecommons.org/licenses/by-nc/4.0/" target="_blank" rel="noopener noreferrer" title="CC BY-NC 4.0 官方许可全文（署名—非商业性使用）">
                <img :src="'resource/image/cc-by-nc-4.0.png'" alt="CC BY-NC 4.0" width="88" height="31">
            </a>
        </h1>
        <div class="content-card" ref="contentRef" v-html="contentHtml" @click="onTabClick"></div>
    </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import { useHtmlContent } from '../composables/useHtmlContent.js'
import { usePageSub } from '../composables/usePageSub.js'

usePageSub()

const { contentHtml } = useHtmlContent('data/license.html')
const contentRef = ref(null)

/** tab 切换：点击 tab-btn 后激活对应面板 */
function onTabClick (e) {
    const btn = e.target.closest('.tab-btn')
    if (!btn || !contentRef.value) return
    const tabId = btn.dataset.tab
    if (!tabId) return

    contentRef.value.querySelectorAll('.tab-btn').forEach(b => {
        b.classList.remove('active')
        b.setAttribute('aria-selected', 'false')
    })
    btn.classList.add('active')
    btn.setAttribute('aria-selected', 'true')
    contentRef.value.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'))
    const panel = contentRef.value.querySelector(`#tab-${tabId}`)
    if (panel) panel.classList.add('active')
}

// 内容加载后激活第一个 tab
watch(contentHtml, async (val) => {
    if (!val) return
    await nextTick()
    initTabsA11y()
    const firstBtn = contentRef.value?.querySelector('.tab-btn')
    if (firstBtn && !firstBtn.classList.contains('active')) {
        firstBtn.click()
    }
}, { once: true })

// v-html 注入的 tab 内容运行时补 ARIA（role/tablist、aria-selected、aria-controls）
function initTabsA11y() {
    const content = contentRef.value
    if (!content) return
    const tabs = content.querySelector('.tabs')
    if (tabs && !tabs.hasAttribute('role')) tabs.setAttribute('role', 'tablist')
    content.querySelectorAll('.tab-btn').forEach(btn => {
        const id = btn.dataset.tab
        btn.setAttribute('role', 'tab')
        btn.setAttribute('aria-selected', btn.classList.contains('active') ? 'true' : 'false')
        if (id) {
            btn.setAttribute('id', 'tab-btn-' + id)
            btn.setAttribute('aria-controls', 'tab-' + id)
        }
    })
    content.querySelectorAll('.tab-panel').forEach(panel => {
        panel.setAttribute('role', 'tabpanel')
        const pid = panel.id
        if (pid) panel.setAttribute('aria-labelledby', 'tab-btn-' + pid.replace(/^tab-/, ''))
    })
}
</script>

<style scoped>
.cc-license-btn {
    display: inline-block;
    vertical-align: middle;
    margin-left: 14px;
    opacity: 0.9;
    transition: opacity 0.2s;
}
.cc-license-btn:hover {
    opacity: 1;
}
.cc-license-btn img {
    display: block;
}

@media (max-width: 500px) {
    .cc-license-btn {
        margin-left: 8px;
    }
    .cc-license-btn img {
        width: 62px;
        height: 22px;
    }
}
</style>
