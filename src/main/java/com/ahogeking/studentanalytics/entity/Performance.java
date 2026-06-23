package com.ahogeking.studentanalytics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_performance")
public class Performance {
    private Integer id;

    @JsonProperty("student_id")
    private Integer studentId;

    @JsonProperty("study_time_weekly")
    private Float studyTimeWeekly;

    private Integer absences;

    private Integer tutoring;

    @JsonProperty("parental_support")
    private Integer parentalSupport;

    private Integer extracurricular;

    private Integer sports;

    private Integer music;

    private Integer volunteering;

    private Float gpa;

    @JsonProperty("grade_class")
    private Integer gradeClass;

    @JsonProperty("data_source")
    private String dataSource;

    @JsonProperty("data_quality_status")
    private Integer dataQualityStatus;

    @JsonProperty("quality_issue")
    private String qualityIssue;

    @JsonIgnore
    private LocalDateTime createdAt;

    @JsonIgnore
    private LocalDateTime updatedAt;
}
