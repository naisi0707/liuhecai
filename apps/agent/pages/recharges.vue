<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { RechargeVO } from '@liuhecai/shared'
definePageMeta({ title: '充值确认' })
const { api, hydrate } = useAgentAuth()

const rechargeList = ref<RechargeVO[]>([])
const rejectReason = ref('凭证不符')

async function loadRecharges() {
  rechargeList.value = await api<RechargeVO[]>('/api/agent/recharges')
}

async function approveRecharge(id: string) {
  try {
    const data = await api<RechargeVO>(`/api/agent/recharges/${id}/approve`, { method: 'POST' })
    ElMessage.success(`已确认${data.username} +${data.amount}`)
    await loadRecharges()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '确认失败')
  }
}

async function rejectRecharge(id: string) {
  try {
    await api(`/api/agent/recharges/${id}/reject`, {
      method: 'POST',
      body: { reason: rejectReason.value },
    })
    ElMessage.success('已拒绝')
    await loadRecharges()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '拒绝失败')
  }
}

onMounted(async () => {
  hydrate()
  await loadRecharges()
})
</script>

<template>
  <el-card>
    <template #header>充值确认（工单）</template>
    <el-space wrap style="margin-bottom:12px;">
      <el-button @click="loadRecharges">刷新</el-button>
      <el-input v-model="rejectReason" placeholder="拒绝原因" style="width:220px" />
    </el-space>
    <el-table :data="rechargeList" stripe>
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="amount" label="金额" width="80" />
      <el-table-column prop="statusLabel" label="状态" width="100" />
      <el-table-column prop="payChannel" label="渠道" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <template v-if="row.status === 0">
            <el-button size="small" type="primary" @click="approveRecharge(row.id)">确认</el-button>
            <el-button size="small" @click="rejectRecharge(row.id)">拒绝</el-button>
          </template>
          <span v-else-if="row.rejectReason">{{ row.rejectReason }}</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
