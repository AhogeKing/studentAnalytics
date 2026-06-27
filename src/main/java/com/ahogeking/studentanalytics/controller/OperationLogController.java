package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.annotation.RequireRole;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/operation-logs")
@RequireRole({"ADMIN"})
public class OperationLogController {
    // TODO: 后续实现操作日志分页查询。
}
