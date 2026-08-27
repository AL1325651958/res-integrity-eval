import request from '@/utils/request'
import type { AnyObj, PageParams, PageResult } from '@/types'

/** GET /achievement/page 成果分页（本人成果或按数据范围） */
export function pageAchievements(params: PageParams) {
  return request.get<PageResult<AnyObj>>('/achievement/page', { params })
}

/** GET /achievement/{id} 成果详情 */
export function getAchievement(id: number | string) {
  return request.get<AnyObj>(`/achievement/${id}`)
}

/** POST /achievement 新增成果（草稿） */
export function createAchievement(data: AnyObj) {
  return request.post<AnyObj>('/achievement', data)
}

/** PUT /achievement/{id} 编辑草稿 */
export function updateAchievement(id: number | string, data: AnyObj) {
  return request.put<AnyObj>(`/achievement/${id}`, data)
}

/** DELETE /achievement/{id} 删除草稿/撤销未入库成果 */
export function deleteAchievement(id: number | string) {
  return request.delete<void>(`/achievement/${id}`)
}

/** POST /achievement/{id}/submit 提交审核（草稿→待科室初审） */
export function submitAchievement(id: number | string) {
  return request.post<void>(`/achievement/${id}/submit`)
}

/** POST /achievement/{id}/audit 审核，体 { auditType, opinion } */
export function auditAchievement(id: number | string, data: AnyObj) {
  return request.post<void>(`/achievement/${id}/audit`, data)
}

/** GET /achievement/audit/page 待审列表 */
export function auditPage(params: PageParams) {
  return request.get<PageResult<AnyObj>>('/achievement/audit/page', { params })
}

/** POST /achievement/{id}/invalidate 作废（科研科），回收计分并触发重算 */
export function invalidateAchievement(id: number | string) {
  return request.post<void>(`/achievement/${id}/invalidate`)
}

/** GET /achievement/export 导出 Excel */
export function exportAchievements(params: PageParams) {
  return request.get<AnyObj>('/achievement/export', { params })
}
