import csv
import json
import os
import random
import sys
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Final

import pymysql.cursors

from student_name_generator import StudentNameGenerator

EXPECTED_COLUMNS: Final[list[str]] = [
    "StudentID",
    "Age",
    "Gender",
    "Ethnicity",
    "ParentalEducation",
    "StudyTimeWeekly",
    "Absences",
    "Tutoring",
    "ParentalSupport",
    "Extracurricular",
    "Sports",
    "Music",
    "Volunteering",
    "GPA",
    "GradeClass",
]

FLOAT_COLUMNS: Final[list[str]] = ["StudyTimeWeekly", "GPA"]
CLASS_COUNT_BY_GRADE: Final[dict[int, int]] = {1: 17, 2: 12, 3: 17}
CLASS_ASSIGNMENT_RANDOM_SEED: Final[int] = 20260623

RANGE_RULES: Final[dict[str, tuple[float, float]]] = {
    "StudentID": (1, 2_147_483_647),
    "Age": (0, 30),
    "Gender": (0, 1),
    "Ethnicity": (0, 3),
    "ParentalEducation": (0, 4),
    "StudyTimeWeekly": (0, 60),
    "Absences": (0, 30),
    "Tutoring": (0, 1),
    "ParentalSupport": (0, 4),
    "Extracurricular": (0, 1),
    "Sports": (0, 1),
    "Music": (0, 1),
    "Volunteering": (0, 1),
    "GPA": (0, 4),
    "GradeClass": (0, 4),
}


def load_env_file(env_path: Path):
    """从 .env 文件加载数据库环境变量。"""
    if not env_path.exists():
        return

    for line in env_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip())


def get_required_env(name: str):
    """读取必需环境变量，缺失时直接报错。"""
    value = os.getenv(name)
    if value is None or value == "":
        raise RuntimeError(f"缺少环境变量: {name}")
    return value


def create_db_connection():
    """创建 MySQL 数据库连接。"""
    load_env_file(Path(__file__).with_name(".env"))
    return pymysql.connect(
        host=os.getenv("MYSQL_HOST", "localhost"),
        user=get_required_env("MYSQL_USER"),
        password=get_required_env("MYSQL_PASSWORD"),
        database=os.getenv("MYSQL_DATABASE", "student_analytics"),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


@dataclass
class ImportResult:
    valid_rows: list[dict[str, int | float]]
    invalid_rows: list[dict[str, object]]


@dataclass
class StudentData:
    student_no: int
    age: int
    grade_level: int
    class_name: str
    gender: int
    ethnicity: int
    parental_education: int
    name: str | None = None


@dataclass
class PerformanceData:
    student_no: int
    study_time_weekly: float
    absences: int
    tutoring: int
    parental_support: int
    extracurricular: int
    sports: int
    music: int
    volunteering: int
    gpa: float
    grade_class: int
    data_source: str = "CSV"
    data_quality_status: int = 0
    quality_issue: str | None = None


@dataclass
class SplitRow:
    student: StudentData
    performance: PerformanceData


class GradeClassAllocator:
    """按年龄均衡分配年级，并在每个年级内均衡随机分班。"""

    def __init__(self, seed: int = CLASS_ASSIGNMENT_RANDOM_SEED):
        self._random = random.Random(seed)
        self._age_grade_counts: dict[tuple[int, int], int] = {}
        self._class_counts: dict[int, dict[int, int]] = {
            grade_level: {class_no: 0 for class_no in range(1, class_count + 1)}
            for grade_level, class_count in CLASS_COUNT_BY_GRADE.items()
        }

    def assign(self, age: int) -> tuple[int, str]:
        grade_level = self._assign_grade_level(age)
        class_no = self._assign_class_no(grade_level)
        return grade_level, f"{grade_level}-{class_no}"

    def _assign_grade_level(self, age: int) -> int:
        if age <= 15:
            return 1
        if age == 16:
            return self._assign_balanced_age_grade(age, [1, 2])
        if age == 17:
            return self._assign_balanced_age_grade(age, [2, 3])
        return 3

    def _assign_balanced_age_grade(self, age: int, candidates: list[int]) -> int:
        min_count = min(self._age_grade_counts.get((age, grade), 0) for grade in candidates)
        least_used = [
            grade
            for grade in candidates
            if self._age_grade_counts.get((age, grade), 0) == min_count
        ]
        grade_level = self._random.choice(least_used)
        self._age_grade_counts[(age, grade_level)] = (
            self._age_grade_counts.get((age, grade_level), 0) + 1
        )
        return grade_level

    def _assign_class_no(self, grade_level: int) -> int:
        class_counts = self._class_counts[grade_level]
        min_count = min(class_counts.values())
        least_used = [
            class_no
            for class_no, count in class_counts.items()
            if count == min_count
        ]
        class_no = self._random.choice(least_used)
        class_counts[class_no] += 1
        return class_no


def validate_header(header: list[str] | None):
    """校验 CSV 表头字段和顺序是否符合预期。"""
    if header is None:
        raise ValueError("header 为空")

    missing_columns = set(EXPECTED_COLUMNS) - set(header)
    unknown_columns = set(header) - set(EXPECTED_COLUMNS)

    if missing_columns:
        raise ValueError(f"CSV 表头缺少字段: {sorted(missing_columns)}")

    if unknown_columns:
        raise ValueError(f"CSV 表头存在未知字段: {sorted(unknown_columns)}")

    if header != EXPECTED_COLUMNS:
        raise ValueError("CSV 表头字段顺序与 EXPECTED_COLUMNS 不一致")


def validate_cell(column: str, value: int | float):
    """校验单个字段值是否在允许范围内。"""
    if column not in RANGE_RULES:
        return f"未知字段：{column}"

    min_val, max_val = RANGE_RULES[column]

    if min_val <= value <= max_val:
        return None
    return f"{column} out of range: {value}"


def validate_row(row: dict[str, int | float]):
    """校验一整行数据，返回错误列表。"""
    errors: list[str] = []

    missing_columns = set(EXPECTED_COLUMNS) - set(row.keys())
    unknown_columns = set(row.keys()) - set(EXPECTED_COLUMNS)

    for column in missing_columns:
        errors.append(f"缺少字段：{column}")

    for column in unknown_columns:
        errors.append(f"未知字段：{column}")

    for column, value in row.items():
        err = validate_cell(column, value)
        if err is not None:
            errors.append(err)

    return errors


def convert_row(row: dict[str, str]):
    """把 CSV 字符串行转换成后续校验使用的数值行。"""
    converted_row = {}
    for column, value in row.items():
        number = float(value)
        if column not in FLOAT_COLUMNS:
            if not number.is_integer():
                raise ValueError(f"{column} should be integer: {value}")
            converted_row[column] = int(number)
        else:
            converted_row[column] = number
    return converted_row


def parse_and_validate_row(row_number: int, row: dict[str, str]):
    """转换并校验单行 CSV 数据，返回合法行或错误记录。"""
    try:
        converted = convert_row(row)
    except (TypeError, ValueError) as exc:
        return None, {
            "row_number": row_number,
            "data": row,
            "errors": [f"字段类型转换失败: {exc}"],
        }

    errors = validate_row(converted)
    if errors:
        return None, {
            "row_number": row_number,
            "data": row,
            "errors": errors,
        }
    return converted, None


def load_student_data_from_csv(csv_path: str | Path, encoding: str = "utf-8"):
    """读取 CSV 文件，并按合法和非法数据分类。"""
    path = Path(csv_path)
    if not path.exists():
        raise FileNotFoundError(f"CSV 文件不存在: {path.resolve()}")
    print(f"正在读取文件: {path.resolve()}\n")

    with path.open("r", encoding=encoding, newline="") as csv_file:
        reader = csv.DictReader(csv_file)
        header = reader.fieldnames
        validate_header(header)

        valid_rows: list[dict[str, int | float]] = []  # 存储验证通过的行
        invalid_rows: list[dict[str, object]] = []  # 存储验证失败的行

        for row_number, row in enumerate(reader, start=2):
            valid_row, invalid_row = parse_and_validate_row(row_number, row)

            if invalid_row is not None:
                invalid_rows.append(invalid_row)
            else:
                valid_rows.append(valid_row)

        print(
            f"count = {len(valid_rows) + len(invalid_rows)}",
            "\n",
            f"valid_count = {len(valid_rows)}",
            "\n",
            f"invalid_count = {len(invalid_rows)}",
        )
        return ImportResult(valid_rows, invalid_rows)


def format_invalid_row_errors(errors: object) -> str:
    """把非法行错误列表格式化成报告中的文本。"""
    if isinstance(errors, list):
        return "; ".join(str(error) for error in errors)
    return str(errors)


def write_invalid_rows_report(
        invalid_rows: list[dict[str, object]], report_path: Path
) -> Path | None:
    """把非法 CSV 行写入报告文件，没有非法行时不生成文件。"""
    if not invalid_rows:
        return None

    report_path.parent.mkdir(parents=True, exist_ok=True)

    with report_path.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=["row_number", "data", "errors"])
        writer.writeheader()

        for row in invalid_rows:
            writer.writerow({
                "row_number": row.get("row_number", ""),
                "data": json.dumps(row.get("data", {}), ensure_ascii=False),
                "errors": format_invalid_row_errors(row.get("errors", [])),
            })

    return report_path


def calculate_grade_class(gpa: float) -> int:
    if gpa >= 3.5:
        return 0
    if gpa >= 3.0:
        return 1
    if gpa >= 2.5:
        return 2
    if gpa >= 2.0:
        return 3
    return 4


def assess_performance_quality(row: dict[str, int | float]) -> tuple[int, str | None]:
    expected_grade_class = calculate_grade_class(row["GPA"])
    actual_grade_class = row["GradeClass"]

    if expected_grade_class != actual_grade_class:
        return (1,
                f"GPA={row['GPA']} 对应 GradeClass={expected_grade_class}, CSV 中为 {actual_grade_class}，已按 GPA 重算入库")
    return 0, None


def split_row(
        row: dict[str, int | float],
        generator: StudentNameGenerator,
        allocator: GradeClassAllocator
):
    """把一行合法 CSV 数据拆成学生信息和成绩表现数据。"""
    grade_level, class_name = allocator.assign(row["Age"])

    student = StudentData(
        student_no=row["StudentID"],
        name=generator.generate(),
        age=row["Age"],
        grade_level=grade_level,
        class_name=class_name,
        gender=row["Gender"],
        ethnicity=row["Ethnicity"],
        parental_education=row["ParentalEducation"],
    )

    quality_status, quality_issue = assess_performance_quality(row)
    grade_class = calculate_grade_class(row["GPA"])

    performance = PerformanceData(
        student_no=row["StudentID"],
        study_time_weekly=row["StudyTimeWeekly"],
        absences=row["Absences"],
        tutoring=row["Tutoring"],
        parental_support=row["ParentalSupport"],
        extracurricular=row["Extracurricular"],
        sports=row["Sports"],
        music=row["Music"],
        volunteering=row["Volunteering"],
        gpa=row["GPA"],
        grade_class=grade_class,
        data_quality_status=quality_status,
        quality_issue=quality_issue
    )

    return SplitRow(student, performance)


def split_rows(import_result: ImportResult):
    """把所有合法 CSV 行拆成后续可入库的数据对象。"""
    split_result: list[SplitRow] = []
    name_generator = StudentNameGenerator()
    allocator = GradeClassAllocator()

    valid_rows = import_result.valid_rows
    for valid_row in valid_rows:
        split_result.append(split_row(valid_row, name_generator, allocator))
    return split_result


def clear_student_tables(conn):
    """导入前清空学生与表现表，并重置自增主键。"""
    statements = [
        "DELETE FROM student_performance",
        "DELETE FROM student",
        "ALTER TABLE student_performance AUTO_INCREMENT = 1",
        "ALTER TABLE student AUTO_INCREMENT = 1",
    ]

    with conn.cursor() as cursor:
        for statement in statements:
            cursor.execute(statement)


def insert_students(conn, student_rows: list[StudentData]) -> int:
    """批量插入 student 表；如果 student_no 已存在，则更新基础信息。"""
    if not student_rows:
        return 0

    sql = """
          INSERT INTO student (student_no,
                               age,
                               grade_level,
                               class_name,
                               gender,
                               ethnicity,
                               parental_education,
                               name)
          VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
          ON DUPLICATE KEY UPDATE age                = VALUES(age),
                                  grade_level        = VALUES(grade_level),
                                  class_name         = VALUES(class_name),
                                  gender             = VALUES(gender),
                                  ethnicity          = VALUES(ethnicity),
                                  parental_education = VALUES(parental_education),
                                  name               = VALUES(name) \
          """

    params = [
        (
            student.student_no,
            student.age,
            student.grade_level,
            student.class_name,
            student.gender,
            student.ethnicity,
            student.parental_education,
            student.name,
        )
        for student in student_rows
    ]

    with conn.cursor() as cursor:
        affected_rows = cursor.executemany(sql, params)

    return affected_rows


def fetch_student_id_map(conn, student_rows: list[StudentData]):
    # TODO: 根据 student_no 批量查询数据库主键 id，返回 student_no -> id 映射。
    student_nos = [s.student_no for s in student_rows]
    placeholders = ", ".join(["%s"] * len(student_nos))
    sql = f"SELECT id, student_no FROM student WHERE student_no IN ({placeholders})"

    with conn.cursor() as cursor:
        cursor.execute(sql, student_nos)
        rows = cursor.fetchall()

    student_id_map = {
        int(row["student_no"]): int(row["id"])
        for row in rows
    }

    missing_student_nos = set(student_nos) - set(student_id_map.keys())
    if missing_student_nos:
        raise RuntimeError(f"部分 student_no 没有查到数据库 id: {sorted(missing_student_nos)}")

    return student_id_map


def insert_performances(
        conn,
        performance_rows: list[PerformanceData],
        student_id_map: dict[int, int]
) -> int:
    """批量插入 student_performance 表；如果学生表现记录已存在，则更新表现数据。"""
    if not performance_rows:
        return 0

    sql = """
          INSERT INTO student_performance (student_id,
                                           study_time_weekly,
                                           absences,
                                           tutoring,
                                           parental_support,
                                           extracurricular,
                                           sports,
                                           music,
                                           volunteering,
                                           gpa,
                                           grade_class,
                                           data_source,
                                           data_quality_status,
                                           quality_issue)
          VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
          ON DUPLICATE KEY UPDATE study_time_weekly   = VALUES(study_time_weekly),
                                  absences            = VALUES(absences),
                                  tutoring            = VALUES(tutoring),
                                  parental_support    = VALUES(parental_support),
                                  extracurricular     = VALUES(extracurricular),
                                  sports              = VALUES(sports),
                                  music               = VALUES(music),
                                  volunteering        = VALUES(volunteering),
                                  gpa                 = VALUES(gpa),
                                  grade_class         = VALUES(grade_class),
                                  data_source         = VALUES(data_source),
                                  data_quality_status = VALUES(data_quality_status),
                                  quality_issue       = VALUES(quality_issue)
          """

    params = []
    for performance in performance_rows:
        student_id = student_id_map.get(performance.student_no)
        if student_id is None:
            raise RuntimeError(f"student_no={performance.student_no} 没有对应的 student.id")

        params.append((
            student_id,
            performance.study_time_weekly,
            performance.absences,
            performance.tutoring,
            performance.parental_support,
            performance.extracurricular,
            performance.sports,
            performance.music,
            performance.volunteering,
            performance.gpa,
            performance.grade_class,
            performance.data_source,
            performance.data_quality_status,
            performance.quality_issue,
        ))

    with conn.cursor() as cursor:
        affected_rows = cursor.executemany(sql, params)

    return affected_rows


def import_to_database(split_result: list[SplitRow]):
    """用一个事务完成 student 与 student_performance 的导入。"""
    conn = create_db_connection()
    student_rows = [row.student for row in split_result]
    performance_rows = [row.performance for row in split_result]

    try:
        clear_student_tables(conn)
        student_affected_rows = insert_students(conn, student_rows)
        student_id_map = fetch_student_id_map(conn, student_rows)
        performance_affected_rows = insert_performances(conn, performance_rows, student_id_map)

        conn.commit()
        print(f"insert_students affected_rows = {student_affected_rows}")
        print(f"insert_performances affected_rows = {performance_affected_rows}")

    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def main() -> int:
    """运行 CSV 读取、校验和拆分流程。"""
    status = 0
    project_root = Path(__file__).resolve().parents[2]
    import_result = load_student_data_from_csv(
        project_root / "dataset" / "Student_performance_data_.csv"
    )
    report_path = write_invalid_rows_report(
        import_result.invalid_rows,
        project_root
        / "reports"
        / f"student_import_invalid_rows_{datetime.now():%Y%m%d_%H%M%S}.csv",
    )
    if report_path is not None:
        print(f"invalid_rows_report = {report_path}")
        status = 1

    split_result = split_rows(import_result)
    print(f"split_count = {len(split_result)}")

    import_to_database(split_result)
    return status


if __name__ == "__main__":
    sys.exit(main())
