import { request } from '@liuhecai/shared'

export function useAuth() {
  const token = useState('user_token', () => '')
  const coinBalance = useState<number | null>('coin_balance', () => null)
  const username = useState('auth_username', () => '')
  const { errorMsg } = useTenant()

  function authHeaders(): Record<string, string> {
    return token.value ? { Authorization: `Bearer ${token.value}` } : {}
  }

  async function refreshProfile() {
    if (!token.value) return
    const me = await request<{ coinBalance: number; username?: string }>('/api/user/me', {
      headers: authHeaders(),
    })
    coinBalance.value = me.coinBalance
    if (me.username) username.value = me.username
  }

  async function login(name: string, password: string) {
    errorMsg.value = ''
    const data = await request<{ token: string }>('/api/user/auth/login', {
      method: 'POST',
      body: { username: name, password },
    })
    token.value = data.token
    if (import.meta.client) localStorage.setItem('user_token', data.token)
    await refreshProfile()
  }

  async function register(name: string, password: string) {
    errorMsg.value = ''
    const data = await request<{ token: string }>('/api/user/auth/register', {
      method: 'POST',
      body: { username: name, password },
    })
    token.value = data.token
    if (import.meta.client) localStorage.setItem('user_token', data.token)
    await refreshProfile()
  }

  function logout() {
    token.value = ''
    coinBalance.value = null
    username.value = ''
    if (import.meta.client) localStorage.removeItem('user_token')
  }

  function hydrateFromStorage() {
    if (!import.meta.client) return
    token.value = localStorage.getItem('user_token') || ''
  }

  return {
    token,
    coinBalance,
    username,
    authHeaders,
    refreshProfile,
    login,
    register,
    logout,
    hydrateFromStorage,
  }
}
