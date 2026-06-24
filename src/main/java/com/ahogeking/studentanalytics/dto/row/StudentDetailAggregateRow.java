package com.ahogeking.studentanalytics.dto.row;

import lombok.Data;

@Data
public class StudentDetailAggregateRow {
    // 概览数据
    private StudentOverviewRow overview;

    // 额外详情数据
    private StudentDetailRow detail;

    private Boolean performanceAvailable;
}
