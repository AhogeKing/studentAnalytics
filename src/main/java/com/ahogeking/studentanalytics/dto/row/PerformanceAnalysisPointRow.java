package com.ahogeking.studentanalytics.dto.row;

import lombok.Data;

import java.math.BigDecimal;

// 表现分析点 Row
@Data
public class PerformanceAnalysisPointRow {
    private Integer studentNo;
    private String name;
    private BigDecimal studyTimeWeekly;
    private Integer absences;
    private BigDecimal gpa;
    private Integer gradeClass;
}
