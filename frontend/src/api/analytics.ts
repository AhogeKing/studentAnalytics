import { apiClient, unwrap } from "./client";
import type {
  GpaDistributionItem,
  GradeClassDistributionItem,
  PerformanceAnalysisPoint
} from "../types";

export function fetchGpaDistribution() {
  return unwrap<GpaDistributionItem[]>(apiClient.get("/analytics/gpa-distribution"));
}

export function fetchGradeClassDistribution() {
  return unwrap<GradeClassDistributionItem[]>(apiClient.get("/analytics/grade-class-distribution"));
}

export function fetchPerformanceAnalysisPoints() {
  return unwrap<PerformanceAnalysisPoint[]>(apiClient.get("/analytics/performance-points"));
}
