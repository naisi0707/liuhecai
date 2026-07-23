<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AgentUserDetail, UserCoinLogItem, UserOrderItem } from '@liuhecai/shared'

definePageMeta({ title: '用户详情' })

const route = useRoute()
const { hydrate } = useAgentAuth()
const {
  getUser,
  setUserEnabled,
  resetUserPassword,
  forceUserLogout,
  adjustCoins,
  userCoinLogs,
  userOrders,
} = useAgentMgmt()

const loading = ref(false)
const detail = ref<AgentUserDetail | null>(null)
const tab = ref('logs')
const logs = ref<UserCoinLogItem[]>([])
const orders = ref<UserOrderItem[]>([])
const logTotal = ref(0)
const orderTotal = ref(0)
const logPage = ref(1)
const orderPage = ref(1)
const echo = ref('')
const coinDialog = ref(false)
const coinForm = reactive({ amount: 100, remark: '' })
const id = computed(() => String(route.params.id))

async function loadDetail() {
  loading.value = true
  try {
    detail.value = await getUser(id.value)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadLogs() {
  const data = await userCoinLogs(id.value, logPage.value, 20)
  logs.value = data.records || []
  logTotal.value = data.total || 0
}

async function loadOrders() {
  const data = await userOrders(id.value, orderPage.value, 20)
  orders.value = data.records || []
  orderTotal.value = data.total || 0
}

async function onToggle() {
  if (!detail.value) return
  const next = detail.value.enabled === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(next === 0 ? '确认停用该用户？' : '确认启用该用户？', '提示', { type: 'warning' })
    detail.value = await setUserEnabled(id.value, next)
    ElMessage.success(next === 1 ? '已启用' : '已停用')
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onReset() {
  try {
    await ElMessageBox.confirm('确认重置密码？', '提示', { type: 'warning' })
    const data = await resetUserPassword(id.value)
    echo.value = `新密码：${data.rawPassword}`
    ElMessage.success('密码已重置')
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '重置失败')
  }
}

async function onForceLogout() {
  try {
    await ElMessageBox.confirm('确认强制失效该用户已登录 Token？', '提示', { type: 'warning' })
    await forceUserLogout(id.value)
    ElMessage.success('已强制下线')
  } catch (e: unknown) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function submitCoins() {
  if (!coinForm.amount) {
    ElMessage.warning('请输入非 0 金额')
    return
  }
  try {
    const data = await adjustCoins(id.value, coinForm.amount, coinForm.remark || undefined)
    ElMessage.success(`余额已更新为 ${data.coinBalance}`)
    coinDialog.value = false
    await loadDetail()
    await loadLogs()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '调币失败')
  }
}

function fmtTime(v?: string | null) {
  return v ? String(v).replace('T', ' ').slice(0, 19) : '-'
}

watch(tab, (v) => {
  if (v === 'logs') loadLogs()
  if (v === 'orders') loadOrders()
})

onMounted(async () => {
  hydrate()
  await loadDetail()
  await loadLogs()
  if (String(route.query.adjust || '') === '1') {
    coinDialog.value = true
    await navigateTo({ path: `/users/${id.value}` }, { replace: true })
  }
})
</script>

<template>
  <div v-loading="loading">
    <el-space style="margin-bottom:12px;" wrap>
      <el-button @click="navigateTo('/users')">返回列表</el-button>
      <el-button @click="loadDetail">刷新</el-button>
      <el-button type="primary" @click="coinDialog = true">加减币</el-button>
      <el-button v-if="detail" @click="onToggle">{{ detail.enabled === 1 ? '停用' : '启用' }}</el-button>
      <el-button v-if="detail" type="warning" @click="onReset">重置密码</el-button>
      <el-button v-if="detail" type="danger" plain @click="onForceLogout">强制下线</el-button>
    </el-space>

    <el-alert v-if="echo" :title="echo" type="success" show-icon style="margin-bottom:12px;" @close="echo = ''" />

    <el-card v-if="detail" shadow="never">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ detail.username }}</el-descriptions-item>
        <el-descriptions-item label="余额">{{ detail.coinBalance ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.enabled === 1 ? 'success' : 'danger'" size="small">
            {{ detail.enabled === 1 ? '正常' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ fmtTime(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top:12px;">
      <el-tabs v-model="tab">
        <el-tab-pane label="金币流水" name="logs">
          <el-table :data="logs" stripe>
            <el-table-column prop="bizType" label="类型" width="110" />
            <el-table-column prop="changeAmount" label="变动" width="100" />
            <el-table-column prop="balanceAfter" label="余额" width="100" />
            <el-table-column prop="remark" label="备注" min-width="160" />
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
          <div style="margin-top:12px; display:flex; justify-content:flex-end;">
            <el-pagination
              v-model:current-page="logPage"
              layout="total, prev, pager, next"
              :total="logTotal"
              @current-change="loadLogs"
            />
          </div>
        </el-tab-pane>
        <el-tab-pane label="购帖订单" name="orders">
          <el-table :data="orders" stripe>
            <el-table-column prop="topicTitle" label="资料" min-width="200" />
            <el-table-column prop="price" label="价格" width="100" />
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
          <div style="margin-top:12px; display:flex; justify-content:flex-end;">
            <el-pagination
              v-model:current-page="orderPage"
              layout="total, prev, pager, next"
              :total="orderTotal"
              @current-change="loadOrders"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="coinDialog" title="加减币" width="420px">
      <el-form label-width="80px">
        <el-form-item label="金额">
          <el-input-number v-model="coinForm.amount" :step="10" />
          <div style="font-size:12px;color:#6b7280;margin-top:4px;">正数加币，负数扣币</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="coinForm.remark" maxlength="256" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="coinDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCoins">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>
