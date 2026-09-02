<template>
    <nav class="top-nav" :class="{ scrolled: isScrolled }" aria-label="主导航">
        <a class="nav-brand" href="#" @click.prevent="onBrandClick">
            ◈ <span>{{ $t('brand_name') }}</span>
        </a>
        <button
            class="nav-toggle"
            type="button"
            :aria-expanded="menuOpen"
            aria-label="展开导航菜单"
            @click="menuOpen = !menuOpen"
        >☰</button>
        <!-- 主页：完整导航 -->
        <div v-if="isHome" class="nav-links" :class="{ open: menuOpen }">
            <router-link to="/#features" @click="closeMenu">{{ $t('nav_intro') }}</router-link>
            <router-link to="/#download" @click="closeMenu">{{ $t('nav_download') }}</router-link>
            <router-link to="/help" @click="closeMenu">
                <span>{{ $t('nav_trouble') }}</span> →
            </router-link>
            <router-link to="/history" @click="closeMenu">
                <span>{{ $t('history_button') }}</span> →
            </router-link>
            <router-link to="/community" @click="closeMenu">
                <span>{{ $t('nav_community_resources') }}</span> →
            </router-link>
        </div>
        <!-- 子页：简化导航 -->
        <div v-else class="nav-links" :class="{ open: menuOpen }">
            <router-link v-if="isCopyrightSubPage" to="/copyright" @click="closeMenu">← <span>返回版权相关</span></router-link>
            <router-link v-else to="/" @click="closeMenu">← <span>{{ $t('back_home') }}</span></router-link>
        </div>
    </nav>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const isHome = computed(() => route.path === '/')
const isCopyrightSubPage = computed(() => route.path.startsWith('/copyright/'))
const isScrolled = ref(false)
const menuOpen = ref(false)

let brandClicks = 0
let brandTimer = null
let raining = false

function onScroll() {
    isScrolled.value = window.scrollY > 10
}

function closeMenu() {
    menuOpen.value = false
}

function onClickOutside(e) {
    if (!menuOpen.value) return
    if (e.target.closest('.top-nav')) return
    menuOpen.value = false
}

function onBrandClick(e) {
    e.preventDefault()
    if (raining) return
    brandClicks++
    clearTimeout(brandTimer)
    brandTimer = setTimeout(() => { brandClicks = 0 }, 2000)
    if (brandClicks >= 5) {
        brandClicks = 0
        raining = true
        triggerEmojiRain(() => { raining = false })
    } else {
        router.push('/')
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }
}

function triggerEmojiRain(onDone) {
    const emojis = ['◈', '✦', '⚡', '🌍', '📦', '🎮', '💻', '🎉', '✨', '🚀']
    const count = 25
    let done = 0
    for (let i = 0; i < count; i++) {
        setTimeout(() => {
            const el = document.createElement('div')
            el.textContent = emojis[Math.floor(Math.random() * emojis.length)]
            const startX = Math.random() * 100
            const drift = (Math.random() - 0.5) * 25
            const duration = (2 + Math.random() * 2) * 1000
            Object.assign(el.style, {
                position: 'fixed',
                left: startX + 'vw',
                top: '-30px',
                fontSize: (1.2 + Math.random() * 1.3) + 'rem',
                pointerEvents: 'none',
                zIndex: '99999',
                opacity: '0.85'
            })
            document.body.appendChild(el)
            const anim = el.animate([
                { top: '-30px', left: startX + 'vw', opacity: 0.85, transform: 'rotate(0deg)' },
                { top: '105vh', left: (startX + drift) + 'vw', opacity: 0, transform: 'rotate(360deg)' }
            ], { duration, easing: 'ease-in', fill: 'forwards' })
            anim.onfinish = () => {
                el.remove()
                done++
                if (done >= count && onDone) onDone()
            }
        }, i * 100)
    }
}

onMounted(() => {
    window.addEventListener('scroll', onScroll, { passive: true })
    document.addEventListener('click', onClickOutside)
})

onUnmounted(() => {
    window.removeEventListener('scroll', onScroll)
    document.removeEventListener('click', onClickOutside)
})
</script>
