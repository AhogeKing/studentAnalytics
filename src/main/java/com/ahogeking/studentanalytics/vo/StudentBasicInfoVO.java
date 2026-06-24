package com.ahogeking.studentanalytics.vo;

import com.ahogeking.studentanalytics.dto.row.StudentOverviewRow;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StudentBasicInfoVO {
    // 全部来自 StudentOverviewRow
    @JsonProperty("student_no")
    private Integer studentNo;

    private String name;

    private Integer age;

    private OptionVO<Integer> gender;

    @JsonProperty("grade_level")
    private Integer gradeLevel;

    @JsonProperty("class_info")
    private ClassInfoVO classInfo;

    public static StudentBasicInfoVO from(StudentOverviewRow row) {
        if (row == null) {
            return null;
        }

        StudentBasicInfoVO vo = new StudentBasicInfoVO();
        vo.setStudentNo(row.getStudentNo());
        vo.setName(row.getName());
        vo.setAge(row.getAge());
        vo.setGender(GenderEnum.toOption(row.getGender()));
        vo.setGradeLevel(row.getGradeLevel());
        vo.setClassInfo(ClassInfoVO.fromRaw(row.getGradeLevel(), row.getClassName()));
        return vo;
    }
}
