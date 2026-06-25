<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import type { FormInstance, FormRules } from "element-plus";
import { ElMessage } from "element-plus";
import { ArrowRight, DataAnalysis, Lock, User, UserFilled } from "@element-plus/icons-vue";
import { login, register } from "../api/auth";
import { saveSession } from "../stores/auth";

type AuthMode = "login" | "register";

const router = useRouter();
const formRef = ref<FormInstance>();
const activeMode = ref<AuthMode>("login");
const submitting = ref(false);

const form = reactive({
  username: "",
  password: "",
  realName: "",
  role: "TEACHER"
});

const rules = computed<FormRules>(() => ({
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { max: 50, message: "用户名长度不能超过 50 个字符", trigger: "blur" }
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 100, message: "密码长度需在 6 到 100 个字符之间", trigger: "blur" }
  ],
  realName:
    activeMode.value === "register"
      ? [{ max: 50, message: "姓名长度不能超过 50 个字符", trigger: "blur" }]
      : []
}));

async function submit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    if (activeMode.value === "register") {
      await register({
        username: form.username,
        password: form.password,
        realName: form.realName,
        role: form.role
      });
      ElMessage.success("注册成功，已为你进入学生概览");
    }

    const user = await login({
      username: form.username,
      password: form.password
    });
    saveSession(user);
    await router.replace({ name: "students", query: { grade_level: "1", class_name: "1-1" } });
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-brand">
      <div class="brand-mark">
        <el-icon><DataAnalysis /></el-icon>
      </div>
      <div>
        <h1>StudentAnalytics</h1>
        <p>面向班级管理的学业数据分析平台</p>
      </div>

      <div class="insight-stack" aria-label="学生数据概览预览">
        <div class="insight-card primary">
          <span>默认视图</span>
          <strong>高一 1 班</strong>
          <small>登录后自动进入学生概览</small>
        </div>
        <div class="metric-row">
          <div>
            <span>GPA 均值</span>
            <strong>3.42</strong>
          </div>
          <div>
            <span>优秀率</span>
            <strong>28%</strong>
          </div>
        </div>
        <div class="mini-chart" aria-hidden="true">
          <i style="height: 42%" />
          <i style="height: 76%" />
          <i style="height: 58%" />
          <i style="height: 88%" />
          <i style="height: 63%" />
        </div>
      </div>
    </section>

    <section class="auth-panel" aria-label="登录注册表单">
      <div class="auth-panel__head">
        <h2>{{ activeMode === "login" ? "登录账户" : "创建账户" }}</h2>
        <p>进入班级成绩分布、学生列表和风险筛查工作台</p>
      </div>

      <div class="mode-switch" role="tablist" aria-label="登录注册切换">
        <button :class="{ active: activeMode === 'login' }" type="button" role="tab" @click="activeMode = 'login'">登录</button>
        <button :class="{ active: activeMode === 'register' }" type="button" role="tab" @click="activeMode = 'register'">注册</button>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form" @submit.prevent="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" size="large" placeholder="请输入用户名">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="至少 6 位密码">
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <template v-if="activeMode === 'register'">
          <el-form-item label="姓名" prop="realName">
            <el-input v-model.trim="form.realName" size="large" placeholder="例如：王老师">
              <template #prefix>
                <el-icon><UserFilled /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="form.role" size="large" class="full-width">
              <el-option label="任课教师" value="TEACHER" />
              <el-option label="年级管理员" value="ADMIN" />
            </el-select>
          </el-form-item>
        </template>

        <el-button class="auth-submit" type="primary" size="large" :loading="submitting" @click="submit">
          <el-icon><ArrowRight /></el-icon>
          {{ activeMode === "login" ? "登录并进入概览" : "注册并进入概览" }}
        </el-button>
      </el-form>
    </section>
  </main>
</template>
