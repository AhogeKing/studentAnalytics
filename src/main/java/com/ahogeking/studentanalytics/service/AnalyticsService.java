package com.ahogeking.studentanalytics.service;

import com.ahogeking.studentanalytics.vo.GpaDistributionItemVO;
import com.ahogeking.studentanalytics.vo.GradeClassDistributionItemVO;
import com.ahogeking.studentanalytics.vo.PerformanceAnalysisPointVO;

import java.util.List;

public interface AnalyticsService {
    List<GpaDistributionItemVO>  selectGpaDistributionItems();

    List<GradeClassDistributionItemVO> selectGradeClassDistributionItems();

    List<PerformanceAnalysisPointVO> selectPerformanceAnalysisPoints();
}
