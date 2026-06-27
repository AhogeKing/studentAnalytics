package com.ahogeking.studentanalytics.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogVO {
    private Integer id;
    private Integer userId;
    private String username;
    private String realName;
    private String userRole;
    private String userRoleLabel;
    private String moduleName;
    private String moduleLabel;
    private String operationType;
    private String operationTypeLabel;
    private String operationResult;
    private String operationResultLabel;
    private String targetType;
    private String targetTypeLabel;
    private String targetId;
    private String businessKey;
    private String operationSummary;
    private String requestMethod;
    private String requestUri;
    private String ipAddress;
    private LocalDateTime createdAt;
}
