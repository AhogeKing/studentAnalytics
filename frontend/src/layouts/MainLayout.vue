<script setup lang="ts">
import { onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { DataAnalysis, SwitchButton, UserFilled } from "@element-plus/icons-vue";
import { fetchMe, logout } from "../api/auth";
import { useAuthStore } from "../stores/auth";

const router = useRouter();
const authStore = useAuthStore();

async function handleLogout() {
  try {
    await logout();
  } catch {
    // The local session should still be cleared when the backend token is already invalid.
  } finally {
    authStore.clear();
    ElMessage.success("已退出登录");
    await router.replace({ name: "login" });
  }
}

onMounted(async () => {
  authStore.hydrate();
  if (!authStore.user) {
    try {
      authStore.setSession(await fetchMe());
    } catch {
      authStore.clear();
      await router.replace({ name: "login" });
    }
  }
});
</script>

<template>
  <main class="app-shell">
    <header class="app-topbar">
      <RouterLink class="app-brand" :to="{ name: 'students' }">
        <span class="brand-mark compact"><el-icon><DataAnalysis /></el-icon></span>
        <span>
          <strong>StudentAnalytics</strong>
          <small>学生信息管理</small>
        </span>
      </RouterLink>

      <nav class="app-nav" aria-label="主导航">
        <RouterLink :to="{ name: 'students' }">学生概览</RouterLink>
        <RouterLink :to="{ name: 'analytics' }">分析图表</RouterLink>
      </nav>

      <div class="topbar-account">
        <div class="user-menu" aria-label="当前用户">
          <el-icon><UserFilled /></el-icon>
          <span>{{ authStore.displayName }}</span>
          <small v-if="authStore.role">{{ authStore.role }}</small>
        </div>
        <el-button class="logout-button" type="danger" plain @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </header>

    <RouterView />
  </main>
</template>
