package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WarningRecordVO {
    private Integer id;

    @JsonProperty("student_no")
    private Integer studentNo;

    @JsonProperty("student_name")
    private String studentName;

    @JsonProperty("class_info")
    private ClassInfoVO classInfo;

    @JsonProperty("prediction_result_id")
    private Integer predictionResultId;

    @JsonProperty("risk_score")
    private Integer riskScore;

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("risk_level_label")
    private String riskLevelLabel;

    @JsonProperty("risk_reasons")
    private List<String> riskReasons;

    private List<String> suggestions;

    private String status;

    @JsonProperty("status_label")
    private String statusLabel;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
