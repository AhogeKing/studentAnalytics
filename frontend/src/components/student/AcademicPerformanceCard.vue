<script setup lang="ts">
import { Medal } from "@element-plus/icons-vue";
import type { AcademicPerformance } from "../../types";

defineProps<{
  performance?: AcademicPerformance | null;
  available: boolean;
}>();
</script>

<template>
  <section class="detail-section">
    <div class="section-title">
      <el-icon><Medal /></el-icon>
      <h2>学业表现</h2>
    </div>
    <el-empty v-if="!available || !performance" :image-size="72" description="暂无学业表现记录" />
    <dl v-else class="detail-grid">
      <div>
        <dt>GPA</dt>
        <dd>{{ performance.gpa == null ? "-" : Number(performance.gpa).toFixed(2) }}</dd>
      </div>
      <div>
        <dt>成绩等级</dt>
        <dd>{{ performance.grade_class?.label || "-" }}</dd>
      </div>
      <div>
        <dt>每周学习时长</dt>
        <dd>{{ performance.study_time_weekly == null ? "-" : `${performance.study_time_weekly} 小时` }}</dd>
      </div>
      <div>
        <dt>缺勤次数</dt>
        <dd>{{ performance.absences ?? "-" }}</dd>
      </div>
    </dl>
  </section>
</template>
