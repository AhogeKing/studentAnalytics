package com.ahogeking.studentanalytics.vo;

import com.ahogeking.studentanalytics.dto.row.StudentDetailRow;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SupportStatusVO {
    // 全部来自 StudentDetailRow
    private Boolean tutoring;

    @JsonProperty("parental_support")
    private OptionVO<Integer> parentalSupport;

    public static SupportStatusVO from(StudentDetailRow row) {
        if (row == null) {
            return null;
        }

        SupportStatusVO vo = new SupportStatusVO();
        vo.setTutoring(YesNoEnum.toBoolean(row.getTutoring()));
        vo.setParentalSupport(ParentalSupportEnum.toOption(row.getParentalSupport()));
        return vo;
    }
}
