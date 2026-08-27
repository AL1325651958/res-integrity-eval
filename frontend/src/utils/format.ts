/**
 * 格式化日期
 * @param value 日期值（时间戳 / 日期字符串 / Date）
 * @param pattern 格式模板，默认 yyyy-MM-dd HH:mm:ss，也支持 yyyy-MM-dd
 * @returns 格式化后的字符串，无效输入返回空字符串
 */
export function formatDate(
  value?: string | number | Date | null,
  pattern = 'yyyy-MM-dd HH:mm:ss'
): string {
  if (value === null || value === undefined || value === '') return ''
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  const map: Record<string, string> = {
    yyyy: String(date.getFullYear()),
    MM: pad(date.getMonth() + 1),
    dd: pad(date.getDate()),
    HH: pad(date.getHours()),
    mm: pad(date.getMinutes()),
    ss: pad(date.getSeconds())
  }
  return pattern.replace(/yyyy|MM|dd|HH|mm|ss/g, (key) => map[key])
}
