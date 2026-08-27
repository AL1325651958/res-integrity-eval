import { defineStore } from 'pinia'
import { getInfo, login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginParams, UserInfo } from '@/types'
import { getToken, removeToken, setToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: null as UserInfo | null,
    roles: [] as string[],
    perms: [] as string[]
  }),
  actions: {
    /** 登录：调用 api/auth.login，保存 token 与用户信息 */
    async login(params: LoginParams) {
      const data = await loginApi(params)
      this.token = data.token
      setToken(data.token)
      if (data.user) {
        this.setUserInfo(data.user)
      }
    },
    /** 获取当前用户信息：api/auth.getInfo */
    async fetchInfo() {
      const user = await getInfo()
      this.setUserInfo(user)
      return user
    },
    /** 统一保存用户信息（state + localStorage，供页面 hasRole 读取） */
    setUserInfo(user: UserInfo) {
      this.userInfo = user
      this.roles = user.roles || []
      this.perms = user.perms || []
      localStorage.setItem('userInfo', JSON.stringify(user))
      localStorage.setItem('integrity_roles', JSON.stringify(user.roles || []))
    },
    /** 退出登录：调用后端接口并清空本地状态 */
    async logout() {
      try {
        await logoutApi()
      } catch {
        // 后端退出失败不阻塞本地清理
      }
      this.reset()
    },
    /** 清空本地登录状态 */
    reset() {
      this.token = ''
      this.userInfo = null
      this.roles = []
      this.perms = []
      removeToken()
      localStorage.removeItem('userInfo')
      localStorage.removeItem('integrity_roles')
    }
  }
})
