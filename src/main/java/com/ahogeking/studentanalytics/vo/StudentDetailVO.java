package com.ahogeking.studentanalytics.vo;

import com.ahogeking.studentanalytics.dto.row.StudentDetailAggregateRow;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StudentDetailVO {
    @JsonProperty("basic_info")
    private StudentBasicInfoVO basicInfo;

    @JsonProperty("academic_performance")
    private AcademicPerformanceVO academicPerformance;

    @JsonProperty("support_status")
    private SupportStatusVO supportStatus;

    @JsonProperty("activity_profile")
    private ActivityProfileVO activityProfile;

    @JsonProperty("performance_available")
    private Boolean performanceAvailable;   // student_performance 是否存在

    public static StudentDetailVO from(StudentDetailAggregateRow row) {
        if (row == null) {
            return null;
        }

        StudentDetailVO vo = new StudentDetailVO();
        vo.setBasicInfo(StudentBasicInfoVO.from(row.getOverview()));
        vo.setAcademicPerformance(AcademicPerformanceVO.from(row.getOverview(), row.getDetail()));
        vo.setSupportStatus(SupportStatusVO.from(row.getDetail()));
        vo.setActivityProfile(ActivityProfileVO.from(row.getDetail()));
        vo.setPerformanceAvailable(Boolean.TRUE.equals(row.getPerformanceAvailable()));
        return vo;
    }
}
