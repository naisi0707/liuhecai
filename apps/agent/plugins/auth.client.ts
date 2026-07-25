import { setUnauthorizedHandler } from '@liuhecai/shared'

export default defineNuxtPlugin(() => {
  const { logout } = useAgentAuth()
  const { clearSiteName } = useAgentSite()
  const router = useRouter()

  const goLogin = () => {
    clearSiteName()
    logout()
    if (router.currentRoute.value.path === '/login') return
    void navigateTo('/login').catch(() => {
      window.location.assign('/login')
    })
  }

  setUnauthorizedHandler(goLogin)

  window.addEventListener('storage', (e) => {
    if (e.key === 'agent_token' && !e.newValue) {
      goLogin()
    }
  })
})
