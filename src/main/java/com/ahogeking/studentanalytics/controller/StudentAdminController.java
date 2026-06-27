package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.annotation.RequireRole;
import com.ahogeking.studentanalytics.common.Result;
import com.ahogeking.studentanalytics.dto.StudentCreateRequest;
import com.ahogeking.studentanalytics.dto.StudentPerformanceUpsertRequest;
import com.ahogeking.studentanalytics.service.StudentService;
import com.ahogeking.studentanalytics.vo.StudentDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/students")
public class StudentAdminController {

    private final StudentService studentService;

    @RequireRole({"ADMIN"})
    @PostMapping
    public Result<StudentDetailVO> createStudent(@RequestBody @Valid StudentCreateRequest request) {
        return Result.success(studentService.createStudent(request));
    }

    @RequireRole({"ADMIN"})
    @PutMapping("/performance/{studentNo}")
    public Result<StudentDetailVO> upsertStudentPerformance(
            @PathVariable Integer studentNo,
            @RequestBody @Valid StudentPerformanceUpsertRequest request) {
        return Result.success(studentService.upsertStudentPerformance(studentNo, request));
    }
}
