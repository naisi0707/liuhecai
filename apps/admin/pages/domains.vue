<script setup lang="ts">
import { ElMessage } from 'element-plus'
definePageMeta({ title: '域名绑定' })
const { api, hydrate } = useAdminAuth()

const tenants = ref<any[]>([])
const bindTenantId = ref<number | string | null>(null)
const bindHost = ref('')
const isPrimary = ref(0)
const role = ref<'FORUM' | 'ENTRY'>('FORUM')
const loading = ref(false)

const lineDrawer = ref(false)
const lineDomain = ref<{ id: number | string; host: string } | null>(null)
const lines = ref<Array<{
  sortOrder: number
  label: string
  color: string
  targetTenantId: number | string | null
  status: number
}>>([])
const linesLoading = ref(false)
const linesSaving = ref(false)

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
      body: { host: bindHost.value.trim(), isPrimary: isPrimary.value, role: role.value },
    })
    bindHost.value = ''
    role.value = 'FORUM'
    await load()
    ElMessage.success('域名已绑定')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '绑定失败')
  } finally {
    loading.value = false
  }
}

const selected = computed(() => tenants.value.find((t) => t.id === bindTenantId.value))

function defaultFive(tenantId: number | string | null) {
  const colors = ['#c62828', '#1565c0', '#2e7d32', '#6a1b9a', '#ef6c00']
  const labels = ['电信临时线路', '移动临时线路', '联通临时线路', '广电临时线路', '澳门直达专线']
  return labels.map((label, i) => ({
    sortOrder: i + 1,
    label,
    color: colors[i],
    targetTenantId: tenantId,
    status: 1,
  }))
}

async function openLines(domain: { id: number | string; host: string; role?: string }) {
  if ((domain.role || 'FORUM') !== 'ENTRY') return
  lineDomain.value = domain
  lineDrawer.value = true
  linesLoading.value = true
  try {
    const data = await api(`/api/admin/entry-domains/${domain.id}/lines`)
    lines.value = Array.isArray(data) ? data : []
    if (!lines.value.length) {
      lines.value = defaultFive(bindTenantId.value || tenants.value[0]?.id)
    }
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加载线路失败')
    lines.value = defaultFive(bindTenantId.value || tenants.value[0]?.id)
  } finally {
    linesLoading.value = false
  }
}

function addLine() {
  lines.value.push({
    sortOrder: lines.value.length + 1,
    label: '新线路',
    color: '#c62828',
    targetTenantId: bindTenantId.value || tenants.value[0]?.id || null,
    status: 1,
  })
}

function removeLine(index: number) {
  lines.value.splice(index, 1)
}

function restoreDefaultFive() {
  lines.value = defaultFive(bindTenantId.value || tenants.value[0]?.id)
}

async function saveLines() {
  if (!lineDomain.value) return
  for (const line of lines.value) {
    if (!line.label?.trim()) {
      ElMessage.warning('线路名称不能为空')
      return
    }
    if (!line.targetTenantId) {
      ElMessage.warning('请为每条线路选择目标租户')
      return
    }
  }
  linesSaving.value = true
  try {
    lines.value = await api(`/api/admin/entry-domains/${lineDomain.value.id}/lines`, {
      method: 'PUT',
      body: {
        lines: lines.value.map((l, idx) => ({
          sortOrder: idx + 1,
          label: l.label.trim(),
          color: (l.color || '#c62828').trim(),
          targetTenantId: l.targetTenantId,
          status: l.status === 0 ? 0 : 1,
        })),
      },
    })
    ElMessage.success('线路已保存')
    lineDrawer.value = false
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    linesSaving.value = false
  }
}

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
          <el-form-item label="角色">
            <el-select v-model="role" style="width:100%">
              <el-option label="论坛 FORUM" value="FORUM" />
              <el-option label="入口伪装 ENTRY" value="ENTRY" />
            </el-select>
          </el-form-item>
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
            <el-table-column label="角色" width="100">
              <template #default="{ row }">{{ row.role || 'FORUM' }}</template>
            </el-table-column>
            <el-table-column label="主域名" width="90">
              <template #default="{ row }">{{ row.isPrimary === 1 ? '是' : '' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button
                  v-if="(row.role || 'FORUM') === 'ENTRY'"
                  link
                  type="primary"
                  @click="openLines(row)"
                >
                  配置线路
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
        <p v-else style="color:#6b7280;">请选择左侧租户查看已有域名</p>
      </el-card>
    </el-col>
  </el-row>

  <el-drawer
    v-model="lineDrawer"
    :title="lineDomain ? `配置线路 · ${lineDomain.host}` : '配置线路'"
    size="720px"
    destroy-on-close
  >
    <div v-loading="linesLoading">
      <el-table :data="lines" size="small" style="width:100%">
        <el-table-column label="排序" width="60">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="名称" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.label" maxlength="64" />
          </template>
        </el-table-column>
        <el-table-column label="颜色" width="120">
          <template #default="{ row }">
            <el-color-picker v-model="row.color" />
          </template>
        </el-table-column>
        <el-table-column label="目标租户" min-width="180">
          <template #default="{ row }">
            <el-select v-model="row.targetTenantId" placeholder="选择租户" style="width:100%">
              <el-option
                v-for="t in tenants"
                :key="t.id"
                :label="`${t.name} (#${t.id})`"
                :value="t.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="" width="70">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeLine($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:12px;display:flex;gap:8px;flex-wrap:wrap;">
        <el-button @click="addLine">新增线路</el-button>
        <el-button @click="restoreDefaultFive">恢复默认五条</el-button>
        <el-button type="primary" :loading="linesSaving" @click="saveLines">保存</el-button>
      </div>
    </div>
  </el-drawer>
</template>
