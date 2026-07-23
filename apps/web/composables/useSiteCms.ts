import { request, type SiteMenuVO, type SitePageVO } from '@liuhecai/shared'

const menusState = () => useState<SiteMenuVO[]>('cms_menus', () => [])
const pageCache = () => useState<Record<string, SitePageVO>>('cms_pages', () => ({}))

export function useSiteCms() {
  const menus = menusState()
  const pages = pageCache()

  async function loadMenus(force = false) {
    if (!force && menus.value.length) return menus.value
    menus.value = await request<SiteMenuVO[]>('/api/site/menus')
    return menus.value
  }

  async function loadPage(pageKey: string, force = false) {
    if (!force && pages.value[pageKey]) return pages.value[pageKey]
    const page = await request<SitePageVO>(`/api/site/pages/${pageKey}`)
    pages.value = { ...pages.value, [pageKey]: page }
    return page
  }

  function pageContent<T extends Record<string, unknown>>(pageKey: string): T | null {
    const p = pages.value[pageKey]
    return (p?.content as T) || null
  }

  return { menus, pages, loadMenus, loadPage, pageContent }
}
