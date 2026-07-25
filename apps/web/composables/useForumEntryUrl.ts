import type { TenantPublicVO } from '@liuhecai/shared'

/** 构造入口伪装跳转到论坛的绝对 URL（本地用 ?host= 演示） */
export function buildForumEntryUrl(tenant: TenantPublicVO | null | undefined): string {
  if (!import.meta.client || !tenant?.forumHost) {
    return ''
  }
  const forumHost = tenant.forumHost.trim().toLowerCase()
  const { protocol, host: pageHost, port } = window.location
  const pageIsLocal = pageHost.startsWith('127.0.0.1') || pageHost.startsWith('localhost')
  const forumIsLocal =
    forumHost === '127.0.0.1'
    || forumHost === 'localhost'
    || forumHost.endsWith('.local')
  // 本地 / ?host= 演示：同端口用 query 切换租户 Host
  if (pageIsLocal || forumIsLocal) {
    return `${protocol}//${pageHost}/?host=${encodeURIComponent(forumHost)}`
  }
  const portPart = port && port !== '80' && port !== '443' ? `:${port}` : ''
  return `${protocol}//${forumHost}${portPart}/`
}

export function useForumEntryUrl() {
  const { tenant } = useTenant()
  return computed(() => buildForumEntryUrl(tenant.value))
}
