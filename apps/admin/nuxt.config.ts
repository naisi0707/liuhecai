// https://nuxt.com/docs/api/configuration/nuxt-config
/**
 * 慢加载根因：@element-plus/nuxt 默认 exclude element-plus → 开发态拆成上百 @fs 请求。
 * 修复：ssr:false + 客户端一次性注册 Element Plus，去掉 @element-plus/nuxt。
 */
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  ssr: false,
  devtools: { enabled: false },
  experimental: {
    appManifest: false,
  },
  css: ['element-plus/dist/index.css'],
  build: {
    transpile: ['@liuhecai/shared'],
  },
  vite: {
    cacheDir: 'F:/vite-cache/liuhecai-admin',
    resolve: {
      alias: {
        'dayjs/dayjs.min.js': 'dayjs',
      },
    },
    optimizeDeps: {
      include: [
        'vue',
        'vue-router',
        'element-plus',
        'lodash-unified',
        '@element-plus/icons-vue',
        'dayjs',
        'dayjs/plugin/customParseFormat.js',
        'dayjs/plugin/advancedFormat.js',
        'dayjs/plugin/localeData.js',
        'dayjs/plugin/weekOfYear.js',
        'dayjs/plugin/weekYear.js',
        'dayjs/plugin/dayOfYear.js',
        'dayjs/plugin/isSameOrAfter.js',
        'dayjs/plugin/isSameOrBefore.js',
        'echarts',
        'vue-echarts',
      ],
    },
  },
  hooks: {
    'vite:extendConfig'(config, { isClient }) {
      if (!isClient) return
      const exclude = config.optimizeDeps?.exclude
      if (Array.isArray(exclude)) {
        config.optimizeDeps!.exclude = exclude.filter(
          (id) =>
            id !== 'element-plus'
            && id !== '@element-plus/icons-vue'
            && id !== 'lodash-unified'
            && id !== 'dayjs'
            && id !== 'echarts'
            && id !== 'vue-echarts',
        )
      }
      config.optimizeDeps ??= {}
      config.optimizeDeps.include ??= []
      for (const id of ['element-plus', '@element-plus/icons-vue', 'lodash-unified', 'dayjs', 'echarts', 'vue-echarts']) {
        if (!config.optimizeDeps.include.includes(id)) {
          config.optimizeDeps.include.push(id)
        }
      }
    },
  },
  runtimeConfig: {
    public: {
      apiBase: 'http://127.0.0.1:8080',
    },
  },
  app: {
    head: {
      title: '超管后台',
    },
  },
})
