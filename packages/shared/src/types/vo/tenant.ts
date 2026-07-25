export interface TenantPublicVO {
  id: number
  name: string
  status: number
  themeJson?: string
  primaryColor?: string
  fontFamily?: string
  logoUrl?: string
  adBanner?: string
  kefuWechat?: string
  kefuQq?: string
  announcement?: string
  host?: string
  /** 当前 Host 角色：ENTRY 入口伪装 / FORUM 论坛 */
  domainRole?: 'ENTRY' | 'FORUM'
  /** 同租户论坛 Host（供 ENTRY 跳转） */
  forumHost?: string | null
}

export interface TenantDirectoryItem {
  id: number
  name: string
  logoUrl?: string
  primaryColor?: string
  /** 公开目录已脱敏，可能为 null；本地演示用名称映射 host */
  primaryHost?: string | null
}
