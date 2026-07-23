import { request, type TenantPublicVO } from '@liuhecai/shared'

export function useTenant() {
  const tenant = useState<TenantPublicVO | null>('tenant', () => null)
  /** 仅前端校验提示（如请先登录）；接口失败走白屏，不写这里 */
  const errorMsg = useState('forum_error', () => '')

  async function loadTenant() {
    tenant.value = await request<TenantPublicVO>('/api/tenant/current')
  }

  const siteName = computed(() => tenant.value?.name || '')
  const domainBadge = computed(() => tenant.value?.host || '')
  const primaryColor = computed(() => tenant.value?.primaryColor || '')

  return { tenant, errorMsg, loadTenant, siteName, domainBadge, primaryColor }
}
