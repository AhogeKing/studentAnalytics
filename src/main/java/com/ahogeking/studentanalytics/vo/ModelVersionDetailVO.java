package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ModelVersionDetailVO {
    private Integer id;
    private String modelName;
    private String versionNo;
    private String algorithm;
    private JsonNode featureColumns;
    private String targetColumn;
    private String criterion;
    private Integer maxDepth;
    private Integer minSamplesLeaf;
    private BigDecimal accuracy;
    private BigDecimal precisionMacro;
    private BigDecimal recallMacro;
    private BigDecimal f1Macro;
    private JsonNode confusionMatrix;
    private JsonNode metrics;
    private String modelPath;
    private Boolean active;
    private LocalDateTime trainedAt;
    private Long trainingDurationMs;
    private LocalDateTime createdAt;
}
