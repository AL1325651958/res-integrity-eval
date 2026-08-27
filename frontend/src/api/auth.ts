import request from '@/utils/request'
import type { LoginParams, LoginResult, UserInfo } from '@/types'

/** POST /auth/login 登录，返回 { token, user } */
export function login(data: LoginParams) {
  return request.post<LoginResult>('/auth/login', data)
}

/** GET /auth/info 当前用户信息 */
export function getInfo() {
  return request.get<UserInfo>('/auth/info')
}

/** POST /auth/logout 退出登录 */
export function logout() {
  return request.post<void>('/auth/logout')
}

/** PUT /auth/password 修改密码，体 { oldPassword, newPassword } */
export function updatePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put<void>('/auth/password', data)
}
