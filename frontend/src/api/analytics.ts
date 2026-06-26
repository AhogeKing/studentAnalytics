import { apiClient, unwrap } from "./client";
import type {
  AnalysisScopeParams,
  GpaDistributionItem,
  GradeClassDistributionItem,
  PerformanceAnalysisPoint
} from "../types";

export function fetchGpaDistribution(params: AnalysisScopeParams = {}) {
  return unwrap<GpaDistributionItem[]>(apiClient.get(withAnalysisQuery("/analytics/gpa-distribution", params)));
}

export function fetchGradeClassDistribution(params: AnalysisScopeParams = {}) {
  return unwrap<GradeClassDistributionItem[]>(
    apiClient.get(withAnalysisQuery("/analytics/grade-class-distribution", params))
  );
}

export function fetchPerformanceAnalysisPoints(params: AnalysisScopeParams = {}) {
  return unwrap<PerformanceAnalysisPoint[]>(apiClient.get(withAnalysisQuery("/analytics/performance-points", params)));
}

export function buildAnalysisSearchParams(scope: AnalysisScopeParams) {
  const params = new URLSearchParams();

  if (scope.grade_level != null) {
    params.append("grade_level", String(scope.grade_level));
  }

  scope.class_name?.forEach((className) => {
    const trimmed = className.trim();
    if (trimmed) {
      params.append("class_name", trimmed);
    }
  });

  return params;
}

function withAnalysisQuery(path: string, scope: AnalysisScopeParams) {
  const query = buildAnalysisSearchParams(scope).toString();
  return query ? `${path}?${query}` : path;
}
