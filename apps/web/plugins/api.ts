import { setApiBaseURL, setForwardedHostResolver, setRequestErrorHandler } from '@liuhecai/shared'

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const serverBase = (config.apiBase as string) || ''
  const publicBase = (config.public.apiBase as string) || ''
  setApiBaseURL(import.meta.server ? (serverBase || publicBase) : publicBase)

  setForwardedHostResolver(() => {
    if (import.meta.server) {
      const headers = useRequestHeaders(['x-forwarded-host', 'host'])
      const raw = (headers['x-forwarded-host'] || headers.host || '').split(',')[0]?.trim() || ''
      return raw.split(':')[0] || ''
    }
    return ''
  })

  const { markSiteBusy } = useSiteBusy()
  setRequestErrorHandler(() => {
    markSiteBusy()
  })
})
