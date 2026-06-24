package com.ahogeking.studentanalytics.vo;

import com.ahogeking.studentanalytics.dto.row.StudentDetailRow;
import lombok.Data;

@Data
public class ActivityProfileVO {
    // 全部来自 StudentDetailRow
    private Boolean extracurricular;

    private Boolean sports;

    private Boolean music;

    private Boolean volunteering;

    public static ActivityProfileVO from(StudentDetailRow row) {
        if (row == null) {
            return null;
        }

        ActivityProfileVO vo = new ActivityProfileVO();
        vo.setExtracurricular(YesNoEnum.toBoolean(row.getExtracurricular()));
        vo.setSports(YesNoEnum.toBoolean(row.getSports()));
        vo.setMusic(YesNoEnum.toBoolean(row.getMusic()));
        vo.setVolunteering(YesNoEnum.toBoolean(row.getVolunteering()));
        return vo;
    }
}
