import type { PageResult } from '../result'

export interface AdminAgentListItem {
  id: number | string
  tenantId: number | string
  tenantName: string
  username: string
  enabled: number
  createdAt: string
  userCount?: number
  rechargeAmount7d?: number
}

export interface AdminAgentTrends {
  dates: string[]
  users: number[]
  orders: number[]
  rechargeAmount: number[]
}

export interface AdminAgentDetail extends AdminAgentListItem {
  userTotal?: number
  userToday?: number
  topicTotal?: number
  topicPending?: number
  rechargePending?: number
  rechargeApprovedAmountToday?: number
  orderCountToday?: number
  orderAmountToday?: number
  orderAmountTotal?: number
  trends?: AdminAgentTrends
}

export interface AdminUserListItem {
  id: number | string
  tenantId: number
  tenantName: string
  username: string
  coinBalance: number
  enabled: number
  createdAt: string
}

export interface AdminUserDetail extends AdminUserListItem {
  updatedAt?: string
}

export interface UserCoinLogItem {
  id: number | string
  changeAmount: number
  balanceAfter: number
  bizType: string
  bizId?: string | null
  remark?: string | null
  createdAt: string
}

export interface UserOrderItem {
  id: number | string
  topicId: number | string
  topicTitle?: string | null
  price: number
  createdAt: string
}

export interface PasswordResetResult {
  id: number | string
  username: string
  rawPassword: string
}

export interface AgentUserListItem {
  id: number | string
  username: string
  coinBalance: number
  enabled: number
  createdAt: string
}

export interface AgentUserDetail extends AgentUserListItem {
  updatedAt?: string
}

export interface AgentDashboardKpis {
  userTotal: number
  userToday: number
  topicPending: number
  rechargePending: number
  rechargeApprovedAmountToday: number
  orderCountToday: number
  orderAmountToday: number
}

export interface AgentDashboardTrends {
  dates: string[]
  users: number[]
  orders: number[]
  rechargeAmount: number[]
}

export interface AgentDashboardVO {
  days: number
  kpis: AgentDashboardKpis
  trends: AgentDashboardTrends
}

export type AdminAgentPage = PageResult<AdminAgentListItem>
export type AdminUserPage = PageResult<AdminUserListItem>
export type AgentUserPage = PageResult<AgentUserListItem>
