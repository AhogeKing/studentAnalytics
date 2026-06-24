package com.ahogeking.studentanalytics.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StudentOverviewQueryRequest {
    private String className;
    private List<String> classNames;
    private String keyword;
    private BigDecimal minGpa;
    private BigDecimal maxGpa;
    private Integer gradeClass;
    private Integer gradeLevel;
    private Integer gender;
    private String sortField;
    private String sortOrder;
}
