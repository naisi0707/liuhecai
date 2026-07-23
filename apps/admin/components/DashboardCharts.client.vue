<script setup lang="ts">
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'
import type { AdminDashboardTrends, AdminNameCount } from '@liuhecai/shared'

use([CanvasRenderer, LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const props = defineProps<{
  trends: AdminDashboardTrends
  topicStatus: AdminNameCount[]
  lotteryTopics: AdminNameCount[]
}>()

const topicStatusLabel: Record<string, string> = {
  '0': '待审',
  '1': '上架',
  '2': '拒绝',
  '3': '下架',
}
const lotteryLabel: Record<string, string> = {
  MACAU_NEW: '新澳门',
  HK: '香港',
  MACAU_OLD: '老澳门',
}

const trendOption = computed(() => {
  const dates = (props.trends?.dates || []).map((d) => String(d).slice(5))
  return {
    color: ['#7f1d1d', '#2563eb', '#059669', '#d97706'],
    tooltip: { trigger: 'axis' },
    legend: { data: ['注册用户', '购帖订单', '充值金额', '新开站点'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '注册用户', type: 'line', smooth: true, data: props.trends?.users || [] },
      { name: '购帖订单', type: 'line', smooth: true, data: props.trends?.orders || [] },
      { name: '充值金额', type: 'bar', data: props.trends?.rechargeAmount || [] },
      { name: '新开站点', type: 'line', smooth: true, data: props.trends?.tenants || [] },
    ],
  }
})

const pieOption = computed(() => ({
  color: ['#d97706', '#059669', '#9ca3af', '#6b7280'],
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['35%', '65%'],
      data: (props.topicStatus || []).map((i) => ({
        name: topicStatusLabel[i.name] || i.name,
        value: i.count,
      })),
      label: { formatter: '{b}: {c}' },
    },
  ],
}))

const lotteryOption = computed(() => ({
  color: ['#7f1d1d'],
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 16, top: 24, bottom: 28 },
  xAxis: {
    type: 'category',
    data: (props.lotteryTopics || []).map((i) => lotteryLabel[i.name] || i.name),
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    {
      type: 'bar',
      barWidth: 28,
      data: (props.lotteryTopics || []).map((i) => i.count),
    },
  ],
}))
</script>

<template>
  <el-row :gutter="16">
    <el-col :xs="24" :lg="14">
      <el-card shadow="never" class="chart-card">
        <template #header>运营走势</template>
        <VChart class="chart-lg" :option="trendOption" autoresize />
      </el-card>
    </el-col>
    <el-col :xs="24" :md="12" :lg="5">
      <el-card shadow="never" class="chart-card">
        <template #header>资料状态分布</template>
        <VChart v-if="topicStatus?.length" class="chart-sm" :option="pieOption" autoresize />
        <el-empty v-else description="暂无资料" :image-size="64" />
      </el-card>
    </el-col>
    <el-col :xs="24" :md="12" :lg="5">
      <el-card shadow="never" class="chart-card">
        <template #header>彩种资料量</template>
        <VChart v-if="lotteryTopics?.length" class="chart-sm" :option="lotteryOption" autoresize />
        <el-empty v-else description="暂无数据" :image-size="64" />
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.chart-card { margin-top: 16px; }
.chart-lg { height: 320px; width: 100%; }
.chart-sm { height: 280px; width: 100%; }
</style>
