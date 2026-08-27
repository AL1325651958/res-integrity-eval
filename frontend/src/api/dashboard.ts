import request from '@/utils/request'
import type {
  DeptDashboardData,
  HospitalDashboardData,
  MyDashboardData
} from '@/types'

/** GET /dashboard/my 个人看板 */
export function myDashboard() {
  return request.get<MyDashboardData>('/dashboard/my')
}

/** GET /dashboard/dept 科室看板 */
export function deptDashboard() {
  return request.get<DeptDashboardData>('/dashboard/dept')
}

/** GET /dashboard/hospital 全院看板 */
export function hospitalDashboard() {
  return request.get<HospitalDashboardData>('/dashboard/hospital')
}
