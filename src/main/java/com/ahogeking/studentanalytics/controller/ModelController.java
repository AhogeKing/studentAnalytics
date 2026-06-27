package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.annotation.RequireRole;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/models")
@RequireRole({"ADMIN"})
public class ModelController {
    // TODO: 后续实现模型训练和模型版本管理。
}
