import { createRouter, createWebHistory } from "vue-router";
import { getToken } from "../stores/auth";
import MainLayout from "../layouts/MainLayout.vue";
import LoginView from "../views/LoginView.vue";
import StudentDetailView from "../views/StudentDetailView.vue";
import StudentOverviewView from "../views/StudentOverviewView.vue";

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
  if (to.name === "login" && hasToken) {
    return { name: "students" };
  }
  return true;
});

export default router;
