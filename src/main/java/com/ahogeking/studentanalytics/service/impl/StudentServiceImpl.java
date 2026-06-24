package com.ahogeking.studentanalytics.service.impl;

import com.ahogeking.studentanalytics.dto.StudentOverviewQueryRequest;
import com.ahogeking.studentanalytics.dto.row.StudentOverviewRow;
import com.ahogeking.studentanalytics.dto.StudentSortOption;
import com.ahogeking.studentanalytics.mapper.StudentMapper;
import com.ahogeking.studentanalytics.service.StudentService;
import com.ahogeking.studentanalytics.vo.ClassInfoVO;
import com.ahogeking.studentanalytics.vo.GenderEnum;
import com.ahogeking.studentanalytics.vo.GradeClassEnum;
import com.ahogeking.studentanalytics.vo.StudentFilterOptionsVO;
import com.ahogeking.studentanalytics.vo.StudentOverviewItemVO;
import com.ahogeking.studentanalytics.vo.StudentOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    @Override
    public List<StudentOverviewItemVO> selectAllStudentOverviewItems() {
        StudentSortOption sortOption = normalizeSortOption(null, null);
        return studentMapper.selectStudentOverviewRows(sortOption.getSortColumn(), sortOption.getSortOrder()).stream()
                .map(this::toStudentOverviewItemVO)
                .toList();
    }

    @Override
    public StudentOverviewVO selectStudentOverview(StudentOverviewQueryRequest query) {
        StudentOverviewQueryRequest safeQuery = query == null ? new StudentOverviewQueryRequest() : query;
        List<String> normalizedClassNames = normalizeClassNames(safeQuery);
        StudentSortOption sortOption = normalizeSortOption(safeQuery.getSortField(), safeQuery.getSortOrder());

        if (safeQuery.getKeyword() != null && !safeQuery.getKeyword().isBlank()) {
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

    private StudentOverviewVO searchStudentOverview(StudentOverviewQueryRequest query, StudentSortOption sortOption) {
        String normalizedKeyword = query.getKeyword().trim();
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

        StudentOverviewVO vo = new StudentOverviewVO();
        vo.setKeyword(normalizedKeyword);
        vo.setSortField(normalizeSortField(query.getSortField()));
        vo.setSortOrder(sortOption.getSortOrder().toLowerCase());
        vo.setStudentCount((long) records.size());
        vo.setStudents(records);
        return vo;
    }

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

    private boolean hasFilter(List<String> classNames, StudentOverviewQueryRequest query) {
        return classNames.size() > 1
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
