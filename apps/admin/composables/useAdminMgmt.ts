import {
  consumeUnauthorizedResult,
  type AdminAgentDetail,
  type AdminAgentListItem,
  type AdminAgentPage,
  type AdminUserDetail,
  type AdminUserPage,
  type PasswordResetResult,
  type PageResult,
  type UserCoinLogItem,
  type UserOrderItem,
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

  function createAgent(tenantId: string | number, username: string) {
    return api<{ id: number | string; username: string; rawPassword: string; isPrimary?: number }>(
      `/api/admin/tenants/${tenantId}/agents`,
      { method: 'POST', body: { username } },
    )
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

  function softDeleteAgent(id: string | number) {
    return api<null>(`/api/admin/agents/${id}/delete`, { method: 'POST' })
  }

  function batchAgentEnabled(ids: Array<string | number>, enabled: number) {
    return api<null>('/api/admin/agents/batch-enabled', {
      method: 'POST',
      body: { ids, enabled },
    })
  }

  function pageUsers(params: Record<string, string | number | undefined>) {
    return api<AdminUserPage>(`/api/admin/users?${qs(params)}`)
  }

  function getUser(id: string | number) {
    return api<AdminUserDetail>(`/api/admin/users/${id}`)
  }

  function createUser(tenantId: string | number, username: string) {
    return api<PasswordResetResult>('/api/admin/users', {
      method: 'POST',
      body: { tenantId, username },
    })
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

  function softDeleteUser(id: string | number) {
    return api<null>(`/api/admin/users/${id}/delete`, { method: 'POST' })
  }

  function adjustUserCoins(id: string | number, amount: number, remark?: string) {
    return api<{ coinBalance: number }>(`/api/admin/users/${id}/coins`, {
      method: 'POST',
      body: { amount, remark },
    })
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

  function updateTenant(id: string | number, body: { name: string; announcement?: string }) {
    return api(`/api/admin/tenants/${id}`, { method: 'PUT', body })
  }

  function setPrimaryAgent(tenantId: string | number, agentId: string | number) {
    return api(`/api/admin/tenants/${tenantId}/primary-agent`, {
      method: 'PUT',
      body: { agentId },
    })
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
    const ct = res.headers.get('content-type') || ''
    if (ct.includes('application/json')) {
      const payload = await res.json()
      if (consumeUnauthorizedResult(payload)) throw new Error(payload?.message || '未登录')
      throw new Error(payload?.message || '导出失败')
    }
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
    createAgent,
    setAgentEnabled,
    resetAgentPassword,
    forceAgentLogout,
    softDeleteAgent,
    batchAgentEnabled,
    pageUsers,
    getUser,
    createUser,
    setUserEnabled,
    resetUserPassword,
    forceUserLogout,
    softDeleteUser,
    adjustUserCoins,
    batchUserEnabled,
    userCoinLogs,
    userOrders,
    updateTenant,
    setPrimaryAgent,
    pageAuditLogs,
    exportUsers,
    exportAgents,
  }
}
