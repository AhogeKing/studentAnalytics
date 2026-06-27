<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { CircleCheck, CircleClose, Refresh, Search, View } from "@element-plus/icons-vue";
import { fetchOperationLogDetail, fetchOperationLogOptions, fetchOperationLogs } from "../api/operationLogs";
import type { OperationLog, OperationLogOptions, OperationLogQuery, OptionVO } from "../types";

const loading = ref(false);
const detailLoading = ref(false);
const logs = ref<OperationLog[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);
const drawerVisible = ref(false);
const selectedLog = ref<OperationLog | null>(null);
const options = ref<OperationLogOptions>({
  modules: [],
  operation_types: [],
  results: [],
  roles: [],
  target_types: []
});

const filters = reactive({
  keyword: "",
  username: "",
  user_role: "",
  module_name: "",
  operation_type: "",
  operation_result: "",
  target_type: "",
  business_key: "",
  timeRange: [] as string[]
});

const fallbackModules = [
  { value: "auth", label: "认证" },
  { value: "user", label: "用户管理" },
  { value: "student", label: "学生管理" },
  { value: "performance", label: "学业表现" },
  { value: "import", label: "数据导入" },
  { value: "model", label: "模型管理" },
  { value: "prediction", label: "学业预测" },
  { value: "warning", label: "风险预警" }
];

const fallbackOperationTypes = [
  { value: "CREATE", label: "新增" },
  { value: "UPDATE", label: "修改" },
  { value: "DELETE", label: "删除" },
  { value: "UPSERT", label: "新增或更新" },
  { value: "UPDATE_STATUS", label: "修改状态" },
  { value: "DISABLE", label: "禁用" },
  { value: "RESET_PASSWORD", label: "重置密码" },
  { value: "LOGOUT", label: "退出登录" },
  { value: "TRAIN", label: "训练模型" },
  { value: "PREDICT", label: "学业预测" }
];

const fallbackResults = [
  { value: "SUCCESS", label: "成功" },
  { value: "FAIL", label: "失败" }
];

const fallbackRoles = [
  { value: "ADMIN", label: "管理员" },
  { value: "TEACHER", label: "教师" },
  { value: "STUDENT", label: "学生" }
];

const fallbackTargetTypes = [
  { value: "USER", label: "用户" },
  { value: "STUDENT", label: "学生" },
  { value: "PERFORMANCE", label: "学业表现" },
  { value: "IMPORT_BATCH", label: "导入批次" },
  { value: "MODEL_VERSION", label: "模型版本" },
  { value: "PREDICTION", label: "预测结果" },
  { value: "WARNING", label: "风险预警" }
];

const moduleOptions = computed(() => normalizeOptions(options.value.modules, fallbackModules));
const operationTypeOptions = computed(() =>
  normalizeOptions(options.value.operation_types || options.value.operationTypes, fallbackOperationTypes)
);
const resultOptions = computed(() => normalizeOptions(options.value.results, fallbackResults));
const roleOptions = computed(() => normalizeOptions(options.value.roles, fallbackRoles));
const targetTypeOptions = computed(() =>
  normalizeOptions(options.value.target_types || options.value.targetTypes, fallbackTargetTypes)
);

const pageSuccessCount = computed(() => logs.value.filter((item) => operationResult(item) === "SUCCESS").length);
const pageFailCount = computed(() => logs.value.filter((item) => operationResult(item) === "FAIL").length);
const pageOperatorCount = computed(() => {
  const names = new Set(logs.value.map((item) => username(item)).filter(Boolean));
  return names.size;
});

const detailRows = computed(() => {
  if (!selectedLog.value) {
    return [];
  }
  const item = selectedLog.value;
  return [
    ["日志ID", String(item.id)],
    ["操作时间", formatTime(createdAt(item))],
    ["操作者", operatorText(item)],
    ["角色", userRoleLabel(item)],
    ["模块", moduleLabel(item)],
    ["操作类型", operationTypeLabel(item)],
    ["操作结果", operationResultLabel(item)],
    ["目标对象", targetTypeLabel(item)],
    ["目标ID", targetId(item) || "-"],
    ["业务标识", businessKey(item) || "-"],
    ["请求方式", requestMethod(item) || "-"],
    ["请求路径", requestUri(item) || "-"],
    ["IP", ipAddress(item) || "-"]
  ];
});

const requestParamsText = computed(() => formatJson(selectedLog.value ? requestParams(selectedLog.value) : null));
const requestBodyText = computed(() => formatJson(selectedLog.value ? requestBody(selectedLog.value) : null));

function normalizeOptions(
  source: OptionVO<string>[] | undefined,
  fallback: Array<{ value: string; label: string }>
): OptionVO<string>[] {
  if (!source || source.length === 0) {
    return fallback;
  }
  return source.filter((item) => item.value !== null);
}

function coalesce<T>(...values: Array<T | null | undefined>) {
  return values.find((value) => value !== undefined && value !== null) ?? null;
}

function username(item: OperationLog) {
  return coalesce(item.username) || "-";
}

function realName(item: OperationLog) {
  return coalesce(item.realName, item.real_name);
}

function userRole(item: OperationLog) {
  return coalesce(item.userRole, item.user_role) || "";
}

function userRoleLabel(item: OperationLog) {
  return coalesce(item.userRoleLabel, item.user_role_label) || roleLabel(userRole(item));
}

function moduleName(item: OperationLog) {
  return coalesce(item.moduleName, item.module_name) || "";
}

function moduleLabel(item: OperationLog) {
  return coalesce(item.moduleLabel, item.module_label) || optionLabel(moduleOptions.value, moduleName(item));
}

function operationType(item: OperationLog) {
  return coalesce(item.operationType, item.operation_type) || "";
}

function operationTypeLabel(item: OperationLog) {
  return coalesce(item.operationTypeLabel, item.operation_type_label) || optionLabel(operationTypeOptions.value, operationType(item));
}

function operationResult(item: OperationLog) {
  return coalesce(item.operationResult, item.operation_result) || "";
}

function operationResultLabel(item: OperationLog) {
  return coalesce(item.operationResultLabel, item.operation_result_label) || optionLabel(resultOptions.value, operationResult(item));
}

function targetType(item: OperationLog) {
  return coalesce(item.targetType, item.target_type) || "";
}

function targetTypeLabel(item: OperationLog) {
  return coalesce(item.targetTypeLabel, item.target_type_label) || optionLabel(targetTypeOptions.value, targetType(item));
}

function targetId(item: OperationLog) {
  return coalesce(item.targetId, item.target_id);
}

function businessKey(item: OperationLog) {
  return coalesce(item.businessKey, item.business_key);
}

function operationSummary(item: OperationLog) {
  return coalesce(item.operationSummary, item.operation_summary) || `${moduleLabel(item)}：${operationTypeLabel(item)}`;
}

function requestMethod(item: OperationLog) {
  return coalesce(item.requestMethod, item.request_method);
}

function requestUri(item: OperationLog) {
  return coalesce(item.requestUri, item.request_uri);
}

function ipAddress(item: OperationLog) {
  return coalesce(item.ipAddress, item.ip_address);
}

function requestParams(item: OperationLog) {
  return coalesce(item.requestParams, item.request_params);
}

function requestBody(item: OperationLog) {
  return coalesce(item.requestBody, item.request_body);
}

function createdAt(item: OperationLog) {
  return coalesce(item.createdAt, item.created_at);
}

function optionLabel(source: OptionVO<string>[], value: string) {
  return source.find((item) => item.value === value)?.label || value || "-";
}

function roleLabel(role: string) {
  return roleOptions.value.find((item) => item.value === role)?.label || role || "-";
}

function operatorText(item: OperationLog) {
  const name = realName(item);
  const account = username(item);
  return name && account !== "-" ? `${name} / ${account}` : account;
}

function resultTagType(result: string) {
  if (result === "SUCCESS") {
    return "success";
  }
  if (result === "FAIL") {
    return "danger";
  }
  return "info";
}

function roleTagType(role: string) {
  if (role === "ADMIN") {
    return "danger";
  }
  if (role === "TEACHER") {
    return "primary";
  }
  return "info";
}

function methodTagType(method: string | null) {
  if (method === "POST") {
    return "success";
  }
  if (method === "PUT" || method === "PATCH") {
    return "warning";
  }
  if (method === "DELETE") {
    return "danger";
  }
  return "info";
}

function formatTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 19);
}

function formatJson(raw?: string | null) {
  if (!raw) {
    return "{}";
  }
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}

function buildQuery(): OperationLogQuery {
  const query: OperationLogQuery = {
    page_num: page.value,
    page_size: pageSize.value
  };
  assignIfPresent(query, "keyword", filters.keyword);
  assignIfPresent(query, "username", filters.username);
  assignIfPresent(query, "user_role", filters.user_role);
  assignIfPresent(query, "module_name", filters.module_name);
  assignIfPresent(query, "operation_type", filters.operation_type);
  assignIfPresent(query, "operation_result", filters.operation_result);
  assignIfPresent(query, "target_type", filters.target_type);
  assignIfPresent(query, "business_key", filters.business_key);
  if (filters.timeRange.length === 2) {
    query.start_time = filters.timeRange[0];
    query.end_time = filters.timeRange[1];
  }
  return query;
}

function assignIfPresent(query: OperationLogQuery, key: keyof OperationLogQuery, value: string) {
  const trimmed = value.trim();
  if (trimmed) {
    Object.assign(query, { [key]: trimmed });
  }
}

function operationLogRowClass({ row }: { row: OperationLog }) {
  return operationResult(row) === "FAIL" ? "operation-log-row--fail" : "";
}

async function loadOptions() {
  try {
    options.value = await fetchOperationLogOptions();
  } catch {
    options.value = {
      modules: fallbackModules,
      operation_types: fallbackOperationTypes,
      results: fallbackResults,
      roles: fallbackRoles,
      target_types: fallbackTargetTypes
    };
  }
}

async function loadLogs() {
  loading.value = true;
  try {
    const result = await fetchOperationLogs(buildQuery());
    logs.value = result.records;
    total.value = result.total;
  } catch {
    logs.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function submitFilters() {
  page.value = 1;
  void loadLogs();
}

function resetFilters() {
  filters.keyword = "";
  filters.username = "";
  filters.user_role = "";
  filters.module_name = "";
  filters.operation_type = "";
  filters.operation_result = "";
  filters.target_type = "";
  filters.business_key = "";
  filters.timeRange = [];
  page.value = 1;
  void loadLogs();
}

async function openDetail(item: OperationLog) {
  drawerVisible.value = true;
  selectedLog.value = item;
  detailLoading.value = true;
  try {
    selectedLog.value = await fetchOperationLogDetail(item.id);
  } finally {
    detailLoading.value = false;
  }
}

function copyJson(text: string) {
  void navigator.clipboard.writeText(text);
  ElMessage.success("JSON 已复制");
}

function handlePageSizeChange() {
  page.value = 1;
  void loadLogs();
}

onMounted(async () => {
  await loadOptions();
  await loadLogs();
});
</script>

<template>
  <section class="page-head operation-log-head">
    <div>
      <p class="eyebrow">操作日志</p>
      <h1>审计记录</h1>
      <p>查看管理员关键写操作、目标对象、请求来源和执行结果。</p>
    </div>
    <div class="head-actions">
      <el-button :loading="loading" @click="loadLogs">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>
  </section>

  <section class="operation-summary" aria-label="当前页日志统计">
    <article>
      <span>日志总数</span>
      <strong>{{ total }}</strong>
      <small>匹配当前筛选条件</small>
    </article>
    <article>
      <span>当前页成功</span>
      <strong>{{ pageSuccessCount }}</strong>
      <small>执行结果为成功</small>
    </article>
    <article>
      <span>当前页失败</span>
      <strong>{{ pageFailCount }}</strong>
      <small>执行结果为失败</small>
    </article>
    <article>
      <span>当前页操作者</span>
      <strong>{{ pageOperatorCount }}</strong>
      <small>按用户名去重</small>
    </article>
  </section>

  <section class="query-panel operation-log-query">
    <div class="query-grid operation-log-query-grid">
      <el-input v-model.trim="filters.keyword" clearable placeholder="关键词" @keyup.enter="submitFilters">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-input v-model.trim="filters.username" clearable placeholder="用户名" @keyup.enter="submitFilters" />
      <el-select v-model="filters.user_role" clearable placeholder="角色">
        <el-option v-for="item in roleOptions" :key="item.value || item.label" :label="item.label" :value="item.value || ''" />
      </el-select>
      <el-select v-model="filters.module_name" clearable placeholder="模块">
        <el-option v-for="item in moduleOptions" :key="item.value || item.label" :label="item.label" :value="item.value || ''" />
      </el-select>
      <el-select v-model="filters.operation_type" clearable placeholder="操作类型">
        <el-option
          v-for="item in operationTypeOptions"
          :key="item.value || item.label"
          :label="item.label"
          :value="item.value || ''"
        />
      </el-select>
      <el-select v-model="filters.operation_result" clearable placeholder="结果">
        <el-option v-for="item in resultOptions" :key="item.value || item.label" :label="item.label" :value="item.value || ''" />
      </el-select>
      <el-select v-model="filters.target_type" clearable placeholder="目标类型">
        <el-option
          v-for="item in targetTypeOptions"
          :key="item.value || item.label"
          :label="item.label"
          :value="item.value || ''"
        />
      </el-select>
      <el-input v-model.trim="filters.business_key" clearable placeholder="业务标识" @keyup.enter="submitFilters" />
      <el-date-picker
        v-model="filters.timeRange"
        class="operation-log-date-range"
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

  <section class="content-panel operation-log-panel">
    <div class="panel-heading">
      <div>
        <h2>日志列表</h2>
        <p>共 {{ total }} 条记录，每页 {{ pageSize }} 条。</p>
      </div>
    </div>

    <div class="table-scroll">
      <el-table
        v-loading="loading"
        :data="logs"
        class="student-table operation-log-table"
        row-key="id"
        :row-class-name="operationLogRowClass"
      >
        <el-table-column label="时间" min-width="180" fixed="left">
          <template #default="{ row }">{{ formatTime(createdAt(row)) }}</template>
        </el-table-column>
        <el-table-column label="操作者" min-width="180">
          <template #default="{ row }">
            <div class="operator-cell">
              <strong>{{ username(row) }}</strong>
              <span>{{ realName(row) || "-" }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="roleTagType(userRole(row))" effect="light">{{ userRoleLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模块" min-width="120">
          <template #default="{ row }">{{ moduleLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="138">
          <template #default="{ row }">
            <el-tag effect="light">{{ operationTypeLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="106">
          <template #default="{ row }">
            <el-tag :type="resultTagType(operationResult(row))" effect="light">
              <el-icon>
                <CircleCheck v-if="operationResult(row) === 'SUCCESS'" />
                <CircleClose v-else />
              </el-icon>
              {{ operationResultLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="摘要" min-width="230">
          <template #default="{ row }">
            <span class="summary-text">{{ operationSummary(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="业务标识" min-width="130">
          <template #default="{ row }">{{ businessKey(row) || "-" }}</template>
        </el-table-column>
        <el-table-column label="请求" min-width="230">
          <template #default="{ row }">
            <div class="request-cell">
              <el-tag :type="methodTagType(requestMethod(row))" effect="plain">{{ requestMethod(row) || "-" }}</el-tag>
              <span>{{ requestUri(row) || "-" }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="IP" min-width="132">
          <template #default="{ row }">{{ ipAddress(row) || "-" }}</template>
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
        @current-change="() => loadLogs()"
        @size-change="handlePageSizeChange"
      />
    </div>
  </section>

  <el-drawer v-model="drawerVisible" class="operation-log-drawer" title="日志详情" size="min(720px, 100vw)">
    <div v-loading="detailLoading" class="operation-log-detail">
      <dl class="operation-detail-grid">
        <div v-for="[label, value] in detailRows" :key="label">
          <dt>{{ label }}</dt>
          <dd>{{ value }}</dd>
        </div>
      </dl>

      <section class="json-section">
        <div class="json-section-head">
          <h3>请求参数</h3>
          <el-button size="small" @click="copyJson(requestParamsText)">复制</el-button>
        </div>
        <pre>{{ requestParamsText }}</pre>
      </section>

      <section class="json-section">
        <div class="json-section-head">
          <h3>请求体</h3>
          <el-button size="small" @click="copyJson(requestBodyText)">复制</el-button>
        </div>
        <pre>{{ requestBodyText }}</pre>
      </section>
    </div>
  </el-drawer>
</template>
