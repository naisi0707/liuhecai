import { buildApiHeaders, request, setApiBaseURL } from '@liuhecai/shared'

export function useAgentAuth() {
  const config = useRuntimeConfig()
  const serverBase = (config.apiBase as string) || ''
  const publicBase = (config.public.apiBase as string) || ''
  setApiBaseURL(import.meta.server ? (serverBase || publicBase) : publicBase)

  const token = useState<string>('agent_token', () => '')

  if (import.meta.client && !token.value) {
    token.value = localStorage.getItem('agent_token') || ''
  }

  function hydrate() {
    if (import.meta.client) {
      token.value = localStorage.getItem('agent_token') || ''
    }
  }

  /** 含 Authorization + X-Forwarded-Host，供 request 与原生 fetch/上传复用 */
  function authHeaders(): Record<string, string> {
    return buildApiHeaders(token.value ? { Authorization: `Bearer ${token.value}` } : {})
  }

  async function api<T>(url: string, options: Record<string, unknown> = {}) {
    const headers = {
      ...((options.headers as Record<string, string>) || {}),
      ...authHeaders(),
    }
    return request<T>(url, { ...options, headers })
  }

  async function login(username: string, password: string) {
    const data = await request<{ token: string }>('/api/agent/auth/login', {
      method: 'POST',
      body: { username, password },
    })
    token.value = data.token
    if (import.meta.client) localStorage.setItem('agent_token', data.token)
    return data
  }

  function logout() {
    token.value = ''
    if (import.meta.client) localStorage.removeItem('agent_token')
  }

  return { token, hydrate, authHeaders, api, login, logout }
}
