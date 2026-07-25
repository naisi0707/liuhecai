import { getTenantHostOverride, setTenantHostOverride, type TenantPublicVO } from '@liuhecai/shared'

/**
 * ENTRY Host：仅允许伪装首页与 /goto；切换 ?host= 时强制重载租户。
 */
export default defineNuxtRouteMiddleware(async (to) => {
  const qHost = typeof to.query.host === 'string' ? to.query.host.trim().toLowerCase() : ''
  if (qHost) {
    setTenantHostOverride(qHost)
  }
  const expectedHost = qHost || getTenantHostOverride() || ''

  const tenant = useState<TenantPublicVO | null>('tenant', () => null)
  const { loadTenant } = useTenant()
  if (!tenant.value || (expectedHost && tenant.value.host !== expectedHost)) {
    try {
      await loadTenant()
    } catch {
      return
    }
  }

  const role = tenant.value?.domainRole || 'FORUM'
  const path = to.path
  if (role === 'ENTRY') {
    const allowed = path === '/' || path === '/goto' || path.startsWith('/camouflage')
    if (!allowed) {
      return navigateTo({ path: '/', query: to.query })
    }
  }
})
