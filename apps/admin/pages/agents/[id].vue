<script setup lang="ts">
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AdminAgentDetail } from '@liuhecai/shared'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

definePageMeta({ title: '代理业绩' })

const route = useRoute()
const { hydrate } = useAdminAuth()
const {
  getAgent,
  setAgentEnabled,
  resetAgentPassword,
  forceAgentLogout,
  softDeleteAgent,
} = useAdminMgmt()

const loading = ref(false)
const detail = ref<AdminAgentDetail | null>(null)
const echo = ref('')
const id = computed(() => String(route.params.id))

const trendOption = computed(() => {
  const t = detail.value?.trends
  const dates = (t?.dates || []).map((d) => String(d).slice(5))
  return {
    color: ['#7f1d1d', '#2563eb', '#059669'],
    tooltip: { trigger: 'axis' },
    legend: { data: ['注册用户', '购帖订单', '充值金额'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '注册用户', type: 'line', smooth: true, data: t?.users || [] },
      { name: '购帖订单', type: 'line', smooth: true, data: t?.orders || [] },
      { name: '充值金额', type: 'bar', data: t?.rechargeAmount || [] },
    ],
  }
})

async function load() {
  loading.value = true
  try {
    detail.value = await getAgent(id.value)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function fmtTime(v?: string) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '-'
}

async function onToggle() {
  if (!detail.value) return
  const next = detail.value.enabled === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      next === 0 ? '确认停用该代理？' : '确认启用该代理？',
      '提示',
      { type: 'warning' },
    )
    await setAgentEnabled(id.value, next)
    ElMessage.success(next === 1 ? '已启用' : '已停用')
    await load()
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onReset() {
  try {
    await ElMessageBox.confirm('确认重置该代理密码？', '提示', { type: 'warning' })
    const data = await resetAgentPassword(id.value)
    echo.value = `新密码：${data.rawPassword}`
    ElMessage.success('密码已重置')
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '重置失败')
  }
}

async function onForceLogout() {
  try {
    await ElMessageBox.confirm('确认强制失效该代理已登录 Token？', '提示', { type: 'warning' })
    await forceAgentLogout(id.value)
    ElMessage.success('已强制下线')
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onDelete() {
  if (detail.value?.isPrimary === 1) {
    ElMessage.warning('主代理不可注销，请先在站点列表转移主代理')
    return
  }
  try {
    await ElMessageBox.confirm(
      '确认注销该代理？将停用并强制下线，流水保留，可再启用恢复。',
      '注销确认',
      { type: 'warning' },
    )
    await softDeleteAgent(id.value)
    ElMessage.success('已注销')
    await navigateTo('/agents')
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '注销失败')
  }
}

onMounted(async () => {
  hydrate()
  await load()
})
</script>

<template>
  <div v-loading="loading">
    <el-space style="margin-bottom:12px;" wrap>
      <el-button @click="navigateTo('/agents')">返回列表</el-button>
      <el-button @click="load">刷新</el-button>
      <el-button v-if="detail" @click="onToggle">{{ detail.enabled === 1 ? '停用' : '启用' }}</el-button>
      <el-button v-if="detail" type="warning" @click="onReset">重置密码</el-button>
      <el-button v-if="detail" type="danger" plain @click="onForceLogout">强制下线</el-button>
      <el-button v-if="detail" type="danger" :disabled="detail.isPrimary === 1" @click="onDelete">注销</el-button>
    </el-space>

    <el-alert v-if="echo" :title="echo" type="success" show-icon closable style="margin-bottom:12px;" @close="echo = ''" />

    <template v-if="detail">
      <el-card shadow="never">
        <template #header>
          {{ detail.tenantName }} · {{ detail.username }}
          <el-tag :type="detail.enabled === 1 ? 'success' : 'info'" size="small" style="margin-left:8px;">
            {{ detail.enabled === 1 ? '启用' : '停用' }}
          </el-tag>
          <el-tag v-if="detail.isPrimary === 1" size="small" type="warning" style="margin-left:6px;">主代理</el-tag>
        </template>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="代理ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="租户ID">{{ detail.tenantId }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ fmtTime(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-row :gutter="12" style="margin-top:12px;">
        <el-col v-for="item in [
          { label: '注册用户', value: detail.userTotal },
          { label: '今日新增', value: detail.userToday },
          { label: '资料帖', value: detail.topicTotal },
          { label: '待审帖', value: detail.topicPending },
          { label: '待审充值', value: detail.rechargePending },
          { label: '今日充值通过', value: detail.rechargeApprovedAmountToday },
          { label: '今日购帖笔数', value: detail.orderCountToday },
          { label: '今日购帖金额', value: detail.orderAmountToday },
          { label: '累计购帖金额', value: detail.orderAmountTotal },
          { label: '近7日充值', value: detail.rechargeAmount7d },
        ]" :key="item.label" :xs="12" :sm="8" :md="6" :lg="4">
          <el-card shadow="never" class="kpi">
            <div class="kpi-label">{{ item.label }}</div>
            <div class="kpi-value">{{ item.value ?? 0 }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" style="margin-top:12px;">
        <template #header>近 7 日走势</template>
        <ClientOnly>
          <VChart class="chart" :option="trendOption" autoresize />
        </ClientOnly>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.kpi { margin-bottom: 12px; }
.kpi-label { font-size: 12px; color: #6b7280; }
.kpi-value { font-size: 22px; font-weight: 700; color: #7f1d1d; margin-top: 4px; }
.chart { height: 320px; width: 100%; }
</style>
