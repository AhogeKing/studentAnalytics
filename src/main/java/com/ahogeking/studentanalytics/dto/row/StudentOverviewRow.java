package com.ahogeking.studentanalytics.dto.row;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentOverviewRow {
    // 学生基本信息 + GPA 摘要
    // student 表
    private Integer studentNo;
    private String name;
    private Integer age;
    private Integer gender;
    private Integer gradeLevel;
    private String className;
    private BigDecimal gpa;
    private Integer gradeClass;
    private LocalDateTime updateTime;
}
