/** 旧镜像路径 → 自有 /site 路径（兼容 CMS/种子里残留的 /bbs/...） */
const BBS_TO_SITE: Record<string, string> = {
  '/bbs/images/': '/site/images/',
  '/bbs/_root/': '/site/root/',
  '/bbs/fta1/': '/site/icons/',
  '/bbs/ftimg/': '/site/icons/',
  '/bbs/promo/': '/site/promo/',
}

export function useMediaUrl() {
  const config = useRuntimeConfig()
  const apiBase = (config.public.apiBase as string) || ''
  const webBase = (config.public.webBase as string) || ''

  function rewriteLegacyPath(path: string): string {
    for (const [from, to] of Object.entries(BBS_TO_SITE)) {
      if (path.startsWith(from)) {
        return to + path.slice(from.length)
      }
    }
    return path
  }

  function mediaUrl(path?: string | null) {
    if (!path) return ''
    if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('data:')) {
      return path
    }
    if (path.startsWith('/uploads/')) {
      return `${apiBase}${path}`
    }
    return rewriteLegacyPath(path)
  }

  return { mediaUrl, apiBase, webBase }
}
