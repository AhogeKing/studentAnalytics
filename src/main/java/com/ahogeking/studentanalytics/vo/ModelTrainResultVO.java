package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ModelTrainResultVO {
    private Integer modelVersionId;
    private String versionNo;
    private String modelName;
    private String algorithm;
    private String targetColumn;
    private JsonNode featureColumns;
    private String searchMode;
    private Integer searchCandidates;
    private Integer trainRows;
    private Integer testRows;
    private JsonNode bestParameters;
    private BigDecimal accuracy;
    private BigDecimal precisionMacro;
    private BigDecimal recallMacro;
    private BigDecimal f1Macro;
    private BigDecimal aucOvrMacro;
    private JsonNode confusionMatrix;
    private String modelPath;
    private Boolean active;
    private LocalDateTime trainedAt;
}
