import type {
  AgentDashboardVO,
  AgentUserDetail,
  AgentUserPage,
  PageResult,
  PasswordResetResult,
  UserCoinLogItem,
  UserOrderItem,
} from '@liuhecai/shared'

export function useAgentMgmt() {
  const { api, authHeaders } = useAgentAuth()
  const config = useRuntimeConfig()
  const apiBase = config.public.apiBase as string

  function fetchDashboard(days = 7) {
    return api<AgentDashboardVO>(`/api/agent/dashboard?days=${days}`)
  }

  function pageUsers(params: Record<string, string | number | undefined>) {
    const q = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== '' && v !== null) q.set(k, String(v))
    })
    return api<AgentUserPage>(`/api/agent/users?${q}`)
  }

  function getUser(id: string | number) {
    return api<AgentUserDetail>(`/api/agent/users/${id}`)
  }

  function setUserEnabled(id: string | number, enabled: number) {
    return api<AgentUserDetail>(`/api/agent/users/${id}/enabled`, {
      method: 'PUT',
      body: { enabled },
    })
  }

  function resetUserPassword(id: string | number) {
    return api<PasswordResetResult>(`/api/agent/users/${id}/reset-password`, { method: 'POST' })
  }

  function forceUserLogout(id: string | number) {
    return api<null>(`/api/agent/users/${id}/force-logout`, { method: 'POST' })
  }

  function batchUserEnabled(ids: Array<string | number>, enabled: number) {
    return api<null>('/api/agent/users/batch-enabled', {
      method: 'POST',
      body: { ids, enabled },
    })
  }

  function adjustCoins(id: string | number, amount: number, remark?: string) {
    return api<{ coinBalance: number }>(`/api/agent/users/${id}/coins`, {
      method: 'POST',
      body: { amount, remark },
    })
  }

  function userCoinLogs(id: string | number, page = 1, size = 20) {
    return api<PageResult<UserCoinLogItem>>(`/api/agent/users/${id}/coin-logs?page=${page}&size=${size}`)
  }

  function userOrders(id: string | number, page = 1, size = 20) {
    return api<PageResult<UserOrderItem>>(`/api/agent/users/${id}/orders?page=${page}&size=${size}`)
  }

  async function exportUsers(params: Record<string, string | number | undefined>) {
    const q = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== '' && v !== null) q.set(k, String(v))
    })
    const res = await fetch(`${apiBase}/api/agent/users/export?${q}`, {
      headers: authHeaders(),
    })
    if (!res.ok) throw new Error('导出失败')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'users.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  return {
    fetchDashboard,
    pageUsers,
    getUser,
    setUserEnabled,
    resetUserPassword,
    forceUserLogout,
    batchUserEnabled,
    adjustCoins,
    userCoinLogs,
    userOrders,
    exportUsers,
  }
}
