package com.ahogeking.studentanalytics.dto.ml;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MlImportantFeatureItem {
    private String feature;
    private BigDecimal importance;
}
