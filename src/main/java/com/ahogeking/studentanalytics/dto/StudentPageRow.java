package com.ahogeking.studentanalytics.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentPageRow {
    private Integer studentNo;
    private String name;
    private Integer age;
    private Integer gender;
    private Integer gradeLevel;
    private String className;
    private Float gpa;
    private Integer gradeClass;
    private LocalDateTime updateTime;
}
