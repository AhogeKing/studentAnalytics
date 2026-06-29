package com.ahogeking.studentanalytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PredictionRequest {
    @JsonProperty("model_version_id")
    private Integer modelVersionId;

    @JsonProperty("generate_warning")
    private Boolean generateWarning = true;
}
