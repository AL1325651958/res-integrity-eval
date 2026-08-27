import request, { download } from '@/utils/request'
import type { AnyObj } from '@/types'

/** POST /import/achievement 成果批量导入（multipart file） */
export function importAchievements(formData: FormData) {
  return request.post<AnyObj>('/import/achievement', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** GET /import/template 下载导入模板 */
export function downloadTemplate() {
  return download('/import/template', undefined, '成果导入模板.xlsx')
}
