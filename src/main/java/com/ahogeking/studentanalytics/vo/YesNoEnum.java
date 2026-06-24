package com.ahogeking.studentanalytics.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum YesNoEnum {
    NO(0, "否", false),
    YES(1, "是", true);

    private final Integer code;
    private final String label;
    private final Boolean boolValue;

    public static Boolean toBoolean(Integer code) {
        for (YesNoEnum yesNoEnum : values()) {
            if (yesNoEnum.getCode().equals(code)) {
                return yesNoEnum.getBoolValue();
            }
        }
        return null;
    }

    public static OptionVO<Integer> toOption(Integer code) {
        for (YesNoEnum yesNoEnum : values()) {
            if (yesNoEnum.getCode().equals(code)) {
                return OptionVO.of(yesNoEnum.getCode(), yesNoEnum.getLabel());
            }
        }
        return OptionVO.unknown();
    }
}
