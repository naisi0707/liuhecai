import { ofetch, type FetchOptions } from 'ofetch'
import type { Result } from '../types/result'

export class ApiError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

export type RequestOptions = {
  method?: string
  body?: unknown
  query?: Record<string, unknown>
  headers?: Record<string, string>
  baseURL?: string
  token?: string | null
}

let defaultBaseURL = ''
const TENANT_HOST_KEY = 'liuhecai_tenant_host'
type RequestErrorHandler = (error: unknown) => void
let requestErrorHandler: RequestErrorHandler | null = null
type HostResolver = () => string
let forwardedHostResolver: HostResolver | null = null

export function setApiBaseURL(baseURL: string) {
  defaultBaseURL = baseURL
}

/** Nuxt SSR 可注入：从请求 Host / X-Forwarded-Host 解析租户 */
export function setForwardedHostResolver(resolver: HostResolver | null) {
  forwardedHostResolver = resolver
}

/** 前台可挂全局失败钩子（如白屏）；admin/agent 可不设 */
export function setRequestErrorHandler(handler: RequestErrorHandler | null) {
  requestErrorHandler = handler
}

function notifyRequestError(error: unknown) {
  try {
    requestErrorHandler?.(error)
  } catch {
    // 钩子失败不影响原错误抛出
  }
}

/** 本地跨站演示：?host=zzws.local 写入后整站请求带该 Host */
export function setTenantHostOverride(host: string | null) {
  if (typeof window === 'undefined') return
  const value = (host || '').trim().toLowerCase()
  if (!value) {
    sessionStorage.removeItem(TENANT_HOST_KEY)
    return
  }
  sessionStorage.setItem(TENANT_HOST_KEY, value)
}

export function getTenantHostOverride(): string | null {
  if (typeof window === 'undefined') return null
  return sessionStorage.getItem(TENANT_HOST_KEY)
}

function resolveForwardedHost(): string {
  if (forwardedHostResolver) {
    const resolved = (forwardedHostResolver() || '').trim().toLowerCase()
    if (resolved) return resolved
  }
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search)
    const qHost = params.get('host')
    if (qHost) {
      setTenantHostOverride(qHost)
    }
    const override = getTenantHostOverride()
    if (override) return override

    const host = window.location.hostname
    return host === '127.0.0.1' ? 'localhost' : host
  }
  return 'localhost'
}

/** 给绕过 request() 的 fetch 复用：补齐 X-Forwarded-Host（及可选 Authorization） */
export function buildApiHeaders(extra: Record<string, string> = {}): Record<string, string> {
  const headers: Record<string, string> = { ...extra }
  if (!headers['X-Forwarded-Host']) {
    headers['X-Forwarded-Host'] = resolveForwardedHost()
  }
  return headers
}

export async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const headers = buildApiHeaders({ ...(options.headers || {}) })
  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`
  }

  const fetchOptions: FetchOptions<'json'> = {
    baseURL: options.baseURL || defaultBaseURL,
    method: (options.method || 'GET') as FetchOptions['method'],
    body: options.body as FetchOptions['body'],
    query: options.query,
    headers,
  }

  try {
    const res = await ofetch<Result<T>>(url, fetchOptions)

    if (res.code !== 0) {
      const err = new ApiError(res.code, res.message || '请求失败')
      notifyRequestError(err)
      throw err
    }

    return res.data
  } catch (e: unknown) {
    if (!(e instanceof ApiError)) {
      notifyRequestError(e)
    }
    throw e
  }
}
