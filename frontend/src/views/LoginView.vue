<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import type { FormInstance, FormRules } from "element-plus";
import { ElMessage } from "element-plus";
import { DataAnalysis, Lock, User } from "@element-plus/icons-vue";
import { login, register } from "../api/auth";
import { useAuthStore } from "../stores/auth";

type AuthMode = "login" | "register";

const router = useRouter();
const authStore = useAuthStore();
const formRef = ref<FormInstance>();
const loading = ref(false);
const mode = ref<AuthMode>("login");

const form = reactive({
  username: "",
  password: "",
  confirmPassword: "",
  realName: ""
});

const rules = computed<FormRules>(() => ({
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { max: 50, message: "用户名长度不能超过 50 个字符", trigger: "blur" }
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    ...(mode.value === "register"
      ? [{ min: 6, max: 100, message: "密码长度在 6 到 100 个字符之间", trigger: "blur" }]
      : [])
  ],
  confirmPassword:
    mode.value === "register"
      ? [
          { required: true, message: "请再次输入密码", trigger: "blur" },
          {
            validator: (_rule, value, callback) => {
              if (value !== form.password) {
                callback(new Error("两次输入的密码不一致"));
                return;
              }
              callback();
            },
            trigger: "blur"
          }
        ]
      : [],
  realName: [{ max: 50, message: "真实姓名长度不能超过 50 个字符", trigger: "blur" }]
}));

const panelTitle = computed(() => (mode.value === "login" ? "登录" : "注册"));
const panelSubtitle = computed(() =>
  mode.value === "login" ? "进入后默认查看高一 1 班学生概览。" : "注册教师账号后即可返回登录。"
);

function switchMode(value: AuthMode) {
  mode.value = value;
  formRef.value?.clearValidate();
}

async function submit() {
  if (mode.value === "login") {
    await handleLogin();
    return;
  }
  await handleRegister();
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  loading.value = true;
  try {
    const user = await login({ username: form.username.trim(), password: form.password });
    authStore.setSession(user);
    ElMessage.success("登录成功");
    await router.replace({ name: "students" });
  } finally {
    loading.value = false;
  }
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  loading.value = true;
  try {
    await register({
      username: form.username.trim(),
      password: form.password,
      realName: form.realName.trim() || undefined
    });
    ElMessage.success("注册成功，请登录");
    mode.value = "login";
    form.confirmPassword = "";
    formRef.value?.clearValidate();
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-copy">
      <span class="brand-mark"><el-icon><DataAnalysis /></el-icon></span>
      <div>
        <p class="eyebrow">StudentAnalytics</p>
        <h1>学生学业信息管理系统</h1>
        <p class="login-lead">当前版本只启用后端已完成的学生概览、详情、编辑与删除能力。</p>
      </div>
    </section>

    <section class="login-panel" aria-label="登录表单">
      <div class="panel-heading">
        <div>
          <h2>{{ panelTitle }}</h2>
          <p>{{ panelSubtitle }}</p>
        </div>
      </div>

      <div class="auth-mode-switch" role="tablist" aria-label="登录注册切换">
        <button :class="{ active: mode === 'login' }" type="button" @click="switchMode('login')">登录</button>
        <button :class="{ active: mode === 'register' }" type="button" @click="switchMode('register')">注册</button>
      </div>

      <el-form ref="formRef" class="login-form" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" size="large" autocomplete="username" placeholder="请输入用户名">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="mode === 'register'" label="真实姓名" prop="realName">
          <el-input v-model.trim="form.realName" size="large" autocomplete="name" placeholder="请输入真实姓名">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
            show-password
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="mode === 'register'" label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            size="large"
            type="password"
            autocomplete="new-password"
            placeholder="请再次输入密码"
            show-password
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button class="full-width" type="primary" size="large" :loading="loading" @click="submit">
          {{ mode === "login" ? "登录" : "注册" }}
        </el-button>
      </el-form>
    </section>
  </main>
</template>
