export interface SiteMenuVO {
  id: string
  code: string
  title: string
  path: string
  sortNo: number
  visible: number
}

export interface SitePageVO {
  id: string
  pageKey: string
  title: string
  content: Record<string, unknown>
}

export interface CmsHomeContent {
  bannerUrl?: string
  drawIframeUrl?: string
  liveIframeUrl?: string
  domainBadge?: string
  showLocalDrawPanel?: boolean
  sisterSites?: Array<{
    name: string
    domain: string
    href: string
    cta?: string
    color?: string
  }>
  bottomImages?: Array<{ src: string; alt?: string }>
  qrWechatUrl?: string
  qrQqUrl?: string
}

export interface CmsRulesContent {
  heading?: string
  intro?: string
  guarantees?: Array<{ title: string; body: string }>
}

export interface CmsRechargeContent {
  heading?: string
  tiers?: string[]
  exchangeRate?: string
  declareText?: string
  notes?: string[]
  qrWechatUrl?: string
  qrQqUrl?: string
}

export interface CmsKefuContent {
  heading?: string
  intro?: string
  qrWechatUrl?: string
  qrQqUrl?: string
}
