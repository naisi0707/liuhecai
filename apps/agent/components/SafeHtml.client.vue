<script setup lang="ts">
const props = defineProps<{ html?: string | null }>()
const config = useRuntimeConfig()
const apiBase = (config.public.apiBase as string) || ''

const ALLOWED_TAGS = [
  'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'ul', 'ol', 'li',
  'h1', 'h2', 'h3', 'blockquote', 'span', 'a', 'img', 'div',
]
const ALLOWED_ATTR = ['href', 'src', 'alt', 'title', 'target', 'rel', 'style', 'class']

const safe = ref('')
const plain = computed(() =>
  (props.html || '')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim(),
)
const ready = ref(false)

async function render() {
  const raw = props.html || ''
  if (!raw) {
    safe.value = ''
    return
  }
  const mod = await import('dompurify')
  const DOMPurify = mod.default
  let cleaned = DOMPurify.sanitize(raw, {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    ALLOW_DATA_ATTR: false,
    FORBID_TAGS: ['script', 'iframe', 'object', 'embed', 'form', 'svg'],
    FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover'],
  })
  if (apiBase) {
    cleaned = cleaned.replace(
      /(src|href)=(["'])(\/uploads\/[^"']+)\2/gi,
      (_m, attr, q, path) => `${attr}=${q}${apiBase}${path}${q}`,
    )
  }
  safe.value = cleaned
}

watch(() => props.html, () => {
  if (ready.value) render()
})

onMounted(async () => {
  ready.value = true
  await render()
})
</script>

<template>
  <div class="rich-content">
    <div v-if="safe" v-html="safe" />
    <div v-else>{{ plain }}</div>
  </div>
</template>

<style>
.rich-content img {
  max-width: 100%;
  height: auto;
}
.rich-content a {
  color: #c62828;
}
</style>
