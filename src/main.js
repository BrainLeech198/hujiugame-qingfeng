import { createApp } from 'vue'
import App from './App.vue'
import router from './router/index.js'
import i18n, { loadLocaleMessages, detectLocale } from './i18n/index.js'
import './styles/common.css'
import './styles/shared.css'

// Google Fonts 动态注入（避免 @import 渲染阻塞）
function loadGoogleFonts() {
    if (document.querySelector('link[href*="fonts.googleapis.com"]')) return
    const head = document.head
    const preconnect1 = document.createElement('link')
    preconnect1.rel = 'preconnect'
    preconnect1.href = 'https://fonts.googleapis.com'
    const preconnect2 = document.createElement('link')
    preconnect2.rel = 'preconnect'
    preconnect2.href = 'https://fonts.gstatic.com'
    preconnect2.crossOrigin = 'anonymous'
    const stylesheet = document.createElement('link')
    stylesheet.rel = 'stylesheet'
    stylesheet.href = 'https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;600;700&display=swap'
    head.prepend(preconnect1, preconnect2, stylesheet)
}

// 图片加载失败兜底
function imgErrorSvg(label, w, h, opts) {
    opts = opts || {}
    const bg = opts.bg || '#b8d4e3'
    const color = opts.color || '#1d4d6b'
    const fontSize = opts.fontSize != null ? opts.fontSize : Math.round(h * 0.24)
    const y = opts.y != null ? opts.y : Math.round(h * 0.55)
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}"><rect width="${w}" height="${h}" fill="${bg}"/><text x="${w / 2}" y="${y}" font-size="${fontSize}" fill="${color}" text-anchor="middle" dominant-baseline="middle">${label}</text></svg>`
    return 'data:image/svg+xml,' + encodeURIComponent(svg)
}

document.addEventListener('error', function (e) {
    const el = e.target
    if (!el || el.tagName !== 'IMG') return
    if (el.dataset.fallback === undefined && el.dataset.fallbackNext === undefined) return
    if (el.src && el.src.indexOf('data:') === 0) return
    if (el.dataset.fallbackNext !== undefined) {
        el.style.display = 'none'
        const ph = el.nextElementSibling
        if (ph) ph.style.display = 'block'
        return
    }
    const label = el.dataset.fallback
    const w = Number(el.dataset.fallbackW || 200)
    const h = Number(el.dataset.fallbackH || 100)
    if (el.dataset.fallbackLogo !== undefined) {
        el.style.filter = 'grayscale(0.3)'
    } else {
        el.classList.add('error')
        if (el.dataset.fallbackAlt) el.alt = el.dataset.fallbackAlt
    }
    el.src = imgErrorSvg(label, w, h, {
        bg: el.dataset.fallbackBg,
        color: el.dataset.fallbackColor,
        fontSize: el.dataset.fallbackFont,
        y: el.dataset.fallbackY,
    })
}, true)

// 初始化
loadGoogleFonts()

const app = createApp(App)
app.use(router)
app.use(i18n)

// 先挂载再异步加载语言包：避免 fetch 阻塞首屏。
// body 初始 opacity:0，入场淡入在语言就绪后再触发，用户感知无 key 闪烁。
const userLang = detectLocale()
app.mount('#app')
loadLocaleMessages(userLang).finally(() => {
    requestAnimationFrame(() => {
        document.body.classList.add('loaded')
    })
})
