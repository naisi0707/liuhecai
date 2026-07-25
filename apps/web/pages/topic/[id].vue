<script setup lang="ts">
import { request, type TopicVO, type PurchaseResultVO } from '@liuhecai/shared'

const route = useRoute()
const { authHeaders, token, coinBalance } = useAuth()
const { errorMsg, tenant, siteName } = useTenant()

const buying = ref(false)

const topicId = computed(() => String(route.params.id || ''))

const { data: topic, refresh } = await useAsyncData(
  () => `topic-${topicId.value}`,
  async () => {
    if (!topicId.value) return null
    try {
      return await request<TopicVO>(`/api/topics/${topicId.value}`, { headers: authHeaders() })
    } catch {
      return null
    }
  },
  { watch: [topicId] },
)

async function purchase() {
  if (!topic.value) return
  if (!token.value) {
    errorMsg.value = '请先登录再购买'
    await navigateTo('/login')
    return
  }
  buying.value = true
  errorMsg.value = ''
  try {
    const result = await request<PurchaseResultVO>(`/api/user/topics/${topic.value.id}/purchase`, {
      method: 'POST',
      headers: authHeaders(),
    })
    coinBalance.value = result.coinBalance
    await refresh()
  } finally {
    buying.value = false
  }
}
</script>

<template>
  <div>
    <HeroBanner />
    <div class="draw-slot"><DrawPanel /></div>

    <article v-if="topic" class="topic-detail">
      <h1 class="topic-detail__title">{{ topic.title }}</h1>

      <div class="topic-detail__meta">
        <img class="topic-detail__avatar" src="/site/icons/headhunt.gif" alt="" width="48" height="48" />
        <div class="topic-detail__meta-text">
          <span>作者：{{ topic.playType || '高手' }}</span>
          <span class="topic-detail__views">浏览量：{{ topic.viewCount ?? 0 }}</span>
        </div>
      </div>

      <div v-if="topic.previewContent" class="topic-detail__preview rich-content">
        <SafeHtml :html="topic.previewContent" />
      </div>

      <div v-if="topic.contentVisible" class="topic-detail__body rich-content">
        <SafeHtml :html="topic.content" />
      </div>
      <blockquote v-else class="topic-detail__locked">
        高手已加密，购买即可查看资料
      </blockquote>

      <p class="topic-detail__price">
        此贴售价 <strong>{{ topic.price }}</strong> 金币，已有 {{ topic.purchaseCount ?? 0 }} 人购买。
      </p>
      <button
        v-if="!topic.contentVisible"
        type="button"
        class="btn-buy"
        :disabled="buying"
        @click="purchase"
      >
        购买资料
      </button>

      <ul v-if="topic.prevTopicId || topic.nextTopicId" class="topic-detail__nav">
        <li v-if="topic.nextTopicId">
          <NuxtLink :to="`/topic/${topic.nextTopicId}`">下一贴：{{ topic.nextTopicTitle }}</NuxtLink>
        </li>
        <li v-if="topic.prevTopicId">
          <NuxtLink :to="`/topic/${topic.prevTopicId}`">上一贴：{{ topic.prevTopicTitle }}</NuxtLink>
        </li>
      </ul>

      <div class="topic-detail__foot">
        <p v-if="siteName">【{{ siteName }}】 永久域名以本站为准</p>
        <p v-if="tenant?.kefuWechat || tenant?.kefuQq">
          <template v-if="tenant?.kefuWechat">充值联系 {{ tenant.kefuWechat }}</template>
          <span v-if="tenant?.kefuQq">　QQ {{ tenant.kefuQq }}</span>
        </p>
        <NuxtLink to="/recharge">点击充值金币</NuxtLink>
      </div>
    </article>
  </div>
</template>
