<script setup lang="ts">
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import type { AgentDashboardTrends } from '@liuhecai/shared'

use([CanvasRenderer, LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

const props = defineProps<{
  trends: AgentDashboardTrends | null | undefined
}>()

const trendOption = computed(() => {
  const t = props.trends
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
</script>

<template>
  <VChart class="chart" :option="trendOption" autoresize />
</template>

<style scoped>
.chart { height: 320px; width: 100%; }
</style>
