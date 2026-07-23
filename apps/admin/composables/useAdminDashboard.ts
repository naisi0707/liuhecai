import type { AdminDashboardVO } from '@liuhecai/shared'

export function useAdminDashboard() {
  const { api } = useAdminAuth()

  async function fetchDashboard(days = 7) {
    return api<AdminDashboardVO>(`/api/admin/dashboard?days=${days}`)
  }

  return { fetchDashboard }
}
