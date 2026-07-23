export type DrawResultVO = {
  lotteryType: string
  lotteryLabel: string
  issueNo?: string
  displayIssue?: string
  drawTime?: string
  numbers: string[]
  specialNumber?: string
  zodiacs: string[]
  wuxings?: string[]
  source?: string
  overridden: boolean
  nextIssueNo?: string
  nextDisplayIssue?: string
  nextDrawTime?: string
  countdownSeconds: number
}

export type DrawHistoryItemVO = {
  issueNo: string
  displayIssue: string
  drawDate?: string
  numbers: string[]
  specialNumber?: string
  zodiacs: string[]
  wuxings?: string[]
}
