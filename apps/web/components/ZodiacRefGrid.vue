<script setup lang="ts">
import { ZODIAC_REF_ROWS } from '~/utils/zodiacRef'

const left = ZODIAC_REF_ROWS.slice(0, Math.ceil(ZODIAC_REF_ROWS.length / 2))
const right = ZODIAC_REF_ROWS.slice(Math.ceil(ZODIAC_REF_ROWS.length / 2))
</script>

<template>
  <section class="zodiac-grid" aria-label="肖类对照">
    <div class="zodiac-grid__col">
      <div v-for="row in left" :key="row.label" class="zodiac-grid__row">
        <span class="zodiac-grid__label">{{ row.label }}</span>
        <span class="zodiac-grid__value">{{ row.value }}</span>
      </div>
    </div>
    <div class="zodiac-grid__col">
      <div v-for="row in right" :key="row.label" class="zodiac-grid__row">
        <span class="zodiac-grid__label">{{ row.label }}</span>
        <span class="zodiac-grid__value">{{ row.value }}</span>
      </div>
    </div>
  </section>
</template>
