package com.ahogeking.studentanalytics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportantFactorVO {
    private String feature;
    private String label;
    private Object value;
    private BigDecimal importance;
}
