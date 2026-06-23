USE student_analytics;

ALTER TABLE student
    ADD COLUMN grade_level TINYINT NOT NULL DEFAULT 1 COMMENT '1 高一, 2 高二, 3 高三' AFTER age,
    ADD COLUMN class_name VARCHAR(10) NOT NULL DEFAULT '1-1' COMMENT '年级-班级，例如 1-3 表示高一 3 班' AFTER grade_level,
    ADD KEY idx_student_grade_level_class_name (grade_level, class_name),
    ADD CONSTRAINT chk_student_grade_level CHECK (grade_level IN (1, 2, 3));
