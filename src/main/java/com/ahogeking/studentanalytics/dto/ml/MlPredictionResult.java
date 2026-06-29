package com.ahogeking.studentanalytics.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class MlPredictionResult {
    @JsonProperty("predicted_grade_class")
    private Integer predictedGradeClass;

    @JsonProperty("predicted_grade_label")
    private String predictedGradeLabel;

    private Map<String, BigDecimal> probabilities;

    @JsonProperty("important_features")
    private List<MlImportantFeatureItem> importantFeatures;
}
