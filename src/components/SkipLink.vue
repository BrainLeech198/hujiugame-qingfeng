<template>
    <a class="skip-link" href="#main" @click.prevent="skipToContent">{{ $t('skip_to_content') }}</a>
</template>

<script setup>
// 按路由指向主内容：子页 .main-wrapper 有 id="main-content"，首页回退 #features
function skipToContent() {
    const target = document.getElementById('main-content') || document.getElementById('features')
    if (!target) return
    if (target.tabIndex < 0) target.setAttribute('tabindex', '-1')
    target.focus({ preventScroll: true })
    target.scrollIntoView()
}
</script>
