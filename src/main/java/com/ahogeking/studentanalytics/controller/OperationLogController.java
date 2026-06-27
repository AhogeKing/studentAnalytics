package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.annotation.RequireRole;
import com.ahogeking.studentanalytics.common.Result;
import com.ahogeking.studentanalytics.dto.OperationLogQueryRequest;
import com.ahogeking.studentanalytics.exception.BusinessException;
import com.ahogeking.studentanalytics.service.OperationLogService;
import com.ahogeking.studentanalytics.vo.OperationLogDetailVO;
import com.ahogeking.studentanalytics.vo.OperationLogOptionsVO;
import com.ahogeking.studentanalytics.vo.OperationLogVO;
import com.ahogeking.studentanalytics.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/admin/operation-logs")
@RequireRole({"ADMIN"})
@RequiredArgsConstructor
public class OperationLogController {
    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResultVO<OperationLogVO>> getOperationLogs(
            @ModelAttribute OperationLogQueryRequest query,
            @RequestParam(name = "page_num", required = false) Integer pageNum,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(name = "user_role", required = false) String userRole,
            @RequestParam(name = "module_name", required = false) String moduleName,
            @RequestParam(name = "operation_type", required = false) String operationType,
            @RequestParam(name = "operation_result", required = false) String operationResult,
            @RequestParam(name = "target_type", required = false) String targetType,
            @RequestParam(name = "target_id", required = false) String targetId,
            @RequestParam(name = "business_key", required = false) String businessKey,
            @RequestParam(name = "start_time", required = false) String startTime,
            @RequestParam(name = "end_time", required = false) String endTime) {
        applySnakeCaseAliases(
                query,
                pageNum,
                pageSize,
                userRole,
                moduleName,
                operationType,
                operationResult,
                targetType,
                targetId,
                businessKey,
                parseDateTime(startTime),
                parseDateTime(endTime)
        );
        return Result.success(operationLogService.selectOperationLogPage(query));
    }

    @GetMapping("/{id}")
    public Result<OperationLogDetailVO> getOperationLogDetail(@PathVariable Integer id) {
        return Result.success(operationLogService.selectOperationLogDetail(id));
    }

    @GetMapping("/options")
    public Result<OperationLogOptionsVO> getOperationLogOptions() {
        return Result.success(operationLogService.selectOperationLogOptions());
    }

    private void applySnakeCaseAliases(
            OperationLogQueryRequest query,
            Integer pageNum,
            Integer pageSize,
            String userRole,
            String moduleName,
            String operationType,
            String operationResult,
            String targetType,
            String targetId,
            String businessKey,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        if (pageNum != null) {
            query.setPageNum(pageNum);
        }
        if (pageSize != null) {
            query.setPageSize(pageSize);
        }
        if (userRole != null) {
            query.setUserRole(userRole);
        }
        if (moduleName != null) {
            query.setModuleName(moduleName);
        }
        if (operationType != null) {
            query.setOperationType(operationType);
        }
        if (operationResult != null) {
            query.setOperationResult(operationResult);
        }
        if (targetType != null) {
            query.setTargetType(targetType);
        }
        if (targetId != null) {
            query.setTargetId(targetId);
        }
        if (businessKey != null) {
            query.setBusinessKey(businessKey);
        }
        if (startTime != null) {
            query.setStartTime(startTime);
        }
        if (endTime != null) {
            query.setEndTime(endTime);
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(trimmed);
            } catch (DateTimeParseException ex) {
                throw new BusinessException("时间格式应为 yyyy-MM-dd HH:mm:ss 或 ISO LocalDateTime");
            }
        }
    }
}
