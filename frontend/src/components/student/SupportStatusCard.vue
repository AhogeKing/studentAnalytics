<script setup lang="ts">
import { FirstAidKit } from "@element-plus/icons-vue";
import type { SupportStatus } from "../../types";

defineProps<{
  status?: SupportStatus | null;
  available: boolean;
}>();

function yesNo(value?: boolean | null) {
  if (value == null) {
    return "-";
  }
  return value ? "是" : "否";
}
</script>

<template>
  <section class="detail-section">
    <div class="section-title">
      <el-icon><FirstAidKit /></el-icon>
      <h2>支持状态</h2>
    </div>
    <el-empty v-if="!available || !status" :image-size="72" description="暂无支持状态记录" />
    <dl v-else class="detail-grid">
      <div>
        <dt>课外辅导</dt>
        <dd>{{ yesNo(status.tutoring) }}</dd>
      </div>
      <div>
        <dt>家长支持</dt>
        <dd>{{ status.parental_support?.label || "-" }}</dd>
      </div>
    </dl>
  </section>
</template>
