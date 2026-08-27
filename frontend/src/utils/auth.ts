const TOKEN_KEY = 'integrity_token'

/** 读取 token（localStorage，key: integrity_token） */
export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

/** 保存 token */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 清除 token */
export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}
