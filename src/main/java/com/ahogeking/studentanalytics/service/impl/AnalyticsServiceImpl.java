package com.ahogeking.studentanalytics.service.impl;

import com.ahogeking.studentanalytics.dto.row.AnalysisCategoryCountRow;
import com.ahogeking.studentanalytics.dto.row.PerformanceAnalysisPointRow;
import com.ahogeking.studentanalytics.mapper.AnalysisMapper;
import com.ahogeking.studentanalytics.service.AnalyticsService;
import com.ahogeking.studentanalytics.vo.GpaBucketEnum;
import com.ahogeking.studentanalytics.vo.GradeClassEnum;
import com.ahogeking.studentanalytics.vo.GpaDistributionItemVO;
import com.ahogeking.studentanalytics.vo.GradeClassDistributionItemVO;
import com.ahogeking.studentanalytics.vo.PerformanceAnalysisPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final AnalysisMapper analysisMapper;

    @Override
    public List<GpaDistributionItemVO> selectGpaDistributionItems() {
        Map<Integer, Long> countMap = toCountMap(analysisMapper.selectGpaBucketCounts());

        long total = countMap.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        return Arrays.stream(GpaBucketEnum.values())
                .sorted((left, right) -> Integer.compare(left.getCode(), right.getCode()))
                .map(bucket -> {
                    long count = countMap.getOrDefault(bucket.getCode(), 0L);
                    return new GpaDistributionItemVO(
                            bucket.getCode(),
                            bucket.getLabel(),
                            bucket.getMin(),
                            bucket.getMax(),
                            count,
                            calculatePercentage(count, total)
                    );
                })
                .toList();
    }

    @Override
    public List<GradeClassDistributionItemVO> selectGradeClassDistributionItems() {
        Map<Integer, Long> countMap = toCountMap(analysisMapper.selectGradeClassCounts());

        long total = countMap.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        return Arrays.stream(GradeClassEnum.values())
                .sorted((left, right) -> Integer.compare(left.getCode(), right.getCode()))
                .map(gradeClass -> {
                    long count = countMap.getOrDefault(gradeClass.getCode(), 0L);
                    return new GradeClassDistributionItemVO(
                            GradeClassEnum.toOption(gradeClass.getCode()),
                            count,
                            calculatePercentage(count, total)
                    );
                })
                .toList();
    }

    @Override
    public List<PerformanceAnalysisPointVO> selectPerformanceAnalysisPoints() {
        return analysisMapper.selectPerformanceAnalysisPoints()
                .stream()
                .map(this::toPerformancePointVO)
                .toList();
    }

    private PerformanceAnalysisPointVO toPerformancePointVO(PerformanceAnalysisPointRow row) {
        return new PerformanceAnalysisPointVO(
                row.getStudentNo(),
                row.getName(),
                row.getStudyTimeWeekly(),
                row.getAbsences(),
                row.getGpa(),
                GradeClassEnum.toOption(row.getGradeClass()),
                GpaBucketEnum.toOption(row.getGpa())
        );
    }

    private Map<Integer, Long> toCountMap(List<AnalysisCategoryCountRow> rows) {
        return rows.stream()
                .filter(row -> row.getCategoryValue() != null)
                .collect(Collectors.toMap(
                        AnalysisCategoryCountRow::getCategoryValue,
                        row -> row.getStudentCount() == null ? 0L : row.getStudentCount(),
                        Long::sum
                ));
    }

    private BigDecimal calculatePercentage(long count, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        return BigDecimal.valueOf(count)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

}
