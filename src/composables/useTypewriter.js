import { ref, watch, onMounted, onUnmounted } from 'vue'

export function useTypewriter(phrases, options = {}) {
    const text = ref('')
    // 系统减动效偏好：开启时跳过逐字动画，直接显示完整文本（由调用方叠加淡入过渡）
    const reducedMotion = ref(window.matchMedia('(prefers-reduced-motion: reduce)').matches)
    const {
        typeSpeed = 65,
        deleteSpeed = 30,
        pauseAfterType = 2200,
        pauseAfterDelete = 400,
        startDelay = 800
    } = options

    let phraseIdx = 0
    let charIdx = 0
    let timer = null

    // phrases 支持普通数组或 ref/computed：切换语言时传入新数组即可触发重置重播
    function resolvePhrases() {
        return Array.isArray(phrases) ? phrases : phrases.value
    }

    function typeNext() {
        const current = resolvePhrases()[phraseIdx]
        if (charIdx < current.length) {
            charIdx++
            text.value = current.substring(0, charIdx)
            timer = setTimeout(typeNext, typeSpeed)
        } else {
            timer = setTimeout(deletePhrase, pauseAfterType)
        }
    }

    function deletePhrase() {
        if (charIdx > 0) {
            charIdx--
            text.value = resolvePhrases()[phraseIdx].substring(0, charIdx)
            timer = setTimeout(deletePhrase, deleteSpeed)
        } else {
            phraseIdx = (phraseIdx + 1) % resolvePhrases().length
            timer = setTimeout(typeNext, pauseAfterDelete)
        }
    }

    // 文本更新（如切换语言）后从头用新内容重新播放
    function resetPhrase() {
        clearTimeout(timer)
        timer = null
        phraseIdx = 0
        charIdx = 0
        text.value = ''
        if (reducedMotion.value) {
            text.value = resolvePhrases()[0] || ''
            return
        }
        timer = setTimeout(typeNext, 100)
    }

    // 页面隐藏时暂停打字，恢复可见后继续，避免后台空转
    function onVisibilityChange() {
        if (document.hidden) {
            clearTimeout(timer)
            timer = null
        } else if (timer === null) {
            timer = setTimeout(typeNext, 100)
        }
    }

    watch(resolvePhrases, resetPhrase)

    onMounted(() => {
        // 减动效偏好：直接显示完整文本，跳过逐字动画
        if (reducedMotion.value) {
            text.value = resolvePhrases()[0]
            return
        }
        timer = setTimeout(typeNext, startDelay)
        document.addEventListener('visibilitychange', onVisibilityChange)
    })

    onUnmounted(() => {
        clearTimeout(timer)
        document.removeEventListener('visibilitychange', onVisibilityChange)
    })

    return { text, reducedMotion }
}
