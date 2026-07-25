<script setup lang="ts">
import { decodeGotoUrl, isAllowedGotoTarget } from '@liuhecai/shared'

definePageMeta({ layout: false })

const route = useRoute()
const { tenant, loadTenant } = useTenant()
const error = ref('')

function collectAllowedHosts(): string[] {
  const allowed = new Set<string>()
  for (const line of tenant.value?.entryLines || []) {
    if (!line.forumUrl) continue
    try {
      const u = new URL(line.forumUrl)
      allowed.add(u.hostname.toLowerCase())
      const h = u.searchParams.get('host')
      if (h) allowed.add(h.toLowerCase())
    } catch {
      /* skip */
    }
  }
  const forumHost = (tenant.value?.forumHost || '').toLowerCase()
  if (forumHost) allowed.add(forumHost)
  allowed.add('127.0.0.1')
  allowed.add('localhost')
  return [...allowed]
}

onMounted(async () => {
  try {
    if (!tenant.value) await loadTenant()
  } catch {
    error.value = '站点不可用'
    return
  }
  const raw = typeof route.query.u === 'string' ? route.query.u : ''
  if (!raw) {
    error.value = '缺少跳转参数'
    return
  }
  let decoded = ''
  try {
    decoded = decodeGotoUrl(raw)
  } catch {
    error.value = '跳转参数无效'
    return
  }
  const allowed = collectAllowedHosts()
  let ok = isAllowedGotoTarget(decoded, allowed)
  if (!ok) {
    try {
      const u = new URL(decoded)
      const hostParam = (u.searchParams.get('host') || '').toLowerCase()
      ok = !!hostParam && allowed.includes(hostParam)
    } catch {
      ok = false
    }
  }
  if (!ok) {
    error.value = '跳转目标不在白名单'
    return
  }
  window.location.replace(decoded)
})
</script>

<template>
  <div class="goto-page">
    <p v-if="error">{{ error }}</p>
    <p v-else>正在进入…</p>
  </div>
</template>

<style scoped>
.goto-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: "Microsoft YaHei", sans-serif;
  color: #333;
}
</style>
