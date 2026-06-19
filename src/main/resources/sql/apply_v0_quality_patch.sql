USE student_analytics;

ALTER TABLE student_performance
    ADD COLUMN data_quality_status TINYINT NOT NULL DEFAULT 0 COMMENT '0 NORMAL, 1 WARNING, 2 INVALID' AFTER data_source,
    ADD COLUMN quality_issue VARCHAR(255) NULL COMMENT '数据质量原因' AFTER data_quality_status,
    ADD CONSTRAINT chk_perf_data_quality_status CHECK (data_quality_status IN (0, 1, 2));

DELETE FROM dict_item
WHERE dict_type = 'grade_class';

INSERT INTO dict_item (dict_type, item_code, item_label, sort_order, description)
VALUES ('grade_class', 0, 'A', 0, '3.5 <= GPA <= 4.0'),
       ('grade_class', 1, 'B', 1, '3.0 <= GPA < 3.5'),
       ('grade_class', 2, 'C', 2, '2.5 <= GPA < 3.0'),
       ('grade_class', 3, 'D', 3, '2.0 <= GPA < 2.5'),
       ('grade_class', 4, 'F', 4, 'GPA < 2.0');

UPDATE student_performance
SET data_quality_status = 1,
    quality_issue = 'GRADE_CLASS_GPA_MISMATCH'
WHERE grade_class <> CASE
    WHEN gpa >= 3.5 THEN 0
    WHEN gpa >= 3.0 THEN 1
    WHEN gpa >= 2.5 THEN 2
    WHEN gpa >= 2.0 THEN 3
    ELSE 4
END;

UPDATE student_performance
SET data_quality_status = 0,
    quality_issue = NULL
WHERE grade_class = CASE
    WHEN gpa >= 3.5 THEN 0
    WHEN gpa >= 3.0 THEN 1
    WHEN gpa >= 2.5 THEN 2
    WHEN gpa >= 2.0 THEN 3
    ELSE 4
END;
