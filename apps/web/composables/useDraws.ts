import { request, type DrawHistoryItemVO, type DrawResultVO } from '@liuhecai/shared'
import { displayIssue } from '~/utils/ballColor'

/** 与原站 a4.html type.js 顺序一致：新澳门 / 香港彩 / 老澳门 */
export const LOTTERY_TABS = [
  { type: 'MACAU_NEW', label: '新澳门' },
  { type: 'HK', label: '香港彩' },
  { type: 'MACAU_OLD', label: '老澳门' },
] as const

export type LotteryType = (typeof LOTTERY_TABS)[number]['type']

export function useDraws() {
  const draws = useState<Record<string, DrawResultVO>>('draws_map', () => ({}))
  const activeLottery = useState<LotteryType>('active_lottery', () => 'MACAU_NEW')
  const countdownText = useState('countdown_text', () => '')
  const historyOpen = useState('draw_history_open', () => false)
  const historyLoading = useState('draw_history_loading', () => false)
  const historyItems = useState<DrawHistoryItemVO[]>('draw_history_items', () => [])
  let timer: ReturnType<typeof setInterval> | null = null

  const currentDraw = computed(() => draws.value[activeLottery.value] || null)

  function formatCountdown(total: number) {
    const s = Math.max(0, Math.floor(total))
    const h = String(Math.floor(s / 3600)).padStart(2, '0')
    const m = String(Math.floor((s % 3600) / 60)).padStart(2, '0')
    const sec = String(s % 60).padStart(2, '0')
    return `${h}:${m}:${sec}`
  }

  function tickCountdown() {
    const d = currentDraw.value
    if (!d?.nextDrawTime) {
      countdownText.value = ''
      return
    }
    const left = (new Date(d.nextDrawTime).getTime() - Date.now()) / 1000
    countdownText.value = formatCountdown(left)
  }

  async function loadDraws(force = false) {
    if (!force && Object.keys(draws.value).length > 0) return
    const list = await request<DrawResultVO[]>('/api/draws/latest-all')
    const map: Record<string, DrawResultVO> = {}
    for (const item of list) map[item.lotteryType] = item
    draws.value = map
    tickCountdown()
  }

  function startCountdown() {
    if (timer) clearInterval(timer)
    tickCountdown()
    timer = setInterval(tickCountdown, 1000)
  }

  function stopCountdown() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function formatNextDraw(d: DrawResultVO | null) {
    if (!d?.nextDrawTime) return ''
    const dt = new Date(d.nextDrawTime)
    const mm = String(dt.getMonth() + 1).padStart(2, '0')
    const dd = String(dt.getDate()).padStart(2, '0')
    const week = ['日', '一', '二', '三', '四', '五', '六'][dt.getDay()]
    const hh = String(dt.getHours()).padStart(2, '0')
    const mi = String(dt.getMinutes()).padStart(2, '0')
    const nextQi = d.nextDisplayIssue
      || displayIssue(d.nextIssueNo)
      || nextFromCurrent(d)
    if (!nextQi) return ''
    return `第${nextQi}期开奖:${mm}月${dd}日 周${week} ${hh}点${mi}分`
  }

  function nextFromCurrent(d: DrawResultVO) {
    const cur = displayIssue(d.issueNo, d.displayIssue)
    const n = Number(cur)
    return Number.isFinite(n) ? String(n + 1) : ''
  }

  async function openHistory() {
    historyOpen.value = true
    historyLoading.value = true
    historyItems.value = []
    try {
      const type = activeLottery.value
      historyItems.value = await request<DrawHistoryItemVO[]>(
        `/api/draws/history?lotteryType=${encodeURIComponent(type)}&pageSize=100`,
      )
    } finally {
      historyLoading.value = false
    }
  }

  function closeHistory() {
    historyOpen.value = false
  }

  return {
    draws,
    activeLottery,
    countdownText,
    currentDraw,
    historyOpen,
    historyLoading,
    historyItems,
    loadDraws,
    startCountdown,
    stopCountdown,
    formatNextDraw,
    tickCountdown,
    openHistory,
    closeHistory,
  }
}
