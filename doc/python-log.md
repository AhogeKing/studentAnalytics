# Python ML 模块开发日志

本文记录当前 `python/ml` 模块已经实现的功能、目录结构、训练方式、测试方式和当前模型效果。

## 2026-06-28 更新记录

### 后端集成参数补充

为了配合 Spring Boot 后端的模型版本管理和后续预测落库，Python ML 脚本已补齐必要的命令行参数。

#### `train_decision.py`

当前训练脚本支持：

```bash
python python/ml/train_decision.py \
  --data-path dataset/Student_performance_data_.csv \
  --artifact-dir runtime/ml/models/{versionNo}
```

参数说明：

| 参数 | 说明 |
| --- | --- |
| `--data-path` | 指定训练 CSV 路径。后端会从 `v_student_model_dataset` 导出当前数据库训练数据到该路径 |
| `--artifact-dir` | 指定训练产物输出目录。后端会为每个 `model_version.version_no` 创建独立目录 |
| `--quick` | 使用快速参数网格 |
| `--exhaustive` | 使用大范围穷举参数网格 |

后端集成后，每次训练推荐输出到：

```text
runtime/ml/models/{versionNo}/
├── grade_class_decision_tree.joblib
├── metrics.json
└── train.log
```

其中：

- `grade_class_decision_tree.joblib`：当前版本的完整 sklearn Pipeline。
- `metrics.json`：当前版本训练指标和最佳参数。
- `train.log`：后端调用 Python 进程时保存的训练输出日志。

#### `predict_descision.py`

当前预测脚本支持：

```bash
python python/ml/predict_descision.py \
  --model-path runtime/ml/models/{versionNo}/grade_class_decision_tree.joblib \
  --json '{"Age":17,"Gender":1,"Ethnicity":0,"ParentalEducation":2,"StudyTimeWeekly":12.5,"Absences":8,"Tutoring":1,"ParentalSupport":3,"Extracurricular":1,"Sports":0,"Music":1,"Volunteering":0}' \
  --output runtime/ml/predictions/result.json
```

参数说明：

| 参数 | 说明 |
| --- | --- |
| `--model-path` | 指定要加载的 `.joblib` 模型文件。后续 Java 可使用 `model_version.model_path` 传入 |
| `--output` | 指定预测结果 JSON 输出路径。不传时仍输出到 stdout |
| `--sample` | 使用内置样例学生 |
| `--input` | 从 JSON 文件读取单个学生输入 |
| `--json` | 从命令行字符串读取单个学生输入 |

`--output` 的行为：

- 如果不传 `--output`，脚本保持原行为，直接向 stdout 打印预测 JSON。
- 如果传入 `--output`，脚本会自动创建父目录，并把预测结果写入指定 JSON 文件。

该参数是后续后端实现 `prediction_result` 落库的基础：Java 可以按当前启用的 `model_version.model_path` 调用脚本，并从 `--output` 文件读取结构化预测结果。

### 验证记录

本次更新已通过：

```bash
python3 -m py_compile python/ml/predict_descision.py
```

当前系统 Python 环境未安装 `joblib`，因此没有执行真实预测命令。实际训练和预测前，需要先安装：

```bash
pip install -r python/ml/requirements.txt
```

## 1. 模块目标

当前 Python 模块用于训练 StudentAnalytics 的第一个机器学习模型：

- 任务：预测学生成绩等级 `A/B/C/D/F`
- 模型：`DecisionTreeClassifier`
- 训练标签：`GradeClassClean`
- 输入特征：不包含 `StudentID`、`GPA`、原始 `GradeClass`
- 当前阶段：独立 Python 训练与预测模块
- 暂未包含：FastAPI 服务、Spring Boot 调用、模型版本落库、预测结果落库、预警生成

设计目标是先让 Python 侧可以单独完成：

1. 读取 CSV 数据。
2. 清洗标签。
3. 构造衍生特征。
4. 训练决策树模型。
5. 输出测试集指标。
6. 保存模型文件和指标文件。
7. 支持单学生预测。

## 2. 数据与标签

默认数据源：

```text
dataset/Student_performance_data_.csv
```

原始字段：

```text
StudentID
Age
Gender
Ethnicity
ParentalEducation
StudyTimeWeekly
Absences
Tutoring
ParentalSupport
Extracurricular
Sports
Music
Volunteering
GPA
GradeClass
```

训练时不直接使用原始 `GradeClass`，而是根据 `GPA` 重新生成 `GradeClassClean`：

```text
GPA >= 3.5       -> A / 0
3.0 <= GPA < 3.5 -> B / 1
2.5 <= GPA < 3.0 -> C / 2
2.0 <= GPA < 2.5 -> D / 3
GPA < 2.0        -> F / 4
```

原因：

- 原始 CSV 中 `GradeClass` 与 GPA 阈值存在不一致。
- 当前统计结果为 `168 / 2392` 条不一致，比例约 `7.02%`。
- 后端当前业务语义是 `grade_class` 由 GPA 派生，因此训练标签使用 `GradeClassClean` 更一致。

## 3. 输入特征

训练输入共 14 个字段。

保留的原始特征：

```text
Age
Gender
Ethnicity
ParentalEducation
StudyTimeWeekly
Absences
Tutoring
ParentalSupport
Extracurricular
Sports
Music
Volunteering
```

新增衍生特征：

```text
ActivityCount = Extracurricular + Sports + Music + Volunteering
StudyAbsenceRatio = StudyTimeWeekly / (Absences + 1)
```

训练时删除：

```text
StudentID
GPA
GradeClass
```

其中 `GPA` 只用于生成训练标签，不作为输入特征，避免标签泄漏。

## 4. 目录结构

当前 Python ML 模块位于：

```text
python/ml/
```

文件说明：

```text
python/ml/__init__.py
python/ml/constants.py
python/ml/dataset.py
python/ml/feature_engineering.py
python/ml/preprocess.py
python/ml/evaluate.py
python/ml/train_decision.py
python/ml/predict_descision.py
python/ml/requirements.txt
python/ml/artifacts/
```

各文件职责：

| 文件 | 职责 |
|---|---|
| `constants.py` | 项目路径、默认数据路径、特征列、标签列、类别标签、随机种子等常量 |
| `dataset.py` | 读取 CSV、校验字段、转换数值类型、构建训练用 `X/y` |
| `feature_engineering.py` | 生成 `GradeClassClean`、`ActivityCount`、`StudyAbsenceRatio` |
| `preprocess.py` | 构建 `ColumnTransformer`、`Pipeline`、参数搜索网格 |
| `evaluate.py` | 计算 Accuracy、Precision、Recall、Macro-F1、AUC、混淆矩阵、分类报告 |
| `train_decision.py` | 训练入口，执行 train/test split、GridSearchCV、保存模型和指标 |
| `predict_descision.py` | 单学生预测入口，加载模型并输出等级、概率和重要特征 |
| `requirements.txt` | Python ML 依赖 |
| `artifacts/` | 训练输出目录 |

注意：`predict_descision.py` 沿用了当前文件名中的 `descision` 拼写，后续如果要统一命名，可以再改为 `predict_decision.py`。

## 5. 环境准备

进入项目根目录：

```bash
cd /Users/xavier/code/studentAnalytics
```

激活虚拟环境：

```bash
source python/venv/bin/activate
```

安装依赖：

```bash
pip install -r python/ml/requirements.txt
```

当前依赖：

```text
numpy
pandas
scikit-learn
joblib
```

## 6. 训练模型

### 6.1 快速训练

```bash
python python/ml/train_decision.py --quick
```

当前 `--quick` 使用 96 组参数：

```text
96 candidates * 5-fold = 480 fits
```

适合频繁本地验证和快速重训。

### 6.2 推荐正式训练

```bash
python python/ml/train_decision.py
```

当前默认正式训练使用 1728 组参数：

```text
1728 candidates * 5-fold = 8640 fits
```

在 Apple M1 上实测耗时约：

```text
160.68 秒，约 2 分 41 秒
```

该档位是当前推荐使用的训练方式，训练量比 `--quick` 更充分，同时控制在约 5 分钟内。

### 6.3 大范围穷举训练

```bash
python python/ml/train_decision.py --exhaustive
```

当前 `--exhaustive` 使用 8400 组参数：

```text
8400 candidates * 5-fold = 42000 fits
```

该模式训练时间较长，不建议在本地 M1 上日常使用。

## 7. 训练输出

训练完成后会在 `python/ml/artifacts/` 下生成模型产物。该目录是当前 ML 模块的运行结果目录，后续 FastAPI 或 Spring Boot 集成时，也会优先读取这里的正式模型文件。

```text
python/ml/artifacts/
├── grade_class_decision_tree.joblib
└── metrics.json
```

说明：

| 文件 | 内容 |
|---|---|
| `grade_class_decision_tree.joblib` | 正式模型文件，保存完整 sklearn Pipeline，包括缺失值处理、One-Hot 编码、特征预处理和最佳决策树模型 |
| `metrics.json` | 训练报告文件，保存数据规模、搜索配置、最佳参数、测试集指标、混淆矩阵、分类报告等 |

### 7.1 模型文件

`grade_class_decision_tree.joblib` 是预测时真正加载的文件。

它不是只保存单独的 `DecisionTreeClassifier`，而是保存整个 Pipeline：

```text
ColumnTransformer
-> numeric / categorical preprocessing
-> DecisionTreeClassifier
```

因此预测时只需要传入原始业务字段，代码会自动：

1. 生成 `ActivityCount`。
2. 生成 `StudyAbsenceRatio`。
3. 对 `Ethnicity` 做 One-Hot 编码。
4. 按训练时相同的 Pipeline 完成预测。

使用位置：

```text
python/ml/predict_descision.py
```

默认加载路径：

```text
python/ml/artifacts/grade_class_decision_tree.joblib
```

### 7.2 指标文件

`metrics.json` 用于记录一次训练的完整结果，方便之后对比不同训练参数和模型版本。

当前写入的主要字段包括：

| 字段 | 含义 |
|---|---|
| `model_type` | 当前模型类型，值为 `DecisionTreeClassifier` |
| `target_column` | 训练标签，当前为 `GradeClassClean` |
| `feature_columns` | 最终输入模型的 14 个特征列 |
| `train_rows` | 训练集行数 |
| `test_rows` | 测试集行数 |
| `test_size` | 测试集比例，当前为 `0.2` |
| `cv_folds` | 交叉验证折数，当前为 `5` |
| `random_state` | 随机种子，当前为 `42` |
| `search_mode` | 本次训练模式，例如 `quick`、`default`、`exhaustive` |
| `search_candidates` | GridSearchCV 搜索的参数组合数量 |
| `best_parameters` | GridSearchCV 找到的最佳参数 |
| `best_cv_f1_macro` | 训练集交叉验证中的最佳 Macro-F1 |
| `accuracy` | 测试集 Accuracy |
| `precision_macro` | 测试集 Macro Precision |
| `recall_macro` | 测试集 Macro Recall |
| `f1_macro` | 测试集 Macro-F1 |
| `auc_ovr_macro` | 测试集 One-vs-Rest Macro AUC |
| `confusion_matrix` | 测试集混淆矩阵 |
| `classification_report` | sklearn 分类报告 |
| `label_noise` | 原始 `GradeClass` 与 `GradeClassClean` 的不一致统计 |

注意：

- 每次重新运行 `python python/ml/train_decision.py` 都会覆盖这两个文件。
- `metrics.json` 适合查看模型效果；`grade_class_decision_tree.joblib` 适合实际预测。
- 如果之后要做模型版本管理，可以把这两个文件按时间或版本号复制到新的目录中，例如 `artifacts/v1/`、`artifacts/v2/`。

## 8. 查看训练指标

直接查看完整指标：

```bash
cat python/ml/artifacts/metrics.json
```

只查看关键指标：

```bash
python - <<'PY'
import json
from pathlib import Path

metrics = json.loads(Path("python/ml/artifacts/metrics.json").read_text(encoding="utf-8"))

for key in [
    "search_mode",
    "search_candidates",
    "train_rows",
    "test_rows",
    "accuracy",
    "precision_macro",
    "recall_macro",
    "f1_macro",
    "auc_ovr_macro",
]:
    print(f"{key}: {metrics[key]}")

print("best_parameters:", metrics["best_parameters"])
PY
```

当前最近一次默认正式训练结果：

```text
search_mode=default
search_candidates=1728
train_rows=1913
test_rows=479
accuracy=0.7432150313152401
precision_macro=0.6867092706738154
recall_macro=0.6214708972307433
f1_macro=0.6293671325294043
auc_ovr_macro=0.9032379157331294
```

当前最佳参数：

```text
model__ccp_alpha=0.0
model__criterion=entropy
model__max_depth=10
model__max_leaf_nodes=None
model__min_samples_leaf=10
model__min_samples_split=2
```

## 9. 测试单学生预测

训练完成后，可以使用内置样例测试预测流程：

```bash
python python/ml/predict_descision.py --sample
```

预期会输出：

```json
{
  "predicted_grade_class": 2,
  "predicted_grade_label": "C",
  "probabilities": {
    "A": 0.0,
    "B": 0.42105263157894735,
    "C": 0.5789473684210527,
    "D": 0.0,
    "F": 0.0
  },
  "important_features": [
    {
      "feature": "Absences",
      "importance": 0.6325802296077937
    }
  ]
}
```

实际输出中的概率和重要特征可能随重新训练结果略有变化。

## 10. 使用自定义 JSON 预测

命令行直接传入 JSON：

```bash
python python/ml/predict_descision.py --json '{"Age":17,"Gender":1,"Ethnicity":0,"ParentalEducation":2,"StudyTimeWeekly":12.5,"Absences":8,"Tutoring":1,"ParentalSupport":3,"Extracurricular":1,"Sports":0,"Music":1,"Volunteering":0}'
```

也可以把学生特征写入 JSON 文件，例如：

```json
{
  "Age": 17,
  "Gender": 1,
  "Ethnicity": 0,
  "ParentalEducation": 2,
  "StudyTimeWeekly": 12.5,
  "Absences": 8,
  "Tutoring": 1,
  "ParentalSupport": 3,
  "Extracurricular": 1,
  "Sports": 0,
  "Music": 1,
  "Volunteering": 0
}
```

然后运行：

```bash
python python/ml/predict_descision.py --input path/to/student.json
```

预测输入不要包含：

```text
StudentID
GPA
GradeClass
```

原因：

- `StudentID` 是标识字段，不应进入模型。
- `GPA` 是生成标签的依据，预测时未知，输入会造成标签泄漏。
- `GradeClass` 是原始标签，不是预测输入。

## 11. 当前训练流程

训练脚本 `train_decision.py` 的主要流程：

```text
读取 CSV
-> 校验字段
-> 数值转换
-> 根据 GPA 生成 GradeClassClean
-> 生成 ActivityCount
-> 生成 StudyAbsenceRatio
-> 构建 X/y
-> train_test_split(test_size=0.2, stratify=y, random_state=42)
-> 训练集内部 5-fold GridSearchCV
-> 使用 Macro-F1 选择最佳模型
-> 在测试集上评估
-> 保存 joblib 模型
-> 保存 metrics.json
```

## 12. 当前预测流程

预测脚本 `predict_descision.py` 的主要流程：

```text
读取单学生 JSON
-> 校验必需特征
-> 转换数值类型
-> 生成 ActivityCount
-> 生成 StudyAbsenceRatio
-> 加载 grade_class_decision_tree.joblib
-> Pipeline 自动执行预处理
-> 输出 predicted_grade_class
-> 输出 predicted_grade_label
-> 输出 A/B/C/D/F 概率
-> 输出全局 top important features
```

## 13. 后续计划

当前阶段暂不开发以下内容：

- FastAPI 模型服务
- Spring Boot 调用 Python
- `model_version` 落库
- `prediction_result` 落库
- `warning_record` 自动生成
- 模型版本激活与回滚
- 在线预测接口

后续建议顺序：

1. 稳定当前训练脚本和预测函数。
2. 为 Python ML 模块补充单元测试。
3. 增加 FastAPI 服务封装。
4. 由 Spring Boot 调用 FastAPI。
5. 将训练元数据写入 `model_version`。
6. 将预测结果写入 `prediction_result`。
7. 基于预测概率和规则生成 `warning_record`。
