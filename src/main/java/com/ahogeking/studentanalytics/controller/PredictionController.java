package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.annotation.LogOperation;
import com.ahogeking.studentanalytics.annotation.RequireRole;
import com.ahogeking.studentanalytics.common.Result;
import com.ahogeking.studentanalytics.common.constant.OperationModule;
import com.ahogeking.studentanalytics.common.constant.OperationTargetType;
import com.ahogeking.studentanalytics.common.constant.OperationType;
import com.ahogeking.studentanalytics.dto.PredictionRequest;
import com.ahogeking.studentanalytics.service.PredictionService;
import com.ahogeking.studentanalytics.vo.PredictionEligibilityVO;
import com.ahogeking.studentanalytics.vo.StudentPredictionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/predictions")
@RequiredArgsConstructor
public class PredictionController {
    private final PredictionService predictionService;

    @PostMapping("/students/{studentNo}")
    @RequireRole({"ADMIN", "TEACHER"})
    @LogOperation(
            module = OperationModule.PREDICTION,
            type = OperationType.PREDICT,
            targetType = OperationTargetType.STUDENT,
            targetId = "#studentNo",
            businessKey = "#studentNo",
            recordRequest = true
    )
    public Result<StudentPredictionVO> predictStudent(
            @PathVariable Integer studentNo,
            @RequestBody(required = false) PredictionRequest request) {
        return Result.success(predictionService.predictStudent(studentNo, request));
    }

    @GetMapping("/students/{studentNo}/eligibility")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<PredictionEligibilityVO> getPredictionEligibility(
            @PathVariable Integer studentNo,
            @RequestParam(name = "model_version_id", required = false) Integer modelVersionId) {
        return Result.success(predictionService.selectPredictionEligibility(studentNo, modelVersionId));
    }

    @GetMapping("/{id}")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<StudentPredictionVO> getPredictionDetail(@PathVariable Integer id) {
        return Result.success(predictionService.selectPredictionDetail(id));
    }

    @GetMapping("/students/{studentNo}/latest")
    @RequireRole({"ADMIN", "TEACHER"})
    public Result<StudentPredictionVO> getLatestPrediction(@PathVariable Integer studentNo) {
        return Result.success(predictionService.selectLatestPredictionByStudentNo(studentNo));
    }
}
