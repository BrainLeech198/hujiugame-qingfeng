<template>
    <div class="lightbox" :class="{ open: isOpen }" role="dialog" aria-modal="true" :aria-hidden="!isOpen">
        <button class="lightbox-close" type="button" :aria-label="$t('close')" @click="close">&times;</button>
        <figure class="lightbox-content">
            <img
                ref="imgRef"
                :src="imgSrc"
                :alt="imgAlt"
                :style="imgStyle"
                @pointerdown="onPointerDown"
                @dblclick="resetView"
            >
            <figcaption class="lightbox-caption">{{ imgAlt }}</figcaption>
        </figure>
    </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'

const isOpen = ref(false)
const imgSrc = ref('')
const imgAlt = ref('')
const imgRef = ref(null)

const scale = ref(1)
const panX = ref(0)
const panY = ref(0)

const PAN_STEP = 60
const ZOOM_STEP = 0.15
const MIN_SCALE = 0.5
const MAX_SCALE = 5

const imgStyle = computed(() => ({
    transform: `translate(${panX.value}px, ${panY.value}px) scale(${scale.value})`
}))

function resetView() {
    scale.value = 1
    panX.value = 0
    panY.value = 0
}

// 焦点管理：打开时聚焦关闭按钮，关闭时归还给触发元素
let lastFocused = null

function open(src, alt) {
    imgSrc.value = src
    imgAlt.value = alt || ''
    isOpen.value = true
    document.body.style.overflow = 'hidden'
    resetView()
    lastFocused = document.activeElement
    nextTick(() => {
        const closeBtn = document.querySelector('.lightbox-close')
        if (closeBtn) closeBtn.focus()
    })
}

function close() {
    isOpen.value = false
    document.body.style.overflow = ''
    imgSrc.value = ''
    resetView()
    if (lastFocused) {
        lastFocused.focus()
        lastFocused = null
    }
}

// 点击图片打开（事件委托）
function onClick(e) {
    const img = e.target.closest('img')
    if (!img) return
    if (img.closest('a, button')) return
    if (!img.offsetParent) return
    open(img.currentSrc || img.src, img.alt)
}

// 滚轮缩放
function onWheel(e) {
    if (!isOpen.value) return
    e.preventDefault()
    const rect = imgRef.value.getBoundingClientRect()
    const mouseX = e.clientX - rect.left - rect.width / 2
    const mouseY = e.clientY - rect.top - rect.height / 2
    const oldScale = scale.value
    const delta = e.deltaY < 0 ? ZOOM_STEP : -ZOOM_STEP
    scale.value = Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale.value + delta * scale.value))
    const ratio = scale.value / oldScale
    panX.value = mouseX - ratio * (mouseX - panX.value)
    panY.value = mouseY - ratio * (mouseY - panY.value)
}

// 键盘控制
function onKeyDown(e) {
    if (!isOpen.value) return
    switch (e.key) {
        case 'Escape': close(); break
        case 'ArrowLeft':  panX.value += PAN_STEP; e.preventDefault(); break
        case 'ArrowRight': panX.value -= PAN_STEP; e.preventDefault(); break
        case 'ArrowUp':    panY.value += PAN_STEP; e.preventDefault(); break
        case 'ArrowDown':  panY.value -= PAN_STEP; e.preventDefault(); break
        case '+': case '=':
            scale.value = Math.min(MAX_SCALE, scale.value + ZOOM_STEP * scale.value)
            e.preventDefault(); break
        case '-': case '_':
            scale.value = Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale.value - ZOOM_STEP * scale.value))
            e.preventDefault(); break
        case '0': resetView(); e.preventDefault(); break
        case 'Tab': trapFocus(e); break
    }
}

// Tab 焦点循环（focus trap）
function trapFocus(e) {
    const lightbox = document.querySelector('.lightbox')
    if (!lightbox) return
    const focusables = lightbox.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])')
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

// 指针交互：单指/鼠标拖拽平移，双指 pinch 缩放（Pointer Events，覆盖触摸屏）
const pointers = new Map()
let dragStartX = 0
let dragStartY = 0
let pinchStartDist = 0
let pinchStartScale = 1
let pinchPrevCenter = null

function onPointerDown(e) {
    if (!isOpen.value) return
    pointers.set(e.pointerId, { x: e.clientX, y: e.clientY })
    if (pointers.size === 1) {
        dragStartX = e.clientX - panX.value
        dragStartY = e.clientY - panY.value
        if (imgRef.value) imgRef.value.style.cursor = 'grabbing'
    } else if (pointers.size === 2) {
        const [a, b] = [...pointers.values()]
        pinchStartDist = Math.hypot(a.x - b.x, a.y - b.y)
        pinchStartScale = scale.value
        pinchPrevCenter = { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 }
    }
    e.preventDefault()
}

function onPointerMove(e) {
    if (!pointers.has(e.pointerId)) return
    pointers.set(e.pointerId, { x: e.clientX, y: e.clientY })
    if (pointers.size === 1) {
        panX.value = e.clientX - dragStartX
        panY.value = e.clientY - dragStartY
    } else if (pointers.size === 2) {
        const [a, b] = [...pointers.values()]
        const dist = Math.hypot(a.x - b.x, a.y - b.y)
        const cx = (a.x + b.x) / 2
        const cy = (a.y + b.y) / 2
        if (pinchStartDist > 0) {
            scale.value = Math.min(MAX_SCALE, Math.max(MIN_SCALE, pinchStartScale * dist / pinchStartDist))
        }
        if (pinchPrevCenter) {
            panX.value += cx - pinchPrevCenter.x
            panY.value += cy - pinchPrevCenter.y
        }
        pinchPrevCenter = { x: cx, y: cy }
    }
}

function onPointerUp(e) {
    if (!pointers.delete(e.pointerId)) return
    if (pointers.size < 2) pinchPrevCenter = null
    if (pointers.size === 1) {
        const [p] = [...pointers.values()]
        dragStartX = p.x - panX.value
        dragStartY = p.y - panY.value
    } else if (pointers.size === 0) {
        if (imgRef.value) imgRef.value.style.cursor = 'grab'
    }
}

onMounted(() => {
    document.addEventListener('click', onClick)
    document.addEventListener('keydown', onKeyDown)
    document.addEventListener('pointermove', onPointerMove)
    document.addEventListener('pointerup', onPointerUp)
    document.addEventListener('pointercancel', onPointerUp)
    // wheel 需要 passive: false
    document.addEventListener('wheel', onWheel, { passive: false })
})

onUnmounted(() => {
    document.removeEventListener('click', onClick)
    document.removeEventListener('keydown', onKeyDown)
    document.removeEventListener('pointermove', onPointerMove)
    document.removeEventListener('pointerup', onPointerUp)
    document.removeEventListener('pointercancel', onPointerUp)
    document.removeEventListener('wheel', onWheel)
})
</script>
