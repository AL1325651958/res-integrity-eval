import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

/**
 * ECharts 组合式函数：
 * - 数据就绪后调用 init(option) 初始化图表（模板中 ref="el"，需先 await nextTick()）
 * - 自动监听 window resize 并 resize 图表
 * - 组件卸载时自动 dispose
 */
export function useChart() {
  const el = ref<HTMLDivElement>()
  let chart: echarts.ECharts | null = null

  function init(option: EChartsOption) {
    if (!el.value) return
    if (!chart) {
      chart = echarts.init(el.value)
    }
    chart.setOption(option)
  }

  function resize() {
    chart?.resize()
  }

  onMounted(() => {
    window.addEventListener('resize', resize)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('resize', resize)
    chart?.dispose()
    chart = null
  })

  return { el, init }
}
