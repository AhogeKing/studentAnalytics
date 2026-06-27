package com.ahogeking.studentanalytics.service.impl;

import com.ahogeking.studentanalytics.common.constant.OperationModule;
import com.ahogeking.studentanalytics.common.constant.OperationResult;
import com.ahogeking.studentanalytics.common.constant.OperationTargetType;
import com.ahogeking.studentanalytics.common.constant.OperationType;
import com.ahogeking.studentanalytics.dto.OperationLogQueryRequest;
import com.ahogeking.studentanalytics.entity.OperationLog;
import com.ahogeking.studentanalytics.exception.BusinessException;
import com.ahogeking.studentanalytics.mapper.OperationLogMapper;
import com.ahogeking.studentanalytics.service.OperationLogService;
import com.ahogeking.studentanalytics.vo.OperationLogDetailVO;
import com.ahogeking.studentanalytics.vo.OperationLogOptionsVO;
import com.ahogeking.studentanalytics.vo.OperationLogVO;
import com.ahogeking.studentanalytics.vo.OptionVO;
import com.ahogeking.studentanalytics.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final OperationLogMapper operationLogMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOperationLog(OperationLog log) {
        if (log == null) {
            return;
        }
        if (log.getRequestParams() == null || log.getRequestParams().isBlank()) {
            log.setRequestParams("{}");
        }
        if (log.getRequestBody() == null || log.getRequestBody().isBlank()) {
            log.setRequestBody("{}");
        }
        operationLogMapper.insert(log);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResultVO<OperationLogVO> selectOperationLogPage(OperationLogQueryRequest query) {
        OperationLogQueryRequest safeQuery = normalizeQuery(query);
        int offset = (safeQuery.getPageNum() - 1) * safeQuery.getPageSize();
        Long total = operationLogMapper.countOperationLogs(safeQuery);
        List<OperationLogVO> records = operationLogMapper
                .selectOperationLogPage(safeQuery, offset, safeQuery.getPageSize())
                .stream()
                .map(this::toOperationLogVO)
                .toList();
        return new PageResultVO<>(total, records);
    }

    @Override
    @Transactional(readOnly = true)
    public OperationLogDetailVO selectOperationLogDetail(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException("日志ID不合法");
        }
        OperationLog log = operationLogMapper.selectOperationLogById(id);
        if (log == null) {
            throw new BusinessException("操作日志不存在");
        }
        return toOperationLogDetailVO(log);
    }

    @Override
    public OperationLogOptionsVO selectOperationLogOptions() {
        OperationLogOptionsVO options = new OperationLogOptionsVO();
        options.setModules(List.of(
                OptionVO.of(OperationModule.AUTH, "认证"),
                OptionVO.of(OperationModule.USER, "用户管理"),
                OptionVO.of(OperationModule.STUDENT, "学生管理"),
                OptionVO.of(OperationModule.PERFORMANCE, "学业表现"),
                OptionVO.of(OperationModule.IMPORT, "数据导入"),
                OptionVO.of(OperationModule.MODEL, "模型管理"),
                OptionVO.of(OperationModule.PREDICTION, "学业预测"),
                OptionVO.of(OperationModule.WARNING, "风险预警")
        ));
        options.setOperationTypes(List.of(
                OptionVO.of(OperationType.CREATE, "新增"),
                OptionVO.of(OperationType.UPDATE, "修改"),
                OptionVO.of(OperationType.DELETE, "删除"),
                OptionVO.of(OperationType.UPSERT, "新增或更新"),
                OptionVO.of(OperationType.ENABLE, "启用"),
                OptionVO.of(OperationType.DISABLE, "禁用"),
                OptionVO.of(OperationType.UPDATE_STATUS, "修改状态"),
                OptionVO.of(OperationType.RESET_PASSWORD, "重置密码"),
                OptionVO.of(OperationType.LOGIN, "登录"),
                OptionVO.of(OperationType.LOGOUT, "退出登录"),
                OptionVO.of(OperationType.IMPORT, "导入"),
                OptionVO.of(OperationType.TRAIN, "训练模型"),
                OptionVO.of(OperationType.ACTIVATE, "启用版本"),
                OptionVO.of(OperationType.PREDICT, "学业预测"),
                OptionVO.of(OperationType.GENERATE_WARNING, "生成预警"),
                OptionVO.of(OperationType.HANDLE_WARNING, "处理预警")
        ));
        options.setResults(List.of(
                OptionVO.of(OperationResult.SUCCESS, "成功"),
                OptionVO.of(OperationResult.FAIL, "失败")
        ));
        options.setRoles(List.of(
                OptionVO.of("ADMIN", "管理员"),
                OptionVO.of("TEACHER", "教师"),
                OptionVO.of("STUDENT", "学生")
        ));
        options.setTargetTypes(List.of(
                OptionVO.of(OperationTargetType.USER, "用户"),
                OptionVO.of(OperationTargetType.STUDENT, "学生"),
                OptionVO.of(OperationTargetType.PERFORMANCE, "学业表现"),
                OptionVO.of(OperationTargetType.IMPORT_BATCH, "导入批次"),
                OptionVO.of(OperationTargetType.MODEL_VERSION, "模型版本"),
                OptionVO.of(OperationTargetType.PREDICTION, "预测结果"),
                OptionVO.of(OperationTargetType.WARNING, "风险预警")
        ));
        return options;
    }

    private OperationLogQueryRequest normalizeQuery(OperationLogQueryRequest query) {
        OperationLogQueryRequest safeQuery = query == null ? new OperationLogQueryRequest() : query;
        if (safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1) {
            safeQuery.setPageNum(DEFAULT_PAGE_NUM);
        }
        if (safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1) {
            safeQuery.setPageSize(DEFAULT_PAGE_SIZE);
        }
        if (safeQuery.getPageSize() > MAX_PAGE_SIZE) {
            safeQuery.setPageSize(MAX_PAGE_SIZE);
        }
        safeQuery.setUsername(normalizeBlank(safeQuery.getUsername()));
        safeQuery.setUserRole(normalizeBlank(safeQuery.getUserRole()));
        safeQuery.setModuleName(normalizeBlank(safeQuery.getModuleName()));
        safeQuery.setOperationType(normalizeBlank(safeQuery.getOperationType()));
        safeQuery.setOperationResult(normalizeBlank(safeQuery.getOperationResult()));
        safeQuery.setTargetType(normalizeBlank(safeQuery.getTargetType()));
        safeQuery.setTargetId(normalizeBlank(safeQuery.getTargetId()));
        safeQuery.setBusinessKey(normalizeBlank(safeQuery.getBusinessKey()));
        safeQuery.setKeyword(normalizeBlank(safeQuery.getKeyword()));
        return safeQuery;
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private OperationLogVO toOperationLogVO(OperationLog log) {
        OperationLogVO vo = new OperationLogVO();
        fillCommonFields(vo, log);
        return vo;
    }

    private OperationLogDetailVO toOperationLogDetailVO(OperationLog log) {
        OperationLogDetailVO vo = new OperationLogDetailVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(log.getUsername());
        vo.setRealName(log.getRealName());
        vo.setUserRole(log.getUserRole());
        vo.setUserRoleLabel(getUserRoleLabel(log.getUserRole()));
        vo.setModuleName(log.getModuleName());
        vo.setModuleLabel(getModuleLabel(log.getModuleName()));
        vo.setOperationType(log.getOperationType());
        vo.setOperationTypeLabel(getOperationTypeLabel(log.getOperationType()));
        vo.setOperationResult(log.getOperationResult());
        vo.setOperationResultLabel(getOperationResultLabel(log.getOperationResult()));
        vo.setTargetType(log.getTargetType());
        vo.setTargetTypeLabel(getTargetTypeLabel(log.getTargetType()));
        vo.setTargetId(log.getTargetId());
        vo.setBusinessKey(log.getBusinessKey());
        vo.setOperationSummary(buildOperationSummary(log));
        vo.setRequestMethod(log.getRequestMethod());
        vo.setRequestUri(log.getRequestUri());
        vo.setIpAddress(log.getIpAddress());
        vo.setRequestParams(log.getRequestParams());
        vo.setRequestBody(log.getRequestBody());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }

    private void fillCommonFields(OperationLogVO vo, OperationLog log) {
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(log.getUsername());
        vo.setRealName(log.getRealName());
        vo.setUserRole(log.getUserRole());
        vo.setUserRoleLabel(getUserRoleLabel(log.getUserRole()));
        vo.setModuleName(log.getModuleName());
        vo.setModuleLabel(getModuleLabel(log.getModuleName()));
        vo.setOperationType(log.getOperationType());
        vo.setOperationTypeLabel(getOperationTypeLabel(log.getOperationType()));
        vo.setOperationResult(log.getOperationResult());
        vo.setOperationResultLabel(getOperationResultLabel(log.getOperationResult()));
        vo.setTargetType(log.getTargetType());
        vo.setTargetTypeLabel(getTargetTypeLabel(log.getTargetType()));
        vo.setTargetId(log.getTargetId());
        vo.setBusinessKey(log.getBusinessKey());
        vo.setOperationSummary(buildOperationSummary(log));
        vo.setRequestMethod(log.getRequestMethod());
        vo.setRequestUri(log.getRequestUri());
        vo.setIpAddress(log.getIpAddress());
        vo.setCreatedAt(log.getCreatedAt());
    }

    private String buildOperationSummary(OperationLog log) {
        String operationLabel = getOperationTypeLabel(log.getOperationType());
        String moduleLabel = getModuleLabel(log.getModuleName());
        String target = firstNotBlank(log.getBusinessKey(), log.getTargetId());
        if (target == null) {
            return moduleLabel + "：" + operationLabel;
        }
        return moduleLabel + "：" + operationLabel + " " + target;
    }

    private String firstNotBlank(String first, String second) {
        String normalizedFirst = normalizeBlank(first);
        return normalizedFirst == null ? normalizeBlank(second) : normalizedFirst;
    }

    private String getUserRoleLabel(String role) {
        if ("ADMIN".equals(role)) {
            return "管理员";
        }
        if ("TEACHER".equals(role)) {
            return "教师";
        }
        if ("STUDENT".equals(role)) {
            return "学生";
        }
        return "未知";
    }

    private String getModuleLabel(String module) {
        if (OperationModule.AUTH.equals(module)) {
            return "认证";
        }
        if (OperationModule.USER.equals(module)) {
            return "用户管理";
        }
        if (OperationModule.STUDENT.equals(module)) {
            return "学生管理";
        }
        if (OperationModule.PERFORMANCE.equals(module)) {
            return "学业表现";
        }
        if (OperationModule.IMPORT.equals(module)) {
            return "数据导入";
        }
        if (OperationModule.MODEL.equals(module)) {
            return "模型管理";
        }
        if (OperationModule.PREDICTION.equals(module)) {
            return "学业预测";
        }
        if (OperationModule.WARNING.equals(module)) {
            return "风险预警";
        }
        return "未知模块";
    }

    private String getOperationTypeLabel(String type) {
        if (OperationType.CREATE.equals(type)) {
            return "新增";
        }
        if (OperationType.UPDATE.equals(type)) {
            return "修改";
        }
        if (OperationType.DELETE.equals(type)) {
            return "删除";
        }
        if (OperationType.UPSERT.equals(type)) {
            return "新增或更新";
        }
        if (OperationType.ENABLE.equals(type)) {
            return "启用";
        }
        if (OperationType.DISABLE.equals(type)) {
            return "禁用";
        }
        if (OperationType.UPDATE_STATUS.equals(type)) {
            return "修改状态";
        }
        if (OperationType.RESET_PASSWORD.equals(type)) {
            return "重置密码";
        }
        if (OperationType.LOGIN.equals(type)) {
            return "登录";
        }
        if (OperationType.LOGOUT.equals(type)) {
            return "退出登录";
        }
        if (OperationType.IMPORT.equals(type)) {
            return "导入";
        }
        if (OperationType.TRAIN.equals(type)) {
            return "训练模型";
        }
        if (OperationType.ACTIVATE.equals(type)) {
            return "启用版本";
        }
        if (OperationType.PREDICT.equals(type)) {
            return "学业预测";
        }
        if (OperationType.GENERATE_WARNING.equals(type)) {
            return "生成预警";
        }
        if (OperationType.HANDLE_WARNING.equals(type)) {
            return "处理预警";
        }
        return "未知操作";
    }

    private String getOperationResultLabel(String result) {
        if (OperationResult.SUCCESS.equals(result)) {
            return "成功";
        }
        if (OperationResult.FAIL.equals(result)) {
            return "失败";
        }
        return "未知";
    }

    private String getTargetTypeLabel(String targetType) {
        if (OperationTargetType.USER.equals(targetType)) {
            return "用户";
        }
        if (OperationTargetType.STUDENT.equals(targetType)) {
            return "学生";
        }
        if (OperationTargetType.PERFORMANCE.equals(targetType)) {
            return "学业表现";
        }
        if (OperationTargetType.IMPORT_BATCH.equals(targetType)) {
            return "导入批次";
        }
        if (OperationTargetType.MODEL_VERSION.equals(targetType)) {
            return "模型版本";
        }
        if (OperationTargetType.PREDICTION.equals(targetType)) {
            return "预测结果";
        }
        if (OperationTargetType.WARNING.equals(targetType)) {
            return "风险预警";
        }
        return "未知对象";
    }
}
