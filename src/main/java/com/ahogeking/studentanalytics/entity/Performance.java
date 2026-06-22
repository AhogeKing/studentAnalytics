package com.ahogeking.studentanalytics.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Performance {
    private Integer id;
    private Integer studentId;
    private BigDecimal studyTimeWeekly;
    private Integer absences;
    private Integer tutoring;
    private Integer parentalSupport;
    private Integer extracurricular;
    private Integer sports;
    private Integer music;
    private Integer volunteering;
    private BigDecimal gpa;
    private Integer gradeClass;
    private String dataSource;
    private Integer dataQualityStatus;
    private String qualityIssue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
