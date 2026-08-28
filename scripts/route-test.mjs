// 路由验证 v2：模拟"刷新直达"两阶段（注册前 → 注册后），验证通配路由不再吞路径
import { createRouter, createMemoryHistory } from 'vue-router'

const BASE = 'http://124.222.92.233/api/v1'
const login = await fetch(`${BASE}/auth/login`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123' })
}).then(r => r.json())
const menus = await fetch(`${BASE}/system/menu/tree`, {
  headers: { Authorization: `Bearer ${login.data.token}` }
}).then(r => r.json()).then(r => r.data)

const Layout = { render: () => null }
const comp = () => ({ render: () => null })
const normPath = (p) => (p ? (p.startsWith('/') ? p : `/${p}`) : '')

const constantRoutes = [
  { path: '/login', name: 'Login', component: comp },
  { path: '/', name: 'Root', component: { render: () => null } },
  { path: '/404', name: 'NotFound', component: comp },
  { path: '/risk/check/:id(\\d+)', component: Layout, children: [{ path: '', component: comp }] },
  { path: '/:pathMatch(.*)*', name: 'CatchAll', component: comp }
]
const router = createRouter({ history: createMemoryHistory(), routes: constantRoutes })

const buildRoutes = (list) => {
  const routes = []
  const walk = (items) => {
    for (const item of items) {
      if (item.menuType === 'C') {
        const path = normPath(item.path || item.component)
        routes.push({ path, component: Layout, children: [{ path: '', name: `Menu-${item.menuId}`, component: comp }] })
      } else if (item.children?.length) walk(item.children)
    }
  }
  walk(list)
  return routes
}

const leaves = []
const walk2 = (items) => { for (const it of items) { if (it.menuType === 'C') leaves.push(normPath(it.path || it.component)); else if (it.children?.length) walk2(it.children) } }
walk2(menus)

let fail = 0
// 阶段1（全部）：刷新瞬间，动态路由未注册，所有叶子路径必须命中 CatchAll（不被吞路径）
for (const p of leaves) {
  const r1 = router.resolve(p)
  if (!r1.matched.some(m => m.name === 'CatchAll')) { fail++; console.log(`❌ ${p} 阶段1 未命中 CatchAll: ${r1.matched.map(m => m.name || m.path).join(',')}`) }
}
// 注册动态路由（模拟守卫）
buildRoutes(menus).forEach(r => router.addRoute(r))
// 阶段2（全部）：守卫重新导航后，所有路径必须命中真实页面
for (const p of leaves) {
  const r2 = router.resolve(p)
  const ok = r2.matched.length >= 2 && !r2.matched.some(m => m.name === 'CatchAll' || m.name === 'NotFound')
  if (!ok) { fail++; console.log(`❌ ${p} 阶段2 未命中页面: ${r2.matched.map(m => m.name || m.path).join(',')}`) }
}
console.log(`共 ${leaves.length} 个路径；阶段1全部命中通配，阶段2全部命中页面；失败 ${fail} 个`)
console.log(fail === 0 ? '✅ 刷新直达修复验证通过' : '❌ 仍有失败')
