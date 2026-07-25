export type { Result, PageResult } from './result'
export { isOk } from './result'
export type { TenantPublicVO, TenantDirectoryItem, EntryLinePublicVO } from './vo/tenant'
export type { DrawResultVO, DrawHistoryItemVO } from './vo/draw'
export type { TopicVO, PurchaseResultVO } from './vo/topic'
export type { RechargeVO } from './vo/recharge'
export type {
  SiteMenuVO,
  SitePageVO,
  CmsHomeContent,
  CmsRulesContent,
  CmsRechargeContent,
  CmsKefuContent,
} from './vo/cms'
export type {
  AdminDashboardVO,
  AdminDashboardKpis,
  AdminDashboardTrends,
  AdminNameCount,
  AdminAgentRow,
  AdminTenantRank,
  AdminActivityItem,
  AdminDrawFreshness,
  AdminRecentTenant,
} from './vo/admin-dashboard'
export type {
  AdminAgentListItem,
  AdminAgentTrends,
  AdminAgentDetail,
  AdminUserListItem,
  AdminUserDetail,
  UserCoinLogItem,
  UserOrderItem,
  PasswordResetResult,
  AgentUserListItem,
  AgentUserDetail,
  AgentDashboardKpis,
  AgentDashboardTrends,
  AgentDashboardVO,
  AdminAgentPage,
  AdminUserPage,
  AgentUserPage,
} from './vo/mgmt'
export type { IpWhitelistEntry, IpWhitelistVO } from './vo/ip-whitelist'
