<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AgentUserListItem } from '@liuhecai/shared'

definePageMeta({ title: '用户管理' })

const { hydrate } = useAgentAuth()
const {
  pageUsers,
  createUser,
  setUserEnabled,
  resetUserPassword,
  forceUserLogout,
  softDeleteUser,
  batchUserEnabled,
  exportUsers,
} = useAgentMgmt()

const loading = ref(false)
const rows = ref<AgentUserListItem[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const filter = reactive({ username: '', enabled: '' as '' | '0' | '1' })
const selected = ref<AgentUserListItem[]>([])
const echo = ref('')
const createVisible = ref(false)
const createUsername = ref('')
const creating = ref(false)

function filterParams() {
  return {
    username: filter.username || undefined,
    enabled: filter.enabled === '' ? undefined : Number(filter.enabled),
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

async function onToggle(row: AgentUserListItem) {
  const next = row.enabled === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      next === 0 ? `确认停用用户 ${row.username}？` : `确认启用用户 ${row.username}？`,
      '提示',
      { type: 'warning' },
    )
    await setUserEnabled(row.id, next)
    ElMessage.success(next === 1 ? '已启用' : '已停用')
    await load()
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onReset(row: AgentUserListItem) {
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

async function onForceLogout(row: AgentUserListItem) {
  try {
    await ElMessageBox.confirm(`确认强制下线用户 ${row.username}？`, '提示', { type: 'warning' })
    await forceUserLogout(row.id)
    ElMessage.success('已强制下线')
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onDelete(row: AgentUserListItem) {
  try {
    await ElMessageBox.confirm(
      `确认注销用户 ${row.username}？将停用并强制下线，流水保留，可再启用恢复。`,
      '注销确认',
      { type: 'warning' },
    )
    await softDeleteUser(row.id)
    ElMessage.success('已注销')
    await load()
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '注销失败')
  }
}

async function onBatch(enabled: number) {
  if (!selected.value.length) {
    ElMessage.warning('请先勾选用户')
    return
  }
  try {
    await ElMessageBox.confirm(
      enabled === 0
        ? `确认批量停用 ${selected.value.length} 个用户？`
        : `确认批量启用 ${selected.value.length} 个用户？`,
      '提示',
      { type: 'warning' },
    )
    await batchUserEnabled(selected.value.map((r) => r.id), enabled)
    ElMessage.success(enabled === 1 ? '批量启用完成' : '批量停用完成')
    selected.value = []
    await load()
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '批量操作失败')
  }
}

async function onCreate() {
  if (!createUsername.value.trim()) {
    ElMessage.warning('请填写用户名')
    return
  }
  creating.value = true
  try {
    const data = await createUser(createUsername.value.trim())
    echo.value = `用户 ${data.username} 初始密码：${data.rawPassword}`
    ElMessage.success('用户已创建')
    createVisible.value = false
    createUsername.value = ''
    await load()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    creating.value = false
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
        <el-form-item label="状态">
          <el-select v-model="filter.enabled" clearable placeholder="全部" style="width:110px;">
            <el-option label="正常" value="1" />
            <el-option label="停用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="() => { page = 1; load() }">查询</el-button>
          <el-button @click="load">刷新</el-button>
          <el-button type="success" @click="createVisible = true">新增用户</el-button>
          <el-button type="danger" :disabled="!selected.length" @click="onBatch(0)">批量停用</el-button>
          <el-button :disabled="!selected.length" @click="onBatch(1)">批量启用</el-button>
          <el-button @click="onExport">导出 CSV</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading" shadow="never">
      <el-table :data="rows" stripe @selection-change="(v: AgentUserListItem[]) => selected = v">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column label="余额" width="100" align="right">
          <template #default="{ row }">{{ row.coinBalance ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'danger'" size="small">
              {{ row.enabled === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="navigateTo(`/users/${row.id}`)">详情</el-button>
            <el-button link type="success" @click="navigateTo(`/users/${row.id}?adjust=1`)">加减币</el-button>
            <el-button link @click="onToggle(row)">{{ row.enabled === 1 ? '停用' : '启用' }}</el-button>
            <el-button link type="warning" @click="onReset(row)">重置密码</el-button>
            <el-button link @click="onForceLogout(row)">强制下线</el-button>
            <el-button link type="danger" @click="onDelete(row)">注销</el-button>
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

    <el-dialog v-model="createVisible" title="新增用户" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户名" required>
          <el-input v-model="createUsername" maxlength="64" placeholder="会员登录名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-alert v-if="echo" :title="echo" type="success" show-icon closable style="margin-top:12px;" @close="echo = ''" />
  </div>
</template>
