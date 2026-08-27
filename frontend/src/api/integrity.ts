import request, { download } from '@/utils/request'
import type { AnyObj, PageParams, PageResult } from '@/types'

/** GET /integrity/my?year= 我的年度评价 */
export function myIntegrity(year?: number | string) {
  return request.get<AnyObj>('/integrity/my', { params: { year } })
}

/** GET /integrity/my/detail?year= 我的年度评价明细 */
export function myIntegrityDetail(year?: number | string) {
  return request.get<AnyObj[]>('/integrity/my/detail', { params: { year } })
}

/** GET /integrity/page 管理端诚信评价列表（分页） */
export function integrityPage(params: PageParams) {
  return request.get<PageResult<AnyObj>>('/integrity/page', { params })
}

/** POST /integrity/calc?year= 触发年度评价计算 */
export function calcIntegrity(year?: number | string) {
  return request.post<void>('/integrity/calc', undefined, { params: { year } })
}

/** GET /integrity/export/pdf?userId=&year= 档案 PDF 导出（blob） */
export function exportPdf(params: AnyObj) {
  return download('/integrity/export/pdf', params, '诚信档案.pdf')
}

/** POST /integrity/{id}/publicity 发起评价结果公示，体见契约 publicity */
export function publicityIntegrity(id: number | string, data: AnyObj) {
  return request.post<void>(`/integrity/${id}/publicity`, data)
}

/** GET /integrity/user/{userId}/detail?year= 某用户年度评价明细（管理端） */
export function userIntegrityDetail(userId: number | string, year?: number | string) {
  return request.get<AnyObj>(`/integrity/user/${userId}/detail`, { params: { year } })
}
