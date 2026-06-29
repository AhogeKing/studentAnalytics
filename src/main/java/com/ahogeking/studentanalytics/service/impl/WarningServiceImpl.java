package com.ahogeking.studentanalytics.service.impl;

import com.ahogeking.studentanalytics.context.SysUserContext;
import com.ahogeking.studentanalytics.dto.WarningQueryRequest;
import com.ahogeking.studentanalytics.dto.WarningStatusUpdateRequest;
import com.ahogeking.studentanalytics.dto.row.WarningGenerationContextRow;
import com.ahogeking.studentanalytics.dto.row.WarningRecordRow;
import com.ahogeking.studentanalytics.entity.WarningRecord;
import com.ahogeking.studentanalytics.exception.BusinessException;
import com.ahogeking.studentanalytics.mapper.WarningMapper;
import com.ahogeking.studentanalytics.service.WarningService;
import com.ahogeking.studentanalytics.vo.ClassInfoVO;
import com.ahogeking.studentanalytics.vo.GradeClassEnum;
import com.ahogeking.studentanalytics.vo.PageResultVO;
import com.ahogeking.studentanalytics.vo.WarningDetailVO;
import com.ahogeking.studentanalytics.vo.WarningRecordVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WarningServiceImpl implements WarningService {
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private static final String STATUS_UNPROCESSED = "UNPROCESSED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_IGNORED = "IGNORED";

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            STATUS_UNPROCESSED,
            STATUS_PROCESSING,
            STATUS_DONE,
            STATUS_IGNORED
    );

    private static final Set<String> ALLOWED_RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");

    private final WarningMapper warningMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public WarningRecordVO generateWarningFromPrediction(Integer predictionResultId) {
        if (predictionResultId == null || predictionResultId <= 0) {
            throw new BusinessException("预测结果ID不合法");
        }

        WarningRecordRow existing = warningMapper.selectWarningByPredictionResultId(predictionResultId);
        if (existing != null) {
            return toWarningRecordVO(existing);
        }

        WarningGenerationContextRow context = warningMapper.selectWarningGenerationContext(predictionResultId);
        if (context == null) {
            throw new BusinessException("预测结果不存在，无法生成风险预警");
        }

        RiskEvaluation evaluation = evaluateRisk(context);
        WarningRecord record = new WarningRecord();
        record.setStudentId(context.getStudentId());
        record.setPredictionResultId(context.getPredictionResultId());
        record.setRiskScore(evaluation.riskScore());
        record.setRiskLevel(evaluation.riskLevel());
        record.setRiskReasonsJson(toJson(evaluation.reasons()));
        record.setSuggestionJson(toJson(evaluation.suggestions()));
        record.setStatus(STATUS_UNPROCESSED);
        record.setHandlerUserId(null);
        warningMapper.insertWarningRecord(record);

        WarningRecordRow saved = warningMapper.selectWarningRecordById(record.getId());
        return toWarningRecordVO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WarningRecordVO selectWarningByPredictionResultId(Integer predictionResultId) {
        if (predictionResultId == null || predictionResultId <= 0) {
            return null;
        }
        WarningRecordRow row = warningMapper.selectWarningByPredictionResultId(predictionResultId);
        return row == null ? null : toWarningRecordVO(row);
    }

    @Override
    @Transactional(readOnly = true)
    public WarningRecordVO selectLatestWarningByStudentNo(Integer studentNo) {
        if (studentNo == null || studentNo <= 0) {
            throw new BusinessException("学生编号不合法");
        }
        WarningRecordRow row = warningMapper.selectLatestWarningByStudentNo(studentNo);
        if (row == null) {
            throw new BusinessException("该学生暂无风险预警记录");
        }
        return toWarningRecordVO(row);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResultVO<WarningRecordVO> selectWarningPage(WarningQueryRequest query) {
        WarningQueryRequest safeQuery = normalizeQuery(query);
        int offset = (safeQuery.getPageNum() - 1) * safeQuery.getPageSize();
        Long total = warningMapper.countWarningRecords(safeQuery);
        List<WarningRecordVO> records = warningMapper
                .selectWarningRecordPage(safeQuery, offset, safeQuery.getPageSize())
                .stream()
                .map(this::toWarningRecordVO)
                .toList();
        return new PageResultVO<>(total, records);
    }

    @Override
    @Transactional(readOnly = true)
    public WarningDetailVO selectWarningDetail(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException("预警ID不合法");
        }
        WarningRecordRow row = warningMapper.selectWarningRecordById(id);
        if (row == null) {
            throw new BusinessException("风险预警记录不存在");
        }
        return toWarningDetailVO(row);
    }

    @Override
    @Transactional
    public WarningDetailVO updateWarningStatus(Integer id, WarningStatusUpdateRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException("预警ID不合法");
        }
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new BusinessException("预警状态不能为空");
        }
        String status = request.getStatus().trim().toUpperCase();
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new BusinessException("预警状态只能是 UNPROCESSED、PROCESSING、DONE 或 IGNORED");
        }

        WarningRecordRow current = warningMapper.selectWarningRecordById(id);
        if (current == null) {
            throw new BusinessException("风险预警记录不存在");
        }

        Integer affected = warningMapper.updateWarningStatus(id, status, SysUserContext.getUserId());
        if (affected == null || affected == 0) {
            throw new BusinessException("更新预警状态失败");
        }
        return selectWarningDetail(id);
    }

    private RiskEvaluation evaluateRisk(WarningGenerationContextRow context) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();

        Integer predictedClass = context.getPredictedGradeClass();
        if (predictedClass != null && predictedClass >= 3) {
            score += 50;
            reasons.add("模型预测成绩等级为 " + gradeLabel(predictedClass) + "，存在学业风险");
            suggestions.add("建议教师持续跟进该学生的学习状态");
            suggestions.add("建议制定阶段性学习改进计划");
        }

        BigDecimal gpa = context.getGpa();
        if (gpa != null && gpa.compareTo(new BigDecimal("2.5")) < 0) {
            score += 35;
            reasons.add("当前 GPA 低于 2.5");
            suggestions.add("建议复盘近期薄弱课程并安排针对性练习");
        }

        Integer absences = context.getAbsences();
        if (absences != null && absences > 20) {
            score += 25;
            reasons.add("缺勤次数较高，超过 20 次");
            suggestions.add("建议联系学生了解缺勤原因");
            suggestions.add("建议加强出勤跟踪与提醒");
        } else if (absences != null && absences > 10) {
            score += 15;
            reasons.add("缺勤次数偏高，超过 10 次");
            suggestions.add("建议关注学生出勤情况");
        }

        BigDecimal studyTime = context.getStudyTimeWeekly();
        if (studyTime != null && studyTime.compareTo(new BigDecimal("5")) < 0) {
            score += 20;
            reasons.add("每周学习时长低于 5 小时");
            suggestions.add("建议帮助学生制定每周学习计划");
        } else if (studyTime != null && studyTime.compareTo(new BigDecimal("8")) < 0) {
            score += 10;
            reasons.add("每周学习时长偏低");
            suggestions.add("建议适当增加课后学习时间");
        }

        Integer parentalSupport = context.getParentalSupport();
        if (parentalSupport != null && parentalSupport <= 1) {
            score += 10;
            reasons.add("家长支持程度较低");
            suggestions.add("建议加强家校沟通");
        }

        Integer tutoring = context.getTutoring();
        if (tutoring != null && tutoring == 0) {
            score += 5;
            reasons.add("当前未参加课外辅导");
            suggestions.add("可根据实际情况考虑安排课后辅导");
        }

        int finalScore = Math.min(score, 100);
        String riskLevel = calculateRiskLevel(finalScore);
        if (reasons.isEmpty()) {
            reasons.add("当前未发现明显高风险因素");
            suggestions.add("建议保持当前学习节奏，并定期关注成绩变化");
        }

        return new RiskEvaluation(finalScore, riskLevel, reasons, new ArrayList<>(suggestions));
    }

    private String calculateRiskLevel(int score) {
        if (score >= 50) {
            return "HIGH";
        }
        if (score >= 25) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String gradeLabel(Integer gradeClass) {
        if (gradeClass == null) {
            return "未知";
        }
        return switch (gradeClass) {
            case 0 -> "优秀";
            case 1 -> "良好";
            case 2 -> "中等";
            case 3 -> "较差";
            case 4 -> "风险";
            default -> "未知";
        };
    }

    private WarningRecordVO toWarningRecordVO(WarningRecordRow row) {
        WarningRecordVO vo = new WarningRecordVO();
        vo.setId(row.getId());
        vo.setStudentNo(row.getStudentNo());
        vo.setStudentName(row.getStudentName());
        vo.setClassInfo(ClassInfoVO.fromRaw(row.getGradeLevel(), row.getClassName()));
        vo.setPredictionResultId(row.getPredictionResultId());
        vo.setRiskScore(row.getRiskScore());
        vo.setRiskLevel(row.getRiskLevel());
        vo.setRiskLevelLabel(riskLevelLabel(row.getRiskLevel()));
        vo.setRiskReasons(readStringList(row.getRiskReasonsJson()));
        vo.setSuggestions(readStringList(row.getSuggestionJson()));
        vo.setStatus(row.getStatus());
        vo.setStatusLabel(statusLabel(row.getStatus()));
        vo.setCreatedAt(row.getCreatedAt());
        vo.setUpdatedAt(row.getUpdatedAt());
        return vo;
    }

    private WarningDetailVO toWarningDetailVO(WarningRecordRow row) {
        WarningDetailVO vo = new WarningDetailVO();
        vo.setId(row.getId());
        vo.setStudentNo(row.getStudentNo());
        vo.setStudentName(row.getStudentName());
        vo.setClassInfo(ClassInfoVO.fromRaw(row.getGradeLevel(), row.getClassName()));
        vo.setPredictionResultId(row.getPredictionResultId());
        vo.setModelVersionId(row.getModelVersionId());
        vo.setModelVersionNo(row.getModelVersionNo());
        if (row.getPredictedGradeClass() != null) {
            vo.setPredictedGradeClass(GradeClassEnum.toOption(row.getPredictedGradeClass()));
        }
        vo.setPredictedGradeLabel(row.getPredictedGradeLabel());
        vo.setRiskScore(row.getRiskScore());
        vo.setRiskLevel(row.getRiskLevel());
        vo.setRiskLevelLabel(riskLevelLabel(row.getRiskLevel()));
        vo.setRiskReasons(readStringList(row.getRiskReasonsJson()));
        vo.setSuggestions(readStringList(row.getSuggestionJson()));
        vo.setStatus(row.getStatus());
        vo.setStatusLabel(statusLabel(row.getStatus()));
        vo.setHandlerUserId(row.getHandlerUserId());
        vo.setHandlerUsername(row.getHandlerUsername());
        vo.setHandlerRealName(row.getHandlerRealName());
        vo.setCreatedAt(row.getCreatedAt());
        vo.setUpdatedAt(row.getUpdatedAt());
        return vo;
    }

    private String riskLevelLabel(String riskLevel) {
        return switch (riskLevel == null ? "" : riskLevel) {
            case "LOW" -> "低风险";
            case "MEDIUM" -> "中风险";
            case "HIGH" -> "高风险";
            default -> "未知";
        };
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case STATUS_UNPROCESSED -> "未处理";
            case STATUS_PROCESSING -> "处理中";
            case STATUS_DONE -> "已完成";
            case STATUS_IGNORED -> "已忽略";
            default -> "未知";
        };
    }

    private WarningQueryRequest normalizeQuery(WarningQueryRequest query) {
        WarningQueryRequest safeQuery = query == null ? new WarningQueryRequest() : query;
        if (safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1) {
            safeQuery.setPageNum(DEFAULT_PAGE_NUM);
        }
        if (safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1) {
            safeQuery.setPageSize(DEFAULT_PAGE_SIZE);
        }
        if (safeQuery.getPageSize() > MAX_PAGE_SIZE) {
            safeQuery.setPageSize(MAX_PAGE_SIZE);
        }
        safeQuery.setStudentName(normalizeBlank(safeQuery.getStudentName()));
        safeQuery.setClassName(normalizeBlank(safeQuery.getClassName()));
        safeQuery.setRiskLevel(normalizeEnum(safeQuery.getRiskLevel(), ALLOWED_RISK_LEVELS, "风险等级只能是 LOW、MEDIUM 或 HIGH"));
        safeQuery.setStatus(normalizeEnum(safeQuery.getStatus(), ALLOWED_STATUSES, "预警状态只能是 UNPROCESSED、PROCESSING、DONE 或 IGNORED"));
        return safeQuery;
    }

    private String normalizeBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEnum(String value, Set<String> allowedValues, String errorMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (!allowedValues.contains(normalized)) {
            throw new BusinessException(errorMessage);
        }
        return normalized;
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private record RiskEvaluation(
            int riskScore,
            String riskLevel,
            List<String> reasons,
            List<String> suggestions) {
    }
}
