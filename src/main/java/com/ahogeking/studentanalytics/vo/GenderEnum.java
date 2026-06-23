package com.ahogeking.studentanalytics.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GenderEnum {
    // Dataset code: 0 = Male, 1 = Female.
    MALE(0, "男"),
    FEMALE(1, "女");

    private final Integer code;
    private final String label;

    public static OptionVO toOption(Integer code) {
        for (GenderEnum genderEnum : values()) {
            if (genderEnum.getCode().equals(code)) {
                return OptionVO.of(genderEnum.getCode(), genderEnum.getLabel());
            }
        }
        return OptionVO.unknown();
    }
}
