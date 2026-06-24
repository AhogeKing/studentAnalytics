package com.ahogeking.studentanalytics.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ParentalSupportEnum {
    NONE(0, "无"),
    LOW(1, "低"),
    MODERATE(2, "中等"),
    HIGH(3, "高"),
    VERY_HIGH(4, "极高");

    private final Integer code;
    private final String label;

    public static OptionVO<Integer> toOption(Integer code) {
        for (ParentalSupportEnum supportEnum : values()) {
            if (supportEnum.getCode().equals(code)) {
                return OptionVO.of(supportEnum.getCode(), supportEnum.getLabel());
            }
        }
        return OptionVO.unknown();
    }
}
