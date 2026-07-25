import { getTenantHostOverride, request, type TenantPublicVO } from '@liuhecai/shared'

export function useTenant() {
  const tenant = useState<TenantPublicVO | null>('tenant', () => null)
  /** 仅前端校验提示（如请先登录）；接口失败走白屏，不写这里 */
  const errorMsg = useState('forum_error', () => '')

  async function loadTenant(force = false) {
    const expectedHost = getTenantHostOverride() || ''
    if (!force && tenant.value) {
      if (!expectedHost || tenant.value.host === expectedHost) {
        return tenant.value
      }
    }
    tenant.value = await request<TenantPublicVO>('/api/tenant/current')
    return tenant.value
  }

  const siteName = computed(() => tenant.value?.name || '')
  const domainBadge = computed(() => tenant.value?.host || '')
  const primaryColor = computed(() => tenant.value?.primaryColor || '')

  return { tenant, errorMsg, loadTenant, siteName, domainBadge, primaryColor }
}
