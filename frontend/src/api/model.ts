import { apiClient, unwrap } from "./client";
import type {
  ModelTrainPayload,
  ModelTrainResult,
  ModelVersion,
  ModelVersionDetail,
  ModelVersionQuery,
  ModelVersionUpdatePayload,
  PageResult
} from "../types";

const TRAIN_TIMEOUT_BY_MODE: Record<string, number> = {
  quick: 5 * 60 * 1000,
  default: 10 * 60 * 1000
};

export function trainDecisionTree(payload: ModelTrainPayload) {
  const mode = payload.mode || "default";
  return unwrap<ModelTrainResult>(
    apiClient.post("/models/decision-tree/train", payload, {
      timeout: TRAIN_TIMEOUT_BY_MODE[mode] || TRAIN_TIMEOUT_BY_MODE.default
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

export function activateModelVersion(id: number) {
  return unwrap<ModelVersion>(apiClient.post(`/models/versions/${id}/activate`));
}

export function updateModelVersion(id: number, payload: ModelVersionUpdatePayload) {
  return unwrap<ModelVersion>(apiClient.patch(`/models/versions/${id}`, payload));
}

export function deleteModelVersion(id: number) {
  return unwrap<void>(apiClient.delete(`/models/versions/${id}`));
}
