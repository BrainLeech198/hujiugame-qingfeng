<template>
    <div id="top" class="app-root">
        <SkipLink />
        <TopNav />
        <router-view v-slot="{ Component }">
            <transition name="fade-slide" mode="out-in">
                <component :is="Component" />
            </transition>
        </router-view>
        <SiteFooter />
        <DownloadModal />
        <Lightbox />
    </div>
</template>

<script setup>
import TopNav from './components/TopNav.vue'
import SiteFooter from './components/SiteFooter.vue'
import DownloadModal from './components/DownloadModal.vue'
import Lightbox from './components/Lightbox.vue'
import SkipLink from './components/SkipLink.vue'
</script>

<style>
.app-root {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    width: 100%;
    align-self: stretch;
}
</style>
