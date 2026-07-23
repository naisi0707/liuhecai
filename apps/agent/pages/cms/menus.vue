<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '导航菜单' })
const { api, hydrate } = useAgentAuth()

type MenuItem = {
  code: string
  title: string
  path: string
  sortNo: number
  visible: number
}

const items = ref<MenuItem[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    items.value = await api<MenuItem[]>('/api/agent/cms/menus')
  } finally {
    loading.value = false
  }
}

async function save() {
  try {
    items.value = await api<MenuItem[]>('/api/agent/cms/menus', {
      method: 'PUT',
      body: { items: items.value },
    })
    ElMessage.success('菜单已保存')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
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
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <span>导航菜单</span>
        <el-button type="primary" @click="save">保存</el-button>
      </div>
    </template>
    <el-table :data="items">
      <el-table-column prop="code" label="代码" width="110" />
      <el-table-column label="标题" min-width="140">
        <template #default="{ row }"><el-input v-model="row.title" /></template>
      </el-table-column>
      <el-table-column label="路径" min-width="140">
        <template #default="{ row }"><el-input v-model="row.path" /></template>
      </el-table-column>
      <el-table-column label="排序" width="110">
        <template #default="{ row }"><el-input-number v-model="row.sortNo" :min="0" :controls="false" /></template>
      </el-table-column>
      <el-table-column label="显示" width="90">
        <template #default="{ row }">
          <el-switch v-model="row.visible" :active-value="1" :inactive-value="0" />
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
