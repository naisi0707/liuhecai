<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '站点列表' })
const { api, hydrate } = useAdminAuth()

const tenants = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    tenants.value = await api('/api/admin/tenants')
  } finally {
    loading.value = false
  }
}

async function toggleStatus(t: any) {
  const status = t.status === 1 ? 0 : 1
  await api(`/api/admin/tenants/${t.id}/status`, { method: 'PUT', body: { status } })
  await load()
  ElMessage.success(status === 1 ? '已启用' : '已停用')
}

function primaryHost(row: any) {
  return (row.domains || []).find((d: any) => d.isPrimary === 1)?.host
    || (row.domains || [])[0]?.host
    || '-'
}

onMounted(async () => {
  hydrate()
  await load()
})
</script>

<template>
  <div>
    <el-space wrap style="margin-bottom:12px;">
      <el-button @click="load">刷新</el-button>
      <el-button type="primary" @click="navigateTo('/tenants/create')">开站创建</el-button>
    </el-space>

    <el-card v-loading="loading">
      <el-table :data="tenants" stripe>
        <el-table-column prop="id" label="ID" width="110" />
        <el-table-column prop="name" label="站名" min-width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="主域名" min-width="140">
          <template #default="{ row }">{{ primaryHost(row) }}</template>
        </el-table-column>
        <el-table-column label="全部域名" min-width="180">
          <template #default="{ row }">
            {{ (row.domains || []).map((d: any) => d.host).join(', ') || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="代理" min-width="200">
          <template #default="{ row }">
            <div v-for="a in (row.agents || [])" :key="a.id" style="margin-bottom:4px;">
              {{ a.username }}
            </div>
            <span v-if="!(row.agents || []).length">-</span>
            <el-button
              v-if="(row.agents || []).length"
              size="small"
              link
              type="primary"
              @click="navigateTo('/agents')"
            >代理管理</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
