package com.ahogeking.studentanalytics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeClassDistributionItemVO {
    /**
     * 响应中自然形成：
     * {
     *   "grade_class": {
     *     "value": 0,
     *     "label": "优秀"
     *   }
     * }
     */
    private OptionVO<Integer> gradeClass;
    private Long studentCount;
    private BigDecimal percentage;
}
