<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { DataAnalysis, Refresh } from "@element-plus/icons-vue";
import { fetchLatestPrediction, predictStudent } from "../../api/prediction";
import type {
  ImportantFactor,
  PredictionProbability,
  PredictionResult,
  StudentPrediction,
  WarningRecord
} from "../../types";

const props = defineProps<{
  studentNo?: number;
  performanceAvailable: boolean;
}>();

const loading = ref(false);
const predicting = ref(false);
const prediction = ref<StudentPrediction | null>(null);
const errorText = ref("");

const predictionResult = computed(() => prediction.value?.prediction || null);
const warning = computed(() => prediction.value?.warning || null);
const probabilities = computed(() => normalizeProbabilities(predictionResult.value?.probabilities || []));
const factors = computed(() => normalizeFactors(importantFactors(predictionResult.value)).slice(0, 5));
const emptyDescription = computed(() => {
  if (!props.performanceAvailable) {
    return "该学生暂无学业表现记录，无法执行模型预测。";
  }
  if (errorText.value) {
    return errorText.value;
  }
  return "暂无预测结果，可点击执行预测生成学业风险预警。";
});

function coalesce<T>(...values: Array<T | null | undefined>) {
  return values.find((value) => value !== undefined && value !== null) ?? null;
}

function predictionId(item?: PredictionResult | null) {
  return coalesce(item?.predictionResultId, item?.prediction_result_id);
}

function modelVersionNo(item?: PredictionResult | null) {
  return coalesce(item?.modelVersionNo, item?.model_version_no) || "-";
}

function predictedGradeLabel(item?: PredictionResult | null) {
  return coalesce(item?.predictedGradeLabel, item?.predicted_grade_label) || "-";
}

function predictedGradeClass(item?: PredictionResult | null) {
  return coalesce(item?.predictedGradeClass, item?.predicted_grade_class);
}

function createdAt(item?: PredictionResult | null) {
  return coalesce(item?.createdAt, item?.created_at);
}

function importantFactors(item?: PredictionResult | null) {
  return coalesce(item?.importantFactors, item?.important_factors) || [];
}

function warningRiskLevel(item?: WarningRecord | null) {
  return coalesce(item?.riskLevel, item?.risk_level) || "";
}

function warningRiskLabel(item?: WarningRecord | null) {
  return coalesce(item?.riskLevelLabel, item?.risk_level_label) || riskLevelLabel(warningRiskLevel(item));
}

function warningRiskScore(item?: WarningRecord | null) {
  return coalesce(item?.riskScore, item?.risk_score);
}

function warningStatus(item?: WarningRecord | null) {
  return coalesce(item?.status, item?.status);
}

function warningStatusLabel(item?: WarningRecord | null) {
  return coalesce(item?.statusLabel, item?.status_label) || statusLabel(warningStatus(item) || "");
}

function warningReasons(item?: WarningRecord | null) {
  return coalesce(item?.riskReasons, item?.risk_reasons) || [];
}

function warningSuggestions(item?: WarningRecord | null) {
  return item?.suggestions || [];
}

function normalizeProbabilities(items: PredictionProbability[]) {
  return items.map((item) => ({
    gradeClass: coalesce(item.gradeClass, item.grade_class),
    label: coalesce(item.gradeLabel, item.grade_label) || item.gradeClass?.label || item.grade_class?.label || "-",
    percent: toPercent(item.probability)
  }));
}

function normalizeFactors(items: ImportantFactor[]) {
  return items.map((item) => ({
    feature: item.feature || "",
    label: item.label || item.feature || "影响因素",
    value: formatValue(item.value),
    importance: item.importance == null ? "-" : Number(item.importance).toFixed(4)
  }));
}

function toPercent(value: number | string) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return 0;
  }
  return Number((numeric <= 1 ? numeric * 100 : numeric).toFixed(2));
}

function formatPercent(value: number) {
  return `${value.toFixed(2)}%`;
}

function formatValue(value: unknown) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  if (typeof value === "number") {
    return Number.isInteger(value) ? String(value) : value.toFixed(2);
  }
  return String(value);
}

function formatTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 19);
}

function riskLevelLabel(level: string) {
  const labels: Record<string, string> = {
    LOW: "低风险",
    MEDIUM: "中风险",
    HIGH: "高风险"
  };
  return labels[level] || level || "-";
}

function riskTagType(level: string) {
  if (level === "HIGH") {
    return "danger";
  }
  if (level === "MEDIUM") {
    return "warning";
  }
  if (level === "LOW") {
    return "success";
  }
  return "info";
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    UNPROCESSED: "未处理",
    PROCESSING: "处理中",
    DONE: "已完成",
    IGNORED: "已忽略"
  };
  return labels[status] || status || "-";
}

function statusTagType(status?: string | null) {
  if (status === "DONE") {
    return "success";
  }
  if (status === "PROCESSING") {
    return "warning";
  }
  if (status === "IGNORED") {
    return "info";
  }
  return "danger";
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : "请求失败";
}

function isEmptyPredictionError(message: string) {
  return message.includes("暂无预测") || message.includes("没有预测");
}

async function loadLatestPrediction() {
  if (!props.studentNo) {
    return;
  }
  loading.value = true;
  errorText.value = "";
  try {
    prediction.value = await fetchLatestPrediction(props.studentNo);
  } catch (error) {
    prediction.value = null;
    const message = errorMessage(error);
    if (!isEmptyPredictionError(message)) {
      errorText.value = message;
    }
  } finally {
    loading.value = false;
  }
}

async function handlePredict() {
  if (!props.studentNo || !props.performanceAvailable) {
    return;
  }
  predicting.value = true;
  errorText.value = "";
  try {
    prediction.value = await predictStudent(props.studentNo, { generate_warning: true });
    ElMessage.success("预测已完成，风险预警已同步生成");
  } catch (error) {
    const message = errorMessage(error);
    errorText.value = message;
    ElMessage.error(message);
  } finally {
    predicting.value = false;
  }
}

onMounted(() => {
  void loadLatestPrediction();
});

watch(
  () => props.studentNo,
  () => {
    prediction.value = null;
    void loadLatestPrediction();
  }
);
</script>

<template>
  <section class="detail-section prediction-section">
    <div class="section-title prediction-title">
      <el-icon><DataAnalysis /></el-icon>
      <div>
        <h2>学业预测与风险预警</h2>
        <p>基于当前启用模型生成单学生成绩等级预测，并同步展示最近一次风险预警。</p>
      </div>
    </div>

    <div class="prediction-actions">
      <el-button :loading="loading" @click="loadLatestPrediction">
        <el-icon><Refresh /></el-icon>
        刷新预测
      </el-button>
      <el-button
        type="primary"
        :loading="predicting"
        :disabled="!studentNo || !performanceAvailable"
        @click="handlePredict"
      >
        执行预测
      </el-button>
    </div>

    <el-skeleton v-if="loading" :rows="5" animated />

    <el-empty
      v-else-if="!predictionResult"
      :image-size="76"
      :description="emptyDescription"
    />

    <div v-else class="prediction-content">
      <div class="prediction-main">
        <article class="prediction-outcome">
          <span>预测成绩等级</span>
          <strong>
            {{ predictedGradeClass(predictionResult)?.label || "等级" }}
            <small>{{ predictedGradeLabel(predictionResult) }}</small>
          </strong>
          <p>使用模型 {{ modelVersionNo(predictionResult) }}</p>
          <p>预测时间 {{ formatTime(createdAt(predictionResult)) }}</p>
          <p v-if="predictionId(predictionResult)">预测结果 #{{ predictionId(predictionResult) }}</p>
        </article>

        <div class="probability-list" aria-label="预测概率">
          <div v-for="item in probabilities" :key="item.label" class="probability-row">
            <div>
              <span>{{ item.gradeClass?.label || item.label }}</span>
              <strong>{{ item.label }}</strong>
            </div>
            <el-progress :percentage="item.percent" :stroke-width="10" :show-text="false" />
            <span class="probability-value">{{ formatPercent(item.percent) }}</span>
          </div>
        </div>
      </div>

      <div class="prediction-subgrid">
        <article class="prediction-box">
          <h3>重要因素</h3>
          <el-empty v-if="factors.length === 0" :image-size="54" description="暂无因素数据" />
          <ul v-else class="factor-list">
            <li v-for="item in factors" :key="item.feature || item.label">
              <div>
                <strong>{{ item.label }}</strong>
                <span>当前值 {{ item.value }}</span>
              </div>
              <em>重要性 {{ item.importance }}</em>
            </li>
          </ul>
        </article>

        <article class="prediction-box warning-box">
          <h3>最近预警</h3>
          <el-empty v-if="!warning" :image-size="54" description="暂无关联预警" />
          <div v-else class="warning-summary-card">
            <div class="warning-summary-head">
              <el-tag :type="riskTagType(warningRiskLevel(warning))" effect="light">
                {{ warningRiskLabel(warning) }}
              </el-tag>
              <el-tag :type="statusTagType(warningStatus(warning))" effect="plain">
                {{ warningStatusLabel(warning) }}
              </el-tag>
              <strong>风险分 {{ warningRiskScore(warning) ?? "-" }}</strong>
            </div>
            <div v-if="warningReasons(warning).length" class="warning-list">
              <span>风险原因</span>
              <ul>
                <li v-for="item in warningReasons(warning)" :key="item">{{ item }}</li>
              </ul>
            </div>
            <div v-if="warningSuggestions(warning).length" class="warning-list">
              <span>处理建议</span>
              <ul>
                <li v-for="item in warningSuggestions(warning)" :key="item">{{ item }}</li>
              </ul>
            </div>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>
