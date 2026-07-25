<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AdminAgentListItem } from '@liuhecai/shared'

definePageMeta({ title: '代理管理' })

const { api, hydrate } = useAdminAuth()
const { pageAgents, setAgentEnabled, resetAgentPassword, exportAgents } = useAdminMgmt()

const loading = ref(false)
const rows = ref<AdminAgentListItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const filter = reactive({ username: '', enabled: '' as '' | '0' | '1', tenantId: '' as '' | number })
const tenants = ref<{ id: number; name: string }[]>([])
const echo = ref('')

async function load() {
  loading.value = true
  try {
    const data = await pageAgents({
      page: page.value,
      size: size.value,
      username: filter.username || undefined,
      enabled: filter.enabled === '' ? undefined : Number(filter.enabled),
      tenantId: filter.tenantId === '' ? undefined : Number(filter.tenantId),
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function onToggle(row: AdminAgentListItem) {
  const next = row.enabled === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      next === 0 ? `确认停用代理 ${row.username}？停用后无法登录。` : `确认启用代理 ${row.username}？`,
      '提示',
      { type: 'warning' },
    )
  } catch {
    return
  }
  try {
    await setAgentEnabled(row.id, next)
    ElMessage.success(next === 1 ? '已启用' : '已停用')
    await load()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onReset(row: AdminAgentListItem) {
  try {
    await ElMessageBox.confirm(`确认重置代理 ${row.username} 的密码？`, '提示', { type: 'warning' })
    const data = await resetAgentPassword(row.id)
    echo.value = `代理 ${data.username} 新密码：${data.rawPassword}`
    ElMessage.success('密码已重置')
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '重置失败')
  }
}

async function onExport() {
  try {
    await exportAgents({
      username: filter.username || undefined,
      enabled: filter.enabled === '' ? undefined : Number(filter.enabled),
      tenantId: filter.tenantId === '' ? undefined : Number(filter.tenantId),
    })
    ElMessage.success('已开始下载')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '导出失败')
  }
}

function fmtTime(v?: string) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '-'
}

onMounted(async () => {
  hydrate()
  tenants.value = await api('/api/admin/tenants')
  await load()
})
</script>

<template>
  <div>
    <el-card shadow="never" style="margin-bottom:12px;">
      <el-form :inline="true" @submit.prevent="load">
        <el-form-item label="用户名">
          <el-input v-model="filter.username" clearable placeholder="模糊搜索" style="width:140px;" />
        </el-form-item>
        <el-form-item label="租户">
          <el-select v-model="filter.tenantId" clearable placeholder="全部" style="width:180px;">
            <el-option
              v-for="t in tenants"
              :key="t.id"
              :label="`${t.name} (#${t.id})`"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.enabled" clearable placeholder="全部" style="width:110px;">
            <el-option label="启用" value="1" />
            <el-option label="停用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="() => { page = 1; load() }">查询</el-button>
          <el-button @click="load">刷新</el-button>
          <el-button @click="onExport">导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading" shadow="never">
      <el-table :data="rows" stripe>
        <el-table-column prop="id" label="ID" width="120" />
        <el-table-column prop="tenantName" label="站点" min-width="140" />
        <el-table-column prop="username" label="代理账号" min-width="120" />
        <el-table-column label="用户数" width="90" align="right">
          <template #default="{ row }">{{ row.userCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="近7日充值" width="110" align="right">
          <template #default="{ row }">{{ row.rechargeAmount7d ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
              {{ row.enabled === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="navigateTo(`/agents/${row.id}`)">业绩</el-button>
            <el-button link @click="onToggle(row)">{{ row.enabled === 1 ? '停用' : '启用' }}</el-button>
            <el-button link type="warning" @click="onReset(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:12px; display:flex; justify-content:flex-end;">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="load"
          @size-change="() => { page = 1; load() }"
        />
      </div>
    </el-card>

    <el-alert v-if="echo" :title="echo" type="success" show-icon style="margin-top:12px;" @close="echo = ''" />
  </div>
</template>
