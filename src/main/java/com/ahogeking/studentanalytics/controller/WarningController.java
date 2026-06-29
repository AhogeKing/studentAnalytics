package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.annotation.LogOperation;
import com.ahogeking.studentanalytics.annotation.RequireRole;
import com.ahogeking.studentanalytics.common.Result;
import com.ahogeking.studentanalytics.common.constant.OperationModule;
import com.ahogeking.studentanalytics.common.constant.OperationTargetType;
import com.ahogeking.studentanalytics.common.constant.OperationType;
import com.ahogeking.studentanalytics.dto.WarningQueryRequest;
import com.ahogeking.studentanalytics.dto.WarningStatusUpdateRequest;
import com.ahogeking.studentanalytics.exception.BusinessException;
import com.ahogeking.studentanalytics.service.WarningService;
import com.ahogeking.studentanalytics.vo.PageResultVO;
import com.ahogeking.studentanalytics.vo.WarningDetailVO;
import com.ahogeking.studentanalytics.vo.WarningRecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/warnings")
@RequiredArgsConstructor
public class WarningController {
    private final WarningService warningService;

    @GetMapping
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<PageResultVO<WarningRecordVO>> getWarnings(
            @ModelAttribute WarningQueryRequest query,
            @RequestParam(name = "page_num", required = false) Integer pageNum,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(name = "student_no", required = false) Integer studentNo,
            @RequestParam(name = "student_name", required = false) String studentName,
            @RequestParam(name = "grade_level", required = false) Integer gradeLevel,
            @RequestParam(name = "class_name", required = false) String className,
            @RequestParam(name = "risk_level", required = false) String riskLevel,
            @RequestParam(name = "start_time", required = false) String startTime,
            @RequestParam(name = "end_time", required = false) String endTime) {
        applySnakeCaseAliases(
                query,
                pageNum,
                pageSize,
                studentNo,
                studentName,
                gradeLevel,
                className,
                riskLevel,
                parseDateTime(startTime),
                parseDateTime(endTime)
        );
        return Result.success(warningService.selectWarningPage(query));
    }

    @GetMapping("/students/{studentNo}/latest")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<WarningRecordVO> getLatestWarningByStudentNo(@PathVariable Integer studentNo) {
        return Result.success(warningService.selectLatestWarningByStudentNo(studentNo));
    }

    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<WarningDetailVO> getWarningDetail(@PathVariable Integer id) {
        return Result.success(warningService.selectWarningDetail(id));
    }

    @PatchMapping("/{id}/status")
    @RequireRole({"ADMIN", "TEACHER"})
    @LogOperation(
            module = OperationModule.WARNING,
            type = OperationType.UPDATE_STATUS,
            targetType = OperationTargetType.WARNING,
            targetId = "#id",
            businessKey = "#id",
            recordRequest = true
    )
    public Result<WarningDetailVO> updateWarningStatus(
            @PathVariable Integer id,
            @RequestBody WarningStatusUpdateRequest request) {
        return Result.success(warningService.updateWarningStatus(id, request));
    }

    private void applySnakeCaseAliases(
            WarningQueryRequest query,
            Integer pageNum,
            Integer pageSize,
            Integer studentNo,
            String studentName,
            Integer gradeLevel,
            String className,
            String riskLevel,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        if (pageNum != null) {
            query.setPageNum(pageNum);
        }
        if (pageSize != null) {
            query.setPageSize(pageSize);
        }
        if (studentNo != null) {
            query.setStudentNo(studentNo);
        }
        if (studentName != null) {
            query.setStudentName(studentName);
        }
        if (gradeLevel != null) {
            query.setGradeLevel(gradeLevel);
        }
        if (className != null) {
            query.setClassName(className);
        }
        if (riskLevel != null) {
            query.setRiskLevel(riskLevel);
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
