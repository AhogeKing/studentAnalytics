import { createRouter, createWebHistory } from "vue-router";
import { getStoredUser, getToken } from "../stores/auth";
import AdminUsersView from "../views/AdminUsersView.vue";
import MainLayout from "../layouts/MainLayout.vue";
import AnalysisChartsView from "../views/AnalysisChartsView.vue";
import LoginView from "../views/LoginView.vue";
import ModelManagementView from "../views/ModelManagementView.vue";
import OperationLogsView from "../views/OperationLogsView.vue";
import StudentDetailView from "../views/StudentDetailView.vue";
import StudentOverviewView from "../views/StudentOverviewView.vue";
import WarningManagementView from "../views/WarningManagementView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/",
      redirect: () => (getToken() ? { name: "students" } : { name: "login" })
    },
    {
      path: "/login",
      name: "login",
      component: LoginView,
      meta: { public: true }
    },
    {
      path: "/auth",
      redirect: { name: "login" }
    },
    {
      path: "/",
      component: MainLayout,
      children: [
        {
          path: "students",
          name: "students",
          component: StudentOverviewView
        },
        {
          path: "analytics",
          name: "analytics",
          component: AnalysisChartsView
        },
        {
          path: "models",
          name: "models",
          component: ModelManagementView
        },
        {
          path: "warnings",
          name: "warnings",
          component: WarningManagementView
        },
        {
          path: "admin/users",
          name: "admin-users",
          component: AdminUsersView,
          meta: { requiresAdmin: true }
        },
        {
          path: "admin/operation-logs",
          name: "operation-logs",
          component: OperationLogsView,
          meta: { requiresAdmin: true }
        },
        {
          path: "students/:studentNo",
          name: "student-detail",
          component: StudentDetailView,
          props: (route) => ({ studentNo: Number(route.params.studentNo) })
        }
      ]
    },
    {
      path: "/:pathMatch(.*)*",
      name: "not-found",
      redirect: { name: "students" }
    }
  ]
});

router.beforeEach((to) => {
  const hasToken = Boolean(getToken());
  if (!to.meta.public && !hasToken) {
    return { name: "login" };
  }
  if (to.meta.requiresAdmin) {
    const role = getStoredUser()?.role?.toLowerCase();
    if (role && role !== "admin") {
      return { name: "students" };
    }
  }
  if (to.name === "login" && hasToken) {
    return { name: "students" };
  }
  return true;
});

export default router;
