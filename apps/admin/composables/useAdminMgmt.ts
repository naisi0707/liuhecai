import type {
  AdminAgentDetail,
  AdminAgentListItem,
  AdminAgentPage,
  AdminUserDetail,
  AdminUserPage,
  PasswordResetResult,
  PageResult,
  UserCoinLogItem,
  UserOrderItem,
} from '@liuhecai/shared'

export function useAdminMgmt() {
  const { api, authHeaders } = useAdminAuth()
  const config = useRuntimeConfig()
  const apiBase = config.public.apiBase as string

  function qs(params: Record<string, string | number | undefined>) {
    const q = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== '' && v !== null) q.set(k, String(v))
    })
    return q.toString()
  }

  function pageAgents(params: Record<string, string | number | undefined>) {
    return api<AdminAgentPage>(`/api/admin/agents?${qs(params)}`)
  }

  function getAgent(id: string | number) {
    return api<AdminAgentDetail>(`/api/admin/agents/${id}`)
  }

  function setAgentEnabled(id: string | number, enabled: number) {
    return api<AdminAgentListItem>(`/api/admin/agents/${id}/enabled`, {
      method: 'PUT',
      body: { enabled },
    })
  }

  function resetAgentPassword(id: string | number) {
    return api<{ id: number | string; username: string; rawPassword: string }>(
      `/api/admin/agents/${id}/reset-password`,
      { method: 'POST' },
    )
  }

  function forceAgentLogout(id: string | number) {
    return api<null>(`/api/admin/agents/${id}/force-logout`, { method: 'POST' })
  }

  function pageUsers(params: Record<string, string | number | undefined>) {
    return api<AdminUserPage>(`/api/admin/users?${qs(params)}`)
  }

  function getUser(id: string | number) {
    return api<AdminUserDetail>(`/api/admin/users/${id}`)
  }

  function setUserEnabled(id: string | number, enabled: number) {
    return api<AdminUserDetail>(`/api/admin/users/${id}/enabled`, {
      method: 'PUT',
      body: { enabled },
    })
  }

  function resetUserPassword(id: string | number) {
    return api<PasswordResetResult>(`/api/admin/users/${id}/reset-password`, { method: 'POST' })
  }

  function forceUserLogout(id: string | number) {
    return api<null>(`/api/admin/users/${id}/force-logout`, { method: 'POST' })
  }

  function batchUserEnabled(ids: Array<string | number>, enabled: number) {
    return api<null>('/api/admin/users/batch-enabled', {
      method: 'POST',
      body: { ids, enabled },
    })
  }

  function userCoinLogs(id: string | number, page = 1, size = 20) {
    return api<PageResult<UserCoinLogItem>>(`/api/admin/users/${id}/coin-logs?page=${page}&size=${size}`)
  }

  function userOrders(id: string | number, page = 1, size = 20) {
    return api<PageResult<UserOrderItem>>(`/api/admin/users/${id}/orders?page=${page}&size=${size}`)
  }

  function pageAuditLogs(params: Record<string, string | number | undefined>) {
    return api<PageResult<{
      id: number | string
      operatorRealm: string
      operatorId: number | string
      operatorName: string
      tenantId?: number | null
      action: string
      targetType: string
      targetId?: string | null
      detail?: string | null
      createdAt: string
    }>>(`/api/admin/audit-logs?${qs(params)}`)
  }

  async function downloadCsv(path: string, filename: string) {
    const res = await fetch(`${apiBase}${path}`, { headers: { ...authHeaders() } })
    if (!res.ok) throw new Error('导出失败')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  }

  function exportUsers(params: Record<string, string | number | undefined>) {
    return downloadCsv(`/api/admin/users/export?${qs(params)}`, 'users.csv')
  }

  function exportAgents(params: Record<string, string | number | undefined>) {
    return downloadCsv(`/api/admin/agents/export?${qs(params)}`, 'agents.csv')
  }

  return {
    pageAgents,
    getAgent,
    setAgentEnabled,
    resetAgentPassword,
    forceAgentLogout,
    pageUsers,
    getUser,
    setUserEnabled,
    resetUserPassword,
    forceUserLogout,
    batchUserEnabled,
    userCoinLogs,
    userOrders,
    pageAuditLogs,
    exportUsers,
    exportAgents,
  }
}
