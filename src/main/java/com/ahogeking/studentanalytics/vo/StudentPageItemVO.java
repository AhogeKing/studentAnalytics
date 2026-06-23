package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentPageItemVO {

    @JsonProperty("student_no")
    private Integer studentNo;

    private String name;

    private Integer age;

    private OptionVO gender;

    @JsonProperty("grade_level")
    private Integer gradeLevel;

    @JsonProperty("class_info")
    private ClassInfoVO classInfo;

    private Float gpa;

    @JsonProperty("grade_class")
    private OptionVO gradeClass;

    @JsonProperty("update_time")
    private LocalDateTime updateTime;
}
