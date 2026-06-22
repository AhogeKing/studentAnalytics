package com.ahogeking.studentanalytics.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Student {
    private Integer id;
    private Integer studentNo;
    private String name;
    private Integer age;
    private Integer gender;
    private Integer ethnicity;
    private Integer parentalEducation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}
