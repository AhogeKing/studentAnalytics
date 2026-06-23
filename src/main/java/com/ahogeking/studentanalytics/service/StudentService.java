package com.ahogeking.studentanalytics.service;

import com.ahogeking.studentanalytics.vo.StudentPageItemVO;

import java.util.List;

public interface StudentService {
    public List<StudentPageItemVO> selectAllStudentPageItems();
}
