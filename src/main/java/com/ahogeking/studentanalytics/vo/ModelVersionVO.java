package com.ahogeking.studentanalytics.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ModelVersionVO {
    private Integer id;
    private String modelName;
    private String versionNo;
    private String algorithm;
    private BigDecimal accuracy;
    private BigDecimal precisionMacro;
    private BigDecimal recallMacro;
    private BigDecimal f1Macro;
    private Boolean active;
    private LocalDateTime trainedAt;
    private LocalDateTime createdAt;
}
