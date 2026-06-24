package com.ahogeking.studentanalytics.mapper;

import com.ahogeking.studentanalytics.dto.row.StudentDetailAggregateRow;
import com.ahogeking.studentanalytics.dto.row.StudentOverviewRow;
import com.ahogeking.studentanalytics.entity.Student;
import com.ahogeking.studentanalytics.vo.ClassInfoVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
    List<StudentOverviewRow> selectStudentOverviewRows(@Param("sortColumn") String sortColumn,
                                                       @Param("sortOrder") String sortOrder);

    List<StudentOverviewRow> selectStudentOverviewRowsByClass(@Param("className") String className,
                                                              @Param("sortColumn") String sortColumn,
                                                              @Param("sortOrder") String sortOrder);

    List<StudentOverviewRow> searchStudentOverviewRows(@Param("keyword") String keyword,
                                                       @Param("studentNo") Integer studentNo,
                                                       @Param("sortColumn") String sortColumn,
                                                       @Param("sortOrder") String sortOrder);

    List<StudentOverviewRow> filterStudentOverviewRows(@Param("minGpa") BigDecimal minGpa,
                                                       @Param("maxGpa") BigDecimal maxGpa,
                                                       @Param("gradeClass") Integer gradeClass,
                                                       @Param("gradeLevel") Integer gradeLevel,
                                                       @Param("gender") Integer gender,
                                                       @Param("classNames") List<String> classNames,
                                                       @Param("sortColumn") String sortColumn,
                                                       @Param("sortOrder") String sortOrder);

    Long countStudentsByClass(@Param("className") String className);

    List<ClassInfoVO> selectClassOptions();

    BigDecimal selectMinGpa();

    BigDecimal selectMaxGpa();

    StudentDetailAggregateRow selectStudentDetailAggregateRow(@Param("studentNo") Integer studentNo);
}
