import { apiClient, unwrap } from "./client";
import type { OperationLog, OperationLogOptions, OperationLogQuery, PageResult } from "../types";

export function fetchOperationLogs(query: OperationLogQuery) {
  return unwrap<PageResult<OperationLog>>(apiClient.get("/admin/operation-logs", { params: query }));
}

export function fetchOperationLogDetail(id: number) {
  return unwrap<OperationLog>(apiClient.get(`/admin/operation-logs/${id}`));
}

export function fetchOperationLogOptions() {
  return unwrap<OperationLogOptions>(apiClient.get("/admin/operation-logs/options"));
}
