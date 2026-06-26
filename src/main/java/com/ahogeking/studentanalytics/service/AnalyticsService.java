package com.ahogeking.studentanalytics.service;

import com.ahogeking.studentanalytics.dto.AnalysisScopeQueryRequest;
import com.ahogeking.studentanalytics.vo.GpaDistributionItemVO;
import com.ahogeking.studentanalytics.vo.GradeClassDistributionItemVO;
import com.ahogeking.studentanalytics.vo.PerformanceAnalysisPointVO;

import java.util.List;

public interface AnalyticsService {
    List<GpaDistributionItemVO>  selectGpaDistributionItems(AnalysisScopeQueryRequest queryRequest);

    List<GradeClassDistributionItemVO> selectGradeClassDistributionItems(AnalysisScopeQueryRequest queryRequest);

    List<PerformanceAnalysisPointVO> selectPerformanceAnalysisPoints(AnalysisScopeQueryRequest queryRequest);
}
