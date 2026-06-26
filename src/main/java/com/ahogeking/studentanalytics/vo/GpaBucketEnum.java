package com.ahogeking.studentanalytics.vo;

import com.ahogeking.studentanalytics.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum GpaBucketEnum {

    RANGE_0(0, "[0.0, 0.5)",
            new BigDecimal("0.0"),
            new BigDecimal("0.5"), false),

    RANGE_1(1, "[0.5, 1.0)",
            new BigDecimal("0.5"),
            new BigDecimal("1.0"), false),

    RANGE_2(2, "[1.0, 1.5)",
            new BigDecimal("1.0"),
            new BigDecimal("1.5"), false),

    RANGE_3(3, "[1.5, 2.0)",
            new BigDecimal("1.5"),
            new BigDecimal("2.0"), false),

    RANGE_4(4, "[2.0, 2.5)",
            new BigDecimal("2.0"),
            new BigDecimal("2.5"), false),

    RANGE_5(5, "[2.5, 3.0)",
            new BigDecimal("2.5"),
            new BigDecimal("3.0"), false),

    RANGE_6(6, "[3.0, 3.5)",
            new BigDecimal("3.0"),
            new BigDecimal("3.5"), false),

    RANGE_7(7, "[3.5, 4.0]",
            new BigDecimal("3.5"),
            new BigDecimal("4.0"), true);

    private final Integer code;
    private final String label;
    private final BigDecimal min;
    private final BigDecimal max;
    private final boolean includeMax;

    public boolean contains(BigDecimal gpa) {
        if (gpa == null) {
            return false;
        }

        boolean greaterThanOrEqualMin =
                gpa.compareTo(min) >= 0;

        boolean lessThanMax = includeMax
                ? gpa.compareTo(max) <= 0
                : gpa.compareTo(max) < 0;

        return greaterThanOrEqualMin && lessThanMax;
    }

    public static GpaBucketEnum fromGpa(BigDecimal gpa) {
        return Arrays.stream(values())
                .filter(bucket -> bucket.contains(gpa))
                .findFirst()
                .orElseThrow(() ->
                        new BusinessException("GPA超出合法范围")
                );
    }

    public static OptionVO<Integer> toOption(BigDecimal gpa) {
        GpaBucketEnum bucket = fromGpa(gpa);
        return new OptionVO<>(
                bucket.getCode(),
                bucket.getLabel()
        );
    }
}
