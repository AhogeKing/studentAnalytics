<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import type { FormInstance, FormRules } from "element-plus";
import { ElMessage, ElMessageBox } from "element-plus";
import { Delete, EditPen, Key, Plus, Refresh, Switch } from "@element-plus/icons-vue";
import {
  createAdminUser,
  deleteAdminUser,
  fetchAdminUsers,
  resetAdminUserPassword,
  updateAdminUser,
  updateAdminUserStatus
} from "../api/adminUsers";
import { useAuthStore } from "../stores/auth";
import type { AdminUser, AdminUserCreatePayload, AdminUserUpdatePayload } from "../types";

const authStore = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const users = ref<AdminUser[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);
const dialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const editingUser = ref<AdminUser | null>(null);
const formRef = ref<FormInstance>();

const currentUserId = computed(() => authStore.user?.userId ?? authStore.user?.user_id);
const dialogTitle = computed(() => (dialogMode.value === "create" ? "新增系统用户" : "编辑系统用户"));

const form = reactive({
  username: "",
  password: "",
  real_name: "",
  role: "TEACHER" as "ADMIN" | "TEACHER",
  status: 1
});

const rules: FormRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { max: 50, message: "用户名长度不能超过 50 个字符", trigger: "blur" }
  ],
  password: [
    {
      validator: (_rule: unknown, value: unknown, callback: (error?: Error) => void) => {
        if (dialogMode.value === "edit") {
          callback();
          return;
        }
        if (!value) {
          callback(new Error("请输入初始密码"));
          return;
        }
        if (String(value).length < 6 || String(value).length > 100) {
          callback(new Error("密码长度为 6-100 个字符"));
          return;
        }
        callback();
      },
      trigger: "blur"
    }
  ],
  real_name: [{ max: 50, message: "真实姓名长度不能超过 50 个字符", trigger: "blur" }],
  role: [{ required: true, message: "请选择角色", trigger: "change" }],
  status: [{ required: true, message: "请选择状态", trigger: "change" }]
};

function displayName(user: AdminUser) {
  return user.real_name || user.realName || "-";
}

function statusLabel(status: number) {
  return status === 1 ? "启用" : "禁用";
}

function statusType(status: number) {
  return status === 1 ? "success" : "info";
}

function roleTagType(role: string) {
  if (role === "ADMIN") {
    return "danger";
  }
  if (role === "TEACHER") {
    return "primary";
  }
  return "info";
}

function formatTime(value?: string | null) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 19);
}

function isSelf(user: AdminUser) {
  return user.id === currentUserId.value;
}

function resetForm() {
  form.username = "";
  form.password = "";
  form.real_name = "";
  form.role = "TEACHER";
  form.status = 1;
  editingUser.value = null;
  formRef.value?.clearValidate();
}

async function loadUsers() {
  loading.value = true;
  try {
    const result = await fetchAdminUsers({
      page_num: page.value,
      page_size: pageSize.value
    });
    users.value = result.records;
    total.value = result.total;
  } catch {
    users.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  dialogMode.value = "create";
  resetForm();
  dialogVisible.value = true;
}

function openEdit(user: AdminUser) {
  dialogMode.value = "edit";
  editingUser.value = user;
  form.username = user.username;
  form.password = "";
  form.real_name = user.real_name || user.realName || "";
  form.role = user.role === "ADMIN" ? "ADMIN" : "TEACHER";
  form.status = user.status;
  formRef.value?.clearValidate();
  dialogVisible.value = true;
}

async function submitUser() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  saving.value = true;
  try {
    if (dialogMode.value === "create") {
      const payload: AdminUserCreatePayload = {
        username: form.username.trim(),
        password: form.password,
        real_name: form.real_name.trim() || undefined,
        role: form.role
      };
      await createAdminUser(payload);
      ElMessage.success("系统用户已新增");
    } else if (editingUser.value) {
      const payload: AdminUserUpdatePayload = {
        real_name: form.real_name.trim() || undefined,
        role: form.role,
        status: form.status
      };
      await updateAdminUser(editingUser.value.id, payload);
      ElMessage.success("系统用户已更新");
    }
    dialogVisible.value = false;
    await loadUsers();
  } finally {
    saving.value = false;
  }
}

async function toggleStatus(user: AdminUser) {
  if (isSelf(user)) {
    ElMessage.warning("不能禁用当前登录用户");
    return;
  }
  const nextStatus = user.status === 1 ? 0 : 1;
  await ElMessageBox.confirm(`确认${nextStatus === 1 ? "启用" : "禁用"}用户 ${user.username}？`, "状态确认", {
    type: "warning",
    confirmButtonText: nextStatus === 1 ? "启用" : "禁用",
    cancelButtonText: "取消"
  });
  await updateAdminUserStatus(user.id, { status: nextStatus });
  ElMessage.success(`用户已${nextStatus === 1 ? "启用" : "禁用"}`);
  await loadUsers();
}

async function resetPassword(user: AdminUser) {
  const { value } = await ElMessageBox.prompt(`为 ${user.username} 设置新密码`, "重置密码", {
    confirmButtonText: "保存",
    cancelButtonText: "取消",
    inputType: "password",
    inputPlaceholder: "请输入 6-100 位新密码",
    inputPattern: /^.{6,100}$/,
    inputErrorMessage: "密码长度为 6-100 个字符"
  });
  await resetAdminUserPassword(user.id, { new_password: value });
  ElMessage.success("密码已重置");
}

async function disableUser(user: AdminUser) {
  if (isSelf(user)) {
    ElMessage.warning("不能禁用当前登录用户");
    return;
  }
  await ElMessageBox.confirm(`删除当前会禁用 ${user.username}，该账号将无法登录。确认继续？`, "删除确认", {
    type: "warning",
    confirmButtonText: "禁用用户",
    cancelButtonText: "取消",
    confirmButtonClass: "el-button--danger"
  });
  await deleteAdminUser(user.id);
  ElMessage.success("用户已禁用");
  await loadUsers();
}

onMounted(() => {
  authStore.hydrate();
  void loadUsers();
});
</script>

<template>
  <section class="page-head">
    <div>
      <p class="eyebrow">用户管理</p>
      <h1>系统用户</h1>
      <p>管理管理员与教师账号，删除操作当前按后端语义处理为禁用账号。</p>
    </div>
    <div class="head-actions">
      <el-button :loading="loading" @click="loadUsers">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新增用户
      </el-button>
    </div>
  </section>

  <section class="content-panel admin-users-panel">
    <div class="panel-heading">
      <div>
        <h2>用户列表</h2>
        <p>共 {{ total }} 个系统用户，每页 {{ pageSize }} 条。</p>
      </div>
    </div>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="users" class="student-table admin-user-table" row-key="id">
        <el-table-column prop="username" label="用户名" min-width="150" fixed="left" />
        <el-table-column label="真实姓名" min-width="140">
          <template #default="{ row }">{{ displayName(row) }}</template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" effect="light">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.updated_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right" align="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button size="small" :icon="EditPen" circle title="编辑" @click="openEdit(row)" />
              <el-button size="small" :icon="Switch" circle :title="row.status === 1 ? '禁用' : '启用'" @click="toggleStatus(row)" />
              <el-button size="small" :icon="Key" circle title="重置密码" @click="resetPassword(row)" />
              <el-button
                size="small"
                type="danger"
                plain
                :icon="Delete"
                circle
                title="禁用用户"
                @click="disableUser(row)"
              />
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        background
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        @current-change="() => loadUsers()"
        @size-change="() => loadUsers()"
      />
    </div>
  </section>

  <el-dialog v-model="dialogVisible" class="student-edit-dialog" :title="dialogTitle" width="560px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="edit-form-grid">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" :disabled="dialogMode === 'edit'" maxlength="50" />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'create'" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password maxlength="100" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="real_name">
          <el-input v-model.trim="form.real_name" maxlength="50" placeholder="可为空" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="dialogMode === 'edit'" label="状态" prop="status">
          <el-select v-model="form.status" :disabled="editingUser ? isSelf(editingUser) : false">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </div>
      <p v-if="dialogMode === 'edit' && editingUser && isSelf(editingUser)" class="form-note">
        当前登录用户不能在这里禁用自身账号。
      </p>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitUser">保存</el-button>
    </template>
  </el-dialog>
</template>
