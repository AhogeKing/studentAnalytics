<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { ArrowLeft, Delete, EditPen, Refresh } from "@element-plus/icons-vue";
import {
  deleteStudentOverview,
  fetchStudentDetail,
  fetchStudentFilterOptions
} from "../api/students";
import AcademicPerformanceCard from "../components/student/AcademicPerformanceCard.vue";
import ActivityProfileCard from "../components/student/ActivityProfileCard.vue";
import BasicInfoCard from "../components/student/BasicInfoCard.vue";
import StudentEditDialog from "../components/student/StudentEditDialog.vue";
import StudentPerformanceDialog from "../components/student/StudentPerformanceDialog.vue";
import SupportStatusCard from "../components/student/SupportStatusCard.vue";
import { useAuthStore } from "../stores/auth";
import type { StudentDetail, StudentEditableItem, StudentFilterOptions } from "../types";

const props = defineProps<{
  studentNo: number;
}>();

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const detail = ref<StudentDetail | null>(null);
const filterOptions = ref<StudentFilterOptions | null>(null);
const editVisible = ref(false);
const performanceVisible = ref(false);

const canEdit = computed(() => authStore.role.toLowerCase() === "admin");
const editableStudent = computed<StudentEditableItem | null>(() => {
  if (!detail.value?.basic_info) {
    return null;
  }
  return {
    student_no: detail.value.basic_info.student_no,
    name: detail.value.basic_info.name,
    age: detail.value.basic_info.age,
    gender: detail.value.basic_info.gender,
    class_info: detail.value.basic_info.class_info,
    gpa: detail.value.academic_performance?.gpa ?? null
  };
});

async function loadDetail() {
  loading.value = true;
  try {
    detail.value = await fetchStudentDetail(props.studentNo);
  } catch {
    detail.value = null;
  } finally {
    loading.value = false;
  }
}

async function loadOptions() {
  try {
    filterOptions.value = await fetchStudentFilterOptions();
  } catch {
    filterOptions.value = null;
  }
}

async function handleDelete() {
  if (!detail.value?.basic_info) {
    return;
  }
  const student = detail.value.basic_info;
  await ElMessageBox.confirm(`确认删除 ${student.name}（${student.student_no}）的学生概览记录？`, "删除确认", {
    type: "warning",
    confirmButtonText: "删除",
    cancelButtonText: "取消",
    confirmButtonClass: "el-button--danger"
  });
  await deleteStudentOverview(student.student_no);
  ElMessage.success("学生记录已删除");
  await router.replace({ name: "students" });
}

function handlePerformanceSaved(updated: StudentDetail) {
  detail.value = updated;
}

onMounted(async () => {
  await Promise.all([loadDetail(), loadOptions()]);
});

watch(
  () => props.studentNo,
  () => {
    void loadDetail();
  }
);
</script>

<template>
  <section class="detail-head">
    <div>
      <el-button text :icon="ArrowLeft" @click="router.push({ name: 'students' })">返回学生概览</el-button>
      <h1>{{ detail?.basic_info?.name || "学生详情" }}</h1>
      <p v-if="detail?.basic_info">
        {{ detail.basic_info.student_no }} · {{ detail.basic_info.class_info?.class_name || "-" }}
      </p>
    </div>
    <div class="head-actions">
      <el-button :loading="loading" @click="loadDetail">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
      <el-button v-if="canEdit" type="primary" :disabled="!detail" @click="editVisible = true">
        <el-icon><EditPen /></el-icon>
        编辑
      </el-button>
      <el-button v-if="canEdit" type="primary" plain :disabled="!detail" @click="performanceVisible = true">
        <el-icon><EditPen /></el-icon>
        {{ detail?.performance_available ? "编辑表现" : "补充表现" }}
      </el-button>
      <el-button v-if="canEdit" type="danger" :disabled="!detail" @click="handleDelete">
        <el-icon><Delete /></el-icon>
        删除
      </el-button>
    </div>
  </section>

  <el-alert
    v-if="detail && !detail.performance_available"
    class="inline-alert"
    type="warning"
    title="该学生暂未有关联学业表现记录，管理员可通过补充表现录入。"
    show-icon
    :closable="false"
  />

  <div v-loading="loading" class="detail-layout">
    <BasicInfoCard v-if="detail?.basic_info" :info="detail.basic_info" />
    <AcademicPerformanceCard
      :performance="detail?.academic_performance"
      :available="Boolean(detail?.performance_available)"
    />
    <SupportStatusCard :status="detail?.support_status" :available="Boolean(detail?.performance_available)" />
    <ActivityProfileCard :profile="detail?.activity_profile" :available="Boolean(detail?.performance_available)" />
  </div>

  <StudentEditDialog
    v-model="editVisible"
    :student="editableStudent"
    :filter-options="filterOptions"
    :performance-available="Boolean(detail?.performance_available)"
    @saved="loadDetail"
  />

  <StudentPerformanceDialog
    v-model="performanceVisible"
    :detail="detail"
    @saved="handlePerformanceSaved"
  />
</template>
