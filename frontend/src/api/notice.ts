import request from '@/utils/request'
import type { NoticeItem, PageParams, PageResult } from '@/types'

/** GET /notice/my 我的通知（分页） */
export function myNotices(params: PageParams) {
  return request.get<PageResult<NoticeItem>>('/notice/my', { params })
}

/** PUT /notice/{id}/read 标记已读 */
export function readNotice(id: number | string) {
  return request.put<void>(`/notice/${id}/read`)
}

/** PUT /notice/readAll 全部已读 */
export function readAllNotices() {
  return request.put<void>('/notice/readAll')
}

/** GET /notice/unreadCount 未读通知数 */
export function unreadCount() {
  return request.get<number>('/notice/unreadCount')
}
