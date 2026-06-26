package com.ahogeking.studentanalytics.service.impl;

import com.ahogeking.studentanalytics.dto.AnalysisScopeQueryRequest;
import com.ahogeking.studentanalytics.dto.row.AnalysisCategoryCountRow;
import com.ahogeking.studentanalytics.dto.row.PerformanceAnalysisPointRow;
import com.ahogeking.studentanalytics.exception.BusinessException;
import com.ahogeking.studentanalytics.mapper.AnalysisMapper;
import com.ahogeking.studentanalytics.service.AnalyticsService;
import com.ahogeking.studentanalytics.vo.ClassInfoVO;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final AnalysisMapper analysisMapper;

    @Override
    public List<GpaDistributionItemVO> selectGpaDistributionItems(AnalysisScopeQueryRequest queryRequest) {
        AnalysisScope scope = normalizeScope(queryRequest);

        Map<Integer, Long> countMap = toCountMap(analysisMapper.selectGpaBucketCounts(scope.gradeLevel(), scope.classNames()));

        long total = countMap.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        return Arrays.stream(GpaBucketEnum.values())
                .sorted(Comparator.comparingInt(GpaBucketEnum::getCode))
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
    public List<GradeClassDistributionItemVO> selectGradeClassDistributionItems(AnalysisScopeQueryRequest queryRequest) {
        AnalysisScope scope = normalizeScope(queryRequest);
        Map<Integer, Long> countMap = toCountMap(analysisMapper.selectGradeClassCounts(scope.gradeLevel(), scope.classNames()));

        long total = countMap.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        return Arrays.stream(GradeClassEnum.values())
                .sorted(Comparator.comparingInt(GradeClassEnum::getCode))
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
    public List<PerformanceAnalysisPointVO> selectPerformanceAnalysisPoints(AnalysisScopeQueryRequest queryRequest) {
        AnalysisScope scope = normalizeScope(queryRequest);
        return analysisMapper.selectPerformanceAnalysisPoints(scope.gradeLevel(), scope.classNames())
                .stream()
                .map(this::toPerformancePointVO)
                .toList();
    }

    private PerformanceAnalysisPointVO toPerformancePointVO(PerformanceAnalysisPointRow row) {
        PerformanceAnalysisPointVO vo = new PerformanceAnalysisPointVO();
        vo.setStudentNo(row.getStudentNo());
        vo.setName(row.getName());
        vo.setClassInfo(ClassInfoVO.fromRaw(row.getGradeLevel(), row.getClassName()));
        vo.setStudyTimeWeekly(row.getStudyTimeWeekly());
        vo.setAbsences(row.getAbsences());
        vo.setGpa(row.getGpa());
        vo.setGradeClass(GradeClassEnum.toOption(row.getGradeClass()));
        vo.setGpaBucket(GpaBucketEnum.toOption(row.getGpa()));
        return vo;
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

    private record AnalysisScope(Integer gradeLevel, List<String> classNames) {
    }

    private AnalysisScope normalizeScope(AnalysisScopeQueryRequest query) {
        Integer gradeLevel = query == null ? null : query.getGradeLevel();
        if (gradeLevel != null && (gradeLevel < 1 || gradeLevel > 3)) {
            throw new BusinessException("Grade level must be between 1 and 3!");
        }

        List<String> classNames =
                query == null || query.getClassNames() == null
                        ? List.of()
                        : query.getClassNames()
                        .stream()
                        .filter(Objects::nonNull)
                        // 兼容前端偶尔传来的 "1-1,1-2"
                        .flatMap(value -> Arrays.stream(value.split(",")))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .toList();

        if (!classNames.isEmpty() && gradeLevel == null) {
            throw new BusinessException("按班级分析时必须指定年级");
        }

        for (String className : classNames) {
            if (!className.matches("[1-3]-\\d+")) {
                throw new BusinessException("班级格式错误：" + className);
            }

            Integer classGradeLevel = Integer.valueOf(className.substring(0, className.indexOf("-")));
            if (!classGradeLevel.equals(gradeLevel)) {
                throw new BusinessException("班级 " + className + "不属于指定年级");
            }
        }
        return new AnalysisScope(gradeLevel, classNames);
    }
}
