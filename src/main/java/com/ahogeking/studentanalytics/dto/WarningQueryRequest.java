package com.ahogeking.studentanalytics.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class WarningQueryRequest {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private Integer studentNo;
    private String studentName;
    private Integer gradeLevel;
    private String className;
    private String riskLevel;
    private String status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
