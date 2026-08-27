import axios from 'axios'
import type { AxiosError, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types'
import { getToken, removeToken } from './auth'

/**
 * 请求实例类型：响应拦截器已解包统一响应体（code===0 时直接返回 data），
 * 因此 get/post/put/delete 的泛型即业务数据类型。
 */
export interface RequestInstance {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api/v1',
  timeout: 30000
})

// 请求拦截：注入 Authorization Bearer token
service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** 401 处理：清除 token 并跳转登录页 */
function handleUnauthorized() {
  removeToken()
  if (!location.pathname.startsWith('/login')) {
    location.href = '/login'
  }
}

// 响应拦截：统一处理 code / 401 / 错误提示
service.interceptors.response.use(
  (response) => {
    // blob 响应（文件下载）直接返回原始数据
    if (response.config.responseType === 'blob') {
      return response.data as AxiosResponse
    }
    const res = response.data as Result
    if (res.code === 0) {
      return res.data as AxiosResponse
    }
    if (res.code === 401) {
      handleUnauthorized()
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error: AxiosError<Result>) => {
    const status = error.response?.status
    if (status === 401) {
      handleUnauthorized()
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

const request = service as unknown as RequestInstance

/**
 * 下载辅助：以 responseType=blob 发起 GET 并触发浏览器下载。
 * 用于 /file/download、/integrity/export/pdf 等流式接口。
 */
export function download(url: string, params?: Record<string, unknown>, fileName = '下载文件') {
  return request.get<Blob>(url, { params, responseType: 'blob' }).then((blob) => {
    const objectUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = objectUrl
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(objectUrl)
  })
}

export default request
