export function useAgentSite() {
  const { api, token } = useAgentAuth()
  const siteName = useState<string>('agent_site_name', () => '')

  async function loadSiteName(force = false) {
    if (!token.value) {
      siteName.value = ''
      return
    }
    if (!force && siteName.value) return siteName.value
    try {
      const data = await api<{ name?: string }>('/api/agent/site-config')
      siteName.value = data.name || '代理后台'
      return siteName.value
    } catch {
      siteName.value = '代理后台'
      return siteName.value
    }
  }

  function clearSiteName() {
    siteName.value = ''
  }

  return { siteName, loadSiteName, clearSiteName }
}
