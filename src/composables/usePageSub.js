import { onMounted, onUnmounted } from 'vue'
import '../styles/page.css'

/**
 * 子页公共生命周期：body 添加 .page-sub 背景类 + 引入 page.css
 * 供 History / Community / Help / Copyright / License / Thirdparty 等子页调用
 */
export function usePageSub () {
    onMounted(() => document.body.classList.add('page-sub'))
    onUnmounted(() => document.body.classList.remove('page-sub'))
}
