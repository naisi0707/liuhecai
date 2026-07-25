import {
  setApiBaseURL,
  setForwardedHostResolver,
  setRequestErrorHandler,
  setUnauthorizedHandler,
} from '@liuhecai/shared'

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

  if (import.meta.client) {
    const { logout } = useAuth()
    const router = useRouter()
    const goLogin = () => {
      logout()
      if (router.currentRoute.value.path === '/login') return
      void navigateTo('/login').catch(() => {
        window.location.assign('/login')
      })
    }
    setUnauthorizedHandler(goLogin)
    window.addEventListener('storage', (e) => {
      if (e.key === 'user_token' && !e.newValue) {
        goLogin()
      }
    })
  }
})
