package com.ahogeking.studentanalytics.service;

import com.ahogeking.studentanalytics.dto.StudentOverviewQueryRequest;
import com.ahogeking.studentanalytics.vo.StudentFilterOptionsVO;
import com.ahogeking.studentanalytics.vo.StudentOverviewItemVO;
import com.ahogeking.studentanalytics.vo.StudentOverviewVO;

import java.util.List;

public interface StudentService {
    public List<StudentOverviewItemVO> selectAllStudentOverviewItems();

    StudentOverviewVO selectStudentOverview(StudentOverviewQueryRequest query);

    StudentFilterOptionsVO selectStudentFilterOptions();
}
