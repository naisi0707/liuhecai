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
}

export interface TenantDirectoryItem {
  id: number
  name: string
  logoUrl?: string
  primaryColor?: string
  /** 公开目录已脱敏，可能为 null；本地演示用名称映射 host */
  primaryHost?: string | null
}
