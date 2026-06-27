<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import type { FormInstance, FormRules } from "element-plus";
import { ElMessage } from "element-plus";
import { createStudent } from "../../api/students";
import type { StudentCreatePayload, StudentDetail, StudentFilterOptions } from "../../types";

const props = defineProps<{
  modelValue: boolean;
  filterOptions: StudentFilterOptions | null;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: boolean];
  saved: [detail: StudentDetail];
}>();

const formRef = ref<FormInstance>();
const saving = ref(false);

const form = reactive({
  student_no: undefined as number | undefined,
  name: "",
  age: 16,
  gender: undefined as number | undefined,
  ethnicity: 0,
  parental_education: 2,
  class_name: "1-1"
});

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit("update:modelValue", value)
});

const ethnicityOptions = [
  { value: 0, label: "高加索 / 白人" },
  { value: 1, label: "非裔" },
  { value: 2, label: "亚裔" },
  { value: 3, label: "其它" }
];

const parentalEducationOptions = [
  { value: 0, label: "无受教育经历" },
  { value: 1, label: "高中" },
  { value: 2, label: "大专" },
  { value: 3, label: "本科" },
  { value: 4, label: "更高学历" }
];

const rules: FormRules = {
  student_no: [{ required: true, message: "请输入学号", trigger: "change" }],
  name: [{ max: 50, message: "姓名长度不能超过 50 个字符", trigger: "blur" }],
  age: [
    { required: true, message: "请输入年龄", trigger: "change" },
    { type: "number", min: 0, max: 30, message: "年龄范围为 0-30", trigger: "change" }
  ],
  gender: [{ required: true, message: "请选择性别", trigger: "change" }],
  ethnicity: [{ required: true, message: "请选择民族类别", trigger: "change" }],
  parental_education: [{ required: true, message: "请选择父母教育程度", trigger: "change" }],
  class_name: [{ required: true, message: "请选择班级", trigger: "change" }]
};

watch(
  () => props.modelValue,
  (isVisible) => {
    if (!isVisible) {
      return;
    }
    form.student_no = undefined;
    form.name = "";
    form.age = 16;
    form.gender = undefined;
    form.ethnicity = 0;
    form.parental_education = 2;
    form.class_name = props.filterOptions?.classes?.[0]?.raw_class_name || "1-1";
    formRef.value?.clearValidate();
  }
);

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid || form.student_no == null || form.gender == null) {
    return;
  }

  const payload: StudentCreatePayload = {
    student_no: form.student_no,
    name: form.name.trim() || undefined,
    age: form.age,
    gender: form.gender,
    ethnicity: form.ethnicity,
    parental_education: form.parental_education,
    class_name: form.class_name
  };

  saving.value = true;
  try {
    const detail = await createStudent(payload);
    ElMessage.success("学生已新增");
    emit("saved", detail);
    visible.value = false;
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <el-dialog v-model="visible" class="student-edit-dialog" title="新增学生" width="640px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="edit-form-grid">
        <el-form-item label="学号" prop="student_no">
          <el-input-number v-model="form.student_no" :min="1" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model.trim="form.name" maxlength="50" placeholder="可为空，后端会自动生成" />
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="0" :max="30" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" placeholder="请选择">
            <el-option v-for="item in filterOptions?.genders ?? []" :key="String(item.value)" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="民族类别" prop="ethnicity">
          <el-select v-model="form.ethnicity">
            <el-option v-for="item in ethnicityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="父母教育程度" prop="parental_education">
          <el-select v-model="form.parental_education">
            <el-option
              v-for="item in parentalEducationOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" prop="class_name" class="edit-form-wide">
          <el-select v-model="form.class_name" filterable placeholder="请选择班级">
            <el-option
              v-for="item in filterOptions?.classes ?? []"
              :key="item.raw_class_name"
              :label="item.class_name"
              :value="item.raw_class_name"
            />
          </el-select>
        </el-form-item>
      </div>
      <p class="form-note">新增学生只写入基础信息，不会自动生成学业表现记录。</p>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存并查看详情</el-button>
    </template>
  </el-dialog>
</template>
