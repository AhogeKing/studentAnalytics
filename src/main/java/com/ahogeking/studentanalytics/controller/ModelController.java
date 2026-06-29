package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.annotation.LogOperation;
import com.ahogeking.studentanalytics.annotation.RequireRole;
import com.ahogeking.studentanalytics.common.Result;
import com.ahogeking.studentanalytics.common.constant.OperationModule;
import com.ahogeking.studentanalytics.common.constant.OperationTargetType;
import com.ahogeking.studentanalytics.common.constant.OperationType;
import com.ahogeking.studentanalytics.dto.ModelTrainRequest;
import com.ahogeking.studentanalytics.dto.ModelVersionQueryRequest;
import com.ahogeking.studentanalytics.exception.BusinessException;
import com.ahogeking.studentanalytics.service.ModelService;
import com.ahogeking.studentanalytics.vo.ModelTrainResultVO;
import com.ahogeking.studentanalytics.vo.ModelVersionDetailVO;
import com.ahogeking.studentanalytics.vo.ModelVersionVO;
import com.ahogeking.studentanalytics.vo.PageResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/models")
@RequiredArgsConstructor
public class ModelController {
    private final ModelService modelService;

    @PostMapping("/decision-tree/train")
    @RequireRole({"ADMIN"})
    @LogOperation(
            module = OperationModule.MODEL,
            type = OperationType.TRAIN,
            targetType = OperationTargetType.MODEL_VERSION,
            recordRequest = true
    )
    public Result<ModelTrainResultVO> trainDecisionTree(
            @RequestBody(required = false) ModelTrainRequest request) {
        return Result.success(modelService.trainDecisionTree(request));
    }

    @GetMapping("/active")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<ModelVersionVO> getActiveModel() {
        return Result.success(modelService.selectActiveModel());
    }

    @GetMapping("/versions")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<PageResultVO<ModelVersionVO>> getModelVersions(
            @ModelAttribute ModelVersionQueryRequest query,
            @RequestParam(name = "page_num", required = false) Integer pageNum,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(name = "start_time", required = false) String startTime,
            @RequestParam(name = "end_time", required = false) String endTime) {
        applySnakeCaseAliases(query, pageNum, pageSize, parseDateTime(startTime), parseDateTime(endTime));
        return Result.success(modelService.selectModelVersionPage(query));
    }

    @GetMapping("/versions/{id}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<ModelVersionDetailVO> getModelVersionDetail(@PathVariable Integer id) {
        return Result.success(modelService.selectModelVersionDetail(id));
    }

    private void applySnakeCaseAliases(
            ModelVersionQueryRequest query,
            Integer pageNum,
            Integer pageSize,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        if (pageNum != null) {
            query.setPageNum(pageNum);
        }
        if (pageSize != null) {
            query.setPageSize(pageSize);
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
