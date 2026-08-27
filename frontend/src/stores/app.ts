import { defineStore } from 'pinia'
import { menuTree } from '@/api/system'
import type { MenuItem } from '@/types'

const MENUS_KEY = 'integrity_menus'

export const useAppStore = defineStore('app', {
  state: () => ({
    menus: [] as MenuItem[],
    /** 动态路由是否已注册（首次进入由路由守卫拉取菜单并 addRoute） */
    routesLoaded: false
  }),
  actions: {
    /** 从后端 /system/menu/tree 加载菜单树，并缓存到 localStorage 供刷新恢复 */
    async loadMenus() {
      try {
        const data = await menuTree()
        this.menus = Array.isArray(data) ? data : []
      } catch {
        // 后端不可用时尝试从本地缓存恢复
        this.menus = this.restoreMenus()
      }
      localStorage.setItem(MENUS_KEY, JSON.stringify(this.menus))
      return this.menus
    },
    /** 从 localStorage 恢复菜单树 */
    restoreMenus(): MenuItem[] {
      const cached = localStorage.getItem(MENUS_KEY)
      if (!cached) return []
      try {
        return JSON.parse(cached) as MenuItem[]
      } catch {
        return []
      }
    },
    /** 清空菜单与动态路由标记（退出登录时调用） */
    clearMenus() {
      this.menus = []
      this.routesLoaded = false
      localStorage.removeItem(MENUS_KEY)
    }
  }
})
