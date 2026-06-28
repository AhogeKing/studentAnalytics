<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Refresh, View } from "@element-plus/icons-vue";
import {
  fetchActiveModel,
  fetchModelVersionDetail,
  fetchModelVersions,
  trainDecisionTree
} from "../api/model";
import { useAuthStore } from "../stores/auth";
import type {
  ModelTrainMode,
  ModelTrainPayload,
  ModelTrainResult,
  ModelVersion,
  ModelVersionDetail,
  ModelVersionQuery
} from "../types";

const authStore = useAuthStore();
const loading = ref(false);
const activeLoading = ref(false);
const detailLoading = ref(false);
const training = ref(false);
const activeModel = ref<ModelVersion | null>(null);
const versions = ref<ModelVersion[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const activeFilter = ref<"all" | "true" | "false">("all");
const drawerVisible = ref(false);
const selectedModel = ref<ModelVersionDetail | null>(null);
const lastTrain = ref<ModelTrainResult | null>(null);

const trainForm = reactive<Required<ModelTrainPayload>>({
  mode: "quick",
  activate: true
});

const canTrain = computed(() => authStore.role.toLowerCase() === "admin");
const activeVersionNo = computed(() => activeModel.value ? versionNo(activeModel.value) : "");
const pageActiveCount = computed(() => versions.value.filter((item) => modelActive(item)).length);
const pageAvgAccuracy = computed(() => {
  const metrics = versions.value.map((item) => Number(metricValue(item.accuracy))).filter(Number.isFinite);
  if (metrics.length === 0) {
    return "-";
  }
  const average = metrics.reduce((sum, item) => sum + item, 0) / metrics.length;
  return formatMetric(average);
});
const selectedFeatureList = computed(() => normalizeArray(coalesce(selectedModel.value?.featureColumns, selectedModel.value?.feature_columns)));
const selectedConfusionMatrix = computed(() => normalizeMatrix(coalesce(selectedModel.value?.confusionMatrix, selectedModel.value?.confusion_matrix)));
const selectedMetricsText = computed(() => formatJson(selectedModel.value?.metrics || {}));
const selectedParamsText = computed(() => formatJson(coalesce(selectedModel.value?.bestParameters, selectedModel.value?.best_parameters) || {}));

function coalesce<T>(...values: Array<T | null | undefined>) {
  return values.find((value) => value !== undefined && value !== null) ?? null;
}

function modelId(item: ModelVersion | ModelVersionDetail | ModelTrainResult | null) {
  return coalesce(item?.id, item?.modelVersionId, item?.model_version_id);
}

function modelName(item: ModelVersion | ModelVersionDetail | ModelTrainResult | null) {
  return coalesce(item?.modelName, item?.model_name) || "-";
}

function versionNo(item: ModelVersion | ModelVersionDetail | ModelTrainResult | null) {
  return coalesce(item?.versionNo, item?.version_no) || "-";
}

function trainedAt(item: ModelVersion | ModelVersionDetail | ModelTrainResult | null) {
  return coalesce(item?.trainedAt, item?.trained_at);
}

function createdAt(item: ModelVersion | ModelVersionDetail | ModelTrainResult | null) {
  return coalesce(item?.createdAt, item?.created_at);
}

function modelActive(item: ModelVersion | ModelVersionDetail | ModelTrainResult | null) {
  return Boolean(coalesce(item?.active, item?.is_active));
}

function precisionMacro(item: ModelVersion | ModelVersionDetail | ModelTrainResult | null) {
  return metricValue(coalesce(item?.precisionMacro, item?.precision_macro));
}

function recallMacro(item: ModelVersion | ModelVersionDetail | ModelTrainResult | null) {
  return metricValue(coalesce(item?.recallMacro, item?.recall_macro));
}

function f1Macro(item: ModelVersion | ModelVersionDetail | ModelTrainResult | null) {
  return metricValue(coalesce(item?.f1Macro, item?.f1_macro));
}

function metricValue(value: number | string | null | undefined) {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : null;
}

function formatMetric(value: number | string | null | undefined) {
  const numeric = metricValue(value);
  if (numeric === null) {
    return "-";
  }
  return `${(numeric <= 1 ? numeric * 100 : numeric).toFixed(2)}%`;
}

function formatCount(value: number | null | undefined) {
  return value == null ? "-" : String(value);
}

function formatTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 19);
}

function trainModeLabel(mode: ModelTrainMode | string) {
  const labels: Record<string, string> = {
    quick: "快速训练",
    default: "标准训练",
    exhaustive: "完整搜索"
  };
  return labels[mode] || mode;
}

function normalizeArray(value: unknown) {
  const parsed = parseMaybeJson(value);
  if (Array.isArray(parsed)) {
    return parsed.map((item) => String(item));
  }
  return [];
}

function normalizeMatrix(value: unknown) {
  const parsed = parseMaybeJson(value);
  if (!Array.isArray(parsed)) {
    return [];
  }
  return parsed
    .filter((row): row is unknown[] => Array.isArray(row))
    .map((row) => row.map((cell) => Number(cell)));
}

function parseMaybeJson(value: unknown) {
  if (typeof value !== "string") {
    return value;
  }
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

function formatJson(value: unknown) {
  const parsed = parseMaybeJson(value);
  try {
    return JSON.stringify(parsed ?? {}, null, 2);
  } catch {
    return String(value ?? "{}");
  }
}

function buildQuery(): ModelVersionQuery {
  const query: ModelVersionQuery = {
    page_num: page.value,
    page_size: pageSize.value
  };
  if (activeFilter.value !== "all") {
    query.active = activeFilter.value === "true";
  }
  return query;
}

async function loadActiveModel() {
  activeLoading.value = true;
  try {
    activeModel.value = await fetchActiveModel();
  } catch {
    activeModel.value = null;
  } finally {
    activeLoading.value = false;
  }
}

async function loadVersions() {
  loading.value = true;
  try {
    const result = await fetchModelVersions(buildQuery());
    versions.value = result.records;
    total.value = result.total;
  } catch {
    versions.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

async function refreshAll() {
  await Promise.all([loadActiveModel(), loadVersions()]);
}

async function handleTrain() {
  training.value = true;
  try {
    lastTrain.value = await trainDecisionTree({
      mode: trainForm.mode,
      activate: trainForm.activate
    });
    ElMessage.success("模型训练完成");
    await refreshAll();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "模型训练失败");
  } finally {
    training.value = false;
  }
}

async function openDetail(item: ModelVersion) {
  const id = modelId(item);
  if (!id) {
    return;
  }
  drawerVisible.value = true;
  selectedModel.value = null;
  detailLoading.value = true;
  try {
    selectedModel.value = await fetchModelVersionDetail(id);
  } finally {
    detailLoading.value = false;
  }
}

function handlePageSizeChange() {
  page.value = 1;
  void loadVersions();
}

function handleActiveFilterChange() {
  page.value = 1;
  void loadVersions();
}

onMounted(() => {
  void refreshAll();
});
</script>

<template>
  <section class="page-head">
    <div>
      <p class="eyebrow">模型管理</p>
      <h1>决策树模型版本</h1>
      <p>查看当前启用模型、历史训练版本和关键指标；管理员可触发新的模型训练。</p>
    </div>
    <div class="head-actions">
      <el-button :loading="loading || activeLoading" @click="refreshAll">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>
  </section>

  <section class="operation-summary model-summary" aria-label="模型概览">
    <article>
      <span>当前启用模型</span>
      <strong>{{ activeVersionNo || "暂无" }}</strong>
      <small>{{ activeModel ? modelName(activeModel) : "请管理员训练并启用模型" }}</small>
    </article>
    <article>
      <span>版本总数</span>
      <strong>{{ total }}</strong>
      <small>匹配当前筛选条件</small>
    </article>
    <article>
      <span>当前页启用</span>
      <strong>{{ pageActiveCount }}</strong>
      <small>通常应只有一个版本处于启用状态</small>
    </article>
    <article>
      <span>当前页平均 Accuracy</span>
      <strong>{{ pageAvgAccuracy }}</strong>
      <small>仅基于本页可见版本计算</small>
    </article>
  </section>

  <section v-if="canTrain" class="query-panel model-train-panel">
    <div class="panel-heading">
      <div>
        <h2>训练决策树模型</h2>
        <p>模型训练可能需要较长时间，请勿重复点击。成功后会刷新启用模型和版本列表。</p>
      </div>
      <el-button type="primary" :loading="training" @click="handleTrain">开始训练</el-button>
    </div>
    <div class="model-train-form">
      <el-select v-model="trainForm.mode" placeholder="训练模式">
        <el-option label="快速训练，适合联调测试" value="quick" />
        <el-option label="标准训练，平衡速度和效果" value="default" />
        <el-option label="完整搜索，耗时更久" value="exhaustive" />
      </el-select>
      <label class="train-switch">
        <span>训练后设为启用模型</span>
        <el-switch v-model="trainForm.activate" />
      </label>
    </div>
    <el-alert
      v-if="lastTrain"
      type="success"
      show-icon
      :closable="false"
      :title="`最近训练完成：${versionNo(lastTrain)}，${trainModeLabel(trainForm.mode)}，Accuracy ${formatMetric(lastTrain.accuracy)}`"
    />
  </section>

  <section class="content-panel">
    <div class="panel-heading">
      <div>
        <h2>模型版本列表</h2>
        <p>共 {{ total }} 个版本，每页 {{ pageSize }} 条。</p>
      </div>
      <el-select v-model="activeFilter" class="model-active-filter" @change="handleActiveFilterChange">
        <el-option label="全部版本" value="all" />
        <el-option label="仅启用" value="true" />
        <el-option label="仅未启用" value="false" />
      </el-select>
    </div>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="versions" class="student-table" row-key="id">
        <el-table-column label="版本号" min-width="230" fixed="left">
          <template #default="{ row }">
            <strong class="summary-text">{{ versionNo(row) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="算法" min-width="130">
          <template #default="{ row }">{{ row.algorithm || "-" }}</template>
        </el-table-column>
        <el-table-column label="Accuracy" width="116">
          <template #default="{ row }">{{ formatMetric(row.accuracy) }}</template>
        </el-table-column>
        <el-table-column label="Precision" width="116">
          <template #default="{ row }">{{ formatMetric(precisionMacro(row)) }}</template>
        </el-table-column>
        <el-table-column label="Recall" width="116">
          <template #default="{ row }">{{ formatMetric(recallMacro(row)) }}</template>
        </el-table-column>
        <el-table-column label="F1" width="116">
          <template #default="{ row }">{{ formatMetric(f1Macro(row)) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="modelActive(row) ? 'success' : 'info'" effect="light">
              {{ modelActive(row) ? "启用" : "未启用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="训练时间" min-width="180">
          <template #default="{ row }">{{ formatTime(trainedAt(row)) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="96" fixed="right" align="right">
          <template #default="{ row }">
            <el-button size="small" :icon="View" circle title="查看详情" @click="openDetail(row)" />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        background
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        @current-change="() => loadVersions()"
        @size-change="handlePageSizeChange"
      />
    </div>
  </section>

  <el-drawer v-model="drawerVisible" class="operation-log-drawer" title="模型版本详情" size="min(780px, 100vw)">
    <div v-loading="detailLoading" class="model-detail">
      <template v-if="selectedModel">
        <dl class="operation-detail-grid">
          <div>
            <dt>版本号</dt>
            <dd>{{ versionNo(selectedModel) }}</dd>
          </div>
          <div>
            <dt>模型名称</dt>
            <dd>{{ modelName(selectedModel) }}</dd>
          </div>
          <div>
            <dt>算法</dt>
            <dd>{{ selectedModel.algorithm || "-" }}</dd>
          </div>
          <div>
            <dt>目标字段</dt>
            <dd>{{ selectedModel.targetColumn || selectedModel.target_column || "-" }}</dd>
          </div>
          <div>
            <dt>Accuracy</dt>
            <dd>{{ formatMetric(selectedModel.accuracy) }}</dd>
          </div>
          <div>
            <dt>Precision Macro</dt>
            <dd>{{ formatMetric(precisionMacro(selectedModel)) }}</dd>
          </div>
          <div>
            <dt>Recall Macro</dt>
            <dd>{{ formatMetric(recallMacro(selectedModel)) }}</dd>
          </div>
          <div>
            <dt>F1 Macro</dt>
            <dd>{{ formatMetric(f1Macro(selectedModel)) }}</dd>
          </div>
          <div>
            <dt>训练时间</dt>
            <dd>{{ formatTime(trainedAt(selectedModel)) }}</dd>
          </div>
          <div>
            <dt>创建时间</dt>
            <dd>{{ formatTime(createdAt(selectedModel)) }}</dd>
          </div>
          <div>
            <dt>最大深度</dt>
            <dd>{{ selectedModel.maxDepth ?? selectedModel.max_depth ?? "-" }}</dd>
          </div>
          <div>
            <dt>最小叶子样本</dt>
            <dd>{{ selectedModel.minSamplesLeaf ?? selectedModel.min_samples_leaf ?? "-" }}</dd>
          </div>
        </dl>

        <section class="model-feature-section">
          <h3>特征字段</h3>
          <div v-if="selectedFeatureList.length" class="model-feature-list">
            <el-tag v-for="item in selectedFeatureList" :key="item" effect="plain">{{ item }}</el-tag>
          </div>
          <el-empty v-else :image-size="54" description="暂无特征字段" />
        </section>

        <section class="model-feature-section">
          <h3>混淆矩阵</h3>
          <div v-if="selectedConfusionMatrix.length" class="matrix-table-wrap">
            <table class="matrix-table">
              <tbody>
                <tr v-for="(row, rowIndex) in selectedConfusionMatrix" :key="rowIndex">
                  <td v-for="(cell, cellIndex) in row" :key="`${rowIndex}-${cellIndex}`">{{ cell }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <el-empty v-else :image-size="54" description="暂无混淆矩阵" />
        </section>

        <section class="json-section">
          <div class="json-section-head">
            <h3>最佳参数</h3>
          </div>
          <pre>{{ selectedParamsText }}</pre>
        </section>

        <section class="json-section">
          <div class="json-section-head">
            <h3>完整 Metrics JSON</h3>
          </div>
          <pre>{{ selectedMetricsText }}</pre>
        </section>

        <section class="model-feature-section">
          <h3>模型路径</h3>
          <p class="model-path">{{ selectedModel.modelPath || selectedModel.model_path || "-" }}</p>
        </section>
      </template>
    </div>
  </el-drawer>
</template>
