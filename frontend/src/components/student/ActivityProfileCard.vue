<script setup lang="ts">
import { Trophy } from "@element-plus/icons-vue";
import type { ActivityProfile } from "../../types";

defineProps<{
  profile?: ActivityProfile | null;
  available: boolean;
}>();

function yesNo(value?: boolean | null) {
  if (value == null) {
    return "-";
  }
  return value ? "参与" : "未参与";
}
</script>

<template>
  <section class="detail-section">
    <div class="section-title">
      <el-icon><Trophy /></el-icon>
      <h2>活动画像</h2>
    </div>
    <el-empty v-if="!available || !profile" :image-size="72" description="暂无活动画像记录" />
    <dl v-else class="detail-grid">
      <div>
        <dt>课外活动</dt>
        <dd>{{ yesNo(profile.extracurricular) }}</dd>
      </div>
      <div>
        <dt>体育</dt>
        <dd>{{ yesNo(profile.sports) }}</dd>
      </div>
      <div>
        <dt>音乐</dt>
        <dd>{{ yesNo(profile.music) }}</dd>
      </div>
      <div>
        <dt>志愿服务</dt>
        <dd>{{ yesNo(profile.volunteering) }}</dd>
      </div>
    </dl>
  </section>
</template>
