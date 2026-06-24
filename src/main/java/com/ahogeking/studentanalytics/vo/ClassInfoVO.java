package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClassInfoVO {
    @JsonProperty("grade_level")
    private Integer gradeLevel;

    @JsonProperty("class_name")
    private String className;

    @JsonProperty("raw_class_name")
    private String rawClassName;

    public ClassInfoVO(Integer gradeLevel, String className) {
        this.gradeLevel = gradeLevel;
        this.rawClassName = className;
        this.className = formatClassName(className);
    }

    public static ClassInfoVO fromRaw(Integer gradeLevel, String className) {
        return new ClassInfoVO(gradeLevel, className);
    }

    public void setClassName(String className) {
        this.rawClassName = className;
        this.className = formatClassName(className);
    }

    private static String formatClassName(String rawClassName) {
        if (rawClassName == null || rawClassName.isBlank()) {
            return rawClassName;
        }

        String[] parts = rawClassName.trim().split("-");
        if (parts.length != 2) {
            return rawClassName;
        }

        Integer gradeLevel = parseInteger(parts[0]);
        Integer classNo = parseInteger(parts[1]);
        if (gradeLevel == null || classNo == null) {
            return rawClassName;
        }

        String gradeName = switch (gradeLevel) {
            case 1 -> "高一";
            case 2 -> "高二";
            case 3 -> "高三";
            default -> null;
        };

        return gradeName == null
                ? rawClassName
                : gradeName + " " + classNo + " 班";
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
