package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PredictionResultVO {
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

    @JsonProperty("dataset_split")
    private String datasetSplit;

    @JsonProperty("dataset_split_label")
    private String datasetSplitLabel;

    private List<PredictionProbabilityVO> probabilities;

    @JsonProperty("important_factors")
    private List<ImportantFactorVO> importantFactors;

    @JsonProperty("predict_input")
    private JsonNode predictInput;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
