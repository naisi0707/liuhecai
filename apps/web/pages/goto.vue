<script setup lang="ts">
import { decodeGotoUrl, isAllowedGotoTarget } from '@liuhecai/shared'

definePageMeta({ layout: false })

const route = useRoute()
const { tenant, loadTenant } = useTenant()
const error = ref('')

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
  const forumHost = (tenant.value?.forumHost || '').toLowerCase()
  const allowed = [forumHost, '127.0.0.1', 'localhost'].filter(Boolean)
  let ok = isAllowedGotoTarget(decoded, allowed)
  if (!ok && forumHost) {
    try {
      const u = new URL(decoded)
      ok = (u.searchParams.get('host') || '').toLowerCase() === forumHost
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
