<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { DrawResultVO } from '@liuhecai/shared'
definePageMeta({ title: '开奖管理' })
const { api, hydrate } = useAgentAuth()

const drawPreview = ref<DrawResultVO[]>([])
const overrideForm = reactive({
  lotteryType: 'MACAU_NEW',
  issueNo: '',
  drawTime: '',
  n1: '01', n2: '02', n3: '03', n4: '04', n5: '05', n6: '06',
  specialNumber: '07',
  note: '手工补录',
})

async function loadDraws() {
  drawPreview.value = await api<DrawResultVO[]>('/api/agent/draws/latest-all')
}

async function fetchDraws() {
  try {
    const summary = await api<{ saved: string[]; failed: string[] }>('/api/agent/draws/fetch', { method: 'POST' })
    ElMessage.success(`拉取完成：成功${summary.saved?.join(',') || '-'}`)
    await loadDraws()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '拉取失败')
  }
}

async function submitOverride() {
  try {
    const drawTime = overrideForm.drawTime || new Date().toISOString().slice(0, 19)
    await api('/api/agent/draws/override', {
      method: 'POST',
      body: {
        lotteryType: overrideForm.lotteryType,
        issueNo: overrideForm.issueNo,
        drawTime,
        numbers: [overrideForm.n1, overrideForm.n2, overrideForm.n3, overrideForm.n4, overrideForm.n5, overrideForm.n6],
        specialNumber: overrideForm.specialNumber,
        note: overrideForm.note,
      },
    })
    ElMessage.success('覆盖已保存')
    await loadDraws()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '覆盖失败')
  }
}

onMounted(async () => {
  hydrate()
  await loadDraws()
})
</script>

<template>
  <el-card>
    <el-space wrap style="margin-bottom:12px;" alignment="center">
      <el-button type="primary" @click="fetchDraws">手动拉取</el-button>
      <el-button @click="loadDraws">刷新</el-button>
      <span style="color:#6b7280;font-size:13px;">公开源拉取，写入共享开奖库（全站可用）</span>
    </el-space>
    <el-descriptions v-for="d in drawPreview" :key="d.lotteryType" :title="d.lotteryLabel" border style="margin-bottom:12px;">
      <el-descriptions-item label="期号">{{ d.issueNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="号码">{{ (d.numbers || []).join(',') }}+{{ d.specialNumber || '' }}</el-descriptions-item>
    </el-descriptions>
    <el-divider>手工覆盖（仅本站展示）</el-divider>
    <el-form inline>
      <el-form-item label="彩种">
        <el-select v-model="overrideForm.lotteryType" style="width:140px;">
          <el-option label="新澳门" value="MACAU_NEW" />
          <el-option label="香港" value="HK" />
          <el-option label="老澳门" value="MACAU_OLD" />
        </el-select>
      </el-form-item>
      <el-form-item label="期号"><el-input v-model="overrideForm.issueNo" /></el-form-item>
    </el-form>
    <el-space wrap>
      <el-input v-model="overrideForm.n1" style="width:56px" />
      <el-input v-model="overrideForm.n2" style="width:56px" />
      <el-input v-model="overrideForm.n3" style="width:56px" />
      <el-input v-model="overrideForm.n4" style="width:56px" />
      <el-input v-model="overrideForm.n5" style="width:56px" />
      <el-input v-model="overrideForm.n6" style="width:56px" />
      <span>+</span>
      <el-input v-model="overrideForm.specialNumber" style="width:56px" />
      <el-button type="primary" @click="submitOverride">保存覆盖</el-button>
    </el-space>
  </el-card>
</template>
