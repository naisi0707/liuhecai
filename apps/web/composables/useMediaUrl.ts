import { resolvePublicMediaUrl } from '@liuhecai/shared'

export function useMediaUrl() {
  const config = useRuntimeConfig()
  const apiBase = (config.public.apiBase as string) || ''
  const webBase = (config.public.webBase as string) || ''

  function mediaUrl(path?: string | null) {
    return resolvePublicMediaUrl(path, apiBase)
  }

  return { mediaUrl, apiBase, webBase }
}
