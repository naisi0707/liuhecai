<script setup lang="ts">
definePageMeta({ title: '操作审计' })

const { hydrate } = useAdminAuth()
const { pageAuditLogs } = useAdminMgmt()

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const filter = reactive({ action: '', operatorName: '', targetType: '' })

const actionLabel: Record<string, string> = {
  USER_ENABLE: '启用用户',
  USER_DISABLE: '封禁用户',
  USER_RESET_PASSWORD: '重置用户密码',
  USER_BATCH_DISABLE: '批量封禁用户',
  USER_FORCE_LOGOUT: '强制用户下线',
  AGENT_ENABLE: '启用代理',
  AGENT_DISABLE: '停用代理',
  AGENT_RESET_PASSWORD: '重置代理密码',
  AGENT_FORCE_LOGOUT: '强制代理下线',
  USER_COIN_ADJUST: '调整金币',
}

async function load() {
  loading.value = true
  try {
    const data = await pageAuditLogs({
      page: page.value,
      size: size.value,
      action: filter.action || undefined,
      operatorName: filter.operatorName || undefined,
      targetType: filter.targetType || undefined,
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
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
        <el-form-item label="动作">
          <el-select v-model="filter.action" clearable placeholder="全部" style="width:180px;">
            <el-option v-for="(label, key) in actionLabel" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="filter.operatorName" clearable style="width:140px;" />
        </el-form-item>
        <el-form-item label="目标类型">
          <el-select v-model="filter.targetType" clearable placeholder="全部" style="width:120px;">
            <el-option label="USER" value="USER" />
            <el-option label="AGENT" value="AGENT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="() => { page = 1; load() }">查询</el-button>
          <el-button @click="load">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading" shadow="never">
      <el-table :data="rows" stripe>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="operatorRealm" label="角色" width="90" />
        <el-table-column label="动作" min-width="140">
          <template #default="{ row }">{{ actionLabel[row.action] || row.action }}</template>
        </el-table-column>
        <el-table-column prop="targetType" label="目标类型" width="100" />
        <el-table-column prop="targetId" label="目标ID" min-width="140" />
        <el-table-column prop="detail" label="详情" min-width="180" show-overflow-tooltip />
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
  </div>
</template>
