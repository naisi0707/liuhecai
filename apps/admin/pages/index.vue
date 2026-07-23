<script setup lang="ts">
import type { AdminDashboardVO } from '@liuhecai/shared'

definePageMeta({ title: '总览' })

const { hydrate } = useAdminAuth()
const { fetchDashboard } = useAdminDashboard()

const loading = ref(false)
const days = ref(7)
const data = ref<AdminDashboardVO | null>(null)

const kpis = computed(() => data.value?.kpis)
const lotteryLabel: Record<string, string> = {
  MACAU_NEW: '新澳门',
  HK: '香港',
  MACAU_OLD: '老澳门',
}

const activityTypeLabel: Record<string, string> = {
  COIN: '金币',
  RECHARGE: '充值',
  TOPIC: '资料',
}

const topicStatusLabel: Record<string, string> = {
  '0': '待审',
  '1': '上架',
  '2': '拒绝',
  '3': '下架',
}

const rechargeStatusLabel: Record<string, string> = {
  '0': '待审',
  '1': '通过',
  '2': '拒绝',
}

function activityTitle(a: NonNullable<AdminDashboardVO['activities']>[number]) {
  const site = a.tenantName || '-'
  if (a.type === 'COIN') {
    return `${site} · ${a.bizType || 'COIN'} ${a.amount ?? 0} · 用户#${a.userId ?? '-'}`
  }
  if (a.type === 'RECHARGE') {
    const st = rechargeStatusLabel[String(a.status ?? '')] || String(a.status ?? '')
    return `${site} · 充值${st} ${a.amount ?? 0} · 用户#${a.userId ?? '-'}`
  }
  if (a.type === 'TOPIC') {
    const st = topicStatusLabel[String(a.status ?? '')] || String(a.status ?? '')
    return `${site} · 资料[${st}] ${a.topicTitle || ''}`
  }
  return site
}

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

function fmtTime(v?: string | null) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

onMounted(async () => {
  hydrate()
  await load()
})
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <div class="toolbar">
      <el-space wrap>
        <el-button type="primary" @click="navigateTo('/tenants/create')">开站创建</el-button>
        <el-button @click="navigateTo('/tenants')">站点列表</el-button>
        <el-button @click="navigateTo('/domains')">域名绑定</el-button>
        <el-button @click="load">刷新</el-button>
      </el-space>
      <el-radio-group v-model="days" size="small" @change="(v: string | number | boolean) => onDaysChange(Number(v))">
        <el-radio-button :value="7">近7天</el-radio-button>
        <el-radio-button :value="14">近14天</el-radio-button>
        <el-radio-button :value="30">近30天</el-radio-button>
      </el-radio-group>
    </div>

    <el-row :gutter="12" class="kpi-row">
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="kpi"><div class="stat-label">站点总数</div><div class="stat-num">{{ kpis?.tenantTotal ?? 0 }}</div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="kpi"><div class="stat-label">启用 / 停用</div><div class="stat-num ok">{{ kpis?.tenantEnabled ?? 0 }} <span class="muted">/ {{ kpis?.tenantDisabled ?? 0 }}</span></div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="kpi"><div class="stat-label">域名数</div><div class="stat-num">{{ kpis?.domainTotal ?? 0 }}</div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="kpi"><div class="stat-label">代理账号</div><div class="stat-num">{{ kpis?.agentTotal ?? 0 }} <span class="muted">启 {{ kpis?.agentEnabled ?? 0 }}</span></div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="kpi"><div class="stat-label">注册用户</div><div class="stat-num">{{ kpis?.userTotal ?? 0 }} <span class="muted">今+{{ kpis?.userToday ?? 0 }}</span></div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="kpi"><div class="stat-label">资料帖</div><div class="stat-num">{{ kpis?.topicTotal ?? 0 }} <span class="muted">待审(代理) {{ kpis?.topicPending ?? 0 }}</span></div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="kpi"><div class="stat-label">待审充值(代理)</div><div class="stat-num warn">{{ kpis?.rechargePending ?? 0 }}</div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="kpi"><div class="stat-label">今日充值通过</div><div class="stat-num ok">{{ kpis?.rechargeApprovedAmountToday ?? 0 }}</div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="kpi"><div class="stat-label">今日购帖</div><div class="stat-num">{{ kpis?.orderCountToday ?? 0 }} <span class="muted">额 {{ kpis?.orderAmountToday ?? 0 }}</span></div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="kpi"><div class="stat-label">今日加币</div><div class="stat-num">{{ kpis?.coinGrantToday ?? 0 }}</div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="kpi"><div class="stat-label">今日充值流水</div><div class="stat-num">{{ kpis?.coinRechargeToday ?? 0 }}</div></el-card>
      </el-col>
      <el-col :xs="12" :sm="8" :md="6" :lg="4">
        <el-card shadow="hover" class="kpi"><div class="stat-label">今日购帖扣币</div><div class="stat-num">{{ kpis?.coinPurchaseToday ?? 0 }}</div></el-card>
      </el-col>
    </el-row>

    <DashboardCharts
      v-if="data"
      :trends="data.trends"
      :topic-status="data.topicStatus"
      :lottery-topics="data.lotteryTopics"
    />

    <el-row :gutter="16" style="margin-top:16px;">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header>代理账号</template>
          <el-table :data="data?.agents || []" stripe size="small" max-height="360">
            <el-table-column prop="username" label="账号" min-width="110" />
            <el-table-column prop="tenantName" label="所属站点" min-width="120" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
                  {{ row.enabled === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="150">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never">
          <template #header>站点排行（用户/购帖）</template>
          <el-table :data="data?.tenantRanks || []" stripe size="small" max-height="360">
            <el-table-column prop="tenantName" label="站点" min-width="110" />
            <el-table-column prop="userCount" label="用户" width="70" />
            <el-table-column prop="orderCount" label="购帖" width="70" />
            <el-table-column prop="primaryHost" label="主域" min-width="120" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:16px;">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>运营动态</template>
          <el-timeline v-if="data?.activities?.length">
            <el-timeline-item
              v-for="(a, i) in data.activities"
              :key="i"
              :timestamp="fmtTime(a.createdAt)"
              placement="top"
            >
              <el-tag size="small" style="margin-right:6px;">{{ activityTypeLabel[a.type] || a.type }}</el-tag>
              {{ activityTitle(a) }}
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无动态" :image-size="72" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" style="margin-bottom:16px;">
          <template #header>开奖新鲜度</template>
          <el-table :data="data?.draws || []" stripe size="small">
            <el-table-column label="彩种" width="100">
              <template #default="{ row }">{{ lotteryLabel[row.lotteryType] || row.lotteryType }}</template>
            </el-table-column>
            <el-table-column prop="issueNo" label="期号" width="100" />
            <el-table-column label="开奖时间" min-width="150">
              <template #default="{ row }">{{ fmtTime(row.drawTime) }}</template>
            </el-table-column>
            <el-table-column prop="source" label="来源" width="90" />
          </el-table>
          <el-empty v-if="!data?.draws?.length" description="暂无开奖" :image-size="64" />
        </el-card>
        <el-card shadow="never">
          <template #header>最近开站</template>
          <el-table :data="data?.recentTenants || []" stripe size="small">
            <el-table-column prop="name" label="站名" min-width="110" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="primaryHost" label="主域" min-width="120" show-overflow-tooltip />
            <el-table-column label="创建" min-width="140">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard { min-height: 360px; }
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.kpi-row { row-gap: 12px; }
.kpi { min-height: 88px; }
.stat-label { color: #6b7280; font-size: 13px; }
.stat-num { font-size: 24px; font-weight: 700; color: #111827; margin-top: 6px; line-height: 1.2; }
.stat-num.ok { color: #059669; }
.stat-num.warn { color: #d97706; }
.muted { font-size: 13px; font-weight: 500; color: #9ca3af; }
</style>
