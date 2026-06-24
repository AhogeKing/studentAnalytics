package com.ahogeking.studentanalytics.vo;

import com.ahogeking.studentanalytics.dto.row.StudentDetailRow;
import com.ahogeking.studentanalytics.dto.row.StudentOverviewRow;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AcademicPerformanceVO {
    // 前两个属性来自 StudentOverviewRow
    private BigDecimal gpa;

    @JsonProperty("grade_class")
    private OptionVO<Integer> gradeClass;

    @JsonProperty("study_time_weekly")
    private BigDecimal studyTimeWeekly;

    private Integer absences;

    public static AcademicPerformanceVO from(StudentOverviewRow overview, StudentDetailRow detail) {
        if (overview == null && detail == null) {
            return null;
        }

        AcademicPerformanceVO vo = new AcademicPerformanceVO();
        if (overview != null) {
            vo.setGpa(overview.getGpa());
            vo.setGradeClass(GradeClassEnum.toOption(overview.getGradeClass()));
        }
        if (detail != null) {
            vo.setStudyTimeWeekly(detail.getStudyTimeWeekly());
            vo.setAbsences(detail.getAbsences());
        }
        return vo;
    }
}
