<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as echarts from "echarts";
import type { ECharts, EChartsOption, SetOptionOpts } from "echarts";
import { Refresh } from "@element-plus/icons-vue";

interface ChartLegendItem {
  value: number;
  label: string;
  color: string;
}

const props = defineProps<{
  title: string;
  subtitle: string;
  option: EChartsOption;
  loading?: boolean;
  error?: string;
  empty?: boolean;
  tall?: boolean;
  seriesOnlyUpdates?: boolean;
  legendItems?: ChartLegendItem[];
  legendActiveValue?: number | null;
}>();

const emit = defineEmits<{
  retry: [];
  chartClick: [params: unknown];
  chartDblclick: [params: unknown];
  legendClick: [params: unknown];
}>();

const chartRef = ref<HTMLDivElement | null>(null);
let chart: ECharts | null = null;
let resizeObserver: ResizeObserver | null = null;
let fullOptionApplied = false;

function ensureChart() {
  if (!chartRef.value || props.loading || props.error || props.empty) {
    return;
  }
  if (!chart) {
    chart = echarts.init(chartRef.value);
    chart.on("click", (params) => {
      if (isLegendEvent(params)) {
        emit("legendClick", params);
        return;
      }
      emit("chartClick", params);
    });
    chart.on("dblclick", (params) => emit("chartDblclick", params));
    chart.on("legendselectchanged", (params) => emit("legendClick", params));
  }
  if (!resizeObserver) {
    resizeObserver = new ResizeObserver(() => chart?.resize());
    resizeObserver.observe(chartRef.value);
  }
  applyOption();
}

function applyOption() {
  if (!chart) {
    return;
  }
  if (props.seriesOnlyUpdates && fullOptionApplied) {
    chart.setOption(toSeriesPatch(props.option), {
      notMerge: false,
      replaceMerge: ["series"]
    } satisfies SetOptionOpts);
    return;
  }
  chart.setOption(props.option, { notMerge: true });
  fullOptionApplied = true;
}

function toSeriesPatch(option: EChartsOption): EChartsOption {
  return {
    legend: option.legend,
    series: option.series
  };
}

function isLegendEvent(params: unknown) {
  return (params as { componentType?: string }).componentType === "legend";
}

function disposeChart() {
  resizeObserver?.disconnect();
  resizeObserver = null;
  chart?.dispose();
  chart = null;
  fullOptionApplied = false;
}

onMounted(() => {
  void nextTick(ensureChart);
});

watch(
  () => [props.option, props.loading, props.error, props.empty],
  () => {
    if (props.loading || props.error || props.empty) {
      disposeChart();
      return;
    }
    void nextTick(ensureChart);
  },
  { deep: true }
);

onBeforeUnmount(() => {
  disposeChart();
});
</script>

<template>
  <section class="analysis-card" :class="{ 'analysis-card--wide': tall }">
    <div class="panel-heading">
      <div>
        <h2>{{ title }}</h2>
        <p>{{ subtitle }}</p>
      </div>
      <el-button v-if="error" text type="primary" @click="emit('retry')">
        <el-icon><Refresh /></el-icon>
        重试
      </el-button>
    </div>

    <div v-if="loading" class="chart-state">
      <el-skeleton animated :rows="6" />
    </div>
    <el-empty v-else-if="empty" class="chart-state" :image-size="76" description="暂无可视化数据" />
    <el-alert v-else-if="error" class="chart-state" type="error" :title="error" show-icon :closable="false" />
    <div v-else class="analysis-chart-frame">
      <div v-if="legendItems?.length" class="chart-legend-overlay" aria-label="散点图图例">
        <button
          v-for="item in legendItems"
          :key="item.value"
          type="button"
          class="chart-legend-item"
          :class="{ active: legendActiveValue === item.value }"
          :title="`${item.label}：点击聚焦分组，再次点击切换显示方式`"
          @click="emit('legendClick', { name: item.label, value: item.value })"
        >
          <span class="chart-legend-dot" :style="{ backgroundColor: item.color }" />
          <span>{{ item.label }}</span>
        </button>
      </div>
      <div ref="chartRef" class="analysis-chart-canvas" :class="{ tall }" />
    </div>
  </section>
</template>
