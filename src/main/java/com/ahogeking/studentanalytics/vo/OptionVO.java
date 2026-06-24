package com.ahogeking.studentanalytics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionVO<T> {
    private T value;
    private String label;

    public static <T> OptionVO<T> of(T value, String label) {
        return new OptionVO<>(value, label);
    }

    public static <T> OptionVO<T> unknown() {
        return new OptionVO<>(null, "未知");
    }
}
