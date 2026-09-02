import { onMounted, onUnmounted } from 'vue'

export function useReveal(selector = '.reveal') {
    let observer = null
    const timers = new Set()

    onMounted(() => {
        // 兜底：无 IntersectionObserver 时直接显示所有内容，避免元素永久隐藏
        if (!('IntersectionObserver' in window)) {
            document.querySelectorAll(selector).forEach(el => {
                el.classList.remove('reveal')
                el.classList.add('done')
            })
            return
        }
        observer = new IntersectionObserver((entries) => {
            for (const entry of entries) {
                if (!entry.isIntersecting) continue
                const el = entry.target
                el.classList.add('in')
                observer.unobserve(el)
                const timer = setTimeout(() => {
                    el.classList.remove('reveal', 'in')
                    el.classList.add('done')
                    el.style.transitionDelay = ''
                }, 700)
                timers.add(timer)
            }
        }, { threshold: 0.08, rootMargin: '0px 0px -6% 0px' })

        document.querySelectorAll(selector).forEach(el => {
            observer.observe(el)
        })
    })

    onUnmounted(() => {
        if (observer) observer.disconnect()
        timers.forEach(clearTimeout)
    })
}
