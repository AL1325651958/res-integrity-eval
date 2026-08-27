import request from '@/utils/request'
import type { AnyObj, PageParams, PageResult } from '@/types'

/** GET /appeal/page 申诉列表（分页） */
export function pageAppeals(params: PageParams) {
  return request.get<PageResult<AnyObj>>('/appeal/page', { params })
}

/** GET /appeal/{id} 申诉详情 */
export function getAppeal(id: number | string) {
  return request.get<AnyObj>(`/appeal/${id}`)
}

/** POST /appeal 提交申诉，体 { appealType, bizType, bizId, reason } */
export function createAppeal(data: AnyObj) {
  return request.post<AnyObj>('/appeal', data)
}

/** POST /appeal/{id}/review 复核裁定，体 { pass, result } */
export function reviewAppeal(id: number | string, data: AnyObj) {
  return request.post<void>(`/appeal/${id}/review`, data)
}
