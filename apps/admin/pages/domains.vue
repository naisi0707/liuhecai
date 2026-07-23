<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '域名绑定' })
const { api, hydrate } = useAdminAuth()

const tenants = ref<any[]>([])
const bindTenantId = ref<number | string | null>(null)
const bindHost = ref('')
const isPrimary = ref(0)
const loading = ref(false)

async function load() {
  tenants.value = await api('/api/admin/tenants')
}

async function bindDomain() {
  if (!bindTenantId.value || !bindHost.value.trim()) {
    ElMessage.warning('请选择租户并填写域名')
    return
  }
  loading.value = true
  try {
    await api(`/api/admin/tenants/${bindTenantId.value}/domains`, {
      method: 'POST',
      body: { host: bindHost.value.trim(), isPrimary: isPrimary.value },
    })
    bindHost.value = ''
    await load()
    ElMessage.success('域名已绑定')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '绑定失败')
  } finally {
    loading.value = false
  }
}

const selected = computed(() => tenants.value.find((t) => t.id === bindTenantId.value))

onMounted(async () => {
  hydrate()
  await load()
})
</script>

<template>
  <el-row :gutter="16">
    <el-col :xs="24" :md="10">
      <el-card>
        <template #header>绑定额外域名</template>
        <el-form label-position="top">
          <el-form-item label="租户">
            <el-select v-model="bindTenantId" placeholder="选择租户" style="width:100%">
              <el-option v-for="t in tenants" :key="t.id" :label="`${t.name} (#${t.id})`" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="域名"><el-input v-model="bindHost" placeholder="demo2.local" /></el-form-item>
          <el-form-item label="设为主域名">
            <el-switch v-model="isPrimary" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-button type="primary" :loading="loading" @click="bindDomain">绑定</el-button>
        </el-form>
      </el-card>
    </el-col>
    <el-col :xs="24" :md="14">
      <el-card>
        <template #header>当前租户域名</template>
        <template v-if="selected">
          <p><strong>{{ selected.name }}</strong></p>
          <el-table :data="selected.domains || []" size="small">
            <el-table-column prop="host" label="域名" />
            <el-table-column label="主域名" width="90">
              <template #default="{ row }">{{ row.isPrimary === 1 ? '是' : '' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template>
            </el-table-column>
          </el-table>
        </template>
        <p v-else style="color:#6b7280;">请选择左侧租户查看已有域名</p>
      </el-card>
    </el-col>
  </el-row>
</template>
