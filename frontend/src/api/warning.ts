import { apiClient, unwrap } from "./client";
import type { AxiosRequestConfig } from "axios";
import type { PageResult, WarningDetail, WarningQuery, WarningRecord, WarningStatus } from "../types";

const silentRequest = { silent: true } as AxiosRequestConfig;

export function fetchWarnings(params: WarningQuery) {
  return unwrap<PageResult<WarningRecord>>(apiClient.get("/warnings", { params }));
}

export function fetchWarningDetail(id: number) {
  return unwrap<WarningDetail>(apiClient.get(`/warnings/${id}`));
}

export function fetchLatestWarning(studentNo: number) {
  return unwrap<WarningRecord>(apiClient.get(`/warnings/students/${studentNo}/latest`, silentRequest));
}

export function updateWarningStatus(id: number, status: WarningStatus | string) {
  return unwrap<WarningDetail>(apiClient.patch(`/warnings/${id}/status`, { status }));
}
