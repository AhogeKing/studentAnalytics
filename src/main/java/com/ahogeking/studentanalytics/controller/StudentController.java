package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.common.Result;
import com.ahogeking.studentanalytics.service.StudentService;
import com.ahogeking.studentanalytics.vo.StudentPageItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/list")
    public Result<List<StudentPageItemVO>> listAllStudentPageItems() {
        return Result.success(studentService.selectAllStudentPageItems());
    }
}
