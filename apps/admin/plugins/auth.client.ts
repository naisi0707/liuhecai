import { setUnauthorizedHandler } from '@liuhecai/shared'

export default defineNuxtPlugin(() => {
  const { logout } = useAdminAuth()
  const router = useRouter()

  const goLogin = () => {
    logout()
    if (router.currentRoute.value.path === '/login') return
    void navigateTo('/login').catch(() => {
      window.location.assign('/login')
    })
  }

  setUnauthorizedHandler(goLogin)

  // 多标签页：其它页退出/被清 token 时同步
  window.addEventListener('storage', (e) => {
    if (e.key === 'admin_token' && !e.newValue) {
      goLogin()
    }
  })
})
