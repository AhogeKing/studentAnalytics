package com.ahogeking.studentanalytics.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GradeClassEnum {
    A(0, "优秀"),
    B(1, "良好"),
    C(2, "中等"),
    D(3, "较差"),
    F(4, "风险");

    private final Integer code;
    private final String label;

    public static OptionVO<Integer> toOption(Integer code) {
        for (GradeClassEnum gradeClassEnum : values()) {
            if (gradeClassEnum.getCode().equals(code)) {
                return OptionVO.of(gradeClassEnum.getCode(), gradeClassEnum.getLabel());
            }
        }
        return OptionVO.unknown();
    }
}
