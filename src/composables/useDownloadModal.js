import { reactive } from 'vue'

/**
 * 下载弹窗共享状态（模块级单例）
 * HomePage / HistoryPage 通过 useDownloadModal() 打开，DownloadModal 组件读取同一状态渲染
 * 取代原先 window.showPlatformSelection / window.__showDownload 全局函数方案
 */
export const downloadModalState = reactive({
    visible: false,
    view: 'grid',        // 'grid' 平台来源选择 | 'list' 下载链接列表
    platform: null,      // 当前平台键（如 'windows'）
    source: null,        // 当前来源键（如 'github' / 'lanzou_intel'）
    sourceList: [],      // 当前平台下含有效链接的来源键
    options: null        // 当前平台完整 options 映射
})

export function useDownloadModal () {
    function openPlatformSelection (platform, download) {
        const options = download ? download[platform] : null
        if (!options || typeof options !== 'object') return false
        const sources = Object.keys(options).filter(src => {
            const data = options[src]
            const items = Array.isArray(data) ? data : [data]
            return items.some(item => item && item.url)
        })
        if (sources.length === 0) return false
        downloadModalState.platform = platform
        downloadModalState.options = options
        downloadModalState.sourceList = sources
        downloadModalState.source = null
        downloadModalState.view = 'grid'
        downloadModalState.visible = true
        return true
    }

    function openDownloadOptions (source) {
        downloadModalState.source = source
        downloadModalState.view = 'list'
    }

    function goBack () {
        downloadModalState.view = 'grid'
    }

    function close () {
        downloadModalState.visible = false
    }

    return { openPlatformSelection, openDownloadOptions, goBack, close }
}
