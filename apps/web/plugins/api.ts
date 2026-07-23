import { setApiBaseURL, setRequestErrorHandler } from '@liuhecai/shared'

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  setApiBaseURL(config.public.apiBase as string)

  const { markSiteBusy } = useSiteBusy()
  setRequestErrorHandler(() => {
    markSiteBusy()
  })
})
