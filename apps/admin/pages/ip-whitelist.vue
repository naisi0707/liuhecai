<script setup lang="ts">
import type { IpWhitelistEntry, IpWhitelistVO } from '@liuhecai/shared'
import { ElMessage } from 'element-plus'

definePageMeta({ title: 'IP 白名单' })

const { api, hydrate } = useAdminAuth()

const loading = ref(false)
const saving = ref(false)
const enabled = ref(false)
const currentIp = ref('')
const rows = ref<Array<{ cidr: string; note: string }>>([])

function ipMatches(clientIp: string, cidr: string) {
  const ip = clientIp.trim()
  const rule = cidr.trim()
  if (!ip || !rule) return false
  if (!rule.includes('/')) return ip === rule
  const [base, prefixRaw] = rule.split('/', 2)
  const prefix = Number(prefixRaw)
  if (!Number.isInteger(prefix) || prefix < 0 || prefix > 32) return false
  const toLong = (v: string) =>
    v.split('.').reduce((acc, p) => ((acc << 8) | Number(p)) >>> 0, 0)
  if (prefix === 0) return true
  const mask = prefix === 32 ? 0xffffffff : (~0 << (32 - prefix)) >>> 0
  return (toLong(base) & mask) === (toLong(ip) & mask)
}

async function load() {
  loading.value = true
  try {
    const data = await api<IpWhitelistVO>('/api/admin/ip-whitelist')
    enabled.value = !!data.enabled
    currentIp.value = data.currentIp || ''
    rows.value = (data.entries || []).map((e: IpWhitelistEntry) => ({
      cidr: e.cidr || '',
      note: e.note || '',
    }))
    if (!rows.value.length) {
      rows.value.push({ cidr: '', note: '' })
    }
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function addRow() {
  rows.value.push({ cidr: '', note: '' })
}

function removeRow(idx: number) {
  rows.value.splice(idx, 1)
  if (!rows.value.length) rows.value.push({ cidr: '', note: '' })
}

function fillCurrentIp() {
  if (!currentIp.value) {
    ElMessage.warning('未能识别当前 IP')
    return
  }
  if (!rows.value.some((r) => r.cidr.trim() === currentIp.value)) {
    const empty = rows.value.findIndex((r) => !r.cidr.trim())
    if (empty >= 0) rows.value[empty].cidr = currentIp.value
    else rows.value.unshift({ cidr: currentIp.value, note: '当前访问 IP' })
  }
}

async function save() {
  const entries = rows.value
    .map((r) => ({ cidr: r.cidr.trim(), note: r.note.trim() || undefined }))
    .filter((r) => r.cidr)
  if (enabled.value) {
    if (!entries.length) {
      ElMessage.warning('启用白名单前请至少添加一条 IP')
      return
    }
    if (!currentIp.value || !entries.some((e) => ipMatches(currentIp.value, e.cidr))) {
      ElMessage.warning(`启用前请先将当前 IP（${currentIp.value || '未知'}）加入白名单，以免锁死`)
      return
    }
  }
  saving.value = true
  try {
    const data = await api<IpWhitelistVO>('/api/admin/ip-whitelist', {
      method: 'PUT',
      body: { enabled: enabled.value, entries },
    })
    enabled.value = !!data.enabled
    currentIp.value = data.currentIp || ''
    rows.value = (data.entries || []).map((e: IpWhitelistEntry) => ({
      cidr: e.cidr || '',
      note: e.note || '',
    }))
    if (!rows.value.length) rows.value.push({ cidr: '', note: '' })
    ElMessage.success(enabled.value ? '已启用白名单' : '已保存（白名单关闭，全放开）')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  hydrate()
  await load()
})
</script>

<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="hdr">
        <span>超管 / 代理 IP 白名单</span>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </div>
    </template>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="关闭时全放开；开启后仅白名单 IP 可访问超管与代理 API（含登录）。论坛前台不受影响。"
      style="margin-bottom: 16px"
    />

    <el-form label-width="120px">
      <el-form-item label="启用白名单">
        <el-switch v-model="enabled" />
        <span class="hint">{{ enabled ? '已开启限制' : '暂时放开（推荐）' }}</span>
      </el-form-item>
      <el-form-item label="当前访问 IP">
        <el-tag type="warning">{{ currentIp || '未知' }}</el-tag>
        <el-button link type="primary" style="margin-left: 8px" @click="fillCurrentIp">加入列表</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" size="small" style="width: 100%; margin-bottom: 12px">
      <el-table-column label="IP / CIDR" min-width="200">
        <template #default="{ row }">
          <el-input v-model="row.cidr" placeholder="例如 1.2.3.4 或 1.2.3.0/24" />
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="160">
        <template #default="{ row }">
          <el-input v-model="row.note" placeholder="可选" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ $index }">
          <el-button link type="danger" @click="removeRow($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button @click="addRow">添加一行</el-button>
  </el-card>
</template>

<style scoped>
.hdr {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.hint {
  margin-left: 12px;
  color: #6b7280;
  font-size: 13px;
}
</style>
