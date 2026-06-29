import { apiClient, unwrap } from "./client";
import type { AxiosRequestConfig } from "axios";
import type { PredictionEligibility, PredictionPayload, StudentPrediction } from "../types";

const silentRequest = { silent: true } as AxiosRequestConfig;

export function predictStudent(studentNo: number, payload: PredictionPayload = {}) {
  return unwrap<StudentPrediction>(apiClient.post(`/predictions/students/${studentNo}`, payload, { timeout: 120000 }));
}

export function fetchPredictionEligibility(studentNo: number, modelVersionId?: number) {
  return unwrap<PredictionEligibility>(
    apiClient.get(`/predictions/students/${studentNo}/eligibility`, {
      params: modelVersionId ? { model_version_id: modelVersionId } : undefined
    })
  );
}

export function fetchPredictionDetail(id: number) {
  return unwrap<StudentPrediction>(apiClient.get(`/predictions/${id}`));
}

export function fetchLatestPrediction(studentNo: number) {
  return unwrap<StudentPrediction>(apiClient.get(`/predictions/students/${studentNo}/latest`, silentRequest));
}
