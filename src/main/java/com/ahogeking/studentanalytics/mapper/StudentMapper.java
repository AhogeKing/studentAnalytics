package com.ahogeking.studentanalytics.mapper;

import com.ahogeking.studentanalytics.dto.StudentPageRow;
import com.ahogeking.studentanalytics.entity.Student;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
    List<StudentPageRow> selectStudentPageRows();
}
