import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * v-perm 权限指令
 * 用法：v-perm="'achievement:audit'" 或 v-perm="['a:create','a:edit']"
 * 校验 store 中的 perms，无权限则移除元素；perms 为空数组时放行。
 */
const perm: Directive<HTMLElement, string | string[]> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    const userStore = useUserStore()
    const perms = userStore.perms || []
    // perms 为空数组时放行
    if (!perms.length) return
    const need = Array.isArray(binding.value) ? binding.value : [binding.value]
    const has = need.some((p) => perms.includes(p))
    if (!has) {
      el.parentNode?.removeChild(el)
    }
  }
}

export default perm
