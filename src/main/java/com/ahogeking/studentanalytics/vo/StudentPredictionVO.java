package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StudentPredictionVO {
    @JsonProperty("student_no")
    private Integer studentNo;

    private String name;

    @JsonProperty("class_info")
    private ClassInfoVO classInfo;

    private PredictionResultVO prediction;

    private WarningRecordVO warning;
}
