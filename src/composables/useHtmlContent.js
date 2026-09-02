import { ref, onMounted } from 'vue'

/**
 * 远程 HTML 内容加载（DOMParser 提取 .container）
 * 用于 LicensePage / ThirdpartyPage 等从静态 HTML 文件注入内容的页面
 *
 * @param {string} url - HTML 文件路径（如 '/data/license.html'）
 * @returns {{ contentHtml: import('vue').Ref<string> }}
 */
export function useHtmlContent (url) {
    const contentHtml = ref('')

    onMounted(async () => {
        try {
            const res = await fetch(url)
            if (res.ok) {
                const html = await res.text()
                const doc = new DOMParser().parseFromString(html, 'text/html')
                const container = doc.querySelector('.container')
                if (container) {
                    container.querySelectorAll('h1, .footer-note, nav, .skip-link').forEach(el => el.remove())
                    contentHtml.value = container.innerHTML
                }
            }
        } catch {
            contentHtml.value = '<p>加载失败</p>'
        }
    })

    return { contentHtml }
}
