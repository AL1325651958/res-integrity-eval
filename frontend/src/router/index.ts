import { createRouter, createWebHistory } from 'vue-router'
import type { RouteComponent, RouteRecordRaw } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import type { MenuItem } from '@/types'
import { findFirstMenuPath, normPath } from '@/utils/menu'

/** 动态页面组件映射：后端菜单 component 字段（如 achievement/MyAchievements）对应 ../views/{component}.vue */
const modules = import.meta.glob('../views/**/*.vue')

type RawRouteComponent = RouteComponent | (() => Promise<RouteComponent>)

/** 根路径占位组件（根路径的实际跳转由路由守卫完成：重定向到第一个动态路由） */
const RootPlaceholder = { render: () => null }

const Layout = () => import('@/layout/index.vue')

/** 静态路由 */
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    name: 'Root',
    component: RootPlaceholder,
    meta: { title: '首页' }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  },
  // 工单详情页：静态子路由，挂在风险与处置菜单目录下（Layout 内渲染）、不在侧边栏显示，
  // 供核查工单列表页（component=risk/CheckList）行点击跳转 /risk/check/:id 进入
  {
    path: '/risk/check/:id(\\d+)',
    component: Layout,
    children: [
      {
        path: '',
        name: 'CheckDetail',
        component: () => import('@/views/risk/CheckDetail.vue'),
        meta: { title: '工单详情', hidden: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

/** 解析后端 component 字段为页面组件，未匹配到文件时回退 404 */
function resolveComponent(component: string): RawRouteComponent {
  const loader = modules[`../views/${component}.vue`]
  if (loader) {
    return loader as unknown as RawRouteComponent
  }
  return () => import('@/views/error/404.vue')
}

/**
 * 根据菜单树构建动态路由：
 * 菜单 path 字段为完整路径（如 /dashboard/my），所有 C 级叶子统一注册为
 * Layout 包裹的顶层路由（嵌套在目录下也只是 UI 分组，路径不再拼接父前缀），
 * 与 utils/menu.ts 的 menuFullPath / findFirstMenuPath 完全一致。
 */
function buildRoutes(menus: MenuItem[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  const walk = (items: MenuItem[]) => {
    for (const item of items) {
      if (item.menuType === 'C') {
        const path = normPath(item.path || item.component)
        routes.push({
          path,
          component: Layout,
          children: [
            {
              path: '',
              name: `Menu-${item.menuId}`,
              component: resolveComponent(item.component),
              meta: { title: item.menuName, icon: item.icon }
            }
          ]
        })
      } else if (item.children?.length) {
        walk(item.children)
      }
    }
  }
  walk(menus)
  return routes
}

/** 动态注册菜单路由 */
function registerDynamicRoutes(menus: MenuItem[]) {
  const routes = buildRoutes(menus)
  routes.forEach((route) => router.addRoute(route))
}

/** 路由守卫：登录校验 + 首次进入加载用户信息/菜单并注册动态路由 */
router.beforeEach(async (to) => {
  const userStore = useUserStore()
  const appStore = useAppStore()

  // 未登录 → 登录页
  if (!userStore.token) {
    if (to.path === '/login') return true
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 已登录访问登录页 → 首页
  if (to.path === '/login') {
    return { path: '/' }
  }

  // 已登录但动态路由未加载 → 拉取用户信息与菜单并注册动态路由
  if (!appStore.routesLoaded) {
    try {
      await userStore.fetchInfo()
      await appStore.loadMenus()
      registerDynamicRoutes(appStore.menus)
      appStore.routesLoaded = true
    } catch {
      userStore.reset()
      appStore.clearMenus()
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    // 重新导航，使刚注册的动态路由生效（刷新页面后直达深层地址也适用）
    return { ...to, replace: true }
  }

  // 根路径 → 第一个动态路由
  if (to.path === '/') {
    const first = findFirstMenuPath(appStore.menus)
    return first || '/404'
  }

  return true
})

export default router
