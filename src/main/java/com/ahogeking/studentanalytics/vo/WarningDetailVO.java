package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WarningDetailVO {
    private Integer id;

    @JsonProperty("student_no")
    private Integer studentNo;

    @JsonProperty("student_name")
    private String studentName;

    @JsonProperty("class_info")
    private ClassInfoVO classInfo;

    @JsonProperty("prediction_result_id")
    private Integer predictionResultId;

    @JsonProperty("model_version_id")
    private Integer modelVersionId;

    @JsonProperty("model_version_no")
    private String modelVersionNo;

    @JsonProperty("predicted_grade_class")
    private OptionVO<Integer> predictedGradeClass;

    @JsonProperty("predicted_grade_label")
    private String predictedGradeLabel;

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

    @JsonProperty("handler_user_id")
    private Integer handlerUserId;

    @JsonProperty("handler_username")
    private String handlerUsername;

    @JsonProperty("handler_real_name")
    private String handlerRealName;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
