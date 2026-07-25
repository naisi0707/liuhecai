<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AdminUserListItem } from '@liuhecai/shared'

definePageMeta({ title: '用户管理' })

const { api, hydrate } = useAdminAuth()
const {
  pageUsers,
  setUserEnabled,
  resetUserPassword,
  batchUserEnabled,
  exportUsers,
} = useAdminMgmt()

const loading = ref(false)
const rows = ref<AdminUserListItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const filter = reactive({ username: '', enabled: '' as '' | '0' | '1', tenantId: '' as '' | number })
const tenants = ref<{ id: number; name: string }[]>([])
const selected = ref<AdminUserListItem[]>([])
const echo = ref('')

function filterParams() {
  return {
    username: filter.username || undefined,
    enabled: filter.enabled === '' ? undefined : Number(filter.enabled),
    tenantId: filter.tenantId === '' ? undefined : Number(filter.tenantId),
  }
}

async function load() {
  loading.value = true
  try {
    const data = await pageUsers({
      page: page.value,
      size: size.value,
      ...filterParams(),
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function onToggle(row: AdminUserListItem) {
  const next = row.enabled === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      next === 0 ? `确认封禁用户 ${row.username}？` : `确认解封用户 ${row.username}？`,
      '提示',
      { type: 'warning' },
    )
    await setUserEnabled(row.id, next)
    ElMessage.success(next === 1 ? '已解封' : '已封禁')
    await load()
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onReset(row: AdminUserListItem) {
  try {
    await ElMessageBox.confirm(`确认重置用户 ${row.username} 的密码？`, '提示', { type: 'warning' })
    const data = await resetUserPassword(row.id)
    echo.value = `用户 ${data.username} 新密码：${data.rawPassword}`
    ElMessage.success('密码已重置')
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '重置失败')
  }
}

async function onBatchBan() {
  if (!selected.value.length) {
    ElMessage.warning('请先勾选用户')
    return
  }
  try {
    await ElMessageBox.confirm(`确认批量封禁 ${selected.value.length} 个用户？将同时强制失效其登录。`, '提示', { type: 'warning' })
    await batchUserEnabled(selected.value.map((r) => r.id), 0)
    ElMessage.success('批量封禁完成')
    selected.value = []
    await load()
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '批量操作失败')
  }
}

async function onExport() {
  try {
    await exportUsers(filterParams())
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
          <el-input v-model="filter.username" clearable style="width:140px;" />
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
            <el-option label="正常" value="1" />
            <el-option label="封禁" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="() => { page = 1; load() }">查询</el-button>
          <el-button @click="load">刷新</el-button>
          <el-button type="danger" :disabled="!selected.length" @click="onBatchBan">批量封禁</el-button>
          <el-button @click="onExport">导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading" shadow="never">
      <el-table :data="rows" stripe @selection-change="(v: AdminUserListItem[]) => selected = v">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="tenantName" label="站点" min-width="140" />
        <el-table-column label="余额" width="100" align="right">
          <template #default="{ row }">{{ row.coinBalance ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'danger'" size="small">
              {{ row.enabled === 1 ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="navigateTo(`/users/${row.id}`)">详情</el-button>
            <el-button link :type="row.enabled === 1 ? 'danger' : 'success'" @click="onToggle(row)">
              {{ row.enabled === 1 ? '封禁' : '解封' }}
            </el-button>
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
