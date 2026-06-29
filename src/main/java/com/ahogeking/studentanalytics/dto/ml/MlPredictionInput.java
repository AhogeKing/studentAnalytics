package com.ahogeking.studentanalytics.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MlPredictionInput {
    @JsonProperty("Age")
    private Integer age;

    @JsonProperty("Gender")
    private Integer gender;

    @JsonProperty("Ethnicity")
    private Integer ethnicity;

    @JsonProperty("ParentalEducation")
    private Integer parentalEducation;

    @JsonProperty("StudyTimeWeekly")
    private BigDecimal studyTimeWeekly;

    @JsonProperty("Absences")
    private Integer absences;

    @JsonProperty("Tutoring")
    private Integer tutoring;

    @JsonProperty("ParentalSupport")
    private Integer parentalSupport;

    @JsonProperty("Extracurricular")
    private Integer extracurricular;

    @JsonProperty("Sports")
    private Integer sports;

    @JsonProperty("Music")
    private Integer music;

    @JsonProperty("Volunteering")
    private Integer volunteering;
}
