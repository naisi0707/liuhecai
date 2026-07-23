import ElementPlus from 'element-plus'
import '@element-plus/icons-vue'
import 'dayjs'
import 'dayjs/plugin/customParseFormat.js'
import 'dayjs/plugin/advancedFormat.js'
import 'dayjs/plugin/localeData.js'
import 'dayjs/plugin/weekOfYear.js'
import 'dayjs/plugin/weekYear.js'
import 'dayjs/plugin/dayOfYear.js'
import 'dayjs/plugin/isSameOrAfter.js'
import 'dayjs/plugin/isSameOrBefore.js'
import 'lodash-unified'

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.use(ElementPlus)
})
