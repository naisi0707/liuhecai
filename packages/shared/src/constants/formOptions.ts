/** 资料帖子标签预设（可自定义新增） */
export const TOPIC_TAG_OPTIONS = [
  '出售帖',
  '高手帖',
  '普通帖',
  '推荐帖',
  '精品帖',
  '热门帖',
] as const

/** 标签展示色（未知标签用默认红） */
export const TOPIC_TAG_COLOR: Record<string, string> = {
  出售帖: '#FF0000',
  高手帖: '#c62828',
  普通帖: '#666666',
  推荐帖: '#0000FF',
  精品帖: '#008000',
  热门帖: '#0000FF',
}

export const DEFAULT_TOPIC_TAG = '出售帖'

export function topicTagColor(tag?: string | null): string {
  if (!tag) return TOPIC_TAG_COLOR[DEFAULT_TOPIC_TAG]
  return TOPIC_TAG_COLOR[tag] || TOPIC_TAG_COLOR[DEFAULT_TOPIC_TAG]
}

/** 玩法预设（亦可填作者昵称等自定义值） */
export const PLAY_TYPE_OPTIONS = [
  '特码',
  '平特一肖',
  '平特二连',
  '平特三连',
  '一码中特',
  '二中二',
  '三中三',
  '综合',
] as const

export const PAY_CHANNEL_OPTIONS = [
  '微信转账',
  '支付宝',
  '银行卡',
  '其它',
] as const

export const REJECT_REASON_OPTIONS = [
  '凭证不符',
  '金额不符',
  '重复提交',
  '信息不完整',
] as const

export const FONT_FAMILY_OPTIONS = [
  'Microsoft YaHei',
  'SimSun',
  'SimHei',
  'PingFang SC',
  'Noto Sans SC',
  'Arial, sans-serif',
] as const
