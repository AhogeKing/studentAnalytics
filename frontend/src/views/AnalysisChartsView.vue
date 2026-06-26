<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import type { EChartsOption } from "echarts";
import { Refresh } from "@element-plus/icons-vue";
import {
  fetchGpaDistribution,
  fetchGradeClassDistribution,
  fetchPerformanceAnalysisPoints
} from "../api/analytics";
import { fetchStudentFilterOptions } from "../api/students";
import AnalysisChartCard from "../components/analysis/AnalysisChartCard.vue";
import AnalysisScopeSelector from "../components/analysis/AnalysisScopeSelector.vue";
import type {
  AnalysisColorMode,
  AnalysisDisplayMode,
  AnalysisScopeMode,
  AnalysisScopeParams,
  ClassInfo,
  GpaDistributionItem,
  GradeClassDistributionItem,
  NormalizedGpaDistributionItem,
  NormalizedGradeClassDistributionItem,
  NormalizedPerformanceAnalysisPoint,
  PerformanceAnalysisPoint,
  StudentFilterOptions
} from "../types";

interface GroupMeta {
  value: number;
  label: string;
  color: string;
  count: number;
}

interface GpaBucketMeta {
  value: number;
  label: string;
  min: number;
  max: number;
  includeMax?: boolean;
}

const router = useRouter();
const loading = ref(false);
const scopeOptionsLoading = ref(false);
const error = ref("");
const filterOptions = ref<StudentFilterOptions | null>(null);
const gpaDistribution = ref<NormalizedGpaDistributionItem[]>([]);
const gradeClassDistribution = ref<NormalizedGradeClassDistributionItem[]>([]);
const performancePoints = ref<NormalizedPerformanceAnalysisPoint[]>([]);
const scopeMode = ref<AnalysisScopeMode>("all");
const selectedGradeLevel = ref<number | undefined>();
const selectedClassNames = ref<string[]>([]);
const colorMode = ref<AnalysisColorMode>("gradeClass");
const focusedGroup = ref<number | null>(null);
const displayMode = ref<AnalysisDisplayMode>("dim");

const gradeColors: Record<number, string> = {
  0: "#2563eb",
  1: "#38bdf8",
  2: "#22c55e",
  3: "#f59e0b",
  4: "#ef4444"
};

const defaultGradeLabels: Record<number, string> = {
  0: "优秀",
  1: "良好",
  2: "中等",
  3: "较差",
  4: "风险"
};

const gpaBuckets: GpaBucketMeta[] = [
  { value: 0, label: "[0.0, 0.5)", min: 0, max: 0.5 },
  { value: 1, label: "[0.5, 1.0)", min: 0.5, max: 1 },
  { value: 2, label: "[1.0, 1.5)", min: 1, max: 1.5 },
  { value: 3, label: "[1.5, 2.0)", min: 1.5, max: 2 },
  { value: 4, label: "[2.0, 2.5)", min: 2, max: 2.5 },
  { value: 5, label: "[2.5, 3.0)", min: 2.5, max: 3 },
  { value: 6, label: "[3.0, 3.5)", min: 3, max: 3.5 },
  { value: 7, label: "[3.5, 4.0]", min: 3.5, max: 4, includeMax: true }
];

const gpaBucketColors: Record<number, string> = {
  0: "#ef4444",
  1: "#f97316",
  2: "#f59e0b",
  3: "#84cc16",
  4: "#22c55e",
  5: "#14b8a6",
  6: "#38bdf8",
  7: "#2563eb"
};

const totalStudents = computed(() => performancePoints.value.length);
const averageGpa = computed(() => average(performancePoints.value.map((item) => item.gpa)).toFixed(2));
const averageAbsences = computed(() => average(performancePoints.value.map((item) => item.absences)).toFixed(1));
const averageStudyTime = computed(() => average(performancePoints.value.map((item) => item.studyTimeWeekly)).toFixed(1));
const absenceAxisMax = computed(() => niceAxisMax(performancePoints.value.map((item) => item.absences)));
const studyTimeAxisMax = computed(() => niceAxisMax(performancePoints.value.map((item) => item.studyTimeWeekly)));
const classOptions = computed(() => normalizeClassOptions(filterOptions.value?.classes ?? []));
const analysisParams = computed<AnalysisScopeParams>(() => {
  if (scopeMode.value === "all" || !selectedGradeLevel.value) {
    return {};
  }

  if (scopeMode.value === "grade") {
    return { grade_level: selectedGradeLevel.value };
  }

  return {
    grade_level: selectedGradeLevel.value,
    class_name: selectedClassNames.value.length ? selectedClassNames.value : undefined
  };
});
const currentScopeLabel = computed(() => {
  if (scopeMode.value === "all") {
    return "全部学生";
  }

  const gradeName = selectedGradeLevel.value ? gradeNameMap[selectedGradeLevel.value] : "未选择年级";
  if (scopeMode.value === "grade" || !selectedClassNames.value.length) {
    return `${gradeName}全部班级`;
  }

  const classLabels = selectedClassNames.value.map((className) => getClassDisplayName(className)).join("、");
  return `${gradeName}：${classLabels}`;
});

const maxGpaBucketCount = computed(() =>
  Math.max(0, ...gpaDistribution.value.map((item) => item.studentCount))
);

const gradeNameMap: Record<number, string> = {
  1: "高一",
  2: "高二",
  3: "高三"
};

const gradeClassGroups = computed<GroupMeta[]>(() => {
  const labelMap = new Map<number, string>();
  Object.entries(defaultGradeLabels).forEach(([value, label]) => labelMap.set(Number(value), label));
  gradeClassDistribution.value.forEach((item) => {
    if (item.gradeClass.value !== null) {
      labelMap.set(Number(item.gradeClass.value), item.gradeClass.label);
    }
  });
  performancePoints.value.forEach((point) => {
    if (point.gradeClass.value !== null) {
      labelMap.set(Number(point.gradeClass.value), point.gradeClass.label);
    }
  });

  return Array.from(labelMap.entries())
    .sort(([left], [right]) => left - right)
    .map(([value, label]) => ({
      value,
      label,
      color: gradeColors[value] || "#64748b",
      count: countPointsByGroup(value, "gradeClass")
    }));
});

const gpaBucketGroups = computed<GroupMeta[]>(() =>
  gpaBuckets.map((bucket) => {
    const distributionLabel = gpaDistribution.value.find((item) => item.bucketIndex === bucket.value)?.label;
    return {
      value: bucket.value,
      label: distributionLabel || bucket.label,
      color: gpaBucketColors[bucket.value] || "#64748b",
      count: countPointsByGroup(bucket.value, "gpaBucket")
    };
  })
);

const activeGroups = computed(() => (colorMode.value === "gradeClass" ? gradeClassGroups.value : gpaBucketGroups.value));

const focusModeLabel = computed(() => (colorMode.value === "gradeClass" ? "成绩等级" : "GPA 区间"));

const gpaBubbleOption = computed<EChartsOption>(() => ({
  color: ["#0891b2"],
  tooltip: {
    trigger: "item",
    formatter: (params: any) => {
      const data = params.data;
      return `${data.name}<br/>人数：${data.studentCount}<br/>占比：${formatPercent(data.percentage)}`;
    }
  },
  grid: { left: 42, right: 22, top: 30, bottom: 58, containLabel: true },
  xAxis: {
    type: "category",
    name: "GPA 区间",
    nameLocation: "middle",
    nameGap: 34,
    nameTextStyle: {
      color: "#164e63",
      fontWeight: 800,
      fontSize: 13
    },
    data: gpaDistribution.value.map((item) => item.label),
    axisTick: { show: false },
    axisLabel: { color: "#607780" },
    axisLine: { lineStyle: { color: "#d6e5e9" } }
  },
  yAxis: {
    type: "value",
    name: "人数",
    splitLine: { lineStyle: { color: "#e6f1f3" } },
    axisLabel: { color: "#607780" }
  },
  series: [
    {
      type: "scatter",
      data: gpaDistribution.value.map((item) => ({
        name: item.label,
        value: [item.label, item.studentCount],
        studentCount: item.studentCount,
        percentage: item.percentage
      })),
      symbolSize: (_value: unknown, params: any) =>
        calculateBubbleSize(params.data.studentCount, maxGpaBucketCount.value),
      label: {
        show: true,
        position: "top",
        color: "#164e63",
        fontWeight: 800,
        formatter: (params: any) => String(params.data.studentCount)
      },
      itemStyle: { opacity: 0.82, borderColor: "#ffffff", borderWidth: 2 }
    }
  ]
}));

const gradeBarOption = computed<EChartsOption>(() => ({
  tooltip: {
    trigger: "axis",
    axisPointer: { type: "shadow" },
    formatter: (params: any) => {
      const data = params[0].data;
      return `${data.name}<br/>人数：${data.value}<br/>占比：${formatPercent(data.percentage)}`;
    }
  },
  grid: { left: 42, right: 22, top: 28, bottom: 34, containLabel: true },
  xAxis: {
    type: "category",
    data: gradeClassDistribution.value.map((item) => item.gradeClass.label),
    axisTick: { show: false },
    axisLabel: { color: "#607780" },
    axisLine: { lineStyle: { color: "#d6e5e9" } }
  },
  yAxis: {
    type: "value",
    name: "人数",
    splitLine: { lineStyle: { color: "#e6f1f3" } },
    axisLabel: { color: "#607780" }
  },
  series: [
    {
      type: "bar",
      barWidth: 34,
      data: gradeClassDistribution.value.map((item) => ({
        name: item.gradeClass.label,
        value: item.studentCount,
        percentage: item.percentage,
        itemStyle: {
          color: gradeColors[Number(item.gradeClass.value)] || "#0891b2",
          borderRadius: [6, 6, 0, 0]
        }
      })),
      label: {
        show: true,
        position: "top",
        color: "#164e63",
        fontWeight: 800
      }
    }
  ]
}));

const absenceGpaOption = computed<EChartsOption>(() =>
  buildScatterOption({
    xName: "缺勤次数",
    xMin: 0,
    xMax: absenceAxisMax.value,
    yName: "GPA",
    yMin: 0,
    yMax: 4,
    value: (point) => [point.absences, point.gpa],
    tooltipTitle: "缺勤次数",
    tooltipValue: (point) => `${point.absences} 次`
  })
);

const studyTimeGpaOption = computed<EChartsOption>(() =>
  buildScatterOption({
    xName: "每周学习时长",
    xMin: 0,
    xMax: studyTimeAxisMax.value,
    yName: "GPA",
    yMin: 0,
    yMax: 4,
    value: (point) => [point.studyTimeWeekly, point.gpa],
    tooltipTitle: "每周学习时长",
    tooltipValue: (point) => `${point.studyTimeWeekly.toFixed(2)} 小时`
  })
);

const jointScatterOption = computed<EChartsOption>(() => ({
  tooltip: {
    trigger: "item",
    formatter: (params: any) => studentTooltip(params.data.point)
  },
  legend: buildScatterLegend(),
  grid: { left: 46, right: 28, top: 48, bottom: 68, containLabel: true },
  xAxis: {
    type: "value",
    name: "每周学习时长",
    nameLocation: "middle",
    nameGap: 34,
    min: 0,
    max: studyTimeAxisMax.value,
    splitLine: { lineStyle: { color: "#e6f1f3" } },
    axisLabel: { color: "#607780" }
  },
  yAxis: {
    type: "value",
    name: "缺勤次数",
    min: 0,
    max: absenceAxisMax.value,
    splitLine: { lineStyle: { color: "#e6f1f3" } },
    axisLabel: { color: "#607780" }
  },
  dataZoom: [
    { type: "inside", xAxisIndex: 0 },
    { type: "inside", yAxisIndex: 0 }
  ],
  series: buildGroupedScatterSeries((point) => [point.studyTimeWeekly, point.absences, point.gpa])
}));

watch(colorMode, () => {
  focusedGroup.value = null;
});

watch(scopeMode, (mode) => {
  if (mode === "all") {
    selectedGradeLevel.value = undefined;
    selectedClassNames.value = [];
  }
  if (mode === "grade") {
    selectedClassNames.value = [];
  }
});

async function loadFilterOptions() {
  scopeOptionsLoading.value = true;
  try {
    filterOptions.value = await fetchStudentFilterOptions();
  } finally {
    scopeOptionsLoading.value = false;
  }
}

async function loadCharts() {
  loading.value = true;
  error.value = "";
  try {
    const params = analysisParams.value;
    const [gpaItems, gradeItems, points] = await Promise.all([
      fetchGpaDistribution(params),
      fetchGradeClassDistribution(params),
      fetchPerformanceAnalysisPoints(params)
    ]);
    gpaDistribution.value = normalizeGpaDistribution(gpaItems);
    gradeClassDistribution.value = normalizeGradeDistribution(gradeItems);
    performancePoints.value = normalizePerformancePoints(points);
  } catch (requestError) {
    error.value = requestError instanceof Error ? requestError.message : "分析图表加载失败";
  } finally {
    loading.value = false;
  }
}

function applyScope() {
  focusedGroup.value = null;
  void loadCharts();
}

function resetScope() {
  scopeMode.value = "all";
  selectedGradeLevel.value = undefined;
  selectedClassNames.value = [];
  focusedGroup.value = null;
  void loadCharts();
}

function normalizeGpaDistribution(items: GpaDistributionItem[]): NormalizedGpaDistributionItem[] {
  return items.map((item) => ({
    bucketIndex: Number(item.bucketIndex ?? item.bucket_index ?? 0),
    label: item.label,
    minGpa: Number(item.minGpa ?? item.min_gpa ?? 0),
    maxGpa: Number(item.maxGpa ?? item.max_gpa ?? 0),
    studentCount: Number(item.studentCount ?? item.student_count ?? 0),
    percentage: Number(item.percentage ?? 0)
  }));
}

function normalizeGradeDistribution(items: GradeClassDistributionItem[]): NormalizedGradeClassDistributionItem[] {
  return items.map((item) => ({
    gradeClass: item.gradeClass ?? item.grade_class ?? { value: null, label: "未知" },
    studentCount: Number(item.studentCount ?? item.student_count ?? 0),
    percentage: Number(item.percentage ?? 0)
  }));
}

function normalizePerformancePoints(items: PerformanceAnalysisPoint[]): NormalizedPerformanceAnalysisPoint[] {
  return items
    .map((item) => {
      const gpa = Number(item.gpa ?? 0);
      return {
        studentNo: Number(item.studentNo ?? item.student_no ?? 0),
        name: item.name,
        classInfo: item.classInfo ?? item.class_info ?? null,
        studyTimeWeekly: Number(item.studyTimeWeekly ?? item.study_time_weekly ?? 0),
        absences: Number(item.absences ?? 0),
        gpa,
        gradeClass: item.gradeClass ?? item.grade_class ?? { value: null, label: "未知" },
        gpaBucket: item.gpaBucket ?? item.gpa_bucket ?? resolveGpaBucket(gpa)
      };
    })
    .filter((item) => item.studentNo > 0);
}

function normalizeClassOptions(items: ClassInfo[]): ClassInfo[] {
  return items
    .map((item) => ({
      grade_level: Number(item.grade_level),
      raw_class_name: item.raw_class_name,
      class_name: item.class_name
    }))
    .filter((item) => Number.isFinite(item.grade_level) && item.raw_class_name)
    .sort((left, right) => left.raw_class_name.localeCompare(right.raw_class_name, "zh-Hans-CN", { numeric: true }));
}

function resolveGpaBucket(gpa: number) {
  const bucket =
    gpaBuckets.find((item) =>
      item.includeMax ? gpa >= item.min && gpa <= item.max : gpa >= item.min && gpa < item.max
    ) || gpaBuckets[0];
  return { value: bucket.value, label: bucket.label };
}

function calculateBubbleSize(count: number, maxCount: number) {
  if (maxCount <= 0) {
    return 12;
  }
  return Math.max(12, Math.sqrt(count / maxCount) * 48);
}

function average(values: number[]) {
  const validValues = values.filter((value) => Number.isFinite(value));
  if (!validValues.length) {
    return 0;
  }
  return validValues.reduce((sum, value) => sum + value, 0) / validValues.length;
}

function formatPercent(value: number) {
  return `${Number(value || 0).toFixed(2)}%`;
}

function niceAxisMax(values: number[]) {
  const validValues = values.filter((value) => Number.isFinite(value) && value >= 0);
  const max = Math.max(1, ...validValues);
  const step = max <= 10 ? 1 : max <= 24 ? 3 : 5;
  return Math.ceil(max / step) * step;
}

function buildScatterOption(config: {
  xName: string;
  xMin: number;
  xMax: number;
  yName: string;
  yMin: number;
  yMax: number;
  value: (point: NormalizedPerformanceAnalysisPoint) => number[];
  tooltipTitle: string;
  tooltipValue: (point: NormalizedPerformanceAnalysisPoint) => string;
}): EChartsOption {
  return {
    tooltip: {
      trigger: "item",
      formatter: (params: any) => {
        const point = params.data.point as NormalizedPerformanceAnalysisPoint;
        return studentTooltip(point, `${config.tooltipTitle}：${config.tooltipValue(point)}`);
      }
    },
    legend: buildScatterLegend(),
    grid: { left: 46, right: 24, top: 48, bottom: 68, containLabel: true },
    xAxis: {
      type: "value",
      name: config.xName,
      nameLocation: "middle",
      nameGap: 34,
      min: config.xMin,
      max: config.xMax,
      splitLine: { lineStyle: { color: "#e6f1f3" } },
      axisLabel: { color: "#607780" }
    },
    yAxis: {
      type: "value",
      name: config.yName,
      min: config.yMin,
      max: config.yMax,
      splitLine: { lineStyle: { color: "#e6f1f3" } },
      axisLabel: { color: "#607780" }
    },
    dataZoom: [{ type: "inside", xAxisIndex: 0 }],
    series: buildGroupedScatterSeries(config.value)
  };
}

function studentTooltip(point: NormalizedPerformanceAnalysisPoint, extraLine?: string) {
  return [
    `学生：${point.studentNo} · ${point.name}`,
    `班级：${point.classInfo?.class_name ?? "-"}`,
    extraLine,
    `学习时长：${point.studyTimeWeekly.toFixed(2)} 小时`,
    `缺勤次数：${point.absences}`,
    `GPA：${point.gpa.toFixed(2)}`,
    `成绩等级：${point.gradeClass.label}`,
    `GPA 区间：${point.gpaBucket.label}`
  ]
    .filter(Boolean)
    .join("<br/>");
}

function buildScatterLegend() {
  return {
    show: false
  };
}

function buildGroupedScatterSeries(valueBuilder: (point: NormalizedPerformanceAnalysisPoint) => number[]): EChartsOption["series"] {
  return activeGroups.value.map((group) => {
    const isFocused = focusedGroup.value === group.value;
    const selected = focusedGroup.value === null || isFocused;
    const hidden = focusedGroup.value !== null && displayMode.value === "only" && !selected;
    return {
      id: `${colorMode.value}-${group.value}`,
      name: group.label,
      type: "scatter",
      data: performancePoints.value
        .filter((point) => getPointGroup(point, colorMode.value) === group.value)
        .map((point) => ({
          value: valueBuilder(point),
          point
        })),
      symbolSize: hidden ? 0 : isFocused ? 10 : selected ? 8 : 6,
      silent: hidden,
      cursor: "pointer",
      animationDurationUpdate: 120,
      itemStyle: {
        color: group.color,
        opacity: hidden ? 0 : focusedGroup.value === null ? 0.68 : selected ? 0.9 : 0.09,
        borderColor: "#ffffff",
        borderWidth: selected ? 1 : 0
      },
      emphasis: {
        focus: "series",
        itemStyle: { opacity: 1, borderColor: "#ffffff", borderWidth: 2 }
      },
      z: selected ? 10 : 1
    };
  }) as EChartsOption["series"];
}

function getPointGroup(point: NormalizedPerformanceAnalysisPoint, mode: AnalysisColorMode) {
  const option = mode === "gradeClass" ? point.gradeClass : point.gpaBucket;
  return option.value === null ? Number.NaN : Number(option.value);
}

function countPointsByGroup(value: number, mode: AnalysisColorMode) {
  return performancePoints.value.filter((point) => getPointGroup(point, mode) === value).length;
}

function getClassDisplayName(rawClassName: string) {
  return classOptions.value.find((item) => item.raw_class_name === rawClassName)?.class_name ?? rawClassName;
}

function resetFocus() {
  focusedGroup.value = null;
}

function toggleFocus(value: number) {
  focusedGroup.value = focusedGroup.value === value ? null : value;
}

function setColorMode(mode: AnalysisColorMode) {
  colorMode.value = mode;
}

function setDisplayMode(mode: AnalysisDisplayMode) {
  displayMode.value = mode;
}

function handlePointFocus(params: unknown) {
  const data = (params as { data?: { point?: NormalizedPerformanceAnalysisPoint } }).data;
  const point = data?.point;
  if (!point) {
    return;
  }
  const groupValue = getPointGroup(point, colorMode.value);
  if (Number.isFinite(groupValue)) {
    focusedGroup.value = groupValue;
  }
}

function handlePointDetail(params: unknown) {
  const data = (params as { data?: { point?: NormalizedPerformanceAnalysisPoint } }).data;
  const studentNo = data?.point?.studentNo;
  if (studentNo) {
    void router.push({ name: "student-detail", params: { studentNo } });
  }
}

function handleLegendToggle(params: unknown) {
  const legendName = (params as { name?: string }).name;
  const group = activeGroups.value.find((item) => item.label === legendName);
  if (!group) {
    return;
  }
  if (focusedGroup.value === group.value) {
    displayMode.value = displayMode.value === "dim" ? "only" : "dim";
    return;
  }
  const wasShowingAll = focusedGroup.value === null;
  focusedGroup.value = group.value;
  if (wasShowingAll) {
    displayMode.value = "dim";
  }
}

onMounted(() => {
  void loadFilterOptions();
  void loadCharts();
});
</script>

<template>
  <section class="page-head analysis-head">
    <div>
      <p class="eyebrow">分析图表</p>
      <h1>学生成绩分析</h1>
      <p>当前范围：{{ currentScopeLabel }}。围绕 GPA、成绩等级、缺勤次数和每周学习时长展示五类可视化。</p>
    </div>
    <div class="head-actions">
      <el-button :loading="loading" type="primary" @click="loadCharts">
        <el-icon><Refresh /></el-icon>
        刷新图表
      </el-button>
    </div>
  </section>

  <AnalysisScopeSelector
    v-model:mode="scopeMode"
    v-model:grade-level="selectedGradeLevel"
    v-model:class-names="selectedClassNames"
    :classes="classOptions"
    :loading="loading || scopeOptionsLoading"
    :current-scope-label="currentScopeLabel"
    @apply="applyScope"
    @reset="resetScope"
  />

  <section class="analysis-summary">
    <article>
      <span>表现点数量</span>
      <strong>{{ totalStudents }}</strong>
      <small>{{ currentScopeLabel }}参与散点分析的学生记录</small>
    </article>
    <article>
      <span>平均 GPA</span>
      <strong>{{ averageGpa }}</strong>
      <small>基于 performance-points 计算</small>
    </article>
    <article>
      <span>平均缺勤</span>
      <strong>{{ averageAbsences }}</strong>
      <small>单位：次</small>
    </article>
    <article>
      <span>平均学习时长</span>
      <strong>{{ averageStudyTime }}</strong>
      <small>单位：小时 / 周</small>
    </article>
  </section>

  <section class="analysis-focus-panel">
    <div class="focus-control-row">
      <span class="focus-control-label">着色依据</span>
      <div class="focus-segment" aria-label="散点图着色依据">
        <button type="button" :class="{ active: colorMode === 'gradeClass' }" @click="setColorMode('gradeClass')">
          成绩等级
        </button>
        <button type="button" :class="{ active: colorMode === 'gpaBucket' }" @click="setColorMode('gpaBucket')">
          GPA 区间
        </button>
      </div>
    </div>

    <div class="focus-control-row focus-control-row--groups">
      <span class="focus-control-label">聚焦学生</span>
      <div class="focus-chip-list" :aria-label="`${focusModeLabel}聚焦`">
        <button type="button" class="focus-chip" :class="{ active: focusedGroup === null }" @click="resetFocus">
          全部
          <strong>{{ totalStudents }}</strong>
        </button>
        <button
          v-for="group in activeGroups"
          :key="`${colorMode}-${group.value}`"
          type="button"
          class="focus-chip"
          :class="{ active: focusedGroup === group.value }"
          @click="toggleFocus(group.value)"
        >
          <span class="focus-chip__swatch" :style="{ backgroundColor: group.color }" />
          {{ group.label }}
          <strong>{{ group.count }}</strong>
        </button>
      </div>
    </div>

    <div class="focus-control-row">
      <span class="focus-control-label">显示方式</span>
      <div class="focus-segment" aria-label="散点图显示方式">
        <button type="button" :class="{ active: displayMode === 'dim' }" @click="setDisplayMode('dim')">
          弱化其他学生
        </button>
        <button type="button" :class="{ active: displayMode === 'only' }" @click="setDisplayMode('only')">
          仅显示选中学生
        </button>
      </div>
    </div>
  </section>

  <section class="analysis-grid">
    <AnalysisChartCard
      title="GPA 区间人数气泡图"
      subtitle="圆形面积按人数平方根映射，直接显示每个 GPA 区间人数。"
      :option="gpaBubbleOption"
      :loading="loading"
      :error="error"
      :empty="!gpaDistribution.length"
      @retry="loadCharts"
    />
    <AnalysisChartCard
      title="GradeClass 成绩等级柱状图"
      subtitle="按后端成绩等级业务顺序展示各等级人数与占比。"
      :option="gradeBarOption"
      :loading="loading"
      :error="error"
      :empty="!gradeClassDistribution.length"
      @retry="loadCharts"
    />
    <AnalysisChartCard
      title="缺勤次数与 GPA 散点图"
      subtitle="每个圆点代表一名学生，单击聚焦同组学生，双击进入学生详情。"
      :option="absenceGpaOption"
      :loading="loading"
      :error="error"
      :empty="!performancePoints.length"
      series-only-updates
      :legend-items="activeGroups"
      :legend-active-value="focusedGroup"
      @retry="loadCharts"
      @chart-click="handlePointFocus"
      @chart-dblclick="handlePointDetail"
      @legend-click="handleLegendToggle"
    />
    <AnalysisChartCard
      title="每周学习时长与 GPA 散点图"
      subtitle="观察学习投入与 GPA 的关系，颜色跟随当前着色依据。"
      :option="studyTimeGpaOption"
      :loading="loading"
      :error="error"
      :empty="!performancePoints.length"
      series-only-updates
      :legend-items="activeGroups"
      :legend-active-value="focusedGroup"
      @retry="loadCharts"
      @chart-click="handlePointFocus"
      @chart-dblclick="handlePointDetail"
      @legend-click="handleLegendToggle"
    />
    <AnalysisChartCard
      title="学习投入、缺勤与 GPA 联合分布"
      subtitle="横轴为每周学习时长，纵轴为缺勤次数，聚焦条件会同步作用于三张散点图。"
      :option="jointScatterOption"
      :loading="loading"
      :error="error"
      :empty="!performancePoints.length"
      tall
      series-only-updates
      :legend-items="activeGroups"
      :legend-active-value="focusedGroup"
      @retry="loadCharts"
      @chart-click="handlePointFocus"
      @chart-dblclick="handlePointDetail"
      @legend-click="handleLegendToggle"
    />
  </section>
</template>
