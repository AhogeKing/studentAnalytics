import axios from "axios";
import type { AxiosRequestConfig } from "axios";
import { ElMessage } from "element-plus";
import router from "../router";
import { getToken, useAuthStore } from "../stores/auth";
import type { ApiResult } from "../types";

type SilentRequestConfig = AxiosRequestConfig & {
  silent?: boolean;
};

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  timeout: 10000
});

apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown>;
    if (typeof result?.code === "number" && result.code !== 0) {
      return Promise.reject(new Error(result.message || "请求失败"));
    }
    return response;
  },
  (error) => {
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message || "网络请求失败";
    const silent = (error.config as SilentRequestConfig | undefined)?.silent;
    if (status === 401) {
      useAuthStore().clear();
      void router.replace({ name: "login" });
    }
    if (!silent) {
      ElMessage.error(status === 403 ? message || "无权限执行此操作" : message);
    }
    return Promise.reject(error);
  }
);

export async function unwrap<T>(request: Promise<{ data: ApiResult<T> }>): Promise<T> {
  const response = await request;
  return response.data.data;
}
