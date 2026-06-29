package com.ahogeking.studentanalytics.dto.row;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarningRecordRow {
    private Integer id;
    private Integer studentId;
    private Integer studentNo;
    private String studentName;
    private Integer gradeLevel;
    private String className;
    private Integer predictionResultId;
    private Integer modelVersionId;
    private String modelVersionNo;
    private Integer predictedGradeClass;
    private String predictedGradeLabel;
    private Integer riskScore;
    private String riskLevel;
    private String riskReasonsJson;
    private String suggestionJson;
    private String status;
    private Integer handlerUserId;
    private String handlerUsername;
    private String handlerRealName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
