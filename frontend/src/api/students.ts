import { apiClient, unwrap } from "./client";
import type { AxiosRequestConfig } from "axios";
import type {
  StudentDetail,
  StudentFilterOptions,
  StudentOverview,
  StudentOverviewItem,
  StudentOverviewQuery,
  StudentOverviewUpdatePayload
} from "../types";

const silentRequest = { silent: true } as AxiosRequestConfig;

export function fetchStudentOverview(query: StudentOverviewQuery) {
  return unwrap<StudentOverview>(apiClient.get("/students/page", { params: query }));
}

export function fetchStudentFilterOptions() {
  return unwrap<StudentFilterOptions>(apiClient.get("/students/filter-options", silentRequest));
}

export function fetchStudentDetail(studentNo: number) {
  return unwrap<StudentDetail>(apiClient.get(`/students/detail/${studentNo}`));
}

export function updateStudentOverview(studentNo: number, payload: StudentOverviewUpdatePayload) {
  return unwrap<StudentOverviewItem>(apiClient.put(`/students/overview/${studentNo}`, payload));
}

export function deleteStudentOverview(studentNo: number) {
  return unwrap<void>(apiClient.delete(`/students/overview/${studentNo}`));
}
