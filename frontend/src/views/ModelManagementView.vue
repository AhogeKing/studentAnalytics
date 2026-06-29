<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import { storeToRefs } from "pinia";
import { ElMessage, ElMessageBox } from "element-plus";
import { CircleCheck, Delete as DeleteIcon, EditPen, QuestionFilled, Refresh, View } from "@element-plus/icons-vue";
import {
  activateModelVersion,
  deleteModelVersion,
  fetchActiveModel,
  fetchModelVersionDetail,
  fetchModelVersions,
  updateModelVersion
} from "../api/model";
import { useAuthStore } from "../stores/auth";
import { useModelTrainingStore } from "../stores/modelTraining";
import type {
  ModelTrainMode,
  ModelTrainPayload,
  ModelTrainResult,
  ModelVersion,
  ModelVersionDetail,
  ModelVersionQuery
} from "../types";

const authStore = useAuthStore();
const modelTrainingStore = useModelTrainingStore();
const { lastTrain, mode: currentTrainingMode, startedAt, training } = storeToRefs(modelTrainingStore);
const loading = ref(false);
const activeLoading = ref(false);
const detailLoading = ref(false);
const activatingId = ref<number | null>(null);
const deletingId = ref<number | null>(null);
const editingId = ref<number | null>(null);
const nowTick = ref(Date.now());
let timerId: number | null = null;
const activeModel = ref<ModelVersion | null>(null);
const versions = ref<ModelVersion[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const activeFilter = ref<"all" | "true" | "false">("all");
const drawerVisible = ref(false);
const selectedModel = ref<ModelVersionDetail | null>(null);

const trainForm = reactive<Required<ModelTrainPayload>>({
  mode: "quick",
  activate: true
});

const metricGuides = [
  {
    key: "accuracy",
    name: "Accuracy",
    text: "整体预测正确率，表示测试集中有多少学生等级被模型预测正确。"
  },
  {
    key: "precision",
    name: "Precision",
    text: "预测为某个等级时有多少是真的，适合观察误报情况；这里使用各等级平均值。"
  },
  {
    key: "recall",
    name: "Recall",
    text: "某个真实等级中有多少被找出来，适合观察漏报情况；这里使用各等级平均值。"
  },
  {
    key: "f1",
    name: "F1",
    text: "Precision 和 Recall 的综合指标，二者更均衡时 F1 更高。"
  }
];

const trainModeGuides = [
  {
    mode: "quick",
    name: "快速训练",
    depth: "最大深度候选 4 / 6 / 8",
    candidates: "约 96 组参数组合",
    duration: "Apple M1 通常约 5-60 秒"
  },
  {
    mode: "default",
    name: "标准训练",
    depth: "最大深度候选 3 / 4 / 5 / 6 / 8 / 10",
    candidates: "约 1728 组参数组合",
    duration: "Apple M1 通常约 2-5 分钟"
  },
  {
    mode: "exhaustive",
    name: "完整搜索",
    depth: "包含不限制深度和更多叶节点候选",
    candidates: "参数空间更大",
    duration: "Apple M1 可能超过 10 分钟"
  }
];

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
const runningTrainingDuration = computed(() => {
  if (!training.value || !startedAt.value) {
    return "-";
  }
  const startTime = new Date(startedAt.value).getTime();
  if (!Number.isFinite(startTime)) {
    return "-";
  }
  return formatDuration(Math.max(0, nowTick.value - startTime));
});

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

function trainingDuration(item: ModelVersion | ModelVersionDetail | ModelTrainResult | null) {
  return coalesce(item?.trainingDurationMs, item?.training_duration_ms);
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

function metricDescription(name: string) {
  const normalized = name.toLowerCase();
  return metricGuides.find((item) => item.key === normalized)?.text || "";
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

function formatDuration(value: number | string | null | undefined) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  const milliseconds = Number(value);
  if (!Number.isFinite(milliseconds)) {
    return "-";
  }
  if (milliseconds < 1000) {
    return `${Math.max(0, Math.round(milliseconds))}ms`;
  }
  const totalSeconds = Math.round(milliseconds / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  if (minutes === 0) {
    return `${seconds}s`;
  }
  return `${minutes}m ${seconds}s`;
}

function trainModeLabel(mode: ModelTrainMode | string) {
  const labels: Record<string, string> = {
    quick: "快速训练",
    default: "标准训练"
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
  if (training.value) {
    ElMessage.warning("模型训练正在进行，请等待当前任务完成");
    return;
  }
  try {
    await modelTrainingStore.startTraining({
      mode: trainForm.mode,
      activate: trainForm.activate
    });
    ElMessage.success("模型训练完成");
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "模型训练失败");
  }
}

async function handleActivate(item: ModelVersion) {
  const id = modelId(item);
  if (!id || modelActive(item)) {
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认将模型版本 ${versionNo(item)} 设为当前启用模型吗？之后不指定版本的预测会使用该模型。`,
      "切换启用模型",
      {
        type: "warning",
        confirmButtonText: "设为启用",
        cancelButtonText: "取消"
      }
    );
  } catch {
    return;
  }

  activatingId.value = id;
  try {
    activeModel.value = await activateModelVersion(id);
    ElMessage.success("当前启用模型已切换");
    await loadVersions();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "切换模型失败");
  } finally {
    activatingId.value = null;
  }
}

async function handleEditVersionNo(item: ModelVersion) {
  const id = modelId(item);
  if (!id) {
    return;
  }
  let value = "";
  try {
    const result = await ElMessageBox.prompt(
      "请输入新的模型版本号，长度不超过 50 个字符。",
      "修改模型版本号",
      {
        inputValue: versionNo(item),
        inputPlaceholder: "例如 dt_cls_20260629183000000",
        confirmButtonText: "保存",
        cancelButtonText: "取消",
        inputValidator: (inputValue) => {
          const trimmed = String(inputValue ?? "").trim();
          if (!trimmed) {
            return "模型版本号不能为空";
          }
          if (trimmed.length > 50) {
            return "模型版本号长度不能超过50个字符";
          }
          return true;
        }
      }
    );
    value = String(result.value ?? "").trim();
  } catch {
    return;
  }

  if (!value || value === versionNo(item)) {
    return;
  }
  editingId.value = id;
  try {
    const updated = await updateModelVersion(id, { version_no: value });
    ElMessage.success("模型版本号已修改");
    if (modelActive(updated)) {
      activeModel.value = updated;
    }
    await loadVersions();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "修改模型版本号失败");
  } finally {
    editingId.value = null;
  }
}

async function handleDeleteVersion(item: ModelVersion) {
  const id = modelId(item);
  if (!id) {
    return;
  }
  if (modelActive(item)) {
    ElMessage.warning("当前启用模型不能删除，请先启用其他模型版本");
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认删除模型版本 ${versionNo(item)} 吗？已被预测结果引用的模型版本不会被删除。`,
      "删除模型版本",
      {
        type: "warning",
        confirmButtonText: "删除",
        cancelButtonText: "取消",
        confirmButtonClass: "el-button--danger"
      }
    );
  } catch {
    return;
  }

  deletingId.value = id;
  try {
    await deleteModelVersion(id);
    ElMessage.success("模型版本已删除");
    if (versions.value.length === 1 && page.value > 1) {
      page.value -= 1;
    }
    await refreshAll();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "删除模型版本失败");
  } finally {
    deletingId.value = null;
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
  timerId = window.setInterval(() => {
    nowTick.value = Date.now();
  }, 1000);
  void refreshAll();
});

onUnmounted(() => {
  if (timerId !== null) {
    window.clearInterval(timerId);
  }
});

watch(lastTrain, (value) => {
  if (value) {
    void refreshAll();
  }
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
      <div class="train-heading-actions">
        <span v-if="training" class="train-running-status">
          正在执行{{ trainModeLabel(currentTrainingMode || trainForm.mode) }}，已用时 {{ runningTrainingDuration }}
        </span>
        <el-button type="primary" :loading="training" :disabled="training" @click="handleTrain">
          {{ training ? "训练中" : "开始训练" }}
        </el-button>
      </div>
    </div>
    <div class="model-train-form">
      <el-select v-model="trainForm.mode" placeholder="训练模式">
        <el-option label="快速训练，适合联调测试（最多等待 5 分钟）" value="quick" />
        <el-option label="标准训练，平衡速度和效果（最多等待 10 分钟）" value="default" />
      </el-select>
      <label class="train-switch">
        <span>训练后设为启用模型</span>
        <el-switch v-model="trainForm.activate" />
      </label>
    </div>
    <div class="model-train-guide" aria-label="训练模式参数说明">
      <article
        v-for="item in trainModeGuides"
        :key="item.mode"
        :class="{ 'is-disabled': item.mode === 'exhaustive' }"
      >
        <div class="train-guide-head">
          <strong>{{ item.name }}</strong>
          <el-tag v-if="item.mode === trainForm.mode" size="small" type="success" effect="light">当前选择</el-tag>
          <el-tag v-else-if="item.mode === 'exhaustive'" size="small" type="info" effect="plain">暂不开放</el-tag>
        </div>
        <dl>
          <div>
            <dt>深度范围</dt>
            <dd>{{ item.depth }}</dd>
          </div>
          <div>
            <dt>搜索规模</dt>
            <dd>{{ item.candidates }}</dd>
          </div>
          <div>
            <dt>M1 参考耗时</dt>
            <dd>{{ item.duration }}</dd>
          </div>
        </dl>
      </article>
    </div>
    <el-alert
      v-if="lastTrain"
      type="success"
      show-icon
      :closable="false"
      :title="`最近训练完成：${versionNo(lastTrain)}，${trainModeLabel(coalesce(lastTrain.searchMode, lastTrain.search_mode, trainForm.mode) || trainForm.mode)}，耗时 ${formatDuration(trainingDuration(lastTrain))}，Accuracy ${formatMetric(lastTrain.accuracy)}`"
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

    <div class="model-metric-guide" aria-label="模型指标解释">
      <article v-for="item in metricGuides" :key="item.key">
        <strong>{{ item.name }}</strong>
        <span>{{ item.text }}</span>
      </article>
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
        <el-table-column width="116">
          <template #header>
            <span class="metric-column-head">
              Accuracy
              <el-tooltip :content="metricDescription('accuracy')" placement="top">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <template #default="{ row }">{{ formatMetric(row.accuracy) }}</template>
        </el-table-column>
        <el-table-column width="116">
          <template #header>
            <span class="metric-column-head">
              Precision
              <el-tooltip :content="metricDescription('precision')" placement="top">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <template #default="{ row }">{{ formatMetric(precisionMacro(row)) }}</template>
        </el-table-column>
        <el-table-column width="116">
          <template #header>
            <span class="metric-column-head">
              Recall
              <el-tooltip :content="metricDescription('recall')" placement="top">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <template #default="{ row }">{{ formatMetric(recallMacro(row)) }}</template>
        </el-table-column>
        <el-table-column width="116">
          <template #header>
            <span class="metric-column-head">
              F1
              <el-tooltip :content="metricDescription('f1')" placement="top">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <template #default="{ row }">{{ formatMetric(f1Macro(row)) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="modelActive(row) ? 'success' : 'info'" effect="light">
              {{ modelActive(row) ? "启用" : "未启用" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="训练耗时" width="120">
          <template #default="{ row }">{{ formatDuration(trainingDuration(row)) }}</template>
        </el-table-column>
        <el-table-column label="训练时间" min-width="180">
          <template #default="{ row }">{{ formatTime(trainedAt(row)) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="264" fixed="right" align="right">
          <template #default="{ row }">
            <div class="model-row-actions">
              <el-button
                v-if="canTrain && !modelActive(row)"
                size="small"
                type="primary"
                plain
                :icon="CircleCheck"
                :loading="activatingId === modelId(row)"
                @click="handleActivate(row)"
              >
                启用
              </el-button>
              <el-button
                v-if="canTrain"
                size="small"
                plain
                :icon="EditPen"
                :loading="editingId === modelId(row)"
                @click="handleEditVersionNo(row)"
              >
                改名
              </el-button>
              <el-button
                v-if="canTrain && !modelActive(row)"
                size="small"
                type="danger"
                plain
                :icon="DeleteIcon"
                :loading="deletingId === modelId(row)"
                @click="handleDeleteVersion(row)"
              >
                删除
              </el-button>
              <el-button size="small" :icon="View" circle title="查看详情" @click="openDetail(row)" />
            </div>
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
            <dt>训练耗时</dt>
            <dd>{{ formatDuration(trainingDuration(selectedModel)) }}</dd>
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
