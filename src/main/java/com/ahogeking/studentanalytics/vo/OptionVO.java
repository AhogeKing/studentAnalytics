package com.ahogeking.studentanalytics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionVO {
    private Integer code;
    private String label;

    public static OptionVO of(Integer code, String label) {
        return new OptionVO(code, label);
    }

    public static OptionVO unknown() {
        return new OptionVO(null, "未知");
    }
}
