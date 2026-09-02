import { createI18n } from 'vue-i18n'

const localeMap = {
    'zh': 'zh',
    'zh-cn': 'zh',
    'zh-tw': 'zh-TW',
    'zh-hk': 'zh-TW',
    'zh-mo': 'zh-TW',
    'en': 'en',
    'en-us': 'en',
    'en-gb': 'en',
    'ja': 'ja',
    'ko': 'ko',
    'ru': 'ru',
    'pt': 'pt',
    'pt-br': 'pt',
    'pt-pt': 'pt',
    'fr': 'fr',
    'fr-fr': 'fr',
    'fr-ca': 'fr',
    'de': 'de',
    'es': 'es',
    'es-es': 'es',
    'es-mx': 'es'
}

const STORAGE_KEY = 'qingfeng_locale'

export function detectLocale() {
    // 用户手动选择优先（持久化在 localStorage）
    try {
        const saved = localStorage.getItem(STORAGE_KEY)
        if (saved && localeMap[saved.toLowerCase()]) return localeMap[saved.toLowerCase()]
    } catch (e) { /* localStorage 不可用时忽略 */ }
    const lang = (navigator.language || navigator.userLanguage || 'zh').toLowerCase()
    if (localeMap[lang]) return localeMap[lang]
    return localeMap[lang.split('-')[0]] || 'zh'
}

function persistLocale(lang) {
    try { localStorage.setItem(STORAGE_KEY, lang) } catch (e) { /* 忽略 */ }
    document.documentElement.lang = lang
}

const i18n = createI18n({
    legacy: false,
    locale: detectLocale(),
    fallbackLocale: 'zh',
    messages: {}
})

// 动态加载语言文件
export async function loadLocaleMessages(lang) {
    try {
        const res = await fetch(`data/locales/${lang}.json`)
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        const messages = await res.json()
        i18n.global.setLocaleMessage(lang, messages)
        i18n.global.locale.value = lang
        persistLocale(lang)
    } catch (err) {
        console.warn(`加载语言文件 ${lang} 失败`, err)
        if (lang !== 'zh') {
            const res = await fetch('data/locales/zh.json')
            const messages = await res.json()
            i18n.global.setLocaleMessage('zh', messages)
            i18n.global.locale.value = 'zh'
            persistLocale('zh')
        }
    }
}

export default i18n
