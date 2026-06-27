package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.annotation.RequireRole;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/predictions")
@RequireRole({"ADMIN"})
public class PredictionController {
    // TODO: 后续实现单学生预测。
}
