package com.ahogeking.studentanalytics.dto.row;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PredictionResultRow {
    private Integer id;
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
    private String probabilityJson;
    private String importantFactorsJson;
    private String predictInputJson;
    private LocalDateTime createdAt;
}
