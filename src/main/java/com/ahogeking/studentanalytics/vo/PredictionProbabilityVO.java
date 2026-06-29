package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionProbabilityVO {
    @JsonProperty("grade_class")
    private OptionVO<Integer> gradeClass;

    @JsonProperty("grade_label")
    private String gradeLabel;

    private BigDecimal probability;
}
