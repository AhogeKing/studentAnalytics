package com.ahogeking.studentanalytics.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarningRecord {
    private Integer id;
    private Integer studentId;
    private Integer predictionResultId;
    private Integer riskScore;
    private String riskLevel;
    private String riskReasonsJson;
    private String suggestionJson;
    private String status;
    private Integer handlerUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
