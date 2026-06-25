package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.common.Result;
import com.ahogeking.studentanalytics.dto.StudentOverviewQueryRequest;
import com.ahogeking.studentanalytics.dto.StudentOverviewUpdateRequest;
import com.ahogeking.studentanalytics.service.StudentService;
import com.ahogeking.studentanalytics.vo.StudentDetailVO;
import com.ahogeking.studentanalytics.vo.StudentFilterOptionsVO;
import com.ahogeking.studentanalytics.vo.StudentOverviewItemVO;
import com.ahogeking.studentanalytics.vo.StudentOverviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/students", "/student"})
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/list")
    public Result<List<StudentOverviewItemVO>> listAllStudentOverviewItems() {
        return Result.success(studentService.selectAllStudentOverviewItems());
    }

    @GetMapping("/page")
    public Result<StudentOverviewVO> getStudentOverview(
            @RequestParam(name = "class_name", required = false) List<String> classNames,
            @RequestParam(name = "min_gpa", required = false) BigDecimal minGpa,
            @RequestParam(name = "max_gpa", required = false) BigDecimal maxGpa,
            @RequestParam(name = "grade_class", required = false) Integer gradeClass,
            @RequestParam(name = "grade_level", required = false) Integer gradeLevel,
            @RequestParam(name = "gender", required = false) Integer gender,
            @RequestParam(name = "sort_field", required = false) String sortField,
            @RequestParam(name = "sort_order", required = false) String sortOrder,
            @ModelAttribute StudentOverviewQueryRequest query) {
        if (classNames != null) {
            query.setClassNames(classNames);
        }
        if (minGpa != null) {
            query.setMinGpa(minGpa);
        }
        if (maxGpa != null) {
            query.setMaxGpa(maxGpa);
        }
        if (gradeClass != null) {
            query.setGradeClass(gradeClass);
        }
        if (gradeLevel != null) {
            query.setGradeLevel(gradeLevel);
        }
        if (gender != null) {
            query.setGender(gender);
        }
        if (sortField != null) {
            query.setSortField(sortField);
        }
        if (sortOrder != null) {
            query.setSortOrder(sortOrder);
        }
        return Result.success(studentService.selectStudentOverview(query));
    }

    @GetMapping("/filter-options")
    public Result<StudentFilterOptionsVO> getStudentFilterOptions() {
        return Result.success(studentService.selectStudentFilterOptions());
    }

    @PutMapping("/overview/{studentNo}")
    public Result<StudentOverviewItemVO> updateStudentOverview(
            @PathVariable Integer studentNo,
            @RequestBody @Valid StudentOverviewUpdateRequest request) {
        return Result.success(studentService.updateStudentOverview(studentNo, request));
    }

    @DeleteMapping("/overview/{studentNo}")
    public Result<Void> deleteStudentOverview(@PathVariable Integer studentNo) {
        studentService.deleteStudentOverview(studentNo);
        return Result.success();
    }

    @GetMapping("/detail/{studentNo}")
    public Result<StudentDetailVO> getStudentDetail(@PathVariable Integer studentNo) {
        StudentDetailVO detail = studentService.selectStudentDetail(studentNo);
        return Result.success(detail);
    }
}
