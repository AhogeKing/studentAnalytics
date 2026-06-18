CREATE
    DATABASE IF NOT EXISTS student_analytics
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE student_analytics;

SET
    FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS import_batch;
DROP TABLE IF EXISTS warning_record;
DROP TABLE IF EXISTS prediction_result;
DROP TABLE IF EXISTS model_version;
DROP TABLE IF EXISTS student_performance;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS dict_item;
DROP TABLE IF EXISTS sys_user;
SET
    FOREIGN_KEY_CHECKS = 1;

-- 1. sys_user：用户表
CREATE TABLE sys_user
(
    id         INT                                  NOT NULL AUTO_INCREMENT COMMENT 'Primary Key',
    username   VARCHAR(50)                          NOT NULL COMMENT 'Login username',
    password   VARCHAR(255)                         NOT NULL COMMENT '加密后的密码',
    real_name  VARCHAR(50)                          NULL COMMENT '学生名称',
    role       ENUM ('STUDENT', 'TEACHER', 'ADMIN') NOT NULL DEFAULT 'TEACHER' COMMENT '角色权限',
    status     TINYINT UNSIGNED                     NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    created_at DATETIME                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME                             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='System user table';

-- 2. Dictionary table: stores labels for coded fields from the dataset.
CREATE TABLE dict_item
(
    id          INT          NOT NULL AUTO_INCREMENT,
    dict_type   VARCHAR(50)  NOT NULL COMMENT 'e.g. gender, ethnicity, parental_education, parental_support, grade_class',
    item_code   TINYINT      NOT NULL COMMENT 'Code value in dataset',
    item_label  VARCHAR(100) NOT NULL COMMENT 'Human-readable label',
    sort_order  INT          NOT NULL DEFAULT 0,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type_code (dict_type, item_code),
    KEY idx_dict_type (dict_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Dictionary item table';

INSERT INTO dict_item (dict_type, item_code, item_label, sort_order, description)
VALUES ('gender', 0, 'Male', 0, '男'),
       ('gender', 1, 'Female', 1, '女'),
       ('ethnicity', 0, 'Caucasian', 0, '高加索 / 白 人'),
       ('ethnicity', 1, 'African American', 1, '非裔'),
       ('ethnicity', 2, 'Asian', 2, '亚裔'),
       ('ethnicity', 3, 'Other', 3, '其它'),
       ('parental_education', 0, 'None', 0, '无受教育经历'),
       ('parental_education', 1, 'High School', 1, '高中'),
       ('parental_education', 2, 'Some College', 2, '大专'),
       ('parental_education', 3, 'Bachelor''s', 3, '本科'),
       ('parental_education', 4, 'Higher', 4, '更高学历'),
       ('parental_support', 0, 'None', 0, '无'),
       ('parental_support', 1, 'Low', 1, '低'),
       ('parental_support', 2, 'Moderate', 2, '中等'),
       ('parental_support', 3, 'High', 3, '高'),
       ('parental_support', 4, 'Very High', 4, '极高'),
       ('yes_no', 0, 'no', 0, '否'),
       ('yes_no', 1, 'yes', 1, '是'),
       ('grade_class', 0, 'A', 0, '3.5 <= GPA <= 4.0'),
       ('grade_class', 1, 'B', 1, '3.0 <= GPA < 3.5'),
       ('grade_class', 2, 'C', 2, '2.5 <= GPA < 3.0'),
       ('grade_class', 3, 'D', 3, '2.0 <= GPA < 2.5'),
       ('grade_class', 4, 'F', 4, 'GPA < 2.0');

-- 3，学生基础信息表
CREATE TABLE student
(
    id                 INT         NOT NULL AUTO_INCREMENT,
    student_no         INT         NOT NULL COMMENT '原始学生编号，即 CSV 文件中的学生 id',
    name               VARCHAR(50) NULL COMMENT '可选择的用于展示的学生姓名；CSV 中并没有此类数据，可以人工生成',
    age                INT         NOT NULL COMMENT '年龄，数据集中的年龄普遍在 15 - 18',
    gender             TINYINT     NOT NULL COMMENT '0/1, see dict_type = gender',
    ethnicity          TINYINT     NOT NULL COMMENT '0-3, see dict_type = ethnicity',
    parental_education TINYINT     NOT NULL COMMENT '0-4, see dict_type = parental_education',
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted            TINYINT     NOT NULL DEFAULT 0 COMMENT 'Soft delete flag: 0 normal, 1 deleted',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_no (student_no),
    KEY idx_student_age (age),
    KEY idx_student_gender (gender),
    KEY idx_student_ethnicity (ethnicity),
    KEY idx_student_parental_education (parental_education),
    CONSTRAINT chk_student_age CHECK (age BETWEEN 0 AND 120),
    CONSTRAINT chk_student_gender CHECK (gender IN (0, 1)),
    CONSTRAINT chk_student_ethnicity CHECK (ethnicity IN (0, 1, 2, 3)),
    CONSTRAINT chk_student_parental_education CHECK (parental_education IN (0, 1, 2, 3, 4)),
    CONSTRAINT chk_student_deleted CHECK (deleted IN (0, 1))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Student basic information table';

-- 4. 学生学习表现表
CREATE TABLE student_performance
(
    id                INT                    NOT NULL AUTO_INCREMENT,
    student_id        INT                    NOT NULL COMMENT 'FK to student.id',
    study_time_weekly DECIMAL(7, 4)          NOT NULL COMMENT 'Weekly study',
    absences          INT                    NOT NULL COMMENT 'Absence count, dataset range 0-29 / 30',
    tutoring          TINYINT                NOT NULL COMMENT '0 no, 1 yes',
    parental_support  TINYINT                NOT NULL COMMENT '0-4, see dict_type = parental_support',
    extracurricular   TINYINT                NOT NULL COMMENT '0 no, 1 yes',
    sports            TINYINT                NOT NULL COMMENT '0 no, 1',
    music             TINYINT                NOT NULL COMMENT '0 no, 1',
    volunteering      TINYINT                NOT NULL COMMENT '0 no, 1 yes',
    gpa               DECIMAL(5, 4)          NOT NULL COMMENT 'GPA, dataset range 0-4',
    grade_class       TINYINT                NOT NULL COMMENT '0 A, 1 B, 2 C, 3 D, 4 F',
    data_source       ENUM ('CSV', 'MANUAL') NOT NULL DEFAULT 'CSV' COMMENT 'Record source',
    created_at        DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_performance_student_id (student_id),
    KEY idx_perf_grade_class (grade_class),
    KEY idx_perf_gpa (gpa),
    KEY idx_perf_absences (absences),
    KEY idx_perf_study_time_weekly (study_time_weekly),
    CONSTRAINT fk_perf_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_perf_study_time CHECK (study_time_weekly >= 0 AND study_time_weekly <= 60),
    CONSTRAINT chk_perf_absences CHECK (absences >= 0 AND absences <= 30),
    CONSTRAINT chk_perf_tutoring CHECK (tutoring IN (0, 1)),
    CONSTRAINT chk_perf_parental_support CHECK (parental_support IN (0, 1, 2, 3, 4)),
    CONSTRAINT chk_perf_extracurricular CHECK (extracurricular IN (0, 1)),
    CONSTRAINT chk_perf_sports CHECK (sports IN (0, 1)),
    CONSTRAINT chk_perf_music CHECK (music IN (0, 1)),
    CONSTRAINT chk_perf_volunteering CHECK (volunteering IN (0, 1)),
    CONSTRAINT chk_perf_gpa CHECK (gpa >= 0 AND gpa <= 4),
    CONSTRAINT chk_perf_grade_class CHECK (grade_class IN (0, 1, 2, 3, 4))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Student learning behavior and performance table';

-- 5. 模型版本表，用于保存模型版本信息
CREATE TABLE model_version
(
    id                    INT           NOT NULL AUTO_INCREMENT,
    model_name            VARCHAR(100)  NOT NULL DEFAULT '模型名称 grade_class_decision_tree',
    version_no            VARCHAR(50)   NOT NULL COMMENT '模型版本 e.g. at_20260617_01',
    algorithm             VARCHAR(50)   NOT NULL COMMENT '算法名称 DecisionTreeClassifier',
    feature_columns       JSON          NOT NULL COMMENT '输入特征字段 Input feature list; do not include GPA when predicting GradeClass',
    target_column         VARCHAR(50)   NOT NULL DEFAULT '预测目标字段 GradeClass',
    criterion             VARCHAR(50)   NULL COMMENT 'gini/entropy/log_loss',
    max_depth             INT           NULL COMMENT '决策树最大深度',
    min_samples_leaf      INT           NULL COMMENT '叶子节点最小样本数',
    accuracy              DECIMAL(6, 4) NULL COMMENT '准确率',
    precision_macro       DECIMAL(6, 4) NULL COMMENT '宏平均 Precision',
    recall_macro          DECIMAL(6, 4) NULL COMMENT '宏平均 Recall',
    f1_macro              DECIMAL(6, 4) NULL COMMENT '宏平均 f10',
    confusion_matrix_json JSON          NULL COMMENT '混淆矩阵',
    model_path            VARCHAR(255)  NULL COMMENT '模型文件路径',
    encoder_path          VARCHAR(255)  NULL COMMENT '编码器文件路径',
    is_active             TINYINT       NOT NULL DEFAULT 0 COMMENT '当前是否启用 1 current model, 0 inactive',
    trained_at            DATETIME      NULL COMMENT '训练时间',
    created_by            INT           NULL,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_model_version_no (version_no),
    KEY idx_model_active (is_active),
    CONSTRAINT fk_model_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_model_active CHECK ( is_active IN (0, 1) )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='ML model version metadata table';

-- 6. 预测结果表
CREATE TABLE prediction_result
(
    id                     INT      NOT NULL AUTO_INCREMENT,
    student_id             INT      NOT NULL COMMENT '学生 ID',
    performance_id         INT      NOT NULL COMMENT '学生表现记录 ID',
    model_version_id       INT      NOT NULL COMMENT '使用的模型版本',
    predicted_grade_class  TINYINT  NOT NULL COMMENT '0 A, 1 B, 2 C, 3 D, 4 F',
    predicted_grade_label  CHAR(1)  NOT NULL COMMENT 'A/B/C/D/F',
    probability_json       JSON     NULL COMMENT '每个等级的预测概率',
    important_factors_json JSON     NULL COMMENT '主要影响因素',
    predict_input_json     JSON     NULL COMMENT '本次预测使用的输入快照',
    created_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_pred_student_id (student_id),
    KEY idx_pred_model_version_id (model_version_id),
    KEY idx_pred_grade_class (predicted_grade_class),
    CONSTRAINT fk_pred_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pred_performance FOREIGN KEY (performance_id) REFERENCES student_performance (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pred_model_version FOREIGN KEY (model_version_id) REFERENCES model_version (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_pred_grade_class CHECK (predicted_grade_class IN (0, 1, 2, 3, 4))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Decision-tree prediction result table';

-- 7. 学业风险预警表
CREATE TABLE warning_record
(
    id                   INT                                                   NOT NULL AUTO_INCREMENT,
    student_id           INT                                                   NOT NULL,
    prediction_result_id INT                                                   NULL COMMENT '关联预测结果',
    risk_score           TINYINT                                               NOT NULL COMMENT '风险分数 0-100',
    risk_level           ENUM ('LOW', 'MEDIUM', 'HIGH')                        NOT NULL,
    risk_reasons_json    JSON                                                  NULL COMMENT '风险原因 e.g. ["Absences > 20", "StudyTimeWeekly < 5"]',
    suggestion_json      JSON                                                  NULL COMMENT '干预建议',
    status               ENUM ('UNPROCESSED', 'PROCESSING', 'DONE', 'IGNORED') NOT NULL DEFAULT 'UNPROCESSED',
    handler_user_id      INT                                                   NULL COMMENT '处理人',
    created_at           DATETIME                                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME                                              NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_warning_student_id (student_id),
    KEY idx_warning_risk_level (risk_level),
    KEY idx_warning_status (status),
    CONSTRAINT fk_warning_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_warning_prediction FOREIGN KEY (prediction_result_id) REFERENCES prediction_result (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_warning_handler FOREIGN KEY (handler_user_id) REFERENCES sys_user (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_warning_risk_score CHECK (risk_score BETWEEN 0 AND 100)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Academic risk warning table';

-- 8. CSV 导入批次表
CREATE TABLE import_batch
(
    id            INT                                              NOT NULL AUTO_INCREMENT,
    file_name     VARCHAR(255)                                     NOT NULL,
    total_count   INT                                              NOT NULL DEFAULT 0,
    success_count INT                                              NOT NULL DEFAULT 0,
    fail_count    INT                                              NOT NULL DEFAULT 0,
    status        ENUM ('PENDING', 'SUCCESS', 'FAILED', 'PARTIAL') NOT NULL DEFAULT 'PENDING',
    error_message TEXT                                             NULL,
    imported_by   INT                                              NULL,
    created_at    DATETIME                                         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_import_status (status),
    CONSTRAINT fk_import_user FOREIGN KEY (imported_by) REFERENCES sys_user (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='CSV import batch table';

CREATE TABLE operation_log
(
    id             INT          NOT NULL AUTO_INCREMENT,
    user_id        INT          NULL,
    module_name    VARCHAR(50)  NOT NULL COMMENT 'student/performance/model/warning/etc.',
    operation_type VARCHAR(50)  NOT NULL COMMENT 'CREATE/UPDATE/DELETE/IMPORT/TRAIN/PREDICT',
    description    VARCHAR(255) NULL,
    request_method VARCHAR(10)  NULL,
    request_uri    VARCHAR(255) NULL,
    ip_address     VARCHAR(64)  NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_log_user_id (user_id),
    KEY idx_log_module_type (module_name, operation_type),
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='System operation log table';

-- 10. View for model training/export: combines the two core business tables into the original CSV-like structure.
CREATE OR REPLACE VIEW v_student_model_dataset AS
SELECT s.student_no         AS StudentID,
       s.age                AS Age,
       s.gender             AS Gender,
       s.ethnicity          AS Ethnicity,
       s.parental_education AS ParentalEducation,
       p.study_time_weekly  AS StudyTimeWeekly,
       p.absences           AS Absences,
       p.tutoring           AS Tutoring,
       p.parental_support   AS ParentalSupport,
       p.extracurricular    AS Extracurricular,
       p.sports             AS Sports,
       p.music              AS Music,
       p.volunteering       AS Volunteering,
       p.gpa                AS GPA,
       p.grade_class        AS GradeClass
FROM student s
         JOIN student_performance p ON p.student_id = s.id
WHERE s.deleted = 0;
