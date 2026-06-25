package com.ahogeking.studentanalytics.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpaDistributionItemVO {
    private Integer bucketIndex;
    private String label;

    // 这里保留 minGpa 和 maxGpa，不仅返回标签，后续前端需要配置 GPA 分段颜色时也能直接使用。
    /**
     * 前 7 个区间：左闭右开
     * 最后一个区间：左右均闭
     */
    private BigDecimal minGpa;
    private BigDecimal maxGpa;

    private Long studentCount;
    private BigDecimal percentage;
}
