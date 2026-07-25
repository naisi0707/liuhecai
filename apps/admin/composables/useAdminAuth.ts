import { buildApiHeaders, request, setApiBaseURL } from '@liuhecai/shared'

export function useAdminAuth() {
  const config = useRuntimeConfig()
  const serverBase = (config.apiBase as string) || ''
  const publicBase = (config.public.apiBase as string) || ''
  setApiBaseURL(import.meta.server ? (serverBase || publicBase) : publicBase)

  const token = useState<string>('admin_token', () => '')

  if (import.meta.client && !token.value) {
    token.value = localStorage.getItem('admin_token') || ''
  }

  function hydrate() {
    if (import.meta.client) {
      token.value = localStorage.getItem('admin_token') || ''
    }
  }

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
    const data = await request<{ token: string }>('/api/admin/auth/login', {
      method: 'POST',
      body: { username, password },
    })
    token.value = data.token
    if (import.meta.client) localStorage.setItem('admin_token', data.token)
    return data
  }

  function logout() {
    token.value = ''
    if (import.meta.client) localStorage.removeItem('admin_token')
  }

  async function changePassword(oldPassword: string, newPassword: string) {
    await api<null>('/api/admin/auth/password', {
      method: 'PUT',
      body: { oldPassword, newPassword },
    })
  }

  return { token, hydrate, authHeaders, api, login, logout, changePassword }
}
