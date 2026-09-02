<template>
    <div class="main-wrapper" id="main-content">
        <h1 class="page-title">
            <span class="sec-emoji">📦</span>
            <span>{{ $t(titleKey) }}</span>
        </h1>
        <div class="content-card" v-html="contentHtml"></div>
    </div>
</template>

<script setup>
import { useHtmlContent } from '../composables/useHtmlContent.js'
import { usePageSub } from '../composables/usePageSub.js'

const props = defineProps({
    dataUrl: { type: String, required: true },
    titleKey: { type: String, required: true }
})

usePageSub()
const { contentHtml } = useHtmlContent(props.dataUrl)
</script>
