package com.ahogeking.studentanalytics.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentCreateRequest {
    @JsonProperty("student_no")
    @JsonAlias("studentNo")
    @NotNull(message = "学生编号不能为空")
    private Integer studentNo;

    @Size(max = 50, message = "学生姓名长度不能超过50个字符")
    private String name;

    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 30, message = "年龄不能超过30")
    private Integer age;

    @NotNull(message = "性别不能为空")
    @Min(value = 0, message = "性别只能是0或1")
    @Max(value = 1, message = "性别只能是0或1")
    private Integer gender;

    @NotNull(message = "民族不能为空")
    @Min(value = 0, message = "民族只能是0到3")
    @Max(value = 3, message = "民族只能是0到3")
    private Integer ethnicity;

    @JsonProperty("parental_education")
    @JsonAlias("parentalEducation")
    @NotNull(message = "父母教育程度不能为空")
    @Min(value = 0, message = "父母教育程度只能是0到4")
    @Max(value = 4, message = "父母教育程度只能是0到4")
    private Integer parentalEducation;

    @JsonProperty("class_name")
    @JsonAlias("className")
    private String className;
}
