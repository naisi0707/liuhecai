/** 旧镜像路径 → 自有 /site 路径（兼容 CMS/种子里残留的 /bbs/...） */
const BBS_TO_SITE: Record<string, string> = {
  '/bbs/images/': '/site/images/',
  '/bbs/_root/': '/site/root/',
  '/bbs/fta1/': '/site/icons/',
  '/bbs/ftimg/': '/site/icons/',
  '/bbs/promo/': '/site/promo/',
}

/** 将 /bbs/... 重写为 /site/...；其它路径原样返回 */
export function rewriteBbsToSite(path: string): string {
  if (!path) return ''
  for (const [from, to] of Object.entries(BBS_TO_SITE)) {
    if (path.startsWith(from)) {
      return to + path.slice(from.length)
    }
  }
  if (path.startsWith('/bbs/')) {
    return `/site/${path.slice('/bbs/'.length)}`
  }
  return path
}

/**
 * 浏览器可加载的媒体地址：
 * - 绝对 URL / data: 原样
 * - /uploads/：可拼 apiBase（同域时留空）
 * - /bbs|/site：同域相对路径（admin/agent 需 nginx 反代到 web 静态）
 */
export function resolvePublicMediaUrl(path?: string | null, apiBase = ''): string {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('data:')) {
    return path
  }
  if (path.startsWith('/uploads/')) {
    return `${apiBase}${path}`
  }
  return rewriteBbsToSite(path)
}
