<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import type { FormInstance, FormRules } from "element-plus";
import { ElMessage } from "element-plus";
import { upsertStudentPerformance } from "../../api/students";
import type { StudentDetail, StudentPerformanceUpsertPayload } from "../../types";

const props = defineProps<{
  modelValue: boolean;
  detail: StudentDetail | null;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  saved: [detail: StudentDetail];
}>();

const formRef = ref<FormInstance>();
const saving = ref(false);

const form = reactive({
  study_time_weekly: 10,
  absences: 0,
  tutoring: false,
  parental_support: 2,
  extracurricular: false,
  sports: false,
  music: false,
  volunteering: false,
  gpa: 3
});

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value)
});

const title = computed(() => (props.detail?.performance_available ? "编辑学业表现" : "补充学业表现"));
const studentNo = computed(() => props.detail?.basic_info?.student_no);

const parentalSupportOptions = [
  { value: 0, label: "无支持" },
  { value: 1, label: "较低" },
  { value: 2, label: "一般" },
  { value: 3, label: "较高" },
  { value: 4, label: "很高" }
];

const rules: FormRules = {
  study_time_weekly: [
    { required: true, message: "请输入每周学习时长", trigger: "change" },
    { type: "number", min: 0, max: 60, message: "学习时长范围为 0-60", trigger: "change" }
  ],
  absences: [
    { required: true, message: "请输入缺勤次数", trigger: "change" },
    { type: "number", min: 0, max: 30, message: "缺勤次数范围为 0-30", trigger: "change" }
  ],
  parental_support: [{ required: true, message: "请选择父母支持程度", trigger: "change" }],
  gpa: [
    { required: true, message: "请输入 GPA", trigger: "change" },
    { type: "number", min: 0, max: 4, message: "GPA 范围为 0-4", trigger: "change" }
  ]
};

watch(
  () => [props.modelValue, props.detail] as const,
  ([isVisible]) => {
    if (!isVisible) {
      return;
    }
    const performance = props.detail?.academic_performance;
    const support = props.detail?.support_status;
    const profile = props.detail?.activity_profile;

    form.study_time_weekly = Number(performance?.study_time_weekly ?? 10);
    form.absences = Number(performance?.absences ?? 0);
    form.gpa = Number(performance?.gpa ?? 3);
    form.tutoring = Boolean(support?.tutoring ?? false);
    form.parental_support = Number(support?.parental_support?.value ?? 2);
    form.extracurricular = Boolean(profile?.extracurricular ?? false);
    form.sports = Boolean(profile?.sports ?? false);
    form.music = Boolean(profile?.music ?? false);
    form.volunteering = Boolean(profile?.volunteering ?? false);
    formRef.value?.clearValidate();
  },
  { immediate: true }
);

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid || !studentNo.value) {
    return;
  }

  const payload: StudentPerformanceUpsertPayload = {
    study_time_weekly: form.study_time_weekly,
    absences: form.absences,
    tutoring: form.tutoring,
    parental_support: form.parental_support,
    extracurricular: form.extracurricular,
    sports: form.sports,
    music: form.music,
    volunteering: form.volunteering,
    gpa: form.gpa
  };

  saving.value = true;
  try {
    const updated = await upsertStudentPerformance(studentNo.value, payload);
    ElMessage.success("学业表现已保存");
    emit("saved", updated);
    visible.value = false;
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <el-dialog v-model="visible" class="student-edit-dialog" :title="title" width="660px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="edit-form-grid">
        <el-form-item label="每周学习时长" prop="study_time_weekly">
          <el-input-number
            v-model="form.study_time_weekly"
            :min="0"
            :max="60"
            :precision="1"
            :step="0.5"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="缺勤次数" prop="absences">
          <el-input-number v-model="form.absences" :min="0" :max="30" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="GPA" prop="gpa">
          <el-input-number
            v-model="form.gpa"
            :min="0"
            :max="4"
            :precision="2"
            :step="0.1"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="父母支持程度" prop="parental_support">
          <el-select v-model="form.parental_support">
            <el-option
              v-for="item in parentalSupportOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </div>

      <div class="switch-grid">
        <label>
          <span>课外辅导</span>
          <el-switch v-model="form.tutoring" />
        </label>
        <label>
          <span>课外活动</span>
          <el-switch v-model="form.extracurricular" />
        </label>
        <label>
          <span>体育活动</span>
          <el-switch v-model="form.sports" />
        </label>
        <label>
          <span>音乐活动</span>
          <el-switch v-model="form.music" />
        </label>
        <label>
          <span>志愿活动</span>
          <el-switch v-model="form.volunteering" />
        </label>
      </div>

      <p class="form-note">成绩等级由后端根据 GPA 自动计算，前端不会提交 grade_class。</p>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存表现</el-button>
    </template>
  </el-dialog>
</template>
