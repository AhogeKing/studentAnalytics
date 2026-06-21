import csv
import os
import sys
from dataclasses import dataclass
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


def split_row(row: dict[str, int | float], generator: StudentNameGenerator):
    """把一行合法 CSV 数据拆成学生信息和成绩表现数据。"""
    student = StudentData(
        student_no=row["StudentID"],
        name=generator.generate(),
        age=row["Age"],
        gender=row["Gender"],
        ethnicity=row["Ethnicity"],
        parental_education=row["ParentalEducation"],
    )

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
        grade_class=row["GradeClass"],
    )

    return SplitRow(student, performance)


def split_rows(import_result: ImportResult):
    """把所有合法 CSV 行拆成后续可入库的数据对象。"""
    split_result: list[SplitRow] = []
    name_generator = StudentNameGenerator()

    valid_rows = import_result.valid_rows
    for valid_row in valid_rows:
        split_result.append(split_row(valid_row, name_generator))
    return split_result


def main() -> int:
    """运行 CSV 读取、校验和拆分流程。"""
    import_result = load_student_data_from_csv(
        Path(__file__).resolve().parents[2]
        / "dataset"
        / "Student_performance_data_.csv"
    )
    split_result = split_rows(import_result)
    print(f"split_count = {len(split_result)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

# with conn:
#     with conn.cursor() as cursor:
#         # read a single record
#         sql = "SELECT * FROM `dict_item`"
#         cursor.execute(sql)
#         result = cursor.fetchall()
#         print(result)
