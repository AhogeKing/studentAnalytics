import { apiClient, unwrap } from "./client";
import type { ModelTrainPayload, ModelTrainResult, ModelVersion, ModelVersionDetail, ModelVersionQuery, PageResult } from "../types";

export function trainDecisionTree(payload: ModelTrainPayload) {
  return unwrap<ModelTrainResult>(
    apiClient.post("/models/decision-tree/train", payload, {
      timeout: 120000
    })
  );
}

export function fetchActiveModel() {
  return unwrap<ModelVersion>(apiClient.get("/models/active"));
}

export function fetchModelVersions(params: ModelVersionQuery) {
  return unwrap<PageResult<ModelVersion>>(apiClient.get("/models/versions", { params }));
}

export function fetchModelVersionDetail(id: number) {
  return unwrap<ModelVersionDetail>(apiClient.get(`/models/versions/${id}`));
}
