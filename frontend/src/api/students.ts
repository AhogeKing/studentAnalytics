import { apiClient, unwrap } from "./client";
import type { AxiosRequestConfig } from "axios";
import type {
  StudentDetail,
  StudentCreatePayload,
  StudentFilterOptions,
  StudentPerformanceUpsertPayload,
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

export function createStudent(payload: StudentCreatePayload) {
  return unwrap<StudentDetail>(apiClient.post("/students", payload));
}

export function updateStudentOverview(studentNo: number, payload: StudentOverviewUpdatePayload) {
  return unwrap<StudentOverviewItem>(apiClient.put(`/students/overview/${studentNo}`, payload));
}

export function upsertStudentPerformance(studentNo: number, payload: StudentPerformanceUpsertPayload) {
  return unwrap<StudentDetail>(apiClient.put(`/students/performance/${studentNo}`, payload));
}

export function deleteStudentOverview(studentNo: number) {
  return unwrap<void>(apiClient.delete(`/students/overview/${studentNo}`));
}
