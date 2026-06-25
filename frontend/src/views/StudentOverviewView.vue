<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import type { Sort } from "element-plus";
import { ArrowLeft, ArrowRight, EditPen, Refresh, VideoPause, VideoPlay } from "@element-plus/icons-vue";
import {
  deleteStudentOverview,
  fetchStudentFilterOptions,
  fetchStudentOverview
} from "../api/students";
import StudentEditDialog from "../components/student/StudentEditDialog.vue";
import StudentQueryBar from "../components/student/StudentQueryBar.vue";
import StudentTable from "../components/student/StudentTable.vue";
import { useAuthStore } from "../stores/auth";
import type {
  ClassInfo,
  StudentEditableItem,
  StudentFilterForm,
  StudentFilterOptions,
  StudentOverview,
  StudentOverviewItem,
  StudentQueryMode,
  StudentSearchForm
} from "../types";

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const filterLoading = ref(false);
const overview = ref<StudentOverview | null>(null);
const filterOptions = ref<StudentFilterOptions | null>(null);
const queryMode = ref<StudentQueryMode>("filter");
const page = ref(1);
const pageSize = 20;
const sortField = ref<string>();
const sortOrder = ref<string>();
const editingStudent = ref<StudentEditableItem | null>(null);
const editVisible = ref(false);
const carouselPlaying = ref(false);
let carouselTimer: ReturnType<typeof window.setInterval> | undefined;

const filters = reactive<StudentFilterForm>({
  class_name: "1-1"
});
const search = reactive<StudentSearchForm>({});

const canEdit = computed(() => authStore.role.toLowerCase() === "admin");
const students = computed(() => overview.value?.students ?? []);
const pagedStudents = computed(() => {
  const start = (page.value - 1) * pageSize;
  return students.value.slice(start, start + pageSize);
});
const currentClassName = computed(() => classNameForRaw(filters.class_name) || "高一 1 班");
const classOptions = computed(() => {
  const fromApi = filterOptions.value?.classes ?? [];
  return fromApi.length ? fromApi : buildFallbackClasses();
});
const currentClassIndex = computed(() =>
  Math.max(0, classOptions.value.findIndex((item) => item.raw_class_name === filters.class_name))
);

function buildFallbackClasses() {
  const classCountByGrade: Record<number, number> = { 1: 17, 2: 12, 3: 17 };
  return Object.entries(classCountByGrade).flatMap(([grade, count]) =>
    Array.from({ length: count }, (_, index) => {
      const gradeLevel = Number(grade);
      const classNo = index + 1;
      return {
        grade_level: gradeLevel,
        raw_class_name: `${gradeLevel}-${classNo}`,
        class_name: `${["", "高一", "高二", "高三"][gradeLevel]} ${classNo} 班`
      };
    })
  );
}

function classNameForRaw(rawClassName?: string) {
  if (!rawClassName) {
    return "";
  }
  return classOptions.value.find((item) => item.raw_class_name === rawClassName)?.class_name || rawClassName;
}

function cleanQuery(query: Record<string, unknown>) {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== undefined && value !== null && value !== "")
  );
}

async function loadFilterOptions() {
  filterLoading.value = true;
  try {
    filterOptions.value = await fetchStudentFilterOptions();
  } finally {
    filterLoading.value = false;
  }
}

async function loadOverview() {
  loading.value = true;
  try {
    const query =
      queryMode.value === "search"
        ? cleanQuery({
            keyword: search.keyword?.trim(),
            sort_field: sortField.value,
            sort_order: sortOrder.value
          })
        : cleanQuery({
            ...filters,
            sort_field: sortField.value,
            sort_order: sortOrder.value
          });
    overview.value = await fetchStudentOverview(query);
    page.value = 1;
  } finally {
    loading.value = false;
  }
}

function submitFilters(value: StudentFilterForm) {
  queryMode.value = "filter";
  Object.assign(filters, value);
  search.keyword = undefined;
  void loadOverview();
}

function submitSearch(value: StudentSearchForm) {
  queryMode.value = "search";
  search.keyword = value.keyword;
  void loadOverview();
}

function resetQuery() {
  queryMode.value = "filter";
  Object.assign(filters, {
    class_name: "1-1",
    min_gpa: undefined,
    max_gpa: undefined,
    grade_class: undefined,
    grade_level: undefined,
    gender: undefined
  });
  search.keyword = undefined;
  sortField.value = undefined;
  sortOrder.value = undefined;
  void loadOverview();
}

function handleSort(value: Sort) {
  const order = value.order === "ascending" ? "asc" : value.order === "descending" ? "desc" : undefined;
  sortField.value = order ? String(value.prop) : undefined;
  sortOrder.value = order;
  void loadOverview();
}

function goToClass(offset: number) {
  if (!classOptions.value.length) {
    return;
  }
  queryMode.value = "filter";
  const nextIndex = (currentClassIndex.value + offset + classOptions.value.length) % classOptions.value.length;
  Object.assign(filters, {
    class_name: classOptions.value[nextIndex].raw_class_name,
    min_gpa: undefined,
    max_gpa: undefined,
    grade_class: undefined,
    grade_level: undefined,
    gender: undefined
  });
  search.keyword = undefined;
  void loadOverview();
}

function toggleCarousel() {
  if (carouselPlaying.value) {
    stopCarousel();
    return;
  }
  carouselPlaying.value = true;
  carouselTimer = window.setInterval(() => goToClass(1), 5000);
}

function stopCarousel() {
  if (carouselTimer) {
    window.clearInterval(carouselTimer);
    carouselTimer = undefined;
  }
  carouselPlaying.value = false;
}

function openDetail(student: StudentOverviewItem) {
  void router.push({ name: "student-detail", params: { studentNo: student.student_no } });
}

function openEdit(student: StudentOverviewItem) {
  editingStudent.value = student;
  editVisible.value = true;
}

async function handleDelete(student: StudentOverviewItem) {
  await ElMessageBox.confirm(`确认删除 ${student.name}（${student.student_no}）的学生概览记录？`, "删除确认", {
    type: "warning",
    confirmButtonText: "删除",
    cancelButtonText: "取消",
    confirmButtonClass: "el-button--danger"
  });
  await deleteStudentOverview(student.student_no);
  ElMessage.success("学生记录已删除");
  await loadOverview();
}

async function handleSaved() {
  await loadOverview();
}

onMounted(async () => {
  await loadFilterOptions();
  await loadOverview();
});

onBeforeUnmount(() => {
  stopCarousel();
});
</script>

<template>
  <section class="page-head">
    <div>
      <p class="eyebrow">学生概览</p>
      <h1>{{ queryMode === "filter" ? currentClassName : "学生搜索结果" }}</h1>
      <p>当前前端只启用学生查询、详情、编辑与删除；默认进入高一 1 班。</p>
    </div>
    <div class="head-actions">
      <el-button :disabled="loading" @click="goToClass(-1)">
        <el-icon><ArrowLeft /></el-icon>
        上一班
      </el-button>
      <el-button :disabled="loading" @click="goToClass(1)">
        下一班
        <el-icon><ArrowRight /></el-icon>
      </el-button>
      <el-button :type="carouselPlaying ? 'warning' : 'primary'" :disabled="loading" @click="toggleCarousel">
        <el-icon><component :is="carouselPlaying ? VideoPause : VideoPlay" /></el-icon>
        {{ carouselPlaying ? "暂停轮播" : "班级轮播" }}
      </el-button>
    </div>
  </section>

  <section class="class-strip">
    <div>
      <strong>{{ currentClassIndex + 1 }} / {{ classOptions.length }}</strong>
      <span>轮播按班级列表顺序切换，每 5 秒自动加载下一班。</span>
    </div>
    <el-select
      v-model="filters.class_name"
      class="class-select"
      :loading="filterLoading"
      filterable
      placeholder="选择班级"
      @change="submitFilters(filters)"
    >
      <el-option
        v-for="item in classOptions"
        :key="item.raw_class_name"
        :label="item.class_name"
        :value="item.raw_class_name"
      />
    </el-select>
  </section>

  <StudentQueryBar
    v-model:mode="queryMode"
    :filters="filters"
    :search="search"
    :filter-options="filterOptions"
    :loading="loading"
    @submit-filter="submitFilters"
    @submit-search="submitSearch"
    @reset="resetQuery"
  />

  <section class="content-panel">
    <div class="panel-heading">
      <div>
        <h2>学生列表</h2>
        <p>共 {{ students.length }} 条结果，前端每页显示 {{ pageSize }} 条。</p>
      </div>
      <el-button :loading="loading" @click="loadOverview">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <StudentTable
      :students="pagedStudents"
      :loading="loading"
      :can-edit="canEdit"
      @view="openDetail"
      @edit="openEdit"
      @delete="handleDelete"
      @sort="handleSort"
    />

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="page"
        background
        layout="prev, pager, next"
        :page-size="pageSize"
        :total="students.length"
        :hide-on-single-page="students.length <= pageSize"
      />
    </div>
  </section>

  <StudentEditDialog
    v-model="editVisible"
    :student="editingStudent"
    :filter-options="filterOptions"
    :performance-available="true"
    @saved="handleSaved"
  />
</template>
