package com.ahogeking.studentanalytics.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PredictionResult {
    private Integer id;
    private Integer studentId;
    private Integer performanceId;
    private Integer modelVersionId;
    private Integer predictedGradeClass;
    private String predictedGradeLabel;
    private String probabilityJson;
    private String importantFactorsJson;
    private String predictInputJson;
    private LocalDateTime createdAt;
}
