package com.ahogeking.studentanalytics.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class OperationLogQueryRequest {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private Integer userId;
    private String username;
    private String userRole;
    private String moduleName;
    private String operationType;
    private String operationResult;
    private String targetType;
    private String targetId;
    private String businessKey;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private String keyword;
}
