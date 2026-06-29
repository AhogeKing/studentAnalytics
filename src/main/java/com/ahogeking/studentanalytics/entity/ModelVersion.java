package com.ahogeking.studentanalytics.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ModelVersion {
    private Integer id;
    private String modelName;
    private String versionNo;
    private String algorithm;
    private String featureColumns;
    private String targetColumn;
    private String criterion;
    private Integer maxDepth;
    private Integer minSamplesLeaf;
    private BigDecimal accuracy;
    private BigDecimal precisionMacro;
    private BigDecimal recallMacro;
    private BigDecimal f1Macro;
    private String confusionMatrixJson;
    private String modelPath;
    private String encoderPath;
    private Integer isActive;
    private LocalDateTime trainedAt;
    private Integer createdBy;
    private LocalDateTime createdAt;
}
