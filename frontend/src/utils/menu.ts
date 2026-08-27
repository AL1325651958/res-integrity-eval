import type { MenuItem } from '@/types'

/** 路径归一化：无斜杠前缀时补上，空值返回空串 */
export function normPath(p?: string): string {
  if (!p) return ''
  return p.startsWith('/') ? p : `/${p}`
}

/** 取 component（如 achievement/MyAchievements）的目录部分（achievement） */
export function dirOf(component?: string): string {
  if (!component) return ''
  const idx = component.lastIndexOf('/')
  return idx > 0 ? component.slice(0, idx) : component
}

/** 收集菜单树中所有 menuType=C 的叶子菜单 */
export function collectLeaves(items: MenuItem[]): MenuItem[] {
  const leaves: MenuItem[] = []
  const walk = (list: MenuItem[]) => {
    list.forEach((it) => {
      if (it.menuType === 'C') leaves.push(it)
      else if (it.children?.length) walk(it.children)
    })
  }
  walk(items)
  return leaves
}

/**
 * 菜单项完整路由路径：后端菜单 path 字段本身即为完整路径（如 /dashboard/my），
 * 顶层与目录下子菜单一致，不再拼接父路径，保证与 router 注册、重定向完全一致。
 */
export function menuFullPath(item: MenuItem): string {
  return normPath(item.path || item.component)
}

/** 第一个可访问页面路径（根路径重定向目标） */
export function findFirstMenuPath(menus: MenuItem[]): string {
  for (const it of menus) {
    if (it.menuType === 'C') return menuFullPath(it)
    if (it.children?.length) {
      const p = findFirstMenuPath(it.children)
      if (p) return p
    }
  }
  return ''
}

/** 在菜单树中查找指定 component（如 notice/index）对应的完整路径 */
export function findMenuPathByComponent(menus: MenuItem[], component: string): string {
  for (const it of menus) {
    if (it.menuType === 'C') {
      if (it.component === component) return menuFullPath(it)
    } else if (it.children?.length) {
      const p = findMenuPathByComponent(it.children, component)
      if (p) return p
    }
  }
  return ''
}
