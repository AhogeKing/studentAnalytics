import csv
import os
from dataclasses import dataclass
from pathlib import Path
from typing import Final

import pymysql.cursors

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
    if not env_path.exists():
        return

    for line in env_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip())


def get_required_env(name: str):
    value = os.getenv(name)
    if value is None or value == "":
        raise RuntimeError(f"缺少环境变量: {name}")
    return value


def create_db_connection():
    load_env_file(Path(__file__).with_name(".env"))
    return pymysql.connect(
        host=os.getenv("MYSQL_HOST", "localhost"),
        user=get_required_env("MYSQL_USER"),
        password=get_required_env("MYSQL_PASSWORD"),
        database=os.getenv("MYSQL_DATABASE", "student_analytics"),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


def validate_header(header: list[str] | None):
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
    """
    验证某个字段值是否在允许范围内。
    合法返回 None，非法返回错误信息。
    """
    if column not in RANGE_RULES:
        return f"未知字段：{column}"

    min_val, max_val = RANGE_RULES[column]

    if min_val <= value <= max_val:
        return None
    return f"{column} out of range: {value}"


def validate_row(row: dict[str, int | float]):
    """
    验证一整行数据是否完整、是否有未知字段、字段值是否在范围内。
    返回 errors；空列表表示该行合法。
    """
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
    converted_row = {}
    for column, value in row.items():
        if column in FLOAT_COLUMNS:
            converted_row[column] = float(value)
        else:
            converted_row[column] = int(float(value))
    return converted_row


def convert_and_validate_student_data_from_csv(csv_path: str | Path, encoding: str = "utf-8"):
    path = Path(csv_path)
    if not path.exists():
        raise FileNotFoundError(f"CSV 文件不存在: {csv_path.resolve()}")
    print(f"正在读取文件: {csv_path.resolve()}\n")

    with path.open("r", encoding=encoding, newline="") as csv_file:
        reader = csv.DictReader(csv_file)
        header = reader.fieldnames
        validate_header(header)

        valid_rows: list[dict[str, int | float]] = []  # 存储验证通过的行
        invalid_rows: list[dict[str, object]] = []  # 存储验证失败的行

        for row_number, row in enumerate(reader, start=2):
            converted_row = convert_row(row)
            row_errors = validate_row(converted_row)

            if row_errors:
                invalid_rows.append({
                    "row_number": row_number,
                    "data": converted_row,
                    "errors": row_errors
                })
            else:
                valid_rows.append(converted_row)

        # print(valid_row, "\n")
        print(
            f"count = {len(valid_rows) + len(invalid_rows)}",
            "\n",
            f"valid_count = {len(valid_rows)}",
            "\n",
            f"invalid_count = {len(invalid_rows)}",
        )
        return valid_rows, invalid_rows


if __name__ == "__main__":
    converted_rows = convert_and_validate_student_data_from_csv(
        Path(__file__).resolve().parents[2]
        / "dataset"
        / "Student_performance_data_.csv"
    )

# with conn:
#     with conn.cursor() as cursor:
#         # read a single record
#         sql = "SELECT * FROM `dict_item`"
#         cursor.execute(sql)
#         result = cursor.fetchall()
#         print(result)
