package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PredictionEligibilityVO {
    @JsonProperty("student_no")
    private Integer studentNo;

    @JsonProperty("model_version_id")
    private Integer modelVersionId;

    @JsonProperty("model_version_no")
    private String modelVersionNo;

    @JsonProperty("dataset_split")
    private String datasetSplit;

    @JsonProperty("dataset_split_label")
    private String datasetSplitLabel;

    @JsonProperty("can_predict")
    private Boolean canPredict;

    private String reason;
}
