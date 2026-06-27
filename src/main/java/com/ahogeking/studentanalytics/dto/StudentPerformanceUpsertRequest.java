package com.ahogeking.studentanalytics.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentPerformanceUpsertRequest {
    @JsonProperty("study_time_weekly")
    @JsonAlias("studyTimeWeekly")
    @NotNull(message = "每周学习时长不能为空")
    @DecimalMin(value = "0.0", message = "每周学习时长不能小于0")
    @DecimalMax(value = "60.0", message = "每周学习时长不能超过60")
    private BigDecimal studyTimeWeekly;

    @NotNull(message = "缺勤次数不能为空")
    @Min(value = 0, message = "缺勤次数不能小于0")
    @Max(value = 30, message = "缺勤次数不能超过30")
    private Integer absences;

    @NotNull(message = "课外辅导不能为空")
    private Boolean tutoring;

    @JsonProperty("parental_support")
    @JsonAlias("parentalSupport")
    @NotNull(message = "父母支持程度不能为空")
    @Min(value = 0, message = "父母支持程度只能是0到4")
    @Max(value = 4, message = "父母支持程度只能是0到4")
    private Integer parentalSupport;

    @NotNull(message = "课外活动不能为空")
    private Boolean extracurricular;

    @NotNull(message = "体育活动不能为空")
    private Boolean sports;

    @NotNull(message = "音乐活动不能为空")
    private Boolean music;

    @NotNull(message = "志愿活动不能为空")
    private Boolean volunteering;

    @NotNull(message = "GPA不能为空")
    @DecimalMin(value = "0.0", message = "GPA不能小于0")
    @DecimalMax(value = "4.0", message = "GPA不能超过4")
    private BigDecimal gpa;

    @JsonProperty("grade_class")
    @JsonAlias("gradeClass")
    private Integer gradeClass;
}
