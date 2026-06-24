package com.ahogeking.studentanalytics.dto.row;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentDetailRow {
    // 学习行为 + 支持情况 + 活动情况
    // student_performance 表
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
