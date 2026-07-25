<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'

definePageMeta({ title: '站点列表' })
const { api, hydrate } = useAdminAuth()
const { updateTenant, setPrimaryAgent } = useAdminMgmt()

const tenants = ref<any[]>([])
const loading = ref(false)
const creating = ref(false)
const editVisible = ref(false)
const editForm = reactive({ id: '' as '' | number, name: '', announcement: '' })
const saving = ref(false)

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

function primaryAgent(row: any) {
  return (row.agents || []).find((a: any) => a.isPrimary === 1) || (row.agents || [])[0] || null
}

function openEdit(row: any) {
  editForm.id = row.id
  editForm.name = row.name || ''
  editForm.announcement = row.announcement || ''
  editVisible.value = true
}

async function saveEdit() {
  if (!editForm.name.trim()) {
    ElMessage.warning('站名不能为空')
    return
  }
  saving.value = true
  try {
    await updateTenant(editForm.id, {
      name: editForm.name.trim(),
      announcement: editForm.announcement,
    })
    ElMessage.success('已保存')
    editVisible.value = false
    await load()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onSetPrimary(row: any, agent: any) {
  if (agent.isPrimary === 1) return
  if (agent.enabled !== 1) {
    ElMessage.warning('只能设置已启用的代理为主代理')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认将 ${agent.username} 设为站点「${row.name}」的主代理？`,
      '设为主代理',
      { type: 'warning' },
    )
    await setPrimaryAgent(row.id, agent.id)
    ElMessage.success('主代理已更新')
    await load()
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '设置失败')
  }
}

async function createAgent(row: any) {
  try {
    const { value } = await ElMessageBox.prompt(
      `为站点「${row.name}」创建代理账号`,
      '创建代理',
      {
        confirmButtonText: '创建',
        cancelButtonText: '取消',
        inputPlaceholder: '代理用户名',
        inputValue: `agent_${row.id}`,
        inputValidator: (v) => (!!v && String(v).trim().length >= 2) || '请输入用户名',
      },
    )
    creating.value = true
    const res = await api<{ username: string; rawPassword?: string; isPrimary?: number }>(
      `/api/admin/tenants/${row.id}/agents`,
      { method: 'POST', body: { username: String(value).trim() } },
    )
    await load()
    if (res?.rawPassword) {
      await ElMessageBox.alert(
        `账号：${res.username}\n初始密码：${res.rawPassword}\n请立即保存，关闭后无法再查看明文。`,
        '代理已创建',
        { confirmButtonText: '已保存' },
      )
    } else {
      ElMessage.success('代理已创建')
    }
  } catch (e: unknown) {
    if (e === 'cancel' || (e && typeof e === 'object' && 'action' in e && (e as any).action === 'cancel')) {
      return
    }
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  } finally {
    creating.value = false
  }
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

    <el-card v-loading="loading || creating">
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
        <el-table-column label="代理" min-width="260">
          <template #default="{ row }">
            <div v-for="a in (row.agents || [])" :key="a.id" style="margin-bottom:4px;">
              {{ a.username }}
              <el-tag v-if="a.isPrimary === 1" size="small" type="warning" style="margin-left:4px;">主</el-tag>
              <el-button
                v-if="a.isPrimary !== 1"
                size="small"
                link
                type="warning"
                @click="onSetPrimary(row, a)"
              >设为主代理</el-button>
            </div>
            <span v-if="!(row.agents || []).length">-</span>
            <div style="margin-top:4px;">
              <el-button size="small" type="primary" link @click="createAgent(row)">添加代理</el-button>
              <el-button
                size="small"
                link
                type="primary"
                @click="navigateTo(`/agents?tenantId=${row.id}`)"
              >代理管理</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" title="编辑站点" width="480px">
      <el-form label-width="80px">
        <el-form-item label="站名" required>
          <el-input v-model="editForm.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="公告">
          <el-input v-model="editForm.announcement" type="textarea" :rows="3" maxlength="512" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
