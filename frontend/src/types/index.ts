/** 通用响应体：{ code, msg, data }，code=0 表示成功，非 0 为业务错误（msg 直接展示） */
export interface Result<T = any> {
  code: number
  msg: string
  data: T
}

/** 分页响应体 */
export interface PageResult<T = any> {
  total: number
  list: T[]
}

/** 分页请求参数：pageNum 默认 1，pageSize 默认 10 */
export interface PageParams {
  pageNum?: number
  pageSize?: number
  [key: string]: unknown
}

/** 宽松对象，用于增删改请求体 */
export type AnyObj = Record<string, any>

/** 登录参数 */
export interface LoginParams {
  username: string
  password: string
}

/** 登录结果 */
export interface LoginResult {
  token: string
  user: UserInfo
}

/** 当前用户信息 */
export interface UserInfo {
  userId: number
  username: string
  realName: string
  deptId: number
  deptName: string
  title: string
  roles: string[]
  perms: string[]
}

/** 菜单项（menuType：M-目录 C-菜单 F-按钮） */
export interface MenuItem {
  menuId: number
  parentId: number
  menuName: string
  menuType: string
  path: string
  component: string
  icon: string
  children?: MenuItem[]
}

/** 通知项 */
export interface NoticeItem {
  id: number
  title: string
  content: string
  isRead: number | boolean
  createTime: string
  [key: string]: unknown
}

/** 统计项（图表数据，兼容 {name,value} 与 {type,count} 两种结构） */
export interface StatItem {
  name?: string
  value?: number
  type?: string
  count?: number
  level?: string
  [key: string]: unknown
}

/** 扣分明细项 */
export interface DeductItem {
  itemName?: string
  score?: number
  createTime?: string
  [key: string]: unknown
}

/** 风险人员/风险记录项 */
export interface RiskUserItem {
  realName?: string
  deptName?: string
  riskType?: string
  riskLevel?: string
  createTime?: string
  [key: string]: unknown
}

/** 个人看板数据：/dashboard/my */
export interface MyDashboardData {
  yearScore?: number
  level?: string
  achStats?: Record<string, number>
  deductList?: DeductItem[]
  pendingCount?: number
  riskCount?: number
  [key: string]: unknown
}

/** 科室看板数据：/dashboard/dept */
export interface DeptDashboardData {
  achTotal?: number
  typeDist?: StatItem[]
  levelDist?: StatItem[]
  riskUsers?: RiskUserItem[]
  unAudited?: number
  [key: string]: unknown
}

/** 全院看板数据：/dashboard/hospital */
export interface HospitalDashboardData {
  summary?: { users?: number; achTotal?: number; avgScore?: number }
  deptRank?: AnyObj[]
  riskTypeStat?: StatItem[]
  yearTrend?: StatItem[]
  seriousList?: AnyObj[]
  pendingChecks?: number
  [key: string]: unknown
}
