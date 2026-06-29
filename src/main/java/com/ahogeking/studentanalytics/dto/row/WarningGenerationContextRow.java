package com.ahogeking.studentanalytics.dto.row;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WarningGenerationContextRow {
    private Integer predictionResultId;
    private Integer studentId;
    private Integer studentNo;
    private String studentName;
    private Integer gradeLevel;
    private String className;
    private Integer performanceId;
    private Integer modelVersionId;
    private String modelVersionNo;
    private Integer predictedGradeClass;
    private String predictedGradeLabel;
    private BigDecimal gpa;
    private Integer absences;
    private BigDecimal studyTimeWeekly;
    private Integer parentalSupport;
    private Integer tutoring;
}
