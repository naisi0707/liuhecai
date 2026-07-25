/** 预览用媒体 URL：把 /uploads/... 拼上 apiBase */
export function previewMediaUrl(path?: string | null): string {
  if (!path) return ''
  const value = path.trim()
  if (!value) return ''
  if (/^https?:\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }
  const config = useRuntimeConfig()
  const apiBase = ((config.public.apiBase as string) || '').replace(/\/$/, '')
  if (value.startsWith('/uploads/') && apiBase) {
    return `${apiBase}${value}`
  }
  return value
}

export function usePreviewMedia() {
  return { previewMediaUrl }
}
