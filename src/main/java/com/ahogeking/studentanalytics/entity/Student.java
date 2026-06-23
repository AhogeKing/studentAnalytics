package com.ahogeking.studentanalytics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student")
public class Student {
    private Integer id;

    @JsonProperty("student_no")
    private Integer studentNo;

    private String name;

    private Integer age;

    @JsonProperty("grade_level")
    private Integer gradeLevel;

    @JsonProperty("class_name")
    private String className;

    private Integer gender;

    private Integer ethnicity;

    @JsonProperty("parental_education")
    private Integer parentalEducation;

    @JsonIgnore
    private LocalDateTime createdAt;

    @JsonIgnore
    private LocalDateTime updatedAt;

    @JsonIgnore
    private Integer deleted;
}
