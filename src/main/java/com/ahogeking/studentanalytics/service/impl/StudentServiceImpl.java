package com.ahogeking.studentanalytics.service.impl;

import com.ahogeking.studentanalytics.dto.StudentCreateRequest;
import com.ahogeking.studentanalytics.dto.StudentOverviewQueryRequest;
import com.ahogeking.studentanalytics.dto.StudentOverviewUpdateRequest;
import com.ahogeking.studentanalytics.dto.StudentPerformanceUpsertRequest;
import com.ahogeking.studentanalytics.entity.Performance;
import com.ahogeking.studentanalytics.entity.Student;
import com.ahogeking.studentanalytics.dto.row.StudentDetailAggregateRow;
import com.ahogeking.studentanalytics.dto.row.StudentOverviewRow;
import com.ahogeking.studentanalytics.dto.StudentSortOption;
import com.ahogeking.studentanalytics.exception.BusinessException;
import com.ahogeking.studentanalytics.mapper.PerformanceMapper;
import com.ahogeking.studentanalytics.mapper.StudentMapper;
import com.ahogeking.studentanalytics.service.StudentService;
import com.ahogeking.studentanalytics.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private static final String DEFAULT_CLASS_NAME = "1-1";
    private static final String DEFAULT_SORT_COLUMN = "s.student_no";
    private static final String DEFAULT_SORT_ORDER = "ASC";

    private final StudentMapper studentMapper;
    private final PerformanceMapper performanceMapper;

    // 查询所有学生概览
    @Override
    public List<StudentOverviewItemVO> selectAllStudentOverviewItems() {
        StudentSortOption sortOption = normalizeSortOption(null, null);
        return studentMapper.selectStudentOverviewRows(sortOption.getSortColumn(), sortOption.getSortOrder()).stream()
                .map(this::toStudentOverviewItemVO)
                .toList();
    }

    // 根据搜索、筛选、班级选择查询方式
    @Override
    public StudentOverviewVO selectStudentOverview(StudentOverviewQueryRequest query) {
        StudentOverviewQueryRequest safeQuery = query == null ? new StudentOverviewQueryRequest() : query;
        List<String> normalizedClassNames = normalizeClassNames(safeQuery);
        StudentSortOption sortOption = normalizeSortOption(safeQuery.getSortField(), safeQuery.getSortOrder());

        if (hasKeyword(safeQuery)) {
            if (hasKeywordConflict(normalizedClassNames, safeQuery)) {
                throw new BusinessException("关键词搜索不能和筛选条件同时使用");
            }
            return searchStudentOverview(safeQuery, sortOption);
        }

        if (hasFilter(normalizedClassNames, safeQuery)) {
            return filterStudentOverview(normalizedClassNames, safeQuery, sortOption);
        }

        String normalizedClassName = normalizedClassNames.isEmpty()
                ? DEFAULT_CLASS_NAME
                : normalizedClassNames.getFirst();

        List<StudentOverviewItemVO> records = studentMapper
                .selectStudentOverviewRowsByClass(normalizedClassName, sortOption.getSortColumn(), sortOption.getSortOrder())
                .stream()
                .map(this::toStudentOverviewItemVO)
                .toList();
        Long total = studentMapper.countStudentsByClass(normalizedClassName);

        ClassInfoVO classInfo = ClassInfoVO.fromRaw(parseGradeLevel(normalizedClassName), normalizedClassName);
        StudentOverviewVO vo = new StudentOverviewVO();
        vo.setClassName(classInfo.getClassName());
        vo.setRawClassName(normalizedClassName);
        vo.setSortField(normalizeSortField(safeQuery.getSortField()));
        vo.setSortOrder(sortOption.getSortOrder().toLowerCase());
        vo.setStudentCount(total);
        vo.setStudents(records);
        return vo;
    }

    // 返回班级、性别、等级和 GPA 范围
    @Override
    public StudentFilterOptionsVO selectStudentFilterOptions() {
        StudentFilterOptionsVO vo = new StudentFilterOptionsVO();
        vo.setClasses(studentMapper.selectClassOptions());
        vo.setGenders(Arrays.stream(GenderEnum.values())
                .map(gender -> GenderEnum.toOption(gender.getCode()))
                .toList());
        vo.setGradeClasses(Arrays.stream(GradeClassEnum.values())
                .map(gradeClass -> GradeClassEnum.toOption(gradeClass.getCode()))
                .toList());
        vo.setMinGpa(studentMapper.selectMinGpa());
        vo.setMaxGpa(studentMapper.selectMaxGpa());
        return vo;
    }

    // 新增学生基础信息，不创建默认表现记录
    @Override
    @Transactional
    public StudentDetailVO createStudent(StudentCreateRequest request) {
        if (request == null) {
            throw new BusinessException("学生信息不能为空");
        }

        String normalizedClassName = normalizeClassNameForUpdate(request.getClassName());
        Integer gradeLevel = parseGradeLevel(normalizedClassName);

        Long exists = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, request.getStudentNo()));
        if (exists != null && exists > 0) {
            throw new BusinessException("学生编号已存在");
        }

        Student student = new Student();
        student.setStudentNo(request.getStudentNo());
        student.setName(normalizeNameForCreate(request.getName(), request.getStudentNo()));
        student.setAge(request.getAge());
        student.setGradeLevel(gradeLevel);
        student.setClassName(normalizedClassName);
        student.setGender(request.getGender());
        student.setEthnicity(request.getEthnicity());
        student.setParentalEducation(request.getParentalEducation());
        student.setDeleted(0);
        studentMapper.insert(student);

        return selectStudentDetail(request.getStudentNo());
    }

    // 修改学生表及表现表中的概览字段
    @Override
    @Transactional
    public StudentOverviewItemVO updateStudentOverview(Integer studentNo, StudentOverviewUpdateRequest request) {
        if (studentNo == null) {
            throw new BusinessException("学生编号不能为空");
        }
        if (request == null) {
            throw new BusinessException("没有可修改的字段");
        }
        validateOverviewUpdateRequest(request);
        if (!hasUpdateField(request)) {
            throw new BusinessException("没有可修改的字段");
        }

        StudentOverviewRow current = studentMapper.selectStudentOverviewRowByStudentNo(studentNo);
        if (current == null) {
            throw new BusinessException("学生不存在或已删除");
        }

        String normalizedClassName = null;
        Integer normalizedGradeLevel = null;
        if (request.getClassName() != null) {
            normalizedClassName = normalizeClassNameForUpdate(request.getClassName());
            Integer gradeLevelFromClassName = parseGradeLevel(normalizedClassName);
            if (request.getGradeLevel() != null && !request.getGradeLevel().equals(gradeLevelFromClassName)) {
                throw new BusinessException("年级和班级不一致");
            }
            normalizedGradeLevel = gradeLevelFromClassName;
        }

        if (hasStudentUpdateField(request, normalizedClassName, normalizedGradeLevel)) {
            Integer affected = studentMapper.updateStudentOverviewByStudentNo(
                    studentNo,
                    normalizeName(request.getName()),
                    request.getAge(),
                    normalizedGradeLevel,
                    normalizedClassName,
                    request.getGender()
            );
            if (affected == null || affected == 0) {
                throw new BusinessException("学生不存在或已删除");
            }
        }

        if (request.getGpa() != null) {
            Integer affected = studentMapper.updateStudentPerformanceByStudentNo(
                    studentNo,
                    request.getGpa(),
                    calculateGradeClass(request.getGpa())
            );
            if (affected == null || affected == 0) {
                throw new BusinessException("该学生没有成绩记录，无法修改GPA");
            }
        }

        return toStudentOverviewItemVO(studentMapper.selectStudentOverviewRowByStudentNo(studentNo));
    }

    // 新增或更新学生学业表现
    @Override
    @Transactional
    public StudentDetailVO upsertStudentPerformance(Integer studentNo, StudentPerformanceUpsertRequest request) {
        if (studentNo == null) {
            throw new BusinessException("学生编号不能为空");
        }
        if (request == null) {
            throw new BusinessException("学业表现不能为空");
        }
        if (request.getGradeClass() != null) {
            throw new BusinessException("成绩等级由GPA自动计算，不能手动提交");
        }

        Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, studentNo)
                .eq(Student::getDeleted, 0)
                .last("LIMIT 1"));
        if (student == null) {
            throw new BusinessException("学生不存在或已删除");
        }

        Performance performance = performanceMapper.selectOne(new LambdaQueryWrapper<Performance>()
                .eq(Performance::getStudentId, student.getId())
                .last("LIMIT 1"));

        if (performance == null) {
            performance = new Performance();
            performance.setStudentId(student.getId());
            fillPerformance(performance, request);
            performance.setDataSource("MANUAL");
            performance.setDataQualityStatus(0);
            performanceMapper.insert(performance);
        } else {
            fillPerformance(performance, request);
            performance.setDataSource("MANUAL");
            performance.setDataQualityStatus(0);
            performance.setQualityIssue(null);
            performanceMapper.updateById(performance);
        }

        return selectStudentDetail(studentNo);
    }

    // 软删除学生
    @Override
    @Transactional
    public void deleteStudentOverview(Integer studentNo) {
        if (studentNo == null) {
            throw new BusinessException("学生编号不能为空");
        }

        Integer affected = studentMapper.softDeleteStudentByStudentNo(studentNo);
        if (affected == null || affected == 0) {
            throw new BusinessException("学生不存在或已删除");
        }
    }

    // 查询学生完整详情
    @Override
    @Transactional(readOnly = true)
    public StudentDetailVO selectStudentDetail(Integer studentNo) {
        if (studentNo == null || studentNo <= 0) {
            throw new BusinessException("学生编号不合法");
        }

        StudentDetailAggregateRow row = studentMapper.selectStudentDetailAggregateRow(studentNo);

        if (row == null || row.getOverview() == null) {
            throw new BusinessException("学生不存在或已删除");
        }
        return StudentDetailVO.from(row);
    }

    // 负责关键词搜索
    private StudentOverviewVO searchStudentOverview(StudentOverviewQueryRequest query, StudentSortOption sortOption) {
        // 尝试把 keyword 转换成学号
        String normalizedKeyword = query.getKeyword().trim();

        // 调用 Mapper 搜索
        /* 同时传 normalizedKeyword 和 ParseStudentNo
         * 目的是让同一个关键字既可以搜索姓名，也可以在纯数字时搜索学号
         */
        // Row 转 ItemVO
        List<StudentOverviewItemVO> records = studentMapper
                .searchStudentOverviewRows(
                        normalizedKeyword,
                        parseStudentNo(normalizedKeyword),
                        sortOption.getSortColumn(),
                        sortOption.getSortOrder()
                )
                .stream()
                .map(this::toStudentOverviewItemVO)
                .toList();

        // 包装成 StudentOverviewVO
        StudentOverviewVO vo = new StudentOverviewVO();
        vo.setKeyword(normalizedKeyword);
        vo.setSortField(normalizeSortField(query.getSortField()));
        vo.setSortOrder(sortOption.getSortOrder().toLowerCase());
        vo.setStudentCount((long) records.size());
        vo.setStudents(records);
        return vo;
    }

    /*
        负责组合筛选：
            GPA 范围
            成绩等级
            年级
            性别
            多个班级
            排序
     */
    private StudentOverviewVO filterStudentOverview(
            List<String> classNames,
            StudentOverviewQueryRequest query,
            StudentSortOption sortOption) {
        List<StudentOverviewItemVO> records = studentMapper
                .filterStudentOverviewRows(
                        normalizeMinGpa(query.getMinGpa(), query.getMaxGpa()),
                        normalizeMaxGpa(query.getMinGpa(), query.getMaxGpa()),
                        query.getGradeClass(),
                        query.getGradeLevel(),
                        query.getGender(),
                        classNames,
                        sortOption.getSortColumn(),
                        sortOption.getSortOrder()
                )
                .stream()
                .map(this::toStudentOverviewItemVO)
                .toList();

        StudentOverviewVO vo = new StudentOverviewVO();
        vo.setClassNames(classNames.isEmpty() ? null : classNames);
        vo.setMinGpa(normalizeMinGpa(query.getMinGpa(), query.getMaxGpa()));
        vo.setMaxGpa(normalizeMaxGpa(query.getMinGpa(), query.getMaxGpa()));
        vo.setGradeClass(query.getGradeClass());
        vo.setGradeLevel(query.getGradeLevel());
        vo.setGender(query.getGender());
        vo.setSortField(normalizeSortField(query.getSortField()));
        vo.setSortOrder(sortOption.getSortOrder().toLowerCase());
        vo.setStudentCount((long) records.size());
        vo.setStudents(records);
        return vo;
    }

    // VO 转换：数据库 Row -> 前端列表项 VO
    private StudentOverviewItemVO toStudentOverviewItemVO(StudentOverviewRow row) {
        StudentOverviewItemVO vo = new StudentOverviewItemVO();
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

    /*
        班级参数规范化
        兼容三种输入：
            "1-1"
            ["1-1", "1-2"]
            ["1-1, 1-2"]
        最后统一成 List<String>
        同时进行：
            过滤 null
            拆分逗号
            统一班级格式
            去重
     */
    private List<String> normalizeClassNames(StudentOverviewQueryRequest query) {
        List<String> classNames = query.getClassNames();
        if ((classNames == null || classNames.isEmpty()) && query.getClassName() != null) {
            classNames = List.of(query.getClassName());
        }

        if (classNames == null || classNames.isEmpty()) {
            return List.of();
        }

        return classNames.stream()
                .filter(Objects::nonNull)
                .flatMap(className -> List.of(className.split(",")).stream())
                .map(this::normalizeClassName)
                .filter(className -> !className.isBlank())
                .distinct()
                .toList();
    }

    // 负责把不同格式的班级统一为数据库格式
    private String normalizeClassName(String className) {
        if (className == null || className.isBlank()) {
            return DEFAULT_CLASS_NAME;
        }

        String trimmed = className.trim();
        if (trimmed.matches("\\d+\\s*-\\s*\\d+")) {
            return trimmed.replaceAll("\\s+", "");
        }

        String compact = trimmed.replaceAll("\\s+", "");
        if (!compact.endsWith("班") || compact.length() < 4) {
            return trimmed;
        }

        Integer gradeLevel = switch (compact.substring(0, 2)) {
            case "高一" -> 1;
            case "高二" -> 2;
            case "高三" -> 3;
            default -> null;
        };
        if (gradeLevel == null) {
            return trimmed;
        }

        String classNo = compact.substring(2, compact.length() - 1);
        if (!classNo.matches("\\d+")) {
            return trimmed;
        }
        return gradeLevel + "-" + classNo;
    }

    // 专门用于更新操作，要求班级不能为空且格式必须正确
    private String normalizeClassNameForUpdate(String className) {
        if (className == null || className.isBlank()) {
            throw new BusinessException("班级不能为空");
        }

        String normalizedClassName = normalizeClassName(className);
        if (!normalizedClassName.matches("[1-3]-\\d+")) {
            throw new BusinessException("班级格式错误，应为1-1或高一 1 班");
        }
        return normalizedClassName;
    }

    // 去掉姓名前后空格，并阻止写入空姓名
    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }

        String trimmed = name.trim();
        if (trimmed.isBlank()) {
            throw new BusinessException("学生姓名不能为空");
        }
        return trimmed;
    }

    private String normalizeNameForCreate(String name, Integer studentNo) {
        if (name == null || name.isBlank()) {
            return "Student " + studentNo;
        }
        return normalizeName(name);
    }

    private void validateOverviewUpdateRequest(StudentOverviewUpdateRequest request) {
        if (request.getGradeLevel() != null && request.getClassName() == null) {
            throw new BusinessException("年级不能单独修改，请同时提交班级或只提交班级由后端推导年级");
        }
        if (request.getGradeClass() != null) {
            throw new BusinessException("成绩等级由GPA自动计算，不能手动提交");
        }
    }

    private Integer calculateGradeClass(BigDecimal gpa) {
        if (gpa.compareTo(new BigDecimal("3.5")) >= 0) {
            return 0;
        }
        if (gpa.compareTo(new BigDecimal("3.0")) >= 0) {
            return 1;
        }
        if (gpa.compareTo(new BigDecimal("2.5")) >= 0) {
            return 2;
        }
        if (gpa.compareTo(new BigDecimal("2.0")) >= 0) {
            return 3;
        }
        return 4;
    }

    private void fillPerformance(Performance performance, StudentPerformanceUpsertRequest request) {
        performance.setStudyTimeWeekly(request.getStudyTimeWeekly());
        performance.setAbsences(request.getAbsences());
        performance.setTutoring(toTinyInt(request.getTutoring()));
        performance.setParentalSupport(request.getParentalSupport());
        performance.setExtracurricular(toTinyInt(request.getExtracurricular()));
        performance.setSports(toTinyInt(request.getSports()));
        performance.setMusic(toTinyInt(request.getMusic()));
        performance.setVolunteering(toTinyInt(request.getVolunteering()));
        performance.setGpa(request.getGpa());
        performance.setGradeClass(calculateGradeClass(request.getGpa()));
    }

    private Integer toTinyInt(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    // 判断请求中有没有任何一个可更新字段
    private boolean hasUpdateField(StudentOverviewUpdateRequest request) {
        return request.getName() != null
                || request.getAge() != null
                || request.getGender() != null
                || request.getGradeLevel() != null
                || request.getClassName() != null
                || request.getGpa() != null;
    }

    // 判断本次是否需要更新 student 表
    private boolean hasStudentUpdateField(
            StudentOverviewUpdateRequest request,
            String normalizedClassName,
            Integer normalizedGradeLevel) {
        return request.getName() != null
                || request.getAge() != null
                || request.getGender() != null
                || normalizedClassName != null
                || normalizedGradeLevel != null;
    }

    // 查询模式判断
    private boolean hasFilter(List<String> classNames, StudentOverviewQueryRequest query) {
        return classNames.size() > 1
                || query.getMinGpa() != null
                || query.getMaxGpa() != null
                || query.getGradeClass() != null
                || query.getGradeLevel() != null
                || query.getGender() != null;
    }

    private boolean hasKeyword(StudentOverviewQueryRequest query) {
        return query.getKeyword() != null && !query.getKeyword().isBlank();
    }

    private boolean hasKeywordConflict(List<String> classNames, StudentOverviewQueryRequest query) {
        return !classNames.isEmpty()
                || query.getMinGpa() != null
                || query.getMaxGpa() != null
                || query.getGradeClass() != null
                || query.getGradeLevel() != null
                || query.getGender() != null;
    }

    private BigDecimal normalizeMinGpa(BigDecimal minGpa, BigDecimal maxGpa) {
        if (minGpa == null || maxGpa == null || minGpa.compareTo(maxGpa) <= 0) {
            return minGpa;
        }
        return maxGpa;
    }

    private BigDecimal normalizeMaxGpa(BigDecimal minGpa, BigDecimal maxGpa) {
        if (minGpa == null || maxGpa == null || minGpa.compareTo(maxGpa) <= 0) {
            return maxGpa;
        }
        return minGpa;
    }

    private StudentSortOption normalizeSortOption(String sortField, String sortOrder) {
        return new StudentSortOption(normalizeSortColumn(sortField), normalizeSortOrder(sortOrder));
    }

    private String normalizeSortField(String sortField) {
        if (sortField == null || sortField.isBlank()) {
            return "studentNo";
        }

        return switch (sortField.trim()) {
            case "studentNo", "name", "age", "gpa", "gradeClass" -> sortField.trim();
            default -> "studentNo";
        };
    }

    private String normalizeSortColumn(String sortField) {
        if (sortField == null || sortField.isBlank()) {
            return DEFAULT_SORT_COLUMN;
        }

        return switch (sortField.trim()) {
            case "gpa" -> "p.gpa";
            case "studentNo" -> "s.student_no";
            case "name" -> "s.name";
            case "age" -> "s.age";
            case "gradeClass" -> "p.grade_class";
            default -> DEFAULT_SORT_COLUMN;
        };
    }

    private String normalizeSortOrder(String sortOrder) {
        if (sortOrder == null || sortOrder.isBlank()) {
            return DEFAULT_SORT_ORDER;
        }

        return switch (sortOrder.trim().toLowerCase()) {
            case "asc" -> "ASC";
            case "desc" -> "DESC";
            default -> DEFAULT_SORT_ORDER;
        };
    }

    // 从 1-3 中提取：1
    private Integer parseGradeLevel(String className) {
        if (className == null || !className.matches("\\d+-\\d+")) {
            return null;
        }
        return Integer.valueOf(className.substring(0, className.indexOf("-")));
    }

    private Integer parseStudentNo(String keyword) {
        if (!keyword.matches("\\d+")) {
            return null;
        }

        try {
            return Integer.valueOf(keyword);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
