import request from '@/utils/request'
import type { AnyObj, PageParams, PageResult } from '@/types'

/** GET /risk/log/page 预警记录（分页） */
export function riskLogPage(params: PageParams) {
  return request.get<PageResult<AnyObj>>('/risk/log/page', { params })
}

/** POST /risk/log/{id}/claim 认领预警并转工单（科研科） */
export function claimRiskLog(id: number | string) {
  return request.post<void>(`/risk/log/${id}/claim`)
}

/** GET /risk/check/page 工单列表（分页） */
export function checkPage(params: PageParams) {
  return request.get<PageResult<AnyObj>>('/risk/check/page', { params })
}

/** GET /risk/check/{id} 工单详情 */
export function getCheck(id: number | string) {
  return request.get<AnyObj>(`/risk/check/${id}`)
}

/** POST /risk/check/{id}/claim 认领工单 */
export function claimCheck(id: number | string) {
  return request.post<void>(`/risk/check/${id}/claim`)
}

/** POST /risk/check/{id}/record 处置记录，体 { recordType, content } */
export function addCheckRecord(id: number | string, data: AnyObj) {
  return request.post<void>(`/risk/check/${id}/record`, data)
}

/** POST /risk/check/{id}/confirm 失信认定，体 { violationType, violationLevel, deductScore, ... } */
export function confirmCheck(id: number | string, data: AnyObj) {
  return request.post<void>(`/risk/check/${id}/confirm`, data)
}

/** POST /risk/check/{id}/dismiss 误报撤销 */
export function dismissCheck(id: number | string) {
  return request.post<void>(`/risk/check/${id}/dismiss`)
}

/** POST /risk/check/{id}/publish 发起失信认定公示 */
export function publishCheck(id: number | string) {
  return request.post<void>(`/risk/check/${id}/publish`)
}

/** POST /risk/check/{id}/effect 公示结束→扣分生效 */
export function effectCheck(id: number | string) {
  return request.post<void>(`/risk/check/${id}/effect`)
}

/** POST /risk/check/{id}/archive 归档 */
export function archiveCheck(id: number | string) {
  return request.post<void>(`/risk/check/${id}/archive`)
}

/** GET /risk/violation/page 违规记录（分页） */
export function violationPage(params: PageParams) {
  return request.get<PageResult<AnyObj>>('/risk/violation/page', { params })
}

/** POST /risk/violation/{id}/reform 提交整改，体 { result } */
export function reformViolation(id: number | string, data: AnyObj) {
  return request.post<void>(`/risk/violation/${id}/reform`, data)
}

/** POST /risk/violation/{id}/reformCheck 整改验收，体 { pass, comment } */
export function reformCheckViolation(id: number | string, data: AnyObj) {
  return request.post<void>(`/risk/violation/${id}/reformCheck`, data)
}
