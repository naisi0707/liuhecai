<script setup lang="ts">
import { ballWave, displayIssue, padBall } from '~/utils/ballColor'
import { LOTTERY_TABS } from '~/composables/useDraws'

const {
  activeLottery,
  currentDraw,
  loadDraws,
  formatNextDraw,
  openHistory,
  closeHistory,
  historyOpen,
  historyLoading,
  historyItems,
} = useDraws()

function meta(i: number) {
  const d = currentDraw.value
  if (!d) return ''
  const z = d.zodiacs?.[i] || ''
  const w = d.wuxings?.[i] || ''
  return z && w ? `${z}/${w}` : z || w
}

function historyMeta(zodiacs: string[] | undefined, wuxings: string[] | undefined, i: number) {
  const z = zodiacs?.[i] || ''
  const w = wuxings?.[i] || ''
  return z && w ? `${z}/${w}` : z || w
}

const nextParts = computed(() => {
  const raw = formatNextDraw(currentDraw.value)
  if (!raw) return null
  const m = raw.match(/^第(\d+)期开奖:(.+)$/)
  if (!m) return { qi: '', rest: raw }
  return { qi: m[1], rest: m[2] }
})
const issueText = computed(() =>
  displayIssue(currentDraw.value?.issueNo, currentDraw.value?.displayIssue),
)
</script>

<template>
  <section class="kj-panel">
    <div class="kj-tab">
      <ul>
        <li
          v-for="tab in LOTTERY_TABS"
          :key="tab.type"
          :class="{ cur: activeLottery === tab.type }"
          @click="activeLottery = tab.type"
        >
          {{ tab.label }}
        </li>
      </ul>
    </div>

    <div v-if="currentDraw?.numbers?.length" class="kj-box">
      <div class="box-tit">
        <div class="box-tit-l">
          {{ currentDraw.lotteryLabel }}
          第<font class="font-red">{{ issueText }}</font>期开奖结果：
        </div>
        <div class="box-tit-m" />
        <div class="box-tit-r">
          <a class="font-red" href="javascript:;" @click.prevent="openHistory">历史记录</a>
        </div>
      </div>

      <div class="box-con">
        <div
          v-for="(n, i) in currentDraw.numbers"
          :key="'n' + i"
          class="bose"
          :class="'bose-' + ballWave(n)"
        >
          <h2><span>{{ padBall(n) }}</span></h2>
          <div class="text">{{ meta(i) }}</div>
        </div>
        <div class="jia">+</div>
        <div
          v-if="currentDraw.specialNumber"
          class="bose"
          :class="'bose-' + ballWave(currentDraw.specialNumber)"
        >
          <h2><span>{{ padBall(currentDraw.specialNumber) }}</span></h2>
          <div class="text">{{ meta(currentDraw.numbers.length) }}</div>
        </div>
      </div>

      <div class="box-foot">
        <div v-if="nextParts" class="box-foot-l">
          <template v-if="nextParts.qi">
            第<font class="font-red">{{ nextParts.qi }}</font>期开奖:{{ nextParts.rest }}
          </template>
          <template v-else>{{ nextParts.rest }}</template>
        </div>
        <div class="box-foot-r">
          <a href="javascript:;" @click.prevent="loadDraws">刷新</a>
        </div>
      </div>
    </div>

    <div v-else class="kj-box kj-box--empty">
      暂无开奖数据
      <a href="javascript:;" class="kj-refresh-link" @click.prevent="loadDraws">刷新</a>
    </div>

    <div v-if="historyOpen" class="kj-history">
      <div class="kj-history__mask" @click="closeHistory" />
      <div class="kj-history__panel">
        <header class="kj-history__head">
          <button type="button" class="kj-history__back" @click="closeHistory">返回</button>
          <span>开奖记录</span>
        </header>
        <div class="kj-history__body">
          <p v-if="historyLoading" class="kj-history__tip">加载中…</p>
          <p v-else-if="!historyItems.length" class="kj-history__tip">暂无记录</p>
          <div v-for="row in historyItems" :key="row.issueNo" class="kj-history__row">
            <div class="kj-history__meta">
              第<span class="font-red">{{ row.displayIssue || row.issueNo }}</span>期
              <span v-if="row.drawDate" class="kj-history__date">{{ row.drawDate }}</span>
            </div>
            <div class="box-con box-con--history">
              <div
                v-for="(n, i) in row.numbers"
                :key="row.issueNo + '-n' + i"
                class="bose"
                :class="'bose-' + ballWave(n)"
              >
                <h2><span>{{ padBall(n) }}</span></h2>
                <div class="text">{{ historyMeta(row.zodiacs, row.wuxings, i) }}</div>
              </div>
              <div class="jia">+</div>
              <div
                v-if="row.specialNumber"
                class="bose"
                :class="'bose-' + ballWave(row.specialNumber)"
              >
                <h2><span>{{ padBall(row.specialNumber) }}</span></h2>
                <div class="text">{{ historyMeta(row.zodiacs, row.wuxings, row.numbers.length) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
