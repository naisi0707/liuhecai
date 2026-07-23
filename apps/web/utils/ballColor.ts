/** 六合彩波色（与原站 a4.html red/blue/green 表一致） */
const RED = new Set([1, 2, 7, 8, 12, 13, 18, 19, 23, 24, 29, 30, 34, 35, 40, 45, 46])
const BLUE = new Set([3, 4, 9, 10, 14, 15, 20, 25, 26, 31, 36, 37, 41, 42, 47, 48])

export type BallWave = 'red' | 'blue' | 'green' | 'grey'

export function ballWave(num: string | number | undefined | null): BallWave {
  const n = Number(num)
  if (!Number.isFinite(n)) return 'grey'
  if (RED.has(n)) return 'red'
  if (BLUE.has(n)) return 'blue'
  return 'green'
}

export function padBall(num: string | number | undefined | null): string {
  const n = Number(num)
  if (!Number.isFinite(n)) return String(num ?? '--')
  return String(n).padStart(2, '0')
}

export function displayIssue(issueNo?: string | null, display?: string | null): string {
  if (display) return display
  if (!issueNo) return ''
  const raw = String(issueNo).trim()
  if (/^\d{7,}$/.test(raw)) return String(Number(raw.slice(-3)))
  return raw.replace(/^0+(?!$)/, '')
}
