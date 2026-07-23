export interface AdminDashboardKpis {
  tenantTotal: number
  tenantEnabled: number
  tenantDisabled: number
  domainTotal: number
  agentTotal: number
  agentEnabled: number
  userTotal: number
  userToday: number
  topicTotal: number
  topicPending: number
  topicPublished: number
  rechargePending: number
  rechargeApprovedAmountToday: number
  orderCountToday: number
  orderAmountToday: number
  coinGrantToday: number
  coinRechargeToday: number
  coinPurchaseToday: number
}

export interface AdminDashboardTrends {
  dates: string[]
  users: number[]
  orders: number[]
  rechargeAmount: number[]
  tenants: number[]
}

export interface AdminNameCount {
  name: string
  count: number
}

export interface AdminAgentRow {
  id: number
  tenantId: number
  tenantName: string
  username: string
  enabled: number
  createdAt: string
}

export interface AdminTenantRank {
  tenantId: number
  tenantName: string
  status: number
  userCount: number
  orderCount: number
  primaryHost?: string | null
}

export interface AdminActivityItem {
  type: 'COIN' | 'RECHARGE' | 'TOPIC' | string
  tenantName: string
  bizType?: string | null
  status?: number | null
  amount?: number | null
  userId?: number | null
  topicTitle?: string | null
  createdAt: string
}

export interface AdminDrawFreshness {
  lotteryType: string
  issueNo: string
  drawTime?: string | null
  updatedAt?: string | null
  source?: string | null
}

export interface AdminRecentTenant {
  id: number
  name: string
  status: number
  primaryHost?: string | null
  createdAt: string
}

export interface AdminDashboardVO {
  days: number
  kpis: AdminDashboardKpis
  trends: AdminDashboardTrends
  topicStatus: AdminNameCount[]
  lotteryTopics: AdminNameCount[]
  agents: AdminAgentRow[]
  tenantRanks: AdminTenantRank[]
  activities: AdminActivityItem[]
  draws: AdminDrawFreshness[]
  recentTenants: AdminRecentTenant[]
}
