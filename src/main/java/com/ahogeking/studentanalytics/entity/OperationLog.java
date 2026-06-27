package com.ahogeking.studentanalytics.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLog {
    private Integer id;
    private Integer userId;
    private String username;
    private String realName;
    private String userRole;
    private String moduleName;
    private String operationType;
    private String operationResult;
    private String targetType;
    private String targetId;
    private String businessKey;
    private String requestMethod;
    private String requestUri;
    private String ipAddress;

    /**
     * JSON 字符串，例如：
     * {"page_num": "1","module_name":"student"}
     */
    private String requestParams;

    /**
     * JSON 字符串，例如：
     * {"name":"Leo Xavier","gpa":"3.8"}
     */
    private String requestBody;

    private LocalDateTime createdAt;
}
