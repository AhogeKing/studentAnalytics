package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentOverviewItemVO {

    @JsonProperty("student_no")
    private Integer studentNo;

    private String name;

    private Integer age;

    private OptionVO<Integer> gender;

    @JsonProperty("grade_level")
    private Integer gradeLevel;

    @JsonProperty("class_info")
    private ClassInfoVO classInfo;

    private BigDecimal gpa;

    @JsonProperty("grade_class")
    private OptionVO<Integer> gradeClass;

    @JsonProperty("update_time")
    private LocalDateTime updateTime;
}
