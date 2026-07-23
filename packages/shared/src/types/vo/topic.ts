export type TopicVO = {
  id: string
  title: string
  lotteryType: string
  issueNo: string
  playType: string
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
