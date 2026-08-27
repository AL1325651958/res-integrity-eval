import request from '@/utils/request'
import type { AnyObj, MenuItem, PageParams, PageResult } from '@/types'

/* ---------------- 用户 ---------------- */

/** GET /system/user/page 用户分页 */
export function userPage(params: PageParams) {
  return request.get<PageResult<AnyObj>>('/system/user/page', { params })
}

/** POST /system/user 新增用户（含 roleIds[]） */
export function createUser(data: AnyObj) {
  return request.post<AnyObj>('/system/user', data)
}

/** PUT /system/user/{id} 编辑用户 */
export function updateUser(id: number | string, data: AnyObj) {
  return request.put<AnyObj>(`/system/user/${id}`, data)
}

/** DELETE /system/user/{id} 删除用户 */
export function deleteUser(id: number | string) {
  return request.delete<void>(`/system/user/${id}`)
}

/** PUT /system/user/{id}/status 启停用户，体 { status } */
export function updateUserStatus(id: number | string, status: string | number) {
  return request.put<void>(`/system/user/${id}/status`, { status })
}

/** PUT /system/user/{id}/resetPwd 重置密码 */
export function resetUserPwd(id: number | string) {
  return request.put<void>(`/system/user/${id}/resetPwd`)
}

/* ---------------- 科室 ---------------- */

/** GET /system/dept/tree 科室树 */
export function deptTree() {
  return request.get<AnyObj[]>('/system/dept/tree')
}

/** POST/PUT /system/dept 新增或编辑科室（有 id 走 PUT） */
export function saveDept(data: AnyObj) {
  const id = data.id
  if (id) return request.put<void>(`/system/dept/${id}`, data)
  return request.post<void>('/system/dept', data)
}

/** DELETE /system/dept/{id} 删除科室 */
export function deleteDept(id: number | string) {
  return request.delete<void>(`/system/dept/${id}`)
}

/* ---------------- 角色 ---------------- */

/** GET /system/role/list 角色列表 */
export function roleList() {
  return request.get<AnyObj[]>('/system/role/list')
}

/** POST/PUT /system/role 新增或编辑角色 */
export function saveRole(data: AnyObj) {
  const id = data.id
  if (id) return request.put<void>(`/system/role/${id}`, data)
  return request.post<void>('/system/role', data)
}

/** DELETE /system/role/{id} 删除角色 */
export function deleteRole(id: number | string) {
  return request.delete<void>(`/system/role/${id}`)
}

/** PUT /system/role/{id}/menus 保存角色菜单，体 { menuIds } */
export function saveRoleMenus(id: number | string, menuIds: (number | string)[]) {
  return request.put<void>(`/system/role/${id}/menus`, { menuIds })
}

/* ---------------- 菜单 ---------------- */

/** GET /system/menu/tree 当前用户菜单树 */
export function menuTree() {
  return request.get<MenuItem[]>('/system/menu/tree')
}

/** POST/PUT /system/menu 新增或编辑菜单 */
export function saveMenu(data: AnyObj) {
  const id = data.id
  if (id) return request.put<void>(`/system/menu/${id}`, data)
  return request.post<void>('/system/menu', data)
}

/** DELETE /system/menu/{id} 删除菜单 */
export function deleteMenu(id: number | string) {
  return request.delete<void>(`/system/menu/${id}`)
}

/* ---------------- 字典 ---------------- */

/** GET /system/dict/type/list 字典类型列表 */
export function dictTypeList() {
  return request.get<AnyObj[]>('/system/dict/type/list')
}

/** GET /system/dict/data/{dictType} 字典数据列表 */
export function dictDataList(dictType: string) {
  return request.get<AnyObj[]>(`/system/dict/data/${dictType}`)
}

/** POST/PUT /system/dict/type 新增或编辑字典类型 */
export function saveDictType(data: AnyObj) {
  const id = data.id
  if (id) return request.put<void>(`/system/dict/type/${id}`, data)
  return request.post<void>('/system/dict/type', data)
}

/** DELETE /system/dict/type/{id} 删除字典类型 */
export function deleteDictType(id: number | string) {
  return request.delete<void>(`/system/dict/type/${id}`)
}

/** POST/PUT /system/dict/data 新增或编辑字典数据 */
export function saveDictData(data: AnyObj) {
  const id = data.id
  if (id) return request.put<void>(`/system/dict/data/${id}`, data)
  return request.post<void>('/system/dict/data', data)
}

/** DELETE /system/dict/data/{id} 删除字典数据 */
export function deleteDictData(id: number | string) {
  return request.delete<void>(`/system/dict/data/${id}`)
}

/* ---------------- 规则 ---------------- */

/** GET /system/rule/page 计分规则分页 */
export function rulePage(params: PageParams) {
  return request.get<PageResult<AnyObj>>('/system/rule/page', { params })
}

/** POST/PUT /system/rule 新增或编辑规则 */
export function saveRule(data: AnyObj) {
  const id = data.id
  if (id) return request.put<void>(`/system/rule/${id}`, data)
  return request.post<void>('/system/rule', data)
}

/** DELETE /system/rule/{id} 删除规则 */
export function deleteRule(id: number | string) {
  return request.delete<void>(`/system/rule/${id}`)
}

/** GET /system/rule/coeff/list 系数列表 */
export function coeffList() {
  return request.get<AnyObj[]>('/system/rule/coeff/list')
}

/** GET /system/rule/level/list 等级列表 */
export function levelList() {
  return request.get<AnyObj[]>('/system/rule/level/list')
}

/* ---------------- 黑名单 ---------------- */

/** GET /system/blacklist/page 黑名单分页 */
export function blacklistPage(params: PageParams) {
  return request.get<PageResult<AnyObj>>('/system/blacklist/page', { params })
}

/** POST/PUT /system/blacklist 新增或编辑黑名单 */
export function saveBlacklist(data: AnyObj) {
  const id = data.id
  if (id) return request.put<void>(`/system/blacklist/${id}`, data)
  return request.post<void>('/system/blacklist', data)
}

/** DELETE /system/blacklist/{id} 删除黑名单 */
export function deleteBlacklist(id: number | string) {
  return request.delete<void>(`/system/blacklist/${id}`)
}

/* ---------------- 日志 ---------------- */

/** GET /system/log/page 操作/登录日志分页 */
export function logPage(params: PageParams) {
  return request.get<PageResult<AnyObj>>('/system/log/page', { params })
}
