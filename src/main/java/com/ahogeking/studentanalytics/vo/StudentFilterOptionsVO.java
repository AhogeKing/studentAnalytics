package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StudentFilterOptionsVO {
    private List<ClassInfoVO> classes;

    private List<OptionVO<Integer>> genders;

    private List<OptionVO<Integer>> gradeClasses;

    @JsonProperty("min_gpa")
    private BigDecimal minGpa;

    @JsonProperty("max_gpa")
    private BigDecimal maxGpa;
}
