export type TopicVO = {
  id: string
  title: string
  lotteryType: string
  issueNo: string
  playType: string
  /** 帖子标签：出售帖 / 高手帖 / 普通帖 / 推荐帖 等 */
  tag?: string
  price: number
  status?: number
  purchased: boolean
  contentVisible: boolean
  /** 往期成绩等公开预览，未购买也返回 */
  previewContent?: string | null
  content?: string | null
  viewCount?: number
  purchaseCount?: number
  prevTopicId?: string | null
  prevTopicTitle?: string | null
  nextTopicId?: string | null
  nextTopicTitle?: string | null
  createdAt?: string
}

export type PurchaseResultVO = {
  topicId: string
  price: number
  coinBalance: number
  alreadyPurchased: boolean
}
