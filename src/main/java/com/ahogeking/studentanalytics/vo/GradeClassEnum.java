package com.ahogeking.studentanalytics.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GradeClassEnum {
    A(0, "A"),
    B(1, "B"),
    C(2, "C"),
    D(3, "D"),
    F(4, "F");

    private final Integer code;
    private final String label;

    public static OptionVO toOption(Integer code) {
        for (GradeClassEnum gradeClassEnum : values()) {
            if (gradeClassEnum.getCode().equals(code)) {
                return OptionVO.of(gradeClassEnum.getCode(), gradeClassEnum.getLabel());
            }
        }
        return OptionVO.unknown();
    }
}
