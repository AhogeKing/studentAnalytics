package com.ahogeking.studentanalytics.dto.row;

import lombok.Data;

// 通用分类计数 Row，GPA 区间统计和 GradeClass 统计都只是：某个分类值 + 人数
@Data
public class AnalysisCategoryCountRow {

    /**
     * GPA 接口中表示 bucketIndex
     * GradeClass 接口中表示 gradeClass 编码
     */
    private Integer categoryValue;

    private Long studentCount;
}
