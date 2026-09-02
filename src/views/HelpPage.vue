<template>
    <div class="main-wrapper" id="main-content">
        <h1 class="page-title">
            <span class="sec-emoji">❓</span>
            <span>{{ $t('nav_trouble') }}</span>
        </h1>
        <div class="accordion" @keydown="onAccordionKeydown">
            <div class="accordion-item" v-for="(item, i) in accordionItems" :key="i" :class="{ open: openAccordion === i }">
                <button
                    class="accordion-trigger"
                    type="button"
                    :id="'accordion-trigger-' + i"
                    :aria-expanded="openAccordion === i"
                    :aria-controls="'accordion-panel-' + i"
                    @click="toggleAccordion(i)"
                >
                    <span class="accordion-icon">{{ item.icon }}</span>
                    <span class="accordion-title">{{ $t(item.titleKey) }}</span>
                    <span class="accordion-arrow">▸</span>
                </button>
                <div class="accordion-panel" :id="'accordion-panel-' + i" role="region" :aria-labelledby="'accordion-trigger-' + i">
                    <div class="accordion-body" v-html="item.bodyHtml"></div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { usePageSub } from '../composables/usePageSub.js'

usePageSub()

const { t } = useI18n()
const openAccordion = ref(null)

const accordionItems = computed(() => [
    {
        icon: '⚡',
        titleKey: 'tip_watt_title',
        bodyHtml: `<p>${t('tip_watt_text')}</p><a href="https://steampp.net/" class="btn btn-primary" target="_blank" rel="noopener">⚡ ${t('tip_watt_button')}</a>`
    },
    {
        icon: '🛠️',
        titleKey: 'tip_repair_title',
        bodyHtml: `<p>${t('tip_repair_text')}</p>
            <div class="repair-steps">
                <div class="repair-step">
                    <div class="repair-step-title">${t('repair_step1_title')}</div>
                    <div class="repair-step-text">${t('repair_step1_text')}</div>
                    <img src="resource/image/repair1.png" alt="修复步骤1" data-fallback-next="1">
                    <div class="img-placeholder">${t('repair_img_placeholder')}</div>
                </div>
                <div class="repair-step">
                    <div class="repair-step-title">${t('repair_step2_title')}</div>
                    <div class="repair-step-text">${t('repair_step2_text')}</div>
                    <img src="resource/image/repair2.png" alt="修复步骤2" data-fallback-next="1">
                    <div class="img-placeholder">${t('repair_img_placeholder')}</div>
                </div>
            </div>`
    },
    {
        icon: '💌',
        titleKey: 'tip_feedback_title',
        bodyHtml: `<p>${t('tip_feedback_text')}</p><a href="mailto:brainleech198@foxmail.com" class="feedback-email">brainleech198@foxmail.com</a><div class="accordion-actions"><a href="mailto:brainleech198@foxmail.com" class="btn btn-primary" target="_blank" rel="noopener">📧 ${t('tip_feedback_button')}</a><button type="button" class="btn btn-secondary footer-copy-btn" data-copy="brainleech198@foxmail.com">📋 ${t('copy_button')}</button></div>`
    }
])

function toggleAccordion(i) {
    openAccordion.value = openAccordion.value === i ? null : i
}

// 手风琴组键盘导航：上下箭头在触发按钮间移动焦点
function onAccordionKeydown(e) {
    if (e.key !== 'ArrowDown' && e.key !== 'ArrowUp') return
    const triggers = [...e.currentTarget.querySelectorAll('.accordion-trigger')]
    const idx = triggers.indexOf(document.activeElement)
    if (idx === -1) return
    const next = e.key === 'ArrowDown'
        ? (triggers[idx + 1] || triggers[0])
        : (triggers[idx - 1] || triggers[triggers.length - 1])
    next.focus()
    e.preventDefault()
}

// 动态设置 accordion-panel 的 max-height 实现平滑动画
watch(openAccordion, async (newVal) => {
    await nextTick()
    document.querySelectorAll('.accordion-panel').forEach((panel, idx) => {
        if (idx === newVal) {
            panel.style.maxHeight = panel.scrollHeight + 'px'
        } else {
            panel.style.maxHeight = '0'
        }
    })
})

</script>

<style scoped>
/* HelpPage 无额外 scoped 样式
   accordion 样式由 index.css 全局定义 */
</style>
