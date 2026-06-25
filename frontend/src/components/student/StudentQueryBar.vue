<script setup lang="ts">
import { reactive, watch } from "vue";
import { Filter, Refresh, Search } from "@element-plus/icons-vue";
import type {
  StudentFilterForm,
  StudentFilterOptions,
  StudentQueryMode,
  StudentSearchForm
} from "../../types";

const props = defineProps<{
  mode: StudentQueryMode;
  filters: StudentFilterForm;
  search: StudentSearchForm;
  filterOptions: StudentFilterOptions | null;
  loading?: boolean;
}>();

const emit = defineEmits<{
  "update:mode": [value: StudentQueryMode];
  "submit-filter": [value: StudentFilterForm];
  "submit-search": [value: StudentSearchForm];
  reset: [];
}>();

const localFilters = reactive<StudentFilterForm>({});
const localSearch = reactive<StudentSearchForm>({});

watch(
  () => props.filters,
  (value) => {
    for (const key of Object.keys(localFilters) as Array<keyof StudentFilterForm>) {
      delete localFilters[key];
    }
    Object.assign(localFilters, value);
  },
  { immediate: true, deep: true }
);

watch(
  () => props.search,
  (value) => {
    for (const key of Object.keys(localSearch) as Array<keyof StudentSearchForm>) {
      delete localSearch[key];
    }
    Object.assign(localSearch, value);
  },
  { immediate: true, deep: true }
);

function setMode(mode: StudentQueryMode) {
  emit("update:mode", mode);
}

function submit() {
  if (props.mode === "search") {
    emit("submit-search", { keyword: localSearch.keyword?.trim() || undefined });
    return;
  }
  emit("submit-filter", { ...localFilters });
}
</script>

<template>
  <section class="query-panel">
    <div class="query-mode" role="tablist" aria-label="查询模式">
      <button :class="{ active: mode === 'filter' }" type="button" @click="setMode('filter')">
        <el-icon><Filter /></el-icon>
        条件筛选
      </button>
      <button :class="{ active: mode === 'search' }" type="button" @click="setMode('search')">
        <el-icon><Search /></el-icon>
        关键字搜索
      </button>
    </div>

    <div v-if="mode === 'filter'" class="query-grid">
      <el-select v-model="localFilters.class_name" clearable filterable placeholder="班级">
        <el-option
          v-for="item in filterOptions?.classes ?? []"
          :key="item.raw_class_name"
          :label="item.class_name"
          :value="item.raw_class_name"
        />
      </el-select>
      <el-select v-model="localFilters.grade_level" clearable placeholder="年级">
        <el-option label="高一" :value="1" />
        <el-option label="高二" :value="2" />
        <el-option label="高三" :value="3" />
      </el-select>
      <el-select v-model="localFilters.gender" clearable placeholder="性别">
        <el-option v-for="item in filterOptions?.genders ?? []" :key="String(item.value)" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="localFilters.grade_class" clearable placeholder="成绩等级">
        <el-option
          v-for="item in filterOptions?.gradeClasses ?? []"
          :key="String(item.value)"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-input-number v-model="localFilters.min_gpa" :min="0" :max="4" :precision="2" :step="0.1" controls-position="right" placeholder="最低 GPA" />
      <el-input-number v-model="localFilters.max_gpa" :min="0" :max="4" :precision="2" :step="0.1" controls-position="right" placeholder="最高 GPA" />
    </div>

    <div v-else class="query-search">
      <el-input v-model.trim="localSearch.keyword" clearable size="large" placeholder="输入学号、姓名或班级关键字">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
    </div>

    <div class="query-actions">
      <el-button :loading="loading" type="primary" @click="submit">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
      <el-button :disabled="loading" @click="emit('reset')">
        <el-icon><Refresh /></el-icon>
        重置
      </el-button>
    </div>
  </section>
</template>
