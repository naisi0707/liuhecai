<script setup lang="ts">
import type { AgentDashboardVO } from '@liuhecai/shared'

definePageMeta({ title: '运营看板' })

const { hydrate } = useAgentAuth()
const { fetchDashboard } = useAgentMgmt()

const loading = ref(false)
const days = ref(7)
const data = ref<AgentDashboardVO | null>(null)
const kpis = computed(() => data.value?.kpis)

async function load() {
  loading.value = true
  try {
    data.value = await fetchDashboard(days.value)
  } finally {
    loading.value = false
  }
}

function onDaysChange(v: number) {
  if (![7, 14, 30].includes(v)) return
  days.value = v
  load()
}

onMounted(async () => {
  hydrate()
  await load()
})
</script>

<template>
  <div v-loading="loading">
    <div class="toolbar">
      <el-space wrap>
        <el-button type="primary" @click="navigateTo('/users')">用户管理</el-button>
        <el-button @click="navigateTo('/recharges')">充值确认</el-button>
        <el-button @click="navigateTo('/topics')">资料管理</el-button>
        <el-button @click="load">刷新</el-button>
      </el-space>
      <el-radio-group :model-value="days" size="small" @change="onDaysChange">
        <el-radio-button :value="7">近7天</el-radio-button>
        <el-radio-button :value="14">近14天</el-radio-button>
        <el-radio-button :value="30">近30天</el-radio-button>
      </el-radio-group>
    </div>

    <el-row :gutter="12">
      <el-col v-for="item in [
        { label: '注册用户', value: kpis?.userTotal },
        { label: '今日新增', value: kpis?.userToday },
        { label: '待审资料', value: kpis?.topicPending },
        { label: '待审充值', value: kpis?.rechargePending },
        { label: '今日充值通过', value: kpis?.rechargeApprovedAmountToday },
        { label: '今日购帖笔数', value: kpis?.orderCountToday },
        { label: '今日购帖金额', value: kpis?.orderAmountToday },
      ]" :key="item.label" :xs="12" :sm="8" :md="6">
        <el-card shadow="never" class="kpi">
          <div class="kpi-label">{{ item.label }}</div>
          <div class="kpi-value">{{ item.value ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top:12px;">
      <template #header>运营走势</template>
      <ClientOnly>
        <DashboardCharts :trends="data?.trends" />
      </ClientOnly>
    </el-card>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.kpi { margin-bottom: 12px; }
.kpi-label { font-size: 12px; color: #6b7280; }
.kpi-value { font-size: 22px; font-weight: 700; color: #7f1d1d; margin-top: 4px; }
</style>
