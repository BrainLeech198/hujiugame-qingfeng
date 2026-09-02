import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
    {
        path: '/',
        name: 'home',
        component: () => import('../views/HomePage.vue')
    },
    {
        path: '/history',
        name: 'history',
        component: () => import('../views/HistoryPage.vue')
    },
    {
        path: '/community',
        name: 'community',
        component: () => import('../views/CommunityPage.vue')
    },
    {
        path: '/help',
        name: 'help',
        component: () => import('../views/HelpPage.vue')
    },
    {
        path: '/copyright',
        name: 'copyright',
        component: () => import('../views/CopyrightPage.vue')
    },
    {
        path: '/copyright/license',
        name: 'license',
        component: () => import('../views/LicensePage.vue')
    },
    {
        path: '/copyright/thirdparty',
        name: 'thirdparty',
        component: () => import('../views/ThirdpartyPage.vue'),
        props: { dataUrl: 'data/thirdparty.html', titleKey: 'thirdparty_licenses_page_title' }
    },
    {
        path: '/copyright/project-thirdparty',
        name: 'project-thirdparty',
        component: () => import('../views/ThirdpartyPage.vue'),
        props: { dataUrl: 'data/project_thirdparty.html', titleKey: 'project_thirdparty_page_title' }
    }
]

const router = createRouter({
    history: createWebHashHistory(),
    routes,
    scrollBehavior(to, from, savedPosition) {
        if (to.hash) {
            return { el: to.hash, behavior: 'smooth', top: 72 }
        }
        if (savedPosition) return savedPosition
        return { top: 0 }
    }
})

export default router
