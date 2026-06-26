<script setup lang="ts">
import { computed } from "vue";
import { Check, Refresh } from "@element-plus/icons-vue";
import type { AnalysisScopeMode, ClassInfo } from "../../types";

const gradeOptions = [
  { value: 1, label: "高一" },
  { value: 2, label: "高二" },
  { value: 3, label: "高三" }
];

const props = defineProps<{
  mode: AnalysisScopeMode;
  gradeLevel?: number;
  classNames: string[];
  classes: ClassInfo[];
  loading?: boolean;
  currentScopeLabel: string;
}>();

const emit = defineEmits<{
  "update:mode": [value: AnalysisScopeMode];
  "update:gradeLevel": [value: number | undefined];
  "update:classNames": [value: string[]];
  apply: [];
  reset: [];
}>();

const availableClassOptions = computed(() => {
  if (!props.gradeLevel) {
    return [];
  }
  return props.classes
    .filter((item) => Number(item.grade_level) === props.gradeLevel)
    .sort((left, right) => left.raw_class_name.localeCompare(right.raw_class_name, "zh-Hans-CN", { numeric: true }));
});

function setMode(mode: AnalysisScopeMode) {
  emit("update:mode", mode);
  if (mode === "all") {
    emit("update:gradeLevel", undefined);
    emit("update:classNames", []);
  }
  if (mode === "grade") {
    emit("update:classNames", []);
  }
}

function setGrade(value: number | undefined) {
  emit("update:gradeLevel", value);
  emit("update:classNames", []);
}
</script>

<template>
  <section class="analysis-scope-panel">
    <div class="panel-heading">
      <div>
        <h2>分析范围</h2>
        <p>{{ currentScopeLabel }}</p>
      </div>
      <div class="scope-actions">
        <el-button :disabled="loading" @click="emit('reset')">
          <el-icon><Refresh /></el-icon>
          重置
        </el-button>
        <el-button :loading="loading" type="primary" @click="emit('apply')">
          <el-icon><Check /></el-icon>
          应用范围
        </el-button>
      </div>
    </div>

    <div class="scope-mode-grid" aria-label="分析范围模式">
      <button type="button" :class="{ active: mode === 'all' }" @click="setMode('all')">
        <span>全部学生</span>
        <small>不限制年级和班级</small>
      </button>
      <button type="button" :class="{ active: mode === 'grade' }" @click="setMode('grade')">
        <span>按年级</span>
        <small>统计某个年级全部班级</small>
      </button>
      <button type="button" :class="{ active: mode === 'classes' }" @click="setMode('classes')">
        <span>多班级</span>
        <small>同年级内合并多个班级</small>
      </button>
    </div>

    <div v-if="mode !== 'all'" class="scope-form-grid">
      <el-select
        :model-value="gradeLevel"
        clearable
        placeholder="选择年级"
        @update:model-value="setGrade"
      >
        <el-option v-for="grade in gradeOptions" :key="grade.value" :label="grade.label" :value="grade.value" />
      </el-select>
      <el-select
        v-if="mode === 'classes'"
        :model-value="classNames"
        multiple
        collapse-tags
        collapse-tags-tooltip
        filterable
        clearable
        :disabled="!gradeLevel"
        placeholder="选择一个或多个班级"
        @update:model-value="emit('update:classNames', $event)"
      >
        <el-option
          v-for="item in availableClassOptions"
          :key="item.raw_class_name"
          :label="item.class_name"
          :value="item.raw_class_name"
        />
      </el-select>
    </div>
  </section>
</template>
