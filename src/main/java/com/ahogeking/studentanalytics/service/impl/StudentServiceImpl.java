package com.ahogeking.studentanalytics.service.impl;

import com.ahogeking.studentanalytics.dto.StudentPageRow;
import com.ahogeking.studentanalytics.mapper.StudentMapper;
import com.ahogeking.studentanalytics.service.StudentService;
import com.ahogeking.studentanalytics.vo.ClassInfoVO;
import com.ahogeking.studentanalytics.vo.GenderEnum;
import com.ahogeking.studentanalytics.vo.GradeClassEnum;
import com.ahogeking.studentanalytics.vo.StudentPageItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;

    @Override
    public List<StudentPageItemVO> selectAllStudentPageItems() {
        return studentMapper.selectStudentPageRows().stream()
                .map(this::toStudentPageItemVO)
                .toList();
    }

    private StudentPageItemVO toStudentPageItemVO(StudentPageRow row) {
        StudentPageItemVO vo = new StudentPageItemVO();
        vo.setStudentNo(row.getStudentNo());
        vo.setName(row.getName());
        vo.setAge(row.getAge());
        vo.setGender(GenderEnum.toOption(row.getGender()));
        vo.setGradeLevel(row.getGradeLevel());
        vo.setClassInfo(ClassInfoVO.fromRaw(row.getGradeLevel(), row.getClassName()));
        vo.setGpa(row.getGpa());
        vo.setGradeClass(GradeClassEnum.toOption(row.getGradeClass()));
        vo.setUpdateTime(row.getUpdateTime());
        return vo;
    }
}
