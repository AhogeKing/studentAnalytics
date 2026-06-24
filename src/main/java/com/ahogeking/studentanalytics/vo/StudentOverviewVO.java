package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class StudentOverviewVO {
    @JsonProperty("class_name")
    private String className;

    @JsonProperty("raw_class_name")
    private String rawClassName;

    @JsonProperty("class_names")
    private List<String> classNames;

    private String keyword;

    @JsonProperty("min_gpa")
    private BigDecimal minGpa;

    @JsonProperty("max_gpa")
    private BigDecimal maxGpa;

    @JsonProperty("grade_class")
    private Integer gradeClass;

    @JsonProperty("grade_level")
    private Integer gradeLevel;

    private Integer gender;

    private String sortField;

    private String sortOrder;

    @JsonProperty("student_count")
    private Long studentCount;

    private List<StudentOverviewItemVO> students;
}
