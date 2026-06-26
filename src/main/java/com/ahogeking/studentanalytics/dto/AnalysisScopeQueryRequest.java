package com.ahogeking.studentanalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisScopeQueryRequest {

    /**
     * 1=高一，2=高二，3=高三
     * null 表示不限制年级
     */
    private Integer gradeLevel;

    /**
     * 数据库存储的原始班级值，例如：
     * ["1-1", "1-2", "1-3"]
     */
    private List<String> classNames;
}
