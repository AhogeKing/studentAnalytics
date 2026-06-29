# 2026-06-29

## 模型版本管理、训练追踪与预测约束

今天后端继续完善“模型训练 -> 模型版本管理 -> 指定模型预测 -> 风险预警”的演示闭环，并补充 Python 训练产物中的可审计信息。

### 模型版本管理接口

新增或完善的接口：

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/StudentAnalytics/models/versions/{id}/activate` | `ADMIN` | 将指定模型版本设为当前启用模型 |
| `PATCH` | `/StudentAnalytics/models/versions/{id}` | `ADMIN` | 修改模型版本号 |
| `DELETE` | `/StudentAnalytics/models/versions/{id}` | `ADMIN` | 删除模型版本 |

版本号修改规则：

- 请求体支持 `versionNo` 和 `version_no`。
- 版本号不能为空。
- 版本号长度不超过 50。
- `version_no` 在 `model_version` 表内不能重复。
- 仅修改 `model_version.version_no`，不重命名模型文件目录；模型加载仍以 `model_path` 为准，避免文件路径失联。

模型删除规则：

- 当前启用模型不能删除。
- 已被 `prediction_result` 引用的模型版本不能删除，避免破坏预测历史。
- 删除数据库记录成功后，会尽力清理该版本目录下的模型文件、`metrics.json` 和 `train.log`。
- 文件清理限定在 `ml.model-root-dir` 下，避免误删其他路径。

新增或调整的后端结构：

- `dto/ModelVersionUpdateRequest.java`
- `controller/ModelController.java`
- `service/ModelService.java`
- `service/impl/ModelServiceImpl.java`
- `mapper/ModelMapper.java`
- `resources/mapper/ModelMapper.xml`

### 训练耗时与模型可审计信息

`model_version` 新增字段：

```sql
training_duration_ms BIGINT NULL COMMENT '模型训练耗时毫秒'
```

新增迁移脚本：

```text
src/main/resources/sql/apply_model_version_training_duration_patch.sql
```

后端训练接口现在会记录从导出训练数据开始，到 Python 训练进程返回结束的完整耗时，并写入 `model_version.training_duration_ms`。训练结果、模型版本列表和模型版本详情都会返回 `trainingDurationMs`。

Python 训练脚本 `python/ml/train_decision.py` 新增写入以下 `metrics.json` 字段：

- `training_started_at`
- `training_finished_at`
- `training_duration_ms`
- `training_duration_seconds`
- `dataset_sha256`

其中 `dataset_sha256` 来自本次训练使用的 CSV 文件内容，用于判断不同训练版本是否使用同一份训练数据。今天排查“quick 和 default 指标一致”时确认：

- quick 实际使用 `search_candidates = 96`；
- default 实际使用 `search_candidates = 1728`；
- 二者并未复用同一训练结果；
- 指标一致是因为当前训练数据哈希一致、随机种子固定，且 default 找到的最优参数正好包含在 quick 参数网格中。

### 预测模型选择与训练/测试集限制

预测接口继续支持请求体传入 `model_version_id`，使用指定模型版本预测。为了避免“用训练集学生做演示预测”的误解，后端补充了模型版本样本来源判断：

- Python 训练 `metrics.json` 中记录 `train_student_ids` 和 `test_student_ids`。
- 后端读取模型版本的 `metrics.json`，判断目标学生属于训练集、测试集还是未知来源。
- 新增预测可用性接口，用于前端判断某学生是否适合用当前模型演示预测。
- 对训练集学生执行主动预测时，后端会返回业务提示，避免将训练样本预测当成泛化能力展示。

相关结构包括：

- `vo/PredictionEligibilityVO.java`
- `controller/PredictionController.java`
- `service/PredictionService.java`
- `service/impl/PredictionServiceImpl.java`
- `vo/PredictionResultVO.java`

### 风险预警删除与风险公式说明

风险预警模块新增删除能力：

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| `DELETE` | `/StudentAnalytics/warnings/{id}` | `ADMIN` | 删除单条风险预警记录 |

删除行为：

- 仅删除 `warning_record`。
- 不删除学生信息。
- 不删除 `prediction_result`。
- 不删除模型版本。
- 接入 `operation_log`，记录 `warning / DELETE` 操作日志。

前端展示的风险分和风险等级说明来自后端当前规则：

- 风险分：`min(各风险项加分之和, 100)`。
- 预测等级为较差或风险：`+50`。
- `GPA < 2.5`：`+35`。
- 缺勤 `> 20`：`+25`；缺勤 `> 10`：`+15`。
- 每周学习时长 `< 5` 小时：`+20`；`< 8` 小时：`+10`。
- 家长支持 `<= 1`：`+10`。
- 未参加课外辅导：`+5`。
- 风险等级：`risk_score >= 50` 为 `HIGH`，`25 <= risk_score < 50` 为 `MEDIUM`，`risk_score < 25` 为 `LOW`。

相关结构：

- `controller/WarningController.java`
- `service/WarningService.java`
- `service/impl/WarningServiceImpl.java`
- `mapper/WarningMapper.java`
- `resources/mapper/WarningMapper.xml`

## 验证记录

今天完成的后端、MyBatis、Python 和前端联动代码已执行过以下验证：

```bash
python3 -m py_compile python/ml/train_decision.py
xmllint --noout src/main/resources/mapper/ModelMapper.xml
xmllint --noout src/main/resources/mapper/WarningMapper.xml
mvn -q -DskipTests compile
npm run build
git diff --check -- src/main/java src/main/resources python frontend/src
```

另外执行过本地接口验证：

- 使用 admin 登录后训练 quick 模型，确认 `trainingDurationMs` 写入 `model_version` 并从模型列表接口返回。
- 使用 `DELETE /warnings/0` 验证风险预警删除路由生效且不会删除真实数据。
- 使用模型版本改名、改回和删除接口验证模型版本管理链路。

# 2026-06-28

## 模型训练与 `model_version` 落库

本节记录“模型训练 -> 模型版本归档 -> `model_version` 落库”这一条主线。单学生预测和风险预警已在后续小节补齐。

### 实现边界

已实现：

- 从数据库视图 `v_student_model_dataset` 读取当前训练数据。
- 按 Python 训练脚本要求导出 CSV 字段：
  - `StudentID`
  - `Age`
  - `Gender`
  - `Ethnicity`
  - `ParentalEducation`
  - `StudyTimeWeekly`
  - `Absences`
  - `Tutoring`
  - `ParentalSupport`
  - `Extracurricular`
  - `Sports`
  - `Music`
  - `Volunteering`
  - `GPA`
  - `GradeClass`
- 调用 `python/ml/train_decision.py` 执行决策树训练。
- 将每次训练产物输出到独立版本目录：

```text
runtime/ml/models/{versionNo}/
├── grade_class_decision_tree.joblib
├── metrics.json
└── train.log
```

- 从 `metrics.json` 读取模型指标和最佳参数。
- 写入 `model_version` 表。
- `activate = true` 时停用旧模型并启用新模型。
- 训练接口接入 `operation_log`，记录 `model / TRAIN` 审计日志。

暂未实现：

- 模型版本回滚接口。
- 异步训练任务队列。
- 训练任务进度查询。

### 新增或补齐的后端结构

配置：

- `config/MlProperties.java`：读取 `ml.*` 配置项。

DTO：

- `dto/ModelTrainRequest.java`：训练请求参数。
- `dto/ModelVersionQueryRequest.java`：模型版本分页筛选参数。
- `dto/ml/MlTrainResult.java`：Python 训练调用结果。
- `dto/row/ModelTrainingRow.java`：训练数据导出行。

实体：

- `entity/ModelVersion.java`：对应 `model_version` 表。

Mapper：

- `mapper/ModelMapper.java`
- `resources/mapper/ModelMapper.xml`

Service：

- `service/MlModelClient.java`
- `service/ModelService.java`
- `service/impl/ProcessMlModelClient.java`
- `service/impl/ModelServiceImpl.java`

VO：

- `vo/ModelTrainResultVO.java`
- `vo/ModelVersionVO.java`
- `vo/ModelVersionDetailVO.java`

Controller：

- `controller/ModelController.java`

### 新增配置

`application.yml` 新增：

```yaml
ml:
  python-path: python3
  project-root: .
  script-dir: python/ml
  train-script-name: train_decision.py
  dataset-path: dataset/Student_performance_data_.csv
  artifact-dir: python/ml/artifacts
  model-root-dir: runtime/ml/models
  train-timeout-seconds: 600
```

当前 Java 后端会把数据库训练数据导出到 `dataset/Student_performance_data_.csv`，然后调用：

```bash
python3 python/ml/train_decision.py \
  --data-path dataset/Student_performance_data_.csv \
  --artifact-dir runtime/ml/models/{versionNo}
```

### 新增接口

基础路径：

```text
/StudentAnalytics/models
```

#### 训练决策树模型

```http
POST /StudentAnalytics/models/decision-tree/train
Authorization: Bearer <token>
Content-Type: application/json
```

权限：

- `ADMIN`

请求体：

```json
{
  "mode": "quick",
  "activate": true
}
```

字段说明：

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `mode` | string | `default` | `quick`、`default`、`exhaustive` |
| `activate` | boolean | `true` | 训练成功后是否设为当前启用模型 |

后端行为：

1. 使用 `v_student_model_dataset` 导出训练 CSV。
2. 生成版本号，例如 `dt_cls_20260628104530123`。
3. 创建版本目录 `runtime/ml/models/{versionNo}`。
4. 调用 Python 训练脚本。
5. 读取 `metrics.json`。
6. 写入 `model_version`。
7. 如果 `activate = true`，先将旧模型 `is_active` 置为 `0`，再将新模型置为 `1`。
8. 返回训练结果。
9. 写入操作日志 `module_name = model`、`operation_type = TRAIN`。

返回数据核心字段：

- `modelVersionId`
- `versionNo`
- `modelName`
- `algorithm`
- `targetColumn`
- `featureColumns`
- `searchMode`
- `searchCandidates`
- `trainRows`
- `testRows`
- `bestParameters`
- `accuracy`
- `precisionMacro`
- `recallMacro`
- `f1Macro`
- `aucOvrMacro`
- `confusionMatrix`
- `modelPath`
- `active`
- `trainedAt`

#### 查询当前启用模型

```http
GET /StudentAnalytics/models/active
Authorization: Bearer <token>
```

权限：

- `ADMIN`
- `TEACHER`

返回：

- 当前 `is_active = 1` 的模型版本。
- 如果没有启用模型，返回业务错误：`当前没有启用的模型，请管理员先训练模型`。

#### 查询模型版本列表

```http
GET /StudentAnalytics/models/versions?page_num=1&page_size=20
Authorization: Bearer <token>
```

权限：

- `ADMIN`
- `TEACHER`

支持参数：

| 参数 | 说明 |
| --- | --- |
| `page_num` | 页码，默认 `1` |
| `page_size` | 每页数量，默认 `20`，最大 `100` |
| `active` | 是否只看启用或未启用模型 |
| `start_time` | 训练开始时间，支持 `yyyy-MM-dd HH:mm:ss` 或 ISO LocalDateTime |
| `end_time` | 训练结束时间，支持 `yyyy-MM-dd HH:mm:ss` 或 ISO LocalDateTime |

返回：

```text
PageResultVO<ModelVersionVO>
```

#### 查询模型版本详情

```http
GET /StudentAnalytics/models/versions/{id}
Authorization: Bearer <token>
```

权限：

- `ADMIN`
- `TEACHER`

返回内容：

- `model_version` 表中的基础信息。
- `feature_columns` JSON。
- `confusion_matrix_json` JSON。
- 对应版本目录中的完整 `metrics.json`。

### 文件与 Git 忽略规则

`.gitignore` 新增运行时产物忽略：

```text
runtime/ml/
python/ml/artifacts/
```

原因：

- `runtime/ml/models/` 是后端运行时训练产物目录。
- `python/ml/artifacts/` 是 Python 独立训练的默认产物目录。
- 两者都可能包含较大的 `.joblib` 模型文件，不应默认进入 Git。

### 验证记录

本次后端代码已通过：

```bash
xmllint --noout src/main/resources/mapper/ModelMapper.xml
mvn -q -DskipTests compile
git diff --check -- src/main/java src/main/resources .gitignore
```

补充说明：

- 当前系统 Python 环境缺少 `joblib`，所以直接执行 `python3 python/ml/train_decision.py --help` 会失败。
- 实际训练接口运行前，需要先安装 `python/ml/requirements.txt` 中的依赖。

## 单学生预测与 `prediction_result` 落库

本次后端继续完成“当前模型版本 -> 单学生预测 -> `prediction_result` 落库”主线。后续小节已经接入 `warning_record` 自动生成。

### 实现边界

已实现：

- 支持按学生编号发起预测。
- 默认使用当前启用的 `model_version`。
- 支持请求体传入 `model_version_id`，使用指定模型版本预测。
- 从 `student` 和 `student_performance` 查询预测所需输入字段。
- 调用 `python/ml/predict_descision.py` 执行预测。
- 使用 `--model-path` 指定 `model_version.model_path`。
- 使用 `--input` 写入单学生预测输入 JSON。
- 使用 `--output` 读取 Python 输出的预测结果 JSON。
- 将预测等级、五类概率、重要因素、输入快照写入 `prediction_result`。
- 默认预测后生成 `warning_record`。
- `generate_warning = false` 时只生成 `prediction_result`。
- 支持查询预测结果详情。
- 支持查询某学生最近一次预测结果。
- 预测接口接入 `operation_log`，记录 `prediction / PREDICT` 审计日志。

暂未实现：

- 预测结果分页列表。
- 删除或归档历史预测结果。

### 新增或补齐的后端结构

DTO：

- `dto/PredictionRequest.java`：预测请求参数，可选 `model_version_id`。
- `dto/ml/MlPredictionInput.java`：传给 Python 的大写字段输入。
- `dto/ml/MlPredictionResult.java`：Python 预测输出。
- `dto/ml/MlImportantFeatureItem.java`：Python 返回的重要因素项。
- `dto/row/PredictionInputRow.java`：预测输入查询行。
- `dto/row/PredictionResultRow.java`：预测结果详情查询行。

实体：

- `entity/PredictionResult.java`：对应 `prediction_result` 表。

Mapper：

- `mapper/PredictionMapper.java`
- `resources/mapper/PredictionMapper.xml`

Service：

- `service/PredictionService.java`
- `service/impl/PredictionServiceImpl.java`

VO：

- `vo/StudentPredictionVO.java`
- `vo/PredictionResultVO.java`
- `vo/PredictionProbabilityVO.java`
- `vo/ImportantFactorVO.java`

Controller：

- `controller/PredictionController.java`

已有结构扩展：

- `config/MlProperties.java`：新增预测脚本、临时目录和超时时间配置。
- `service/MlModelClient.java`：新增 `predictGradeClass(...)`。
- `service/impl/ProcessMlModelClient.java`：新增 Python 预测进程调用。

### 新增配置

`application.yml` 中 `ml.*` 新增：

```yaml
ml:
  predict-script-name: predict_descision.py
  prediction-temp-dir: runtime/ml/predictions
  predict-timeout-seconds: 60
```

预测时 Java 后端会调用：

```bash
python3 python/ml/predict_descision.py \
  --model-path runtime/ml/models/{versionNo}/grade_class_decision_tree.joblib \
  --input runtime/ml/predictions/predict-input-{uuid}.json \
  --output runtime/ml/predictions/predict-output-{uuid}.json
```

`runtime/ml/` 已经被 `.gitignore` 忽略，因此预测输入、输出临时文件和模型运行时产物不会进入 Git。

### 新增接口

基础路径：

```text
/StudentAnalytics/predictions
```

#### 单学生预测

```http
POST /StudentAnalytics/predictions/students/{studentNo}
Authorization: Bearer <token>
Content-Type: application/json
```

权限：

- `ADMIN`
- `TEACHER`

请求体可以为空：

```json
{}
```

也可以指定模型版本：

```json
{
  "model_version_id": 3
}
```

还可以关闭自动生成预警：

```json
{
  "generate_warning": false
}
```

后端行为：

1. 校验学生编号。
2. 查询学生基础信息和学业表现。
3. 学生不存在或已删除时返回业务错误。
4. 学生没有 `student_performance` 时返回业务错误。
5. 未指定 `model_version_id` 时读取当前启用模型。
6. 指定 `model_version_id` 时读取指定模型版本。
7. 调用 Python 预测脚本。
8. 将五类概率统一保存为 A/B/C/D/F 顺序。
9. 将重要因素转换为包含中文标签和值的结构。
10. 保存 `prediction_result`。
11. 默认调用 `WarningService.generateWarningFromPrediction(...)` 生成预警。
12. 返回保存后的预测结果和预警结果。
13. 写入操作日志 `module_name = prediction`、`operation_type = PREDICT`。

返回数据核心字段：

- `student_no`
- `name`
- `class_info`
- `prediction.prediction_result_id`
- `prediction.model_version_id`
- `prediction.model_version_no`
- `prediction.predicted_grade_class`
- `prediction.predicted_grade_label`
- `prediction.probabilities`
- `prediction.important_factors`
- `prediction.predict_input`
- `prediction.created_at`
- `warning`

#### 查询预测结果详情

```http
GET /StudentAnalytics/predictions/{id}
Authorization: Bearer <token>
```

权限：

- `ADMIN`
- `TEACHER`

返回：

- 指定 `prediction_result.id` 的预测结果详情。
- 如果预测结果不存在，返回业务错误：`预测结果不存在`。

#### 查询学生最近一次预测

```http
GET /StudentAnalytics/predictions/students/{studentNo}/latest
Authorization: Bearer <token>
```

权限：

- `ADMIN`
- `TEACHER`

返回：

- 指定学生最近一次预测结果。
- 如果没有预测记录，返回业务错误：`该学生暂无预测记录`。

### `prediction_result` 写入内容

| 字段 | 写入内容 |
| --- | --- |
| `student_id` | 学生表主键 |
| `performance_id` | 学业表现表主键 |
| `model_version_id` | 本次使用的模型版本 |
| `predicted_grade_class` | 预测等级编码，0-4 |
| `predicted_grade_label` | 预测等级字母，A/B/C/D/F |
| `probability_json` | A/B/C/D/F 五类概率数组 |
| `important_factors_json` | 重要因素数组，包含字段名、中文标签、当前值、重要性 |
| `predict_input_json` | 本次预测输入快照，包含学生编号、模型版本、特征、当前 GPA 和当前等级 |

### 验证记录

本次预测链路代码已通过：

```bash
xmllint --noout src/main/resources/mapper/PredictionMapper.xml src/main/resources/mapper/ModelMapper.xml
mvn -q -DskipTests compile
git diff --check -- src/main/java src/main/resources
```

补充说明：

- 没有执行真实 HTTP 预测请求。
- 真实预测依赖已存在且启用的 `model_version.model_path`。
- 真实预测还依赖本地 Python 环境安装 `python/ml/requirements.txt` 中的 `joblib`、`numpy`、`pandas`、`scikit-learn`。

## 风险预警与 `warning_record` 落库

本次后端完成“预测结果 -> 风险评分 -> `warning_record` 落库 -> 预警查询和状态处理”主线。该模块仍是 V1 规则版本，不引入干预记录表，不做批量预警任务。

### 实现边界

已实现：

- 预测成功后默认生成风险预警。
- 支持 `generate_warning = false` 关闭本次预警生成。
- 同一个 `prediction_result` 不重复生成多条 `warning_record`。
- 根据预测结果和当前学业表现计算风险分数。
- 风险分数限制在 `0-100`。
- 风险等级固定为 `LOW`、`MEDIUM`、`HIGH`。
- 风险原因和建议以 JSON 数组写入数据库。
- 支持预警分页列表。
- 支持预警详情查询。
- 支持学生最近一次预警查询。
- 支持修改预警状态。
- 修改状态时写入当前登录用户到 `handler_user_id`。
- 修改状态接口接入 `operation_log`，记录 `warning / UPDATE_STATUS`。

暂未实现：

- 批量生成预警。
- 干预记录或跟进备注表。
- 预警删除或归档。
- 风险规则后台配置。
- 前端预警页面。

### 新增或补齐的后端结构

DTO：

- `dto/WarningQueryRequest.java`：预警列表筛选和分页参数。
- `dto/WarningStatusUpdateRequest.java`：预警状态更新请求。
- `dto/row/WarningGenerationContextRow.java`：生成预警时需要的预测和学业表现上下文。
- `dto/row/WarningRecordRow.java`：预警列表和详情查询行。

实体：

- `entity/WarningRecord.java`：对应 `warning_record` 表。

Mapper：

- `mapper/WarningMapper.java`
- `resources/mapper/WarningMapper.xml`

Service：

- `service/WarningService.java`
- `service/impl/WarningServiceImpl.java`

VO：

- `vo/WarningRecordVO.java`
- `vo/WarningDetailVO.java`

Controller：

- `controller/WarningController.java`

已有结构扩展：

- `dto/PredictionRequest.java`：新增 `generate_warning`。
- `vo/StudentPredictionVO.java`：新增 `warning`。
- `service/impl/PredictionServiceImpl.java`：预测成功后默认调用 `WarningService.generateWarningFromPrediction(...)`，查询预测详情和最近预测时带出关联预警。

### 风险评分规则

当前风险分数由以下规则叠加得到，最高截断为 `100`：

| 规则 | 加分 | 原因 |
| --- | --- | --- |
| 预测等级为 D/F，即 `predicted_grade_class >= 3` | `+50` | 模型预测存在学业风险 |
| 当前 `GPA < 2.5` | `+35` | 当前 GPA 偏低 |
| 缺勤次数 `> 20` | `+25` | 缺勤次数较高 |
| 缺勤次数 `> 10` 且 `<= 20` | `+15` | 缺勤次数偏高 |
| 每周学习时长 `< 5` 小时 | `+20` | 学习时长过低 |
| 每周学习时长 `< 8` 且 `>= 5` 小时 | `+10` | 学习时长偏低 |
| 家长支持程度 `<= 1` | `+10` | 家长支持程度较低 |
| 未参加辅导，即 `tutoring = 0` | `+5` | 当前未参加课外辅导 |

风险等级：

| 分数区间 | 风险等级 | 展示文案 |
| --- | --- | --- |
| `0-24` | `LOW` | 低风险 |
| `25-49` | `MEDIUM` | 中风险 |
| `50-100` | `HIGH` | 高风险 |

如果没有命中明显风险规则，仍会生成一条 `LOW` 预警，原因写为“当前未发现明显高风险因素”，建议保持学习节奏并定期关注成绩变化。

### 新增接口

基础路径：

```text
/StudentAnalytics/warnings
```

#### 预警列表

```http
GET /StudentAnalytics/warnings?page_num=1&page_size=20
Authorization: Bearer <token>
```

权限：

- `ADMIN`
- `TEACHER`

支持参数：

| 参数 | 说明 |
| --- | --- |
| `page_num` | 页码，默认 `1` |
| `page_size` | 每页数量，默认 `20`，最大 `100` |
| `student_no` | 学生编号 |
| `student_name` | 学生姓名模糊查询 |
| `grade_level` | 年级 |
| `class_name` | 班级 |
| `risk_level` | `LOW`、`MEDIUM`、`HIGH` |
| `status` | `UNPROCESSED`、`PROCESSING`、`DONE`、`IGNORED` |
| `start_time` | 创建开始时间，支持 `yyyy-MM-dd HH:mm:ss` 或 ISO LocalDateTime |
| `end_time` | 创建结束时间，支持 `yyyy-MM-dd HH:mm:ss` 或 ISO LocalDateTime |

返回：

```text
PageResultVO<WarningRecordVO>
```

列表默认只返回未软删除学生的预警记录。

#### 预警详情

```http
GET /StudentAnalytics/warnings/{id}
Authorization: Bearer <token>
```

权限：

- `ADMIN`
- `TEACHER`

返回内容：

- 学生编号、姓名、班级。
- 关联预测结果 ID。
- 使用的模型版本。
- 预测等级。
- 风险分数、风险等级。
- 风险原因、建议。
- 当前处理状态。
- 处理人信息。
- 创建和更新时间。

#### 学生最近一次预警

```http
GET /StudentAnalytics/warnings/students/{studentNo}/latest
Authorization: Bearer <token>
```

权限：

- `ADMIN`
- `TEACHER`

返回：

- 指定学生最近一次风险预警。
- 如果不存在，返回业务错误：`该学生暂无风险预警记录`。

#### 修改预警状态

```http
PATCH /StudentAnalytics/warnings/{id}/status
Authorization: Bearer <token>
Content-Type: application/json
```

权限：

- `ADMIN`
- `TEACHER`

请求体：

```json
{
  "status": "PROCESSING"
}
```

可选状态：

| 状态 | 含义 |
| --- | --- |
| `UNPROCESSED` | 未处理 |
| `PROCESSING` | 处理中 |
| `DONE` | 已完成 |
| `IGNORED` | 已忽略 |

后端行为：

1. 校验预警 ID。
2. 校验状态枚举。
3. 更新 `warning_record.status`。
4. 写入当前用户 ID 到 `warning_record.handler_user_id`。
5. 返回更新后的预警详情。
6. 写入操作日志 `module_name = warning`、`operation_type = UPDATE_STATUS`。

### `warning_record` 写入内容

| 字段 | 写入内容 |
| --- | --- |
| `student_id` | 学生表主键 |
| `prediction_result_id` | 关联预测结果 |
| `risk_score` | 规则计算后的风险分数，0-100 |
| `risk_level` | `LOW`、`MEDIUM`、`HIGH` |
| `risk_reasons_json` | 风险原因 JSON 数组 |
| `suggestion_json` | 建议 JSON 数组 |
| `status` | 默认 `UNPROCESSED` |
| `handler_user_id` | 新生成时为空；修改状态时写入当前用户 |

### 验证记录

本次预警链路代码已通过：

```bash
xmllint --noout src/main/resources/mapper/WarningMapper.xml src/main/resources/mapper/PredictionMapper.xml
mvn -q -DskipTests compile
git diff --check -- src/main/java src/main/resources
```

补充说明：

- 没有执行真实 HTTP 预测和预警请求。
- 真实链路仍依赖可用的启用模型文件和 Python ML 依赖。

# 2026-06-27

## 操作日志审计模块

本次后端根据 `operation_log` 表结构补齐 V1 操作日志模块，定位为关键操作审计，不承载模型指标、预测结果或预警详情等业务大对象。

### 数据表边界

`operation_log` 只记录以下审计索引信息：

- 谁操作：`user_id`、`username`、`real_name`、`user_role`。
- 做了什么：`module_name`、`operation_type`、`operation_result`。
- 操作对象：`target_type`、`target_id`、`business_key`。
- 请求来源：`request_method`、`request_uri`、`ip_address`。
- 请求内容：`request_params`、`request_body`。
- 操作时间：`created_at`。

当前不记录：

- 失败原因详情。
- 接口耗时。
- 模型训练指标。
- 预测结果详情。
- 风险预警详情全文。

后续模型训练、预测和预警模块仍应把业务详情分别写入 `model_version`、`prediction_result`、`warning_record` 等业务表，`operation_log` 只保留“谁在什么时候触发了什么操作”的审计记录。

### 新增后端类

新增或补齐以下后端结构：

- `annotation/LogOperation.java`：方法级操作日志注解。
- `aspect/OperationLogAspect.java`：AOP 统一记录成功和失败日志。
- `common/constant/OperationModule.java`：模块常量。
- `common/constant/OperationType.java`：操作类型常量。
- `common/constant/OperationResult.java`：操作结果常量。
- `common/constant/OperationTargetType.java`：目标对象类型常量。
- `entity/OperationLog.java`：对应 `operation_log` 表。
- `dto/OperationLogQueryRequest.java`：操作日志分页筛选参数。
- `mapper/OperationLogMapper.java` 和 `mapper/OperationLogMapper.xml`：日志写入、分页查询、详情查询。
- `service/OperationLogService.java` 和 `service/impl/OperationLogServiceImpl.java`：日志保存、列表、详情、筛选选项。
- `vo/OperationLogVO.java`：列表页 VO，不包含请求 JSON。
- `vo/OperationLogDetailVO.java`：详情页 VO，包含 `request_params` 和 `request_body`。
- `vo/OperationLogOptionsVO.java`：筛选项 VO。

`pom.xml` 新增 `spring-boot-starter-aop`，用于启用操作日志切面。

### 日志注解

`@LogOperation` 支持以下字段：

- `module`：模块名，例如 `user`、`student`、`performance`、`auth`。
- `type`：操作类型，例如 `CREATE`、`UPDATE`、`DELETE`、`UPSERT`、`RESET_PASSWORD`、`LOGOUT`。
- `targetType`：操作对象类型，例如 `USER`、`STUDENT`、`PERFORMANCE`。
- `targetId`：SpEL 表达式，例如 `#id`、`#studentNo`、`#request.username`。
- `businessKey`：业务标识 SpEL 表达式。
- `recordRequest`：是否记录请求参数和请求体，默认记录。

### AOP 记录规则

`OperationLogAspect` 使用 `@Around` 拦截带 `@LogOperation` 的 Controller 方法：

- Controller 正常返回后写入 `operation_result = SUCCESS`。
- Controller 抛出异常时写入 `operation_result = FAIL`，然后继续抛出原异常。
- 日志写入异常不会影响主业务。
- 日志保存方法使用 `REQUIRES_NEW` 事务，避免主业务回滚时失败日志也被回滚。

日志会从 `SysUserContext` 中读取当前登录用户并保存操作者快照。即使用户后续改名、改角色、禁用或删除，历史日志仍能显示当时的用户名、真实姓名和角色。

HTTP 信息记录规则：

- `request_method` 取当前请求方法。
- `request_uri` 取当前请求路径。
- `ip_address` 优先取 `X-Forwarded-For`，其次取 `X-Real-IP`，最后取 `remoteAddr`。

请求内容记录规则：

- `request_params` 保存 URL 查询参数 JSON。
- `request_body` 保存 Controller 方法参数中的业务请求对象。
- 自动过滤 `HttpServletRequest`、`HttpServletResponse`、`MultipartFile`、`BindingResult` 等框架对象。
- 字段名包含 `password`、`token`、`authorization` 时统一脱敏为 `***`。
- 没有内容时写入 `{}`，保证 MySQL JSON 字段合法。

### 操作日志管理接口

全部接口需要 `ADMIN` 权限：

基础路径：

```text
/StudentAnalytics/admin/operation-logs
```

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/admin/operation-logs` | 操作日志分页列表 |
| `GET` | `/admin/operation-logs/{id}` | 操作日志详情 |
| `GET` | `/admin/operation-logs/options` | 日志筛选选项 |

分页和筛选参数：

- `page_num`
- `page_size`
- `userId`
- `username`
- `user_role`
- `module_name`
- `operation_type`
- `operation_result`
- `target_type`
- `target_id`
- `business_key`
- `start_time`
- `end_time`
- `keyword`

分页默认：

- `page_num = 1`
- `page_size = 20`
- 最大 `page_size = 100`

时间参数支持：

- `yyyy-MM-dd HH:mm:ss`
- ISO LocalDateTime，例如 `2026-06-27T20:30:00`

列表返回 `PageResultVO<OperationLogVO>`，不返回 `request_params` 和 `request_body`，避免表格数据过大。

详情返回 `OperationLogDetailVO`，包含完整请求参数和请求体 JSON。

### 已接入自动日志的接口

用户管理：

| 方法 | 路径 | 模块 | 操作类型 |
| --- | --- | --- | --- |
| `POST` | `/admin/users` | `user` | `CREATE` |
| `PUT` | `/admin/users/{id}` | `user` | `UPDATE` |
| `PATCH` | `/admin/users/{id}/status` | `user` | `UPDATE_STATUS` |
| `PATCH` | `/admin/users/{id}/password` | `user` | `RESET_PASSWORD` |
| `DELETE` | `/admin/users/{id}` | `user` | `DISABLE` |

学生管理：

| 方法 | 路径 | 模块 | 操作类型 |
| --- | --- | --- | --- |
| `POST` | `/students` | `student` | `CREATE` |
| `PUT` | `/students/overview/{studentNo}` | `student` | `UPDATE` |
| `DELETE` | `/students/overview/{studentNo}` | `student` | `DELETE` |

学业表现：

| 方法 | 路径 | 模块 | 操作类型 |
| --- | --- | --- | --- |
| `PUT` | `/students/performance/{studentNo}` | `performance` | `UPSERT` |

认证：

| 方法 | 路径 | 模块 | 操作类型 |
| --- | --- | --- | --- |
| `POST` | `/logout` | `auth` | `LOGOUT` |

登录日志暂未自动记录。原因是 `/login` 发生在 JWT 鉴权之前，AOP 无法从 `SysUserContext` 读取当前用户。后续如果需要登录成功 / 失败日志，应在 `SysUserServiceImpl.login()` 内手动写入。

### 验证记录

本次后端实现已通过：

```bash
mvn -q test
xmllint --noout src/main/resources/mapper/OperationLogMapper.xml src/main/resources/mapper/StudentMapper.xml src/main/resources/mapper/AnalysisMapper.xml
git diff --cached --check
```

## 昨晚新增后端能力整理

本节只记录 2026-06-26 晚间新增或修复、且会影响后续前端开发的后端能力。此前前端已经适配的学生查询、学生详情、筛选项和分析图表接口不在这里重复展开。

## 系统用户删除策略判断

`sys_user` 和学生核心数据、模型训练数据之间没有直接强绑定：

- `student` 不依赖 `sys_user`。
- `student_performance` 不依赖 `sys_user`。
- 当前模型训练视图 `v_student_model_dataset` 只读取 `student` 和 `student_performance`。

因此，物理删除系统用户不会直接破坏学生数据和模型训练数据。

但 `sys_user` 在 V0 数据库中仍被以下辅助业务表引用：

- `model_version.created_by`
- `warning_record.handler_user_id`
- `import_batch.imported_by`
- `operation_log.user_id`

这些外键当前都设计为 `ON DELETE SET NULL`，所以物理删除用户不会导致外键错误，但会让历史记录失去操作者信息。

当前实现选择：

- `PATCH /admin/users/{id}/status`：启用或禁用账号。
- `DELETE /admin/users/{id}`：当前也执行禁用语义，即 `status = 0`。

这更接近“账号停用”，不是严格意义上的软删除。因为 `sys_user` 当前没有 `deleted` 字段。

后续如果要区分软删除和物理删除，可以设计成：

| 操作 | 建议接口 | 语义 |
| --- | --- | --- |
| 禁用账号 | `PATCH /admin/users/{id}/status` | 保留账号和历史关联，仅禁止登录 |
| 软删除账号 | 可新增 `deleted` 字段后使用 `DELETE /admin/users/{id}` | 从列表默认隐藏，但保留记录 |
| 物理删除账号 | 可新增 `DELETE /admin/users/{id}/physical` | 真实删除，历史表中的用户外键置空 |

当前阶段建议先保持 `status = 0` 的禁用语义即可。原因是前端用户管理、权限校验和旧 token 失效都已经围绕 `status` 工作，继续推进前端时不需要先改表。

## 权限与鉴权修复

### 已新增能力

- 新增 `@RequireRole` 注解。
- 新增 `RoleInterceptor`，用于拦截管理员专用接口。
- `AuthInterceptor` 在解析 JWT 后会用 `userId` 查询 `sys_user`。
- 如果用户不存在或 `status = 0`，返回 `401`，旧 token 不能继续访问。
- `ForbiddenException` 用于角色不足时返回 `403`。

### 2026-06-27 修复

JJWT 0.12 返回的 `Claims` 是不可变对象，不能执行 `claims.put(...)`。

修复方式：

- `SysUserContext` 不再保存 JWT `Claims`。
- `SysUserContext` 改为保存数据库查询出的 `SysUser`。
- `/me`、权限判断、当前用户 ID 都从数据库用户上下文读取。

这保证了用户状态和角色以数据库为准，也避免修改不可变 `Claims` 的运行时报错。

## 管理员用户管理接口

这些接口供后续前端新增“用户管理”页面使用。全部需要 `ADMIN` token。

基础路径：

```text
/StudentAnalytics/admin/users
```

| 方法 | 路径 | 用途 | 前端状态 |
| --- | --- | --- | --- |
| `GET` | `/admin/users?page_num=1&page_size=20` | 用户分页列表 | 待接入 |
| `GET` | `/admin/users/{id}` | 用户详情 | 待接入 |
| `POST` | `/admin/users` | 新增用户 | 待接入 |
| `PUT` | `/admin/users/{id}` | 修改真实姓名、角色、状态 | 待接入 |
| `PATCH` | `/admin/users/{id}/status` | 启用或禁用用户 | 待接入 |
| `PATCH` | `/admin/users/{id}/password` | 重置密码 | 待接入 |
| `DELETE` | `/admin/users/{id}` | 当前语义为禁用用户 | 待接入 |

分页默认：

- `page_num = 1`
- `page_size = 20`
- 最大 `page_size = 100`

用户列表返回 `PageResultVO<AdminUserVO>`：

- `total`
- `records`

`AdminUserVO` 字段：

- `id`
- `username`
- `real_name`
- `role`
- `status`
- `created_at`
- `updated_at`

前端注意：

- 不返回密码。
- `role` 当前只允许 `ADMIN` 和 `TEACHER`。
- 禁止创建 `STUDENT`。
- 禁止禁用当前登录用户。
- 禁止禁用或降级最后一个启用状态的 `ADMIN`。

## 学生新增接口

该接口供后续前端新增“新增学生”表单或按钮使用。需要 `ADMIN` token。

```text
POST /StudentAnalytics/students
```

请求体：

```json
{
  "student_no": 3001,
  "name": "New Student",
  "age": 16,
  "gender": 0,
  "ethnicity": 1,
  "parental_education": 2,
  "class_name": "1-3"
}
```

规则：

- `student_no` 必填且唯一。
- `name` 可空；为空时后端生成 `Student {studentNo}`。
- `age` 按数据库约束为 `0-30`。
- `gender` 为 `0/1`。
- `ethnicity` 为 `0-3`。
- `parental_education` 为 `0-4`。
- `class_name` 支持 `1-3` 或 `高一 3 班`。
- `grade_level` 由后端从 `class_name` 反推。
- 新增学生只写 `student` 表，不自动创建 `student_performance`。

返回：

```text
Result<StudentDetailVO>
```

前端保存成功后可以直接跳转详情页或刷新详情页。

## 学业表现 Upsert 接口

该接口供后续前端在学生详情页补充或编辑学习表现使用。需要 `ADMIN` token。

```text
PUT /StudentAnalytics/students/performance/{studentNo}
```

请求体：

```json
{
  "study_time_weekly": 12.5,
  "absences": 3,
  "tutoring": true,
  "parental_support": 3,
  "extracurricular": true,
  "sports": false,
  "music": true,
  "volunteering": false,
  "gpa": 3.7
}
```

语义：

- 如果学生已有 `student_performance`，则更新。
- 如果学生没有 `student_performance`，则新增。
- 学生不存在或已删除时返回业务错误。

规则：

- 不允许前端提交 `grade_class`。
- `grade_class` 由后端根据 GPA 自动计算。
- `data_source = MANUAL`。
- `data_quality_status = 0`。
- 更新已有记录时会清空 `quality_issue`。

返回：

```text
Result<StudentDetailVO>
```

前端保存成功后直接用返回的详情数据刷新页面即可。

## 现有写操作权限变化

以下接口昨晚新增了 `ADMIN` 权限限制：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| `PUT` | `/students/overview/{studentNo}` | 修改学生概览字段 |
| `DELETE` | `/students/overview/{studentNo}` | 软删除学生 |

`TEACHER` 仍保留已有查看能力：

- 学生列表和筛选。
- 学生详情。
- 分析图表。
- 当前用户信息。

## 前端下一步边界

下一步写前端时，不需要重新做已经适配的后端接口：

- `GET /students/list`
- `GET /students/page`
- `GET /students/filter-options`
- `GET /students/detail/{studentNo}`
- `GET /analytics/gpa-distribution`
- `GET /analytics/grade-class-distribution`
- `GET /analytics/performance-points`

前端下一步应优先接入：

1. 管理员用户管理页面，对接 `/admin/users` 系列接口。
2. 学生新增入口，对接 `POST /students`。
3. 学生详情页的学习表现新增 / 编辑入口，对接 `PUT /students/performance/{studentNo}`。
4. 基于当前用户 `role` 控制按钮显示：`ADMIN` 显示新增、编辑、删除、用户管理；`TEACHER` 只显示查看。
5. 统一处理 `401` 和 `403`：`401` 走重新登录，`403` 给出无权限提示。

## 本次建议验证

后端提交或继续前端开发前，建议保留以下验证结果：

```bash
mvn -q -DskipTests compile
xmllint --noout src/main/resources/mapper/StudentMapper.xml src/main/resources/mapper/AnalysisMapper.xml
git diff --check -- src/main/java src/main/resources
```

`doc/backend-log.md` 被 `.gitignore` 的 `doc/*` 忽略。如果需要把这份日志提交到 Git，需要使用：

```bash
git add -f doc/backend-log.md
```

# 2026-06-26

## 文档用途

这份文档作为后端开发日志使用。后续后端相关的新增接口、Service 规则、Mapper 调整、数据库设计变化和验证结果，都继续追加到本文件中，并以当天日期作为一级标题。

本次记录基于当前 Spring Boot 后端实际代码，不包含前端实现细节。

## 当前后端总体状态

后端当前已形成以下可用能力：

- 用户认证与权限：注册、登录、退出、当前登录用户信息、基于角色的接口访问控制。
- 管理员用户管理：用户分页查询、详情、新增、修改、启用/禁用、重置密码、删除即禁用。
- 学生管理：学生概览列表、筛选、搜索、排序、新增、更新、软删除、详情聚合。
- 学业表现管理：查看学生表现，并支持按学生新增或更新表现记录。
- 分析统计：GPA 区间分布、成绩等级分布、学习表现散点数据，并支持按年级和班级限定分析范围。

运行配置：

- 服务端口：`8081`
- 上下文路径：`/StudentAnalytics`
- 数据库：`student_analytics`
- 数据源配置来自 `application.yml`，支持通过 `MYSQL_HOST`、`MYSQL_DATABASE`、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_URL` 覆盖。
- MyBatis XML 位置：`classpath:mapper/*.xml`
- MyBatis 开启下划线到驼峰映射：`map-underscore-to-camel-case: true`

统一返回结构使用 `Result<T>`。业务异常通过 `BusinessException` 返回普通错误结果，JWT 鉴权异常通过 `JwtAuthenticationException` 返回 `401`，角色权限不足通过 `ForbiddenException` 返回 `403`。

## 鉴权与用户模块

### 已实现接口

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| `POST` | `/register` | 注册系统用户 |
| `POST` | `/login` | 登录并返回 JWT |
| `POST` | `/logout` | 清理当前线程中的用户上下文 |
| `GET` | `/me` | 返回当前 JWT 中解析出的用户信息 |
| `GET` | `/hello` | 简单连通性测试 |

在完整访问地址中，需要带上上下文路径，例如：

```text
POST /StudentAnalytics/login
GET /StudentAnalytics/me
```

### 业务规则

- 注册时根据用户名查重，重复则抛出 `用户名已存在`。
- 密码通过 `PasswordUtil.encode()` 加密保存。
- 公开注册接口只允许创建 `TEACHER` 账号；未显式提交角色时，默认角色为 `TEACHER`。
- 公开注册接口不允许创建 `ADMIN` 或 `STUDENT`，管理员账号应通过管理员用户管理接口或数据库初始化准备。
- 登录时用户名不存在或密码不匹配，统一返回 `用户名或密码错误`。
- 登录成功后通过 `JwtUtil.createLoginToken()` 写入 `userId`、`username`、`realName`、`role`。

### 拦截器

`WebConfig` 注册 `AuthInterceptor`：

- 拦截路径：`/**`
- 放行路径：`/login`、`/register`

这意味着除注册和登录外，其它接口都应携带有效 `Authorization` 请求头。

`AuthInterceptor` 当前会在每次请求中：

- 读取 `Authorization` 请求头。
- 解析 JWT，取得 `userId`。
- 通过 `sys_user.id` 查询当前用户。
- 如果用户不存在或 `status = 0`，抛出 `JwtAuthenticationException`，旧 token 不能继续访问。
- 将当前数据库中的 `username`、`realName`、`role` 写回 `SysUserContext`。

`WebConfig` 还注册了 `RoleInterceptor`：

- 读取 Controller 类或方法上的 `@RequireRole`。
- 未标注 `@RequireRole` 的接口，只要求登录，不做角色限制。
- 标注了 `@RequireRole({"ADMIN"})` 的接口只允许管理员访问。
- 当前权限策略是简单角色控制，不引入 `permission`、`role_permission`、`user_role` 等 RBAC 表。

### 权限规则

当前已落地的权限规则：

| 模块 | ADMIN | TEACHER |
| --- | --- | --- |
| 登录 / 退出 / 当前用户 | 是 | 是 |
| 学生查询 / 详情 | 是 | 是 |
| 分析图表 | 是 | 是 |
| 学生新增 | 是 | 否 |
| 学生修改 | 是 | 否 |
| 学生删除 | 是 | 否 |
| 学业表现新增 / 修改 | 是 | 否 |
| 用户管理 | 是 | 否 |
| 操作日志查看 | 暂未实现，仅保留 TODO | 否 |
| 模型训练 | 暂未实现，仅保留 TODO | 否 |
| 单学生预测 | 暂未实现，仅保留 TODO | 否 |
| 风险预警 | 暂未实现，仅保留 TODO | 否 |

## 管理员用户管理模块

### 已实现接口

Controller 基础路径：

```text
/admin/users
```

全部接口都标注 `@RequireRole({"ADMIN"})`。

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| `GET` | `/admin/users` | 分页查询用户列表 |
| `GET` | `/admin/users/{id}` | 查询用户详情 |
| `POST` | `/admin/users` | 新增用户 |
| `PUT` | `/admin/users/{id}` | 修改用户信息 |
| `PATCH` | `/admin/users/{id}/status` | 启用或禁用用户 |
| `PATCH` | `/admin/users/{id}/password` | 重置用户密码 |
| `DELETE` | `/admin/users/{id}` | 删除用户，实际语义为禁用 |

### 分页查询

`GET /admin/users` 支持参数：

| 参数 | 默认值 | 含义 |
| --- | --- | --- |
| `page_num` | `1` | 页码，从 1 开始 |
| `page_size` | `20` | 每页数量，最大限制为 100 |

返回结构使用 `PageResultVO<AdminUserVO>`：

- `total`：用户总数。
- `records`：当前页用户列表。

`AdminUserVO` 不返回密码字段，只返回：

- `id`
- `username`
- `real_name`
- `role`
- `status`
- `created_at`
- `updated_at`

分页查询当前由 `SysUserMapper.selectAdminUserPage()` 和 `countAdminUsers()` 显式 SQL 实现，没有依赖 MyBatis-Plus 分页插件。

### 新增用户

`POST /admin/users` 请求体：

```json
{
  "username": "teacher01",
  "password": "123456",
  "real_name": "Teacher 01",
  "role": "TEACHER"
}
```

规则：

- `username` 必填且唯一。
- `password` 必填，长度为 `6-100`，保存前通过 `PasswordUtil.encode()` 加密。
- `real_name` 可空，空白字符串会规范化为 `null`。
- `role` 只允许 `ADMIN` 或 `TEACHER`。
- 不开放创建 `STUDENT`。
- `status` 默认写入 `1`。

### 修改用户

`PUT /admin/users/{id}` 可修改：

- `real_name`
- `role`
- `status`

规则：

- 不支持修改 `username`，避免影响登录标识。
- `role` 只允许 `ADMIN` 或 `TEACHER`。
- `status` 只能为 `0` 或 `1`。
- 不能禁用当前登录用户。
- 不能禁用或降级最后一个启用状态的 `ADMIN`。

### 启用 / 禁用用户

`PATCH /admin/users/{id}/status` 请求体：

```json
{
  "status": 0
}
```

规则：

- `status = 1` 表示启用。
- `status = 0` 表示禁用。
- 不能禁用当前登录用户。
- 不能禁用最后一个启用状态的 `ADMIN`。
- 用户被禁用后，即使持有旧 JWT，也会在 `AuthInterceptor` 中被拒绝访问。

### 重置密码

`PATCH /admin/users/{id}/password` 请求体：

```json
{
  "new_password": "newPassword123"
}
```

规则：

- `new_password` 必填，长度为 `6-100`。
- 保存前通过 `PasswordUtil.encode()` 加密。

### 删除用户

`DELETE /admin/users/{id}` 当前不物理删除 `sys_user`，而是执行禁用语义：

```text
status = 0
```

原因是 `sys_user` 当前没有 `deleted` 字段，且用户可能被 `operation_log`、`model_version` 等表引用。

## 学生概览模块

### 已实现接口

Controller 路径同时兼容：

- `/students`
- `/student`

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| `GET` | `/students/list` | 查询所有未删除学生概览 |
| `GET` | `/students/page` | 查询默认班级、关键词搜索或组合筛选后的学生概览 |
| `GET` | `/students/filter-options` | 返回前端筛选项 |
| `POST` | `/students` | 新增学生基础信息 |
| `PUT` | `/students/overview/{studentNo}` | 更新学生概览字段 |
| `DELETE` | `/students/overview/{studentNo}` | 按学号软删除学生 |
| `GET` | `/students/detail/{studentNo}` | 查询学生详情聚合信息 |

说明：

- 查询类接口仍兼容 `/students` 和 `/student` 两个 Controller 路径。
- 新增学生接口由 `StudentAdminController` 单独挂在 `/students`，不会生成 `/student` 别名。
- `POST /students`、`PUT /students/overview/{studentNo}`、`DELETE /students/overview/{studentNo}` 都要求 `ADMIN`。

### 查询能力

`GET /students/page` 支持以下参数：

| 参数 | 含义 |
| --- | --- |
| `class_name` | 班级，可重复传参，也兼容逗号分隔字符串 |
| `min_gpa` | GPA 下限 |
| `max_gpa` | GPA 上限 |
| `grade_class` | 成绩等级，`0-4` |
| `grade_level` | 年级，`1=高一`、`2=高二`、`3=高三` |
| `gender` | 性别，`0/1` |
| `sort_field` | 排序字段 |
| `sort_order` | `asc` 或 `desc` |
| `keyword` | 姓名或纯数字学号关键词 |

查询模式规则：

- 没有关键词和筛选条件时，默认查 `1-1` 班。
- 关键词搜索支持姓名模糊匹配；如果关键词为纯数字，也同时匹配 `student_no`。
- 关键词搜索不能与班级、GPA、成绩等级、年级、性别筛选混用。
- GPA 上下限如果传反，后端会自动交换。
- 排序字段白名单：`studentNo`、`name`、`age`、`gpa`、`gradeClass`。
- 排序 SQL 列由后端白名单转换，避免直接使用前端传入值拼接任意列。

### 班级格式

数据库存储班级为原始格式：

```text
1-1
1-2
2-3
```

后端同时支持把展示格式规范化为数据库格式：

```text
高一 1 班 -> 1-1
高二 3 班 -> 2-3
```

返回给前端时使用 `ClassInfoVO` 同时保留：

- `grade_level`：年级数字。
- `raw_class_name`：数据库原始班级值。
- `class_name`：中文展示值，例如 `高一 1 班`。

### 新增学生

`POST /students` 请求体：

```json
{
  "student_no": 3001,
  "name": "New Student",
  "age": 16,
  "gender": 0,
  "ethnicity": 1,
  "parental_education": 2,
  "class_name": "1-3"
}
```

也支持驼峰字段：

- `studentNo`
- `parentalEducation`
- `className`

新增规则：

- `student_no` 必填且唯一。
- `name` 可空；为空时后端自动生成 `Student {studentNo}`。
- `age` 按数据库约束校验为 `0-30`。
- `gender` 只能为 `0` 或 `1`。
- `ethnicity` 只能为 `0-3`。
- `parental_education` 只能为 `0-4`。
- `class_name` 必填，支持 `1-3` 或 `高一 3 班`。
- `grade_level` 不由前端提交，而是从 `class_name` 自动反推。
- `deleted` 默认写入 `0`。
- 新增学生只写入 `student` 表，不创建默认 `student_performance`。

返回：

```text
Result<StudentDetailVO>
```

新增后会立即调用 `selectStudentDetail(studentNo)` 返回详情。因为表现记录尚未创建时，`performance_available = false`。

### 更新能力

`PUT /students/overview/{studentNo}` 当前支持更新：

- `name`
- `age`
- `gender`
- `className`
- `gradeLevel`
- `gpa`

更新规则：

- `studentNo` 不能为空。
- 请求体不能为空，且必须包含至少一个可更新字段。
- `gradeClass` 不能手动提交，成绩等级由 GPA 自动计算。
- `gradeLevel` 不能单独修改；提交班级时后端会从班级推导年级。
- 如果同时提交 `gradeLevel` 和 `className`，二者必须一致。
- 修改 `gpa` 时同步更新 `student_performance.grade_class`。
- 如果学生没有成绩记录，不能修改 GPA。

GPA 到 `grade_class` 的规则：

| GPA 范围 | grade_class |
| --- | --- |
| `3.5 <= GPA <= 4.0` | `0` |
| `3.0 <= GPA < 3.5` | `1` |
| `2.5 <= GPA < 3.0` | `2` |
| `2.0 <= GPA < 2.5` | `3` |
| `GPA < 2.0` | `4` |

### 软删除

`DELETE /students/overview/{studentNo}` 不物理删除学生，而是更新：

```sql
deleted = 1
updated_at = CURRENT_TIMESTAMP
```

后续概览、筛选、详情和分析查询都以 `s.deleted = 0` 作为基础条件。

## 学业表现管理模块

### 已实现接口

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| `PUT` | `/students/performance/{studentNo}` | 新增或更新指定学生的学业表现 |

该接口要求 `ADMIN`。

### 请求体

```json
{
  "study_time_weekly": 12.5,
  "absences": 3,
  "tutoring": true,
  "parental_support": 3,
  "extracurricular": true,
  "sports": false,
  "music": true,
  "volunteering": false,
  "gpa": 3.7
}
```

也支持驼峰字段：

- `studyTimeWeekly`
- `parentalSupport`

### Upsert 语义

后端按 `studentNo` 查找未删除学生：

- 学生不存在或已删除：返回 `学生不存在或已删除`。
- 已有 `student_performance`：更新原表现记录。
- 没有 `student_performance`：插入新表现记录。

写入规则：

- `study_time_weekly` 范围为 `0-60`，Java 使用 `BigDecimal`。
- `absences` 范围为 `0-30`。
- `tutoring`、`extracurricular`、`sports`、`music`、`volunteering` 由 Boolean 转为数据库 `0/1`。
- `parental_support` 范围为 `0-4`。
- `gpa` 范围为 `0-4`，Java 使用 `BigDecimal`。
- 前端不能提交 `grade_class`，成绩等级由 GPA 自动计算。
- 新增或更新后，`data_source` 写入 `MANUAL`。
- 新增或更新后，`data_quality_status` 写入 `0`。
- 更新已有表现记录时，清空 `quality_issue`。

返回：

```text
Result<StudentDetailVO>
```

保存成功后会重新查询并返回完整学生详情。

当前不开放：

```text
DELETE /students/performance/{studentNo}
```

原因是 `student_performance` 是分析统计、模型训练、预测和预警的核心数据，删除会引入较多后续一致性处理。

## 学生详情模块

### 已实现接口

```text
GET /students/detail/{studentNo}
```

### 返回结构

`StudentDetailVO` 当前由以下部分组成：

- `basic_info`：学生基础信息。
- `academic_performance`：GPA、成绩等级、每周学习时间、缺勤次数。
- `support_status`：是否参加辅导、家长支持程度。
- `activity_profile`：课外活动、体育、音乐、志愿活动。
- `performance_available`：是否存在 `student_performance` 记录。

### 数据来源

详情查询通过 `StudentMapper.selectStudentDetailAggregateRow()` 一次性聚合：

- `student` 表基础信息。
- `student_performance` 表学习表现。
- `performance_available` 标记是否存在表现记录。

详情内部使用 `StudentDetailAggregateRow`，再转换为 `StudentDetailVO`。转换时复用已有枚举和 VO：

- `GenderEnum`
- `GradeClassEnum`
- `ParentalSupportEnum`
- `YesNoEnum`
- `ClassInfoVO`
- `OptionVO<T>`

## 分析统计模块

### 已实现接口

Controller 基础路径：

```text
/analytics
```

| 方法 | 路径 | 功能 |
| --- | --- | --- |
| `GET` | `/analytics/gpa-distribution` | GPA 区间人数和占比 |
| `GET` | `/analytics/grade-class-distribution` | 成绩等级人数和占比 |
| `GET` | `/analytics/performance-points` | 学习表现散点数据 |

三类接口都支持相同范围参数：

| 参数 | 含义 |
| --- | --- |
| `grade_level` | 年级，`1-3`，可不传 |
| `class_name` | 班级列表，可重复传参，也兼容逗号分隔 |

范围校验规则：

- `grade_level` 只能为 `1`、`2`、`3`。
- 如果传了 `class_name`，必须同时传 `grade_level`。
- `class_name` 必须匹配 `[1-3]-数字`。
- 班级所属年级必须与 `grade_level` 一致。

### GPA 区间统计

`GET /analytics/gpa-distribution` 返回 `GpaDistributionItemVO`：

- `bucketIndex`
- `label`
- `minGpa`
- `maxGpa`
- `studentCount`
- `percentage`

GPA 区间定义集中在 `GpaBucketEnum`：

| code | label |
| --- | --- |
| `0` | `[0.0, 0.5)` |
| `1` | `[0.5, 1.0)` |
| `2` | `[1.0, 1.5)` |
| `3` | `[1.5, 2.0)` |
| `4` | `[2.0, 2.5)` |
| `5` | `[2.5, 3.0)` |
| `6` | `[3.0, 3.5)` |
| `7` | `[3.5, 4.0]` |

百分比用 `BigDecimal` 计算，保留 2 位小数，`RoundingMode.HALF_UP`。

### 成绩等级统计

`GET /analytics/grade-class-distribution` 返回 `GradeClassDistributionItemVO`：

- `gradeClass`：`OptionVO<Integer>`
- `studentCount`
- `percentage`

等级标签来自 `GradeClassEnum`，并按 code 升序返回。

### 学习表现散点数据

`GET /analytics/performance-points` 返回 `PerformanceAnalysisPointVO`：

- `studentNo`
- `name`
- `classInfo`
- `studyTimeWeekly`
- `absences`
- `gpa`
- `gradeClass`
- `gpaBucket`

该接口可支撑多种图表：

- 缺勤次数与 GPA。
- 每周学习时间与 GPA。
- 学习时间、缺勤次数、GPA 三变量联合散点图。
- 按 GPA 连续色带、GPA 分桶或成绩等级着色。

## 数据库设计现状

当前主脚本为 `src/main/resources/sql/V0.sql`，已定义以下表：

| 表 | 当前状态 |
| --- | --- |
| `sys_user` | 已被用户认证和管理员用户管理模块使用 |
| `dict_item` | 字典表已建表，当前 Java 侧主要使用枚举类做翻译 |
| `student` | 已被学生新增、概览、详情、分析模块使用 |
| `student_performance` | 已被学业表现 upsert、学生概览、详情、分析模块使用 |
| `model_version` | 表结构已准备，当前未开放 Java 接口 |
| `prediction_result` | 表结构已准备，当前未开放 Java 接口 |
| `warning_record` | 表结构已准备，当前未开放 Java 接口 |
| `import_batch` | 表结构已准备，当前未开放 Java 接口 |
| `operation_log` | 表结构已准备，当前未开放 Java 接口 |

关键字段设计：

- `student.grade_level`：年级，`1=高一`、`2=高二`、`3=高三`。
- `student.class_name`：数据库原始班级，例如 `1-3`。
- `student.deleted`：软删除标记。
- `student.age`：数据库约束为 `0-30`，后端新增和修改都按该范围校验。
- `student.ethnicity`：取值 `0-3`，新增学生时必填。
- `student.parental_education`：取值 `0-4`，新增学生时必填。
- `student_performance.study_time_weekly`：`DECIMAL(7,4)`，Java 使用 `BigDecimal`。
- `student_performance.gpa`：`DECIMAL(5,4)`，Java 使用 `BigDecimal`。
- `student_performance.grade_class`：由 GPA 派生，取值 `0-4`。
- `student_performance.data_source`：新增或人工更新表现记录时写入 `MANUAL`。
- `student_performance.data_quality_status` 和 `quality_issue`：用于记录导入数据质量。

## 枚举与翻译

当前后端通过 VO 枚举集中处理前端展示翻译：

- `GenderEnum`：性别。
- `GradeClassEnum`：成绩等级。
- `ParentalSupportEnum`：家长支持程度。
- `YesNoEnum`：`0/1` 转布尔或选项。
- `GpaBucketEnum`：GPA 分桶。
- `ClassInfoVO`：班级原始值和中文展示值转换。
- `OptionVO<T>`：统一承载 `{ value, label }`。

## Mapper 与查询结构

### StudentMapper

当前覆盖：

- 学生概览行查询。
- 按班级默认查询。
- 按关键词搜索。
- 多条件筛选。
- 班级筛选项查询。
- GPA 最大/最小值查询。
- 学生详情聚合查询。
- 学生概览字段更新。
- 学生成绩字段更新。
- 学生软删除。
- 学生新增依赖 MyBatis-Plus `BaseMapper<Student>.insert()`。
- 学生存在性和唯一性检查依赖 MyBatis-Plus 条件查询。

详情查询使用 `resultMap + association` 聚合 `StudentOverviewRow` 和 `StudentDetailRow`，并遵守 MyBatis DTD 顺序：`result` 在 `association` 前。

### PerformanceMapper

新增 `PerformanceMapper extends BaseMapper<Performance>`，用于学业表现 upsert：

- 通过 `student_id` 查询是否已有表现记录。
- 没有记录时插入 `student_performance`。
- 已有记录时按主键更新 `student_performance`。

### SysUserMapper

当前覆盖：

- 按启用状态用户名查询登录用户。
- 按用户名查询任意状态用户，用于注册和管理员新增用户查重。
- 管理员用户列表分页查询。
- 管理员用户总数查询。
- 其它单条查询、插入、更新依赖 MyBatis-Plus `BaseMapper<SysUser>`。

用户分页查询目前使用显式 SQL：

```sql
SELECT id, username, password, real_name AS realName, role, status,
       created_at AS createdAt, updated_at AS updatedAt
FROM sys_user
ORDER BY created_at DESC, id ASC
LIMIT #{pageSize} OFFSET #{offset}
```

### AnalysisMapper

当前覆盖：

- `selectGpaBucketCounts`
- `selectGradeClassCounts`
- `selectPerformanceAnalysisPoints`

三类查询共用 `AnalysisScopeCondition`：

- 按 `grade_level` 过滤。
- 按多个 `class_name` 过滤。

## 当前未完成或仅准备的部分

以下内容在数据库层或结构层已有准备，但当前后端接口尚未真正接入：

- 模型版本管理。
- 成绩等级预测结果写入和查询。
- 学业风险预警。
- 导入批次管理接口。
- 操作日志记录和查询接口。
- 数据字典接口；当前字典翻译主要由 Java 枚举承担。

本次已为暂不实现的模块保留空 Controller 和 TODO 注释：

| Controller | 基础路径 | 当前状态 |
| --- | --- | --- |
| `OperationLogController` | `/admin/operation-logs` | 仅 TODO，后续实现操作日志分页查询 |
| `ModelController` | `/models` | 仅 TODO，后续实现模型训练和模型版本管理 |
| `PredictionController` | `/predictions` | 仅 TODO，后续实现单学生预测 |
| `WarningController` | `/warnings` | 仅 TODO，后续实现风险预警查看和处理 |

这些 Controller 均已标注 `@RequireRole({"ADMIN"})`，但暂时没有具体接口方法。

## 本次新增文件和调整

### 新增核心文件

- `annotation/RequireRole.java`：角色权限注解。
- `interceptor/RoleInterceptor.java`：读取 `@RequireRole` 并校验当前用户角色。
- `exception/ForbiddenException.java`：角色权限不足异常，统一返回 `403`。
- `controller/StudentAdminController.java`：只挂 `/students`，提供学生新增和学业表现 upsert。
- `controller/AdminUserController.java`：管理员用户管理接口。
- `mapper/PerformanceMapper.java`：学业表现表 MyBatis-Plus Mapper。
- `vo/PageResultVO.java`：通用分页返回结构。
- `vo/AdminUserVO.java`：管理员用户列表和详情响应 VO。

### 新增请求 DTO

- `StudentCreateRequest`
- `StudentPerformanceUpsertRequest`
- `UserCreateRequest`
- `UserUpdateRequest`
- `UserStatusUpdateRequest`
- `UserPasswordResetRequest`

### 调整文件

- `WebConfig`：注册 `RoleInterceptor`。
- `AuthInterceptor`：解析 token 后按 `userId` 查库，确保禁用用户无法继续使用旧 token。
- `GlobalExceptionHandler`：新增 `ForbiddenException` 处理。
- `SysUser`：补充 `createdAt`、`updatedAt` 字段，供管理员用户列表返回。
- `SysUserMapper`：补充用户查重和分页 SQL。
- `SysUserService` / `SysUserServiceImpl`：补充管理员用户管理业务。
- `StudentService` / `StudentServiceImpl`：补充学生新增和学业表现 upsert。
- `StudentController`：已有修改和删除接口加 `@RequireRole({"ADMIN"})`。

## 最近后端提交

最近一次后端提交：

```text
0cc8c35 完善分析接口范围筛选
```

主要内容：

- 分析接口支持 `grade_level` 和 `class_name` 范围筛选。
- 新增 `AnalysisScopeQueryRequest`。
- `AnalysisMapper.xml` 新增分析范围 SQL 条件。
- `PerformanceAnalysisPointVO` 增加 `classInfo`。
- 学习表现点返回 `gradeClass` 和 `gpaBucket`。

## 验证记录

本次新增接口和文档更新前，后端已通过：

```bash
mvn -q -DskipTests compile
xmllint --noout src/main/resources/mapper/AnalysisMapper.xml
```

本次新增学生 CRUD 补齐、学业表现 upsert、权限控制、管理员用户管理和本文档更新后，已通过：

```bash
mvn -q -DskipTests compile
xmllint --noout src/main/resources/mapper/StudentMapper.xml src/main/resources/mapper/AnalysisMapper.xml
git diff --check -- src/main/java src/main/resources
```

注意：当前仓库 `.gitignore` 忽略 `doc/*`，如果后续需要把本日志提交到 Git，需要使用：

```bash
git add -f doc/backend-log.md
```
