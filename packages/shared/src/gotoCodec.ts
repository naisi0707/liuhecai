/** 与原站 311992 goto 数字串编解码一致：charCode±1000，每字符 4 位数字 */

export function encodeGotoUrl(url: string): string {
  let out = ''
  for (let i = 0; i < url.length; i++) {
    out += String(url.charCodeAt(i) + 1000)
  }
  return out
}

export function decodeGotoUrl(encoded: string): string {
  if (!encoded || encoded.length % 4 !== 0) {
    throw new Error('invalid goto payload')
  }
  const chars: string[] = []
  for (let j = 4; j <= encoded.length; j += 4) {
    const n = parseInt(encoded.slice(j - 4, j), 10)
    if (Number.isNaN(n)) {
      throw new Error('invalid goto payload')
    }
    chars.push(String.fromCharCode(n - 1000))
  }
  return chars.join('')
}

/** 校验解码后的 URL 主机是否在允许列表中 */
export function isAllowedGotoTarget(decodedUrl: string, allowedHosts: string[]): boolean {
  try {
    const u = new URL(decodedUrl)
    const host = u.hostname.toLowerCase()
    return allowedHosts.some((h) => h.toLowerCase() === host)
  } catch {
    return false
  }
}
