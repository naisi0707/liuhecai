<script setup lang="ts">
import type { CmsKefuContent, CmsRechargeContent, RechargeVO } from '@liuhecai/shared'
import { request } from '@liuhecai/shared'

const props = withDefaults(defineProps<{
  mode?: 'recharge' | 'kefu'
}>(), { mode: 'recharge' })

const { tenant, errorMsg } = useTenant()
const { token, authHeaders, refreshProfile } = useAuth()
const { loadPage, pageContent } = useSiteCms()
const { mediaUrl } = useMediaUrl()

const recharge = computed(() => pageContent<CmsRechargeContent>('recharge'))
const kefu = computed(() => pageContent<CmsKefuContent>('kefu'))

const tiers = computed(() => recharge.value?.tiers || [])
const exchangeRate = computed(() => recharge.value?.exchangeRate || '')
const declareText = computed(() => recharge.value?.declareText || '')
const notes = computed(() => recharge.value?.notes || [])

const form = reactive({
  amount: 500,
  payChannel: '微信转账',
  remark: '已按客服指引转账',
})
const list = ref<RechargeVO[]>([])

const title = computed(() => {
  if (props.mode === 'kefu') return kefu.value?.heading || ''
  return recharge.value?.heading || ''
})

const contact = computed(() => tenant.value?.kefuWechat || '')

const qrWechat = computed(() => {
  const url = props.mode === 'kefu'
    ? (kefu.value?.qrWechatUrl || recharge.value?.qrWechatUrl)
    : recharge.value?.qrWechatUrl
  return url ? mediaUrl(url) : ''
})
const qrQq = computed(() => {
  const url = props.mode === 'kefu'
    ? (kefu.value?.qrQqUrl || recharge.value?.qrQqUrl)
    : recharge.value?.qrQqUrl
  return url ? mediaUrl(url) : ''
})

const intro = computed(() => (props.mode === 'kefu' ? (kefu.value?.intro || '') : ''))

async function loadList() {
  if (!token.value) return
  list.value = await request<RechargeVO[]>('/api/user/recharges', { headers: authHeaders() })
}

async function submit() {
  errorMsg.value = ''
  if (!token.value) {
    errorMsg.value = '请先登录再提交充值申请'
    await navigateTo('/login')
    return
  }
  await request('/api/user/recharges', {
    method: 'POST',
    headers: authHeaders(),
    body: { ...form },
  })
  await loadList()
  await refreshProfile()
}

onMounted(async () => {
  try {
    await loadPage('recharge')
    if (props.mode === 'kefu') await loadPage('kefu')
    await loadList()
  } catch {
    // busy 已标记
  }
})
</script>

<template>
  <section class="recharge-box">
    <h3 v-if="title" class="recharge-box__title">{{ title }}</h3>
    <SafeHtml v-if="intro" :html="intro" style="text-align:left;margin:8px 0;" />
    <div v-if="contact || tenant?.kefuQq" class="recharge-box__contact">
      <template v-if="contact">充值金币联系：{{ contact }}</template>
      <span v-if="tenant?.kefuQq">　QQ：{{ tenant.kefuQq }}</span>
    </div>

    <div v-if="qrWechat || qrQq" class="recharge-box__qr-ph">
      <img v-if="qrWechat" :src="qrWechat" alt="微信" width="206" />
      <img v-if="qrQq" :src="qrQq" alt="QQ" width="206" />
    </div>

    <ul v-if="mode === 'recharge' && tiers.length" class="recharge-box__tiers">
      <li v-for="t in tiers" :key="t">{{ t }}</li>
    </ul>
    <div v-if="mode === 'recharge' && exchangeRate" class="recharge-box__rate">{{ exchangeRate }}</div>

    <div v-if="mode === 'recharge' && declareText" class="recharge-box__declare" style="text-align:left;">
      <strong>声明：</strong>
      <SafeHtml :html="declareText" />
    </div>
    <div v-for="(n, i) in (mode === 'recharge' ? notes : [])" :key="i" class="recharge-box__note">
      <SafeHtml :html="n" />
    </div>

    <div v-if="mode === 'recharge'" class="recharge-box__form" style="text-align:left;margin-top:16px;border-top:1px dashed #ddd;padding-top:12px;">
      <h4>提交充值申请</h4>
      <label style="display:flex;gap:8px;margin-bottom:8px;">金额 <input v-model.number="form.amount" type="number" min="1" style="flex:1;height:36px;" /></label>
      <label style="display:flex;gap:8px;margin-bottom:8px;">渠道 <input v-model="form.payChannel" style="flex:1;height:36px;" /></label>
      <label style="display:flex;gap:8px;margin-bottom:8px;">备注 <input v-model="form.remark" style="flex:1;height:36px;" /></label>
      <button type="button" class="btn-buy" @click="submit">提交申请</button>
      <div v-for="r in list" :key="r.id" style="padding:6px 0;border-bottom:1px dashed #eee;">
        {{ r.amount }}币 · {{ r.statusLabel }}
        <span v-if="r.rejectReason" style="color:#c00;margin-left:8px;">{{ r.rejectReason }}</span>
      </div>
    </div>
  </section>
</template>
