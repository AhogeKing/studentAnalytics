package com.ahogeking.studentanalytics.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentOverviewUpdateRequest {
    @Size(max = 50, message = "学生姓名长度不能超过50个字符")
    private String name;

    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 30, message = "年龄不能超过30")
    private Integer age;

    @Min(value = 0, message = "性别只能是0或1")
    @Max(value = 1, message = "性别只能是0或1")
    private Integer gender;

    @JsonProperty("grade_level")
    @JsonAlias("gradeLevel")
    @Min(value = 1, message = "年级只能是1、2、3")
    @Max(value = 3, message = "年级只能是1、2、3")
    private Integer gradeLevel;

    @JsonProperty("class_name")
    @JsonAlias("className")
    private String className;

    @DecimalMin(value = "0.0", message = "GPA不能小于0")
    @DecimalMax(value = "4.0", message = "GPA不能超过4")
    private BigDecimal gpa;

    @JsonProperty("grade_class")
    @JsonAlias("gradeClass")
    @Min(value = 0, message = "成绩等级只能是0到4")
    @Max(value = 4, message = "成绩等级只能是0到4")
    private Integer gradeClass;
}
