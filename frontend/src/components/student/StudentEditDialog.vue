<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import type { FormInstance, FormRules } from "element-plus";
import { ElMessage } from "element-plus";
import { updateStudentOverview } from "../../api/students";
import type {
  StudentEditableItem,
  StudentFilterOptions,
  StudentOverviewItem,
  StudentOverviewUpdatePayload
} from "../../types";

const props = defineProps<{
  modelValue: boolean;
  student: StudentEditableItem | null;
  filterOptions: StudentFilterOptions | null;
  performanceAvailable?: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  saved: [student: StudentOverviewItem];
}>();

const formRef = ref<FormInstance>();
const saving = ref(false);

const form = reactive({
  student_no: 0,
  name: "",
  age: 0,
  gender: undefined as number | undefined,
  class_name: "",
  gpa: undefined as number | undefined
});

const original = reactive({ ...form });

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value)
});

const rules: FormRules = {
  name: [
    { required: true, message: "请输入姓名", trigger: "blur" },
    { max: 50, message: "姓名长度不能超过 50 个字符", trigger: "blur" }
  ],
  age: [
    { required: true, message: "请输入年龄", trigger: "blur" },
    { type: "number", min: 0, max: 30, message: "年龄范围为 0-30", trigger: "change" }
  ],
  gender: [{ required: true, message: "请选择性别", trigger: "change" }],
  class_name: [{ required: true, message: "请选择班级", trigger: "change" }],
  gpa: [{ type: "number", min: 0, max: 4, message: "GPA 范围为 0-4", trigger: "change" }]
};

watch(
  () => props.student,
  (student) => {
    if (!student) {
      return;
    }
    form.student_no = student.student_no;
    form.name = student.name;
    form.age = student.age;
    form.gender = student.gender?.value ?? undefined;
    form.class_name = student.class_info?.raw_class_name || "";
    form.gpa = student.gpa == null ? undefined : Number(student.gpa);
    Object.assign(original, form);
  },
  { immediate: true }
);

function buildPayload() {
  const payload: StudentOverviewUpdatePayload = {};
  const nextName = form.name.trim();
  if (nextName !== original.name) {
    payload.name = nextName;
  }
  if (form.age !== original.age) {
    payload.age = form.age;
  }
  if (form.gender !== original.gender) {
    payload.gender = form.gender;
  }
  if (form.class_name !== original.class_name) {
    payload.class_name = form.class_name;
  }
  if (props.performanceAvailable !== false && form.gpa !== original.gpa) {
    payload.gpa = form.gpa;
  }
  return payload;
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid || !props.student) {
    return;
  }

  const payload = buildPayload();
  if (!Object.keys(payload).length) {
    ElMessage.info("没有需要保存的修改");
    visible.value = false;
    return;
  }

  saving.value = true;
  try {
    const updated = await updateStudentOverview(props.student.student_no, payload);
    ElMessage.success("学生信息已更新");
    emit("saved", updated);
    visible.value = false;
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <el-dialog v-model="visible" class="student-edit-dialog" title="编辑学生概览" width="560px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="edit-form-grid">
        <el-form-item label="学号">
          <el-input v-model="form.student_no" disabled />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model.trim="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="0" :max="30" controls-position="right" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" placeholder="请选择">
            <el-option v-for="item in filterOptions?.genders ?? []" :key="String(item.value)" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="class_name">
          <el-select v-model="form.class_name" filterable placeholder="请选择班级">
            <el-option
              v-for="item in filterOptions?.classes ?? []"
              :key="item.raw_class_name"
              :label="item.class_name"
              :value="item.raw_class_name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="GPA" prop="gpa">
          <el-input-number
            v-model="form.gpa"
            :min="0"
            :max="4"
            :precision="2"
            :step="0.1"
            controls-position="right"
            :disabled="performanceAvailable === false"
          />
        </el-form-item>
      </div>
      <p v-if="performanceAvailable === false" class="form-note">该学生暂未有关联成绩记录，当前不能编辑 GPA。</p>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存修改</el-button>
    </template>
  </el-dialog>
</template>
