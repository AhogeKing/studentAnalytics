package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.annotation.RequireRole;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/warnings")
@RequireRole({"ADMIN"})
public class WarningController {
    // TODO: 后续实现风险预警查看和处理。
}
