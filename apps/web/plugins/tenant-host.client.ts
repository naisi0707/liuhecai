import { setTenantHostOverride } from '@liuhecai/shared'

export default defineNuxtPlugin(() => {
  if (!import.meta.client) return
  const params = new URLSearchParams(window.location.search)
  const host = params.get('host')
  if (host) {
    setTenantHostOverride(host)
  }
})
