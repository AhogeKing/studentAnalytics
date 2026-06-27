package com.ahogeking.studentanalytics.service;

import com.ahogeking.studentanalytics.dto.StudentCreateRequest;
import com.ahogeking.studentanalytics.dto.StudentOverviewQueryRequest;
import com.ahogeking.studentanalytics.dto.StudentOverviewUpdateRequest;
import com.ahogeking.studentanalytics.dto.StudentPerformanceUpsertRequest;
import com.ahogeking.studentanalytics.vo.StudentDetailVO;
import com.ahogeking.studentanalytics.vo.StudentFilterOptionsVO;
import com.ahogeking.studentanalytics.vo.StudentOverviewItemVO;
import com.ahogeking.studentanalytics.vo.StudentOverviewVO;

import java.util.List;

public interface StudentService {
    public List<StudentOverviewItemVO> selectAllStudentOverviewItems();

    StudentOverviewVO selectStudentOverview(StudentOverviewQueryRequest query);

    StudentFilterOptionsVO selectStudentFilterOptions();

    StudentDetailVO createStudent(StudentCreateRequest request);

    StudentOverviewItemVO updateStudentOverview(Integer studentNo, StudentOverviewUpdateRequest request);

    StudentDetailVO upsertStudentPerformance(Integer studentNo, StudentPerformanceUpsertRequest request);

    void deleteStudentOverview(Integer studentNo);

    StudentDetailVO selectStudentDetail(Integer studentNo);
}
