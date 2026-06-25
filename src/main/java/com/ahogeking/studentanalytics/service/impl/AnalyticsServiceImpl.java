package com.ahogeking.studentanalytics.service.impl;

import com.ahogeking.studentanalytics.dto.row.AnalysisCategoryCountRow;
import com.ahogeking.studentanalytics.dto.row.PerformanceAnalysisPointRow;
import com.ahogeking.studentanalytics.mapper.AnalysisMapper;
import com.ahogeking.studentanalytics.service.AnalyticsService;
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

    private static final List<GpaBucket> GPA_BUCKETS = List.of(
            new GpaBucket(0, "[0.0, 0.5)",
                    new BigDecimal("0.0"),
                    new BigDecimal("0.5")),
            new GpaBucket(1, "[0.5, 1.0)",
                    new BigDecimal("0.5"),
                    new BigDecimal("1.0")),
            new GpaBucket(2, "[1.0, 1.5)",
                    new BigDecimal("1.0"),
                    new BigDecimal("1.5")),
            new GpaBucket(3, "[1.5, 2.0)",
                    new BigDecimal("1.5"),
                    new BigDecimal("2.0")),
            new GpaBucket(4, "[2.0, 2.5)",
                    new BigDecimal("2.0"),
                    new BigDecimal("2.5")),
            new GpaBucket(5, "[2.5, 3.0)",
                    new BigDecimal("2.5"),
                    new BigDecimal("3.0")),
            new GpaBucket(6, "[3.0, 3.5)",
                    new BigDecimal("3.0"),
                    new BigDecimal("3.5")),
            new GpaBucket(7, "[3.5, 4.0]",
                    new BigDecimal("3.5"),
                    new BigDecimal("4.0"))
    );

    private final AnalysisMapper analysisMapper;

    @Override
    public List<GpaDistributionItemVO> selectGpaDistributionItems() {
        Map<Integer, Long> countMap = toCountMap(analysisMapper.selectGpaBucketCounts());

        long total = countMap.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        return GPA_BUCKETS.stream()
                .map(bucket -> {
                    long count = countMap.getOrDefault(bucket.index(), 0L);
                    return new GpaDistributionItemVO(
                            bucket.index(),
                            bucket.label(),
                            bucket.minGpa(),
                            bucket.maxGpa(),
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
                GradeClassEnum.toOption(row.getGradeClass())
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

    private record GpaBucket(
            int index,
            String label,
            BigDecimal minGpa,
            BigDecimal maxGpa
    ) {
    }
}
