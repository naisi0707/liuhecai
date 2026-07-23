/** 服务器异常时全站白屏，文案固定不展示后端细节 */
export function useSiteBusy() {
  const busy = useState('site_busy', () => false)

  function markSiteBusy() {
    busy.value = true
  }

  return { busy, markSiteBusy }
}
