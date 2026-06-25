package com.ahogeking.studentanalytics.mapper;

import com.ahogeking.studentanalytics.dto.row.AnalysisCategoryCountRow;
import com.ahogeking.studentanalytics.dto.row.PerformanceAnalysisPointRow;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AnalysisMapper {

    // 查询各 GPA 区间人数
    List<AnalysisCategoryCountRow> selectGpaBucketCounts();

    // 查询各 GradeClass 人数
    List<AnalysisCategoryCountRow> selectGradeClassCounts();

    // 查询散点图需要的学生表现数据
    List<PerformanceAnalysisPointRow> selectPerformanceAnalysisPoints();
}
