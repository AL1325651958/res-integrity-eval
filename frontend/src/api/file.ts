import request, { download } from '@/utils/request'

export interface UploadResult {
  fileId: number
  fileName: string
  fileSize: number
}

/** POST /file/upload 上传文件（multipart：file, bizType, bizId） */
export function uploadFile(formData: FormData) {
  return request.post<UploadResult>('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** GET /file/download?id= 下载文件（blob 流，自动触发浏览器下载） */
export function downloadFile(id: number | string) {
  return download('/file/download', { id }, '文件下载')
}

/** DELETE /file/{id} 删除文件 */
export function deleteFile(id: number | string) {
  return request.delete<void>(`/file/${id}`)
}
