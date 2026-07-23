// https://nuxt.com/docs/api/configuration/nuxt-config
/**
 * 慢加载根因（已验证）：
 * @element-plus/nuxt 在 cache=false（默认）时会把 element-plus 写入 optimizeDeps.exclude，
 * 开发态变成数百～上千个 @fs 零散请求（Windows + pnpm 极慢）。
 *
 * 修复：后台 ssr:false；去掉该模块；客户端一次性 app.use(ElementPlus)，让 Vite 正常预构建。
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
    transpile: ['@liuhecai/shared', '@wangeditor/editor', '@wangeditor/editor-for-vue'],
  },
  vite: {
    cacheDir: 'F:/vite-cache/liuhecai-agent',
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
      ],
      // wangEditor 仅在进 CMS 富文本页时加载；不要 exclude 以免干扰整次预构建
    },
    ssr: {
      external: ['@wangeditor/editor', '@wangeditor/editor-for-vue'],
    },
  },
  hooks: {
    'vite:extendConfig'(config, { isClient }) {
      if (!isClient) return
      // 确保关键库不被误伤进 exclude（Nuxt 会塞一长串）
      const exclude = config.optimizeDeps?.exclude
      if (Array.isArray(exclude)) {
        config.optimizeDeps!.exclude = exclude.filter(
          (id) =>
            id !== 'element-plus'
            && id !== '@element-plus/icons-vue'
            && id !== 'lodash-unified'
            && id !== 'dayjs',
        )
      }
      // 强制 include
      config.optimizeDeps ??= {}
      config.optimizeDeps.include ??= []
      for (const id of ['element-plus', '@element-plus/icons-vue', 'lodash-unified', 'dayjs']) {
        if (!config.optimizeDeps.include.includes(id)) {
          config.optimizeDeps.include.push(id)
        }
      }
    },
  },
  runtimeConfig: {
    public: {
      apiBase: 'http://127.0.0.1:8080',
      webBase: 'http://127.0.0.1:3000',
    },
  },
  nitro: {
    devProxy: {
      '/uploads': { target: 'http://127.0.0.1:8080/uploads', changeOrigin: true },
    },
  },
  app: {
    head: {
      title: '代理后台',
    },
  },
})
