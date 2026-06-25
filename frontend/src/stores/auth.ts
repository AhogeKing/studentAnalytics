import { defineStore } from "pinia";
import type { UserProfile } from "../types";

const TOKEN_KEY = "student-analytics-token";
const USER_KEY = "student-analytics-user";

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser(): UserProfile | null {
  const rawUser = localStorage.getItem(USER_KEY);
  if (!rawUser) {
    return null;
  }

  try {
    return JSON.parse(rawUser) as UserProfile;
  } catch {
    clearSession();
    return null;
  }
}

export function saveSession(user: UserProfile) {
  if (user.token) {
    localStorage.setItem(TOKEN_KEY, user.token);
  }
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: getToken(),
    user: getStoredUser()
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    displayName: (state) =>
      state.user?.realName || state.user?.real_name || state.user?.userName || state.user?.user_name || "教师用户",
    role: (state) => state.user?.role || ""
  },
  actions: {
    setSession(user: UserProfile) {
      saveSession(user);
      this.token = user.token || getToken();
      this.user = user;
    },
    clear() {
      clearSession();
      this.token = null;
      this.user = null;
    },
    hydrate() {
      this.token = getToken();
      this.user = getStoredUser();
    }
  }
});
