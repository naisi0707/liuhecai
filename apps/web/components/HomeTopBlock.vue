<script setup lang="ts">
import { request, type CmsHomeContent, type TenantDirectoryItem } from '@liuhecai/shared'

const { tenant } = useTenant()
const { pageContent } = useSiteCms()
const { mediaUrl } = useMediaUrl()

const isDev = import.meta.dev
const DEMO_HOST_BY_NAME: Record<string, string> = {
  刘伯温论坛: 'lbw.local',
  至尊无上论坛: 'zzws.local',
  神算子论坛: 'ssz.local',
}

const IFRAME_HOST_ALLOWLIST = [
  'zhibo.77kj.vip',
  '1e.36351c.com',
  '36351c.com',
]

const home = computed(() => pageContent<CmsHomeContent>('home'))
const otherTenants = ref<TenantDirectoryItem[]>([])

/** 桌面/原站对齐：姊妹站表格每行 2 个 */
const tenantRows = computed(() => {
  const list = otherTenants.value
  const rows: TenantDirectoryItem[][] = []
  for (let i = 0; i < list.length; i += 2) {
    rows.push(list.slice(i, i + 2))
  }
  return rows
})

const wx = computed(() => tenant.value?.kefuWechat || '')
const qq = computed(() => tenant.value?.kefuQq || '')

const qrWechat = computed(() => {
  const url = home.value?.qrWechatUrl
  return url ? mediaUrl(url) : ''
})
const qrQq = computed(() => {
  const url = home.value?.qrQqUrl
  return url ? mediaUrl(url) : ''
})

const liveIframe = computed(() => sanitizeIframeUrl(home.value?.liveIframeUrl))
const domainBadge = computed(() => home.value?.domainBadge || '')
const announcement = computed(() => tenant.value?.announcement || '')

function demoHost(t: TenantDirectoryItem): string | null {
  if (t.primaryHost) return t.primaryHost
  if (!isDev) return null
  return DEMO_HOST_BY_NAME[t.name] || null
}

function tenantHref(t: TenantDirectoryItem) {
  const host = demoHost(t)
  if (!host) return '#'
  return `/?host=${encodeURIComponent(host)}`
}

function hostAllowed(hostname: string): boolean {
  const h = hostname.toLowerCase()
  return IFRAME_HOST_ALLOWLIST.some((allowed) => h === allowed || h.endsWith('.' + allowed))
}

function sanitizeIframeUrl(raw: string | undefined | null): string | null {
  const value = (raw || '').trim()
  if (!value) return null
  if (value.startsWith('/') && !value.startsWith('//')) return value
  try {
    const u = new URL(value)
    if (u.protocol !== 'http:' && u.protocol !== 'https:') return null
    if (!hostAllowed(u.hostname)) return null
    return u.toString()
  } catch {
    return null
  }
}

onMounted(async () => {
  try {
    // home CMS 由首页 owner 拉取；此处仅拉姊妹站目录
    otherTenants.value = await request<TenantDirectoryItem[]>('/api/site/tenants')
  } catch {
    // busy 已标记
  }
})
</script>

<template>
  <div class="home-after-draw">
    <div class="draw-slot">
      <DrawPanel />
    </div>

    <div v-if="liveIframe" class="home-live-iframe">
      <ClientOnly>
        <iframe
          id="live-iframe"
          width="100%"
          height="300"
          :src="liveIframe"
          frameborder="0"
          scrolling="no"
          title="开奖直播"
          sandbox="allow-scripts allow-same-origin"
          referrerpolicy="no-referrer"
        />
      </ClientOnly>
    </div>

    <div v-if="otherTenants.length" class="white-box">
      <div class="xgam">
        <table width="100%" border="1">
          <tbody>
            <template v-for="(row, rowIndex) in tenantRows" :key="'row-' + rowIndex">
              <tr>
                <td v-for="t in row" :key="t.id" bgcolor="#FFFFFF" width="50%">
                  <div class="xgam-tit">
                    <p align="center">
                      <img src="/site/root/92.gif" alt="" />
                      <span :style="t.primaryColor ? { color: t.primaryColor } : undefined">{{ t.name }}</span>
                    </p>
                  </div>
                  <div class="xgam-web">
                    <p align="center">
                      <a v-if="demoHost(t)" target="_blank" rel="noopener" :href="tenantHref(t)">
                        <span style="color:#000000">{{ demoHost(t) }}</span>
                      </a>
                    </p>
                    <p v-if="t.logoUrl" align="center" style="margin:6px 0;">
                      <img :src="mediaUrl(t.logoUrl)" alt="" height="28" />
                    </p>
                  </div>
                </td>
                <!-- 奇数个时补空格，保持两列对齐 -->
                <td v-if="row.length === 1" bgcolor="#FFFFFF" width="50%" />
              </tr>
              <tr>
                <td
                  v-for="t in row"
                  :key="'cta-' + t.id"
                  width="50%"
                  :bgcolor="t.primaryColor || undefined"
                >
                  <a
                    v-if="demoHost(t)"
                    target="_blank"
                    rel="noopener"
                    :href="tenantHref(t)"
                  >
                    <img src="/site/root/ye.gif" alt="" />
                    <span style="color:#FFFFFF"><b> 查看{{ t.name }}</b></span>
                  </a>
                  <span v-else style="color:#FFFFFF"><b> 查看{{ t.name }}</b></span>
                </td>
                <td v-if="row.length === 1" width="50%" bgcolor="#FFFFFF" />
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </div>

    <table v-if="domainBadge" class="home-domain-bar" border="3" width="100%" bordercolor="#FF00FF">
      <tbody>
        <tr>
          <td align="center" bgcolor="#006699">
            <img src="/site/icons/jiantou.gif" width="50" height="16" alt="" />
            <a href="/" style="color:#fff;text-decoration:none;">{{ domainBadge }}</a>
            <p style="margin:0;color:#fff;">
              <img src="/site/icons/macau.png" width="25" height="25" alt="" />
              请收藏本站跟踪
            </p>
          </td>
          <td align="center" bgcolor="#006699">
            <img src="/site/icons/jiantou.gif" width="50" height="16" alt="" />
            <span style="color:#fff;">{{ domainBadge }}</span>
            <p style="margin:0;color:#fff;">
              <img src="/site/icons/macau.png" width="25" height="25" alt="" />
              请收藏本站跟踪
            </p>
          </td>
        </tr>
      </tbody>
    </table>

    <table v-if="announcement" class="home-announce">
      <tbody>
        <tr>
          <td>
            <p class="home-announce__text">
              <span class="home-announce__body">{{ announcement }}</span>
            </p>
          </td>
        </tr>
        <tr>
          <td class="home-announce__cta">
            <NuxtLink to="/recharge">
              <img src="/site/icons/wechat2.png" alt="" />
              <span class="home-rcg-pill">点击充值金币</span>
            </NuxtLink>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="wx || qq" class="home-contact-bar">
      <span>充值金币联系</span>
      <template v-if="wx">
        <img src="/site/icons/wechat2.png" alt="" />
        <span>：{{ wx }}</span>
      </template>
      <span v-if="qq" style="margin-left:12px;">QQ：{{ qq }}</span>
    </div>

    <p v-if="qrWechat || qrQq" class="home-qr-row">
      <img v-if="qrWechat" :src="qrWechat" alt="微信" width="206" />
      <img v-if="qrQq" :src="qrQq" alt="QQ" width="206" />
    </p>

    <table class="home-quick-row">
      <tbody>
        <tr>
          <td>
            <NuxtLink class="home-quick-btn" to="/register">用户注册</NuxtLink>
          </td>
          <td>
            <NuxtLink class="home-quick-btn" to="/recharge">金币充值</NuxtLink>
          </td>
          <td>
            <NuxtLink class="home-quick-btn" to="/login">用户登录</NuxtLink>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
