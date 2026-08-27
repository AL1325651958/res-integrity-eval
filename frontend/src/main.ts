import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import perm from './directives/perm'

const app = createApp(App)

app.use(createPinia())
app.use(router)
// Element Plus 中文语言包
app.use(ElementPlus, { locale: zhCn })

// 全局注册图标组件，供后端菜单 icon 字段按名称动态渲染
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局注册权限指令 v-perm
app.directive('perm', perm)

app.mount('#app')
