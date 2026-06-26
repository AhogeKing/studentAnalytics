package com.ahogeking.studentanalytics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceAnalysisPointVO {
    /**
     * 这里返回学号和姓名，是为了支持：
     * 鼠标悬浮显示学生信息
     * 点击散点跳转学生详情
     */
    private Integer studentNo;
    private String name;

    private BigDecimal studyTimeWeekly;
    private Integer absences;
    private BigDecimal gpa;
    private OptionVO<Integer> gradeClass;

    /**
     * GPA 所属区间，例如：
     * value = 7
     * label = "[3.5, 4.0]"
     */
    private OptionVO<Integer> gpaBucket;
}
