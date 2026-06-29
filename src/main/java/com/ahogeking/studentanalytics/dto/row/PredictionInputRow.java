package com.ahogeking.studentanalytics.dto.row;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PredictionInputRow {
    private Integer studentId;
    private Integer studentNo;
    private String name;
    private Integer gradeLevel;
    private String className;
    private Integer performanceId;
    private Integer age;
    private Integer gender;
    private Integer ethnicity;
    private Integer parentalEducation;
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
}
