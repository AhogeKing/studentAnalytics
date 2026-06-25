<script setup lang="ts">
import { UserFilled } from "@element-plus/icons-vue";
import type { StudentBasicInfo } from "../../types";

defineProps<{
  info: StudentBasicInfo;
}>();
</script>

<template>
  <section class="detail-section">
    <div class="section-title">
      <el-icon><UserFilled /></el-icon>
      <h2>基础信息</h2>
    </div>
    <dl class="detail-grid">
      <div>
        <dt>学号</dt>
        <dd>{{ info.student_no }}</dd>
      </div>
      <div>
        <dt>姓名</dt>
        <dd>{{ info.name }}</dd>
      </div>
      <div>
        <dt>年龄</dt>
        <dd>{{ info.age }}</dd>
      </div>
      <div>
        <dt>性别</dt>
        <dd>{{ info.gender?.label || "-" }}</dd>
      </div>
      <div>
        <dt>年级</dt>
        <dd>高{{ ["", "一", "二", "三"][info.grade_level] || info.grade_level }}</dd>
      </div>
      <div>
        <dt>班级</dt>
        <dd>{{ info.class_info?.class_name || "-" }}</dd>
      </div>
    </dl>
  </section>
</template>
