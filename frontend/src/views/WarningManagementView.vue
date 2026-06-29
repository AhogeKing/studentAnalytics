<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete as DeleteIcon, QuestionFilled, Refresh, Search, View } from "@element-plus/icons-vue";
import { deleteWarning, fetchWarningDetail, fetchWarnings, updateWarningStatus } from "../api/warning";
import { useAuthStore } from "../stores/auth";
import type { ClassInfo, WarningDetail, WarningQuery, WarningRecord, WarningStatus } from "../types";

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const detailLoading = ref(false);
const statusSaving = ref(false);
const deletingId = ref<number | null>(null);
const warnings = ref<WarningRecord[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);
const drawerVisible = ref(false);
const selectedWarning = ref<WarningDetail | null>(null);
const selectedStatus = ref<WarningStatus | "">("");

const filters = reactive({
  student_no: undefined as number | undefined,
  student_name: "",
  grade_level: undefined as number | undefined,
  class_name: "",
  risk_level: "",
  status: "",
  timeRange: [] as string[]
});

const riskOptions = [
  { value: "LOW", label: "低风险" },
  { value: "MEDIUM", label: "中风险" },
  { value: "HIGH", label: "高风险" }
];

const statusOptions: Array<{ value: WarningStatus; label: string }> = [
  { value: "UNPROCESSED", label: "未处理" },
  { value: "PROCESSING", label: "处理中" },
  { value: "DONE", label: "已完成" },
  { value: "IGNORED", label: "已忽略" }
];

const riskScoreFormula = [
  "来源：后端根据预测结果和当前学业表现生成 warning_record.risk_score。",
  "公式：min(各风险项加分之和, 100)。",
  "加分项：预测等级为较差/风险 +50；GPA < 2.5 +35；缺勤 >20 +25，缺勤 >10 +15；每周学习时长 <5 小时 +20，<8 小时 +10；家长支持 <=1 +10；未参加课外辅导 +5。"
];

const riskLevelFormula = [
  "来源：后端根据 risk_score 映射 warning_record.risk_level。",
  "公式：risk_score >= 50 为 HIGH 高风险；25 <= risk_score < 50 为 MEDIUM 中风险；risk_score < 25 为 LOW 低风险。"
];

const highRiskCount = computed(() => warnings.value.filter((item) => riskLevel(item) === "HIGH").length);
const processingCount = computed(() => warnings.value.filter((item) => status(item) === "PROCESSING").length);
const pendingCount = computed(() => warnings.value.filter((item) => status(item) === "UNPROCESSED").length);
const canDelete = computed(() => authStore.role.toLowerCase() === "admin");

function coalesce<T>(...values: Array<T | null | undefined>) {
  return values.find((value) => value !== undefined && value !== null) ?? null;
}

function studentNo(item: WarningRecord | WarningDetail | null) {
  return coalesce(item?.studentNo, item?.student_no);
}

function studentName(item: WarningRecord | WarningDetail | null) {
  return coalesce(item?.studentName, item?.student_name) || "-";
}

function classInfo(item: WarningRecord | WarningDetail | null): ClassInfo | null {
  return coalesce(item?.classInfo, item?.class_info);
}

function predictionResultId(item: WarningRecord | WarningDetail | null) {
  return coalesce(item?.predictionResultId, item?.prediction_result_id);
}

function riskScore(item: WarningRecord | WarningDetail | null) {
  return coalesce(item?.riskScore, item?.risk_score);
}

function riskLevel(item: WarningRecord | WarningDetail | null) {
  return coalesce(item?.riskLevel, item?.risk_level) || "";
}

function riskLevelLabel(item: WarningRecord | WarningDetail | null) {
  return coalesce(item?.riskLevelLabel, item?.risk_level_label) || optionLabel(riskOptions, riskLevel(item));
}

function status(item: WarningRecord | WarningDetail | null) {
  return coalesce(item?.status, item?.status) || "";
}

function statusLabel(item: WarningRecord | WarningDetail | null) {
  return coalesce(item?.statusLabel, item?.status_label) || optionLabel(statusOptions, status(item));
}

function riskReasons(item: WarningRecord | WarningDetail | null) {
  return coalesce(item?.riskReasons, item?.risk_reasons) || [];
}

function suggestions(item: WarningRecord | WarningDetail | null) {
  return item?.suggestions || [];
}

function modelVersionNo(item: WarningDetail | null) {
  return coalesce(item?.modelVersionNo, item?.model_version_no) || "-";
}

function predictedGradeLabel(item: WarningDetail | null) {
  return coalesce(item?.predictedGradeLabel, item?.predicted_grade_label) || "-";
}

function predictedGradeClass(item: WarningDetail | null) {
  return coalesce(item?.predictedGradeClass, item?.predicted_grade_class);
}

function handlerText(item: WarningDetail | null) {
  const realName = coalesce(item?.handlerRealName, item?.handler_real_name);
  const username = coalesce(item?.handlerUsername, item?.handler_username);
  if (realName && username) {
    return `${realName} / ${username}`;
  }
  return realName || username || "-";
}

function createdAt(item: WarningRecord | WarningDetail | null) {
  return coalesce(item?.createdAt, item?.created_at);
}

function updatedAt(item: WarningRecord | WarningDetail | null) {
  return coalesce(item?.updatedAt, item?.updated_at);
}

function optionLabel(options: Array<{ value: string; label: string }>, value: string) {
  return options.find((item) => item.value === value)?.label || value || "-";
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

function statusTagType(value: string) {
  if (value === "DONE") {
    return "success";
  }
  if (value === "PROCESSING") {
    return "warning";
  }
  if (value === "IGNORED") {
    return "info";
  }
  return "danger";
}

function formatTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 19);
}

function buildQuery(): WarningQuery {
  const query: WarningQuery = {
    page_num: page.value,
    page_size: pageSize.value
  };
  if (filters.student_no != null) {
    query.student_no = filters.student_no;
  }
  assignIfPresent(query, "student_name", filters.student_name);
  if (filters.grade_level != null) {
    query.grade_level = filters.grade_level;
  }
  assignIfPresent(query, "class_name", filters.class_name);
  assignIfPresent(query, "risk_level", filters.risk_level);
  assignIfPresent(query, "status", filters.status);
  if (filters.timeRange.length === 2) {
    query.start_time = filters.timeRange[0];
    query.end_time = filters.timeRange[1];
  }
  return query;
}

function assignIfPresent(query: WarningQuery, key: keyof WarningQuery, value: string) {
  const trimmed = value.trim();
  if (trimmed) {
    Object.assign(query, { [key]: trimmed });
  }
}

async function loadWarnings() {
  loading.value = true;
  try {
    const result = await fetchWarnings(buildQuery());
    warnings.value = result.records;
    total.value = result.total;
  } catch {
    warnings.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function submitFilters() {
  page.value = 1;
  void loadWarnings();
}

function resetFilters() {
  filters.student_no = undefined;
  filters.student_name = "";
  filters.grade_level = undefined;
  filters.class_name = "";
  filters.risk_level = "";
  filters.status = "";
  filters.timeRange = [];
  page.value = 1;
  void loadWarnings();
}

async function openDetail(item: WarningRecord) {
  drawerVisible.value = true;
  selectedWarning.value = null;
  selectedStatus.value = "";
  detailLoading.value = true;
  try {
    selectedWarning.value = await fetchWarningDetail(item.id);
    selectedStatus.value = status(selectedWarning.value) as WarningStatus;
  } finally {
    detailLoading.value = false;
  }
}

async function saveStatus() {
  if (!selectedWarning.value || !selectedStatus.value) {
    return;
  }
  statusSaving.value = true;
  try {
    selectedWarning.value = await updateWarningStatus(selectedWarning.value.id, selectedStatus.value);
    selectedStatus.value = status(selectedWarning.value) as WarningStatus;
    ElMessage.success("状态更新成功");
    await loadWarnings();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "状态更新失败");
  } finally {
    statusSaving.value = false;
  }
}

async function handleDeleteWarning(item: WarningRecord) {
  if (!item.id) {
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认删除 ${studentName(item)} / ${studentNo(item) || "-"} 的这条风险预警吗？删除不会影响学生信息、预测结果或模型版本。`,
      "删除风险预警",
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

  deletingId.value = item.id;
  try {
    await deleteWarning(item.id);
    ElMessage.success("风险预警已删除");
    if (warnings.value.length === 1 && page.value > 1) {
      page.value -= 1;
    }
    await loadWarnings();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "删除风险预警失败");
  } finally {
    deletingId.value = null;
  }
}

function goStudentDetail(item: WarningRecord | WarningDetail | null) {
  const no = studentNo(item);
  if (!no) {
    return;
  }
  void router.push({ name: "student-detail", params: { studentNo: no } });
}

function handlePageSizeChange() {
  page.value = 1;
  void loadWarnings();
}

onMounted(() => {
  void loadWarnings();
});
</script>

<template>
  <section class="page-head">
    <div>
      <p class="eyebrow">风险预警</p>
      <h1>学生学业风险处理</h1>
      <p>查看预测生成的风险预警，按学生、班级、等级和状态筛选，并跟进处理状态。</p>
    </div>
    <div class="head-actions">
      <el-button :loading="loading" @click="loadWarnings">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>
  </section>

  <section class="operation-summary" aria-label="当前页预警统计">
    <article>
      <span>预警总数</span>
      <strong>{{ total }}</strong>
      <small>匹配当前筛选条件</small>
    </article>
    <article>
      <span>当前页高风险</span>
      <strong>{{ highRiskCount }}</strong>
      <small>风险等级为 HIGH</small>
    </article>
    <article>
      <span>当前页未处理</span>
      <strong>{{ pendingCount }}</strong>
      <small>需要教师尽快跟进</small>
    </article>
    <article>
      <span>当前页处理中</span>
      <strong>{{ processingCount }}</strong>
      <small>已进入跟进流程</small>
    </article>
  </section>

  <section class="query-panel">
    <div class="query-grid warning-query-grid">
      <el-input-number v-model="filters.student_no" :min="0" controls-position="right" placeholder="学生编号" />
      <el-input v-model.trim="filters.student_name" clearable placeholder="学生姓名" @keyup.enter="submitFilters">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-input-number v-model="filters.grade_level" :min="1" :max="12" controls-position="right" placeholder="年级" />
      <el-input v-model.trim="filters.class_name" clearable placeholder="班级，例如 高一1班" @keyup.enter="submitFilters" />
      <el-select v-model="filters.risk_level" clearable placeholder="风险等级">
        <el-option v-for="item in riskOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="处理状态">
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-date-picker
        v-model="filters.timeRange"
        class="warning-date-range"
        type="datetimerange"
        value-format="YYYY-MM-DD HH:mm:ss"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
      />
    </div>
    <div class="query-actions">
      <el-button @click="resetFilters">重置</el-button>
      <el-button type="primary" @click="submitFilters">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
    </div>
  </section>

  <section class="content-panel">
    <div class="panel-heading">
      <div>
        <h2>预警列表</h2>
        <p>共 {{ total }} 条记录，每页 {{ pageSize }} 条。</p>
      </div>
    </div>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="warnings" class="student-table" row-key="id">
        <el-table-column label="预警时间" min-width="180" fixed="left">
          <template #default="{ row }">{{ formatTime(createdAt(row)) }}</template>
        </el-table-column>
        <el-table-column label="学生" min-width="180">
          <template #default="{ row }">
            <button class="link-button" type="button" @click="goStudentDetail(row)">
              {{ studentName(row) }} / {{ studentNo(row) || "-" }}
            </button>
          </template>
        </el-table-column>
        <el-table-column label="班级" min-width="128">
          <template #default="{ row }">{{ classInfo(row)?.class_name || "-" }}</template>
        </el-table-column>
        <el-table-column width="126">
          <template #header>
            <span class="metric-column-head">
              风险等级
              <el-tooltip placement="top" popper-class="warning-formula-tooltip">
                <template #content>
                  <div class="warning-tooltip-content">
                    <p v-for="item in riskLevelFormula" :key="item">{{ item }}</p>
                  </div>
                </template>
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <template #default="{ row }">
            <el-tag :type="riskTagType(riskLevel(row))" effect="light">{{ riskLevelLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column width="116">
          <template #header>
            <span class="metric-column-head">
              风险分
              <el-tooltip placement="top" popper-class="warning-formula-tooltip">
                <template #content>
                  <div class="warning-tooltip-content">
                    <p v-for="item in riskScoreFormula" :key="item">{{ item }}</p>
                  </div>
                </template>
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </span>
          </template>
          <template #default="{ row }">{{ riskScore(row) ?? "-" }}</template>
        </el-table-column>
        <el-table-column label="处理状态" width="116">
          <template #default="{ row }">
            <el-tag :type="statusTagType(status(row))" effect="plain">{{ statusLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预测结果ID" width="120">
          <template #default="{ row }">{{ predictionResultId(row) || "-" }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">{{ formatTime(updatedAt(row)) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="208" fixed="right" align="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" :icon="View" circle title="查看详情" @click="openDetail(row)" />
              <el-button size="small" type="primary" plain @click="goStudentDetail(row)">学生</el-button>
              <el-button
                v-if="canDelete"
                size="small"
                type="danger"
                plain
                :icon="DeleteIcon"
                :loading="deletingId === row.id"
                @click="handleDeleteWarning(row)"
              >
                删除
              </el-button>
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
        @current-change="() => loadWarnings()"
        @size-change="handlePageSizeChange"
      />
    </div>
  </section>

  <el-drawer v-model="drawerVisible" class="operation-log-drawer" title="预警详情" size="min(720px, 100vw)">
    <div v-loading="detailLoading" class="warning-detail">
      <template v-if="selectedWarning">
        <dl class="operation-detail-grid">
          <div>
            <dt>预警ID</dt>
            <dd>{{ selectedWarning.id }}</dd>
          </div>
          <div>
            <dt>学生</dt>
            <dd>
              <button class="link-button" type="button" @click="goStudentDetail(selectedWarning)">
                {{ studentName(selectedWarning) }} / {{ studentNo(selectedWarning) || "-" }}
              </button>
            </dd>
          </div>
          <div>
            <dt>班级</dt>
            <dd>{{ classInfo(selectedWarning)?.class_name || "-" }}</dd>
          </div>
          <div>
            <dt>预测等级</dt>
            <dd>{{ predictedGradeClass(selectedWarning)?.label || "-" }} / {{ predictedGradeLabel(selectedWarning) }}</dd>
          </div>
          <div>
            <dt>使用模型</dt>
            <dd>{{ modelVersionNo(selectedWarning) }}</dd>
          </div>
          <div>
            <dt>预测结果ID</dt>
            <dd>{{ predictionResultId(selectedWarning) || "-" }}</dd>
          </div>
          <div>
            <dt>风险等级</dt>
            <dd>{{ riskLevelLabel(selectedWarning) }}</dd>
          </div>
          <div>
            <dt>风险分</dt>
            <dd>{{ riskScore(selectedWarning) ?? "-" }}</dd>
          </div>
          <div>
            <dt>处理人</dt>
            <dd>{{ handlerText(selectedWarning) }}</dd>
          </div>
          <div>
            <dt>更新时间</dt>
            <dd>{{ formatTime(updatedAt(selectedWarning)) }}</dd>
          </div>
        </dl>

        <section class="warning-status-panel">
          <div>
            <h3>处理状态</h3>
            <p>后端允许四种状态直接切换，前端第一版不强制流转。</p>
          </div>
          <div class="warning-status-actions">
            <el-select v-model="selectedStatus" placeholder="处理状态">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button type="primary" :loading="statusSaving" @click="saveStatus">保存状态</el-button>
          </div>
        </section>

        <section class="warning-detail-section">
          <h3>风险原因</h3>
          <ul v-if="riskReasons(selectedWarning).length" class="warning-list compact">
            <li v-for="item in riskReasons(selectedWarning)" :key="item">{{ item }}</li>
          </ul>
          <el-empty v-else :image-size="54" description="暂无风险原因" />
        </section>

        <section class="warning-detail-section">
          <h3>处理建议</h3>
          <ul v-if="suggestions(selectedWarning).length" class="warning-list compact">
            <li v-for="item in suggestions(selectedWarning)" :key="item">{{ item }}</li>
          </ul>
          <el-empty v-else :image-size="54" description="暂无处理建议" />
        </section>
      </template>
    </div>
  </el-drawer>
</template>
