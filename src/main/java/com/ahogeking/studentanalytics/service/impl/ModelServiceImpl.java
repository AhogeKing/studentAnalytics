package com.ahogeking.studentanalytics.service.impl;

import com.ahogeking.studentanalytics.config.MlProperties;
import com.ahogeking.studentanalytics.context.SysUserContext;
import com.ahogeking.studentanalytics.dto.ModelTrainRequest;
import com.ahogeking.studentanalytics.dto.ModelVersionQueryRequest;
import com.ahogeking.studentanalytics.dto.ml.MlTrainResult;
import com.ahogeking.studentanalytics.dto.row.ModelTrainingRow;
import com.ahogeking.studentanalytics.entity.ModelVersion;
import com.ahogeking.studentanalytics.exception.BusinessException;
import com.ahogeking.studentanalytics.mapper.ModelMapper;
import com.ahogeking.studentanalytics.service.MlModelClient;
import com.ahogeking.studentanalytics.service.ModelService;
import com.ahogeking.studentanalytics.vo.ModelTrainResultVO;
import com.ahogeking.studentanalytics.vo.ModelVersionDetailVO;
import com.ahogeking.studentanalytics.vo.ModelVersionVO;
import com.ahogeking.studentanalytics.vo.PageResultVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String MODEL_NAME = "grade_class_decision_tree";
    private static final String ALGORITHM = "DecisionTreeClassifier";
    private static final String DEFAULT_TARGET_COLUMN = "GradeClassClean";
    private static final DateTimeFormatter VERSION_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ReentrantLock trainLock = new ReentrantLock();
    private final ModelMapper modelMapper;
    private final MlModelClient mlModelClient;
    private final MlProperties mlProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ModelTrainResultVO trainDecisionTree(ModelTrainRequest request) {
        ModelTrainRequest safeRequest = normalizeTrainRequest(request);
        if (!trainLock.tryLock()) {
            throw new BusinessException("当前已有模型训练任务正在执行，请稍后再试");
        }
        try {
            String versionNo = generateVersionNo();
            Path projectRoot = Paths.get(mlProperties.getProjectRoot()).toAbsolutePath().normalize();
            Path datasetPath = projectRoot.resolve(mlProperties.getDatasetPath()).normalize();
            Path versionOutputDir = projectRoot.resolve(mlProperties.getModelRootDir()).resolve(versionNo).normalize();

            exportTrainingDataset(datasetPath);
            MlTrainResult trainResult = mlModelClient.trainDecisionTree(datasetPath, versionOutputDir, safeRequest);
            ModelVersion modelVersion = buildModelVersion(versionNo, trainResult);

            if (Boolean.TRUE.equals(safeRequest.getActivate())) {
                modelMapper.deactivateAllModels();
                modelVersion.setIsActive(1);
            } else {
                modelVersion.setIsActive(0);
            }
            modelMapper.insertModelVersion(modelVersion);
            return toTrainResultVO(modelVersion, trainResult.getMetrics());
        } finally {
            trainLock.unlock();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ModelVersionVO selectActiveModel() {
        ModelVersion active = modelMapper.selectActiveModelVersion();
        if (active == null) {
            throw new BusinessException("当前没有启用的模型，请管理员先训练模型");
        }
        return toModelVersionVO(active);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResultVO<ModelVersionVO> selectModelVersionPage(ModelVersionQueryRequest query) {
        ModelVersionQueryRequest safeQuery = normalizeVersionQuery(query);
        int offset = (safeQuery.getPageNum() - 1) * safeQuery.getPageSize();
        Long total = modelMapper.countModelVersions(safeQuery);
        List<ModelVersionVO> records = modelMapper
                .selectModelVersionPage(safeQuery, offset, safeQuery.getPageSize())
                .stream()
                .map(this::toModelVersionVO)
                .toList();
        return new PageResultVO<>(total, records);
    }

    @Override
    @Transactional(readOnly = true)
    public ModelVersionDetailVO selectModelVersionDetail(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException("模型版本ID不合法");
        }
        ModelVersion modelVersion = modelMapper.selectModelVersionById(id);
        if (modelVersion == null) {
            throw new BusinessException("模型版本不存在");
        }
        return toModelVersionDetailVO(modelVersion);
    }

    private void exportTrainingDataset(Path datasetPath) {
        List<ModelTrainingRow> rows = modelMapper.selectModelTrainingRows();
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException("没有可用于模型训练的数据");
        }
        try {
            Files.createDirectories(datasetPath.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(
                    datasetPath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                writer.write("StudentID,Age,Gender,Ethnicity,ParentalEducation,"
                        + "StudyTimeWeekly,Absences,Tutoring,ParentalSupport,"
                        + "Extracurricular,Sports,Music,Volunteering,GPA,GradeClass");
                writer.newLine();
                for (ModelTrainingRow row : rows) {
                    writer.write(toCsvLine(row));
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new BusinessException("导出模型训练数据失败");
        }
    }

    private String toCsvLine(ModelTrainingRow row) {
        return String.join(
                ",",
                value(row.getStudentId()),
                value(row.getAge()),
                value(row.getGender()),
                value(row.getEthnicity()),
                value(row.getParentalEducation()),
                value(row.getStudyTimeWeekly()),
                value(row.getAbsences()),
                value(row.getTutoring()),
                value(row.getParentalSupport()),
                value(row.getExtracurricular()),
                value(row.getSports()),
                value(row.getMusic()),
                value(row.getVolunteering()),
                value(row.getGpa()),
                value(row.getGradeClass())
        );
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private ModelVersion buildModelVersion(String versionNo, MlTrainResult trainResult) {
        JsonNode metrics = trainResult.getMetrics();
        JsonNode bestParameters = metrics.path("best_parameters");
        ModelVersion modelVersion = new ModelVersion();
        modelVersion.setModelName(text(metrics, "model_name", MODEL_NAME));
        modelVersion.setVersionNo(versionNo);
        modelVersion.setAlgorithm(text(metrics, "algorithm", text(metrics, "model_type", ALGORITHM)));
        modelVersion.setFeatureColumns(jsonString(metrics.path("feature_columns")));
        modelVersion.setTargetColumn(text(metrics, "target_column", DEFAULT_TARGET_COLUMN));
        modelVersion.setCriterion(text(bestParameters, "model__criterion", null));
        modelVersion.setMaxDepth(nullableInt(bestParameters, "model__max_depth"));
        modelVersion.setMinSamplesLeaf(nullableInt(bestParameters, "model__min_samples_leaf"));
        modelVersion.setAccuracy(decimal(metrics, "accuracy"));
        modelVersion.setPrecisionMacro(decimal(metrics, "precision_macro"));
        modelVersion.setRecallMacro(decimal(metrics, "recall_macro"));
        modelVersion.setF1Macro(decimal(metrics, "f1_macro"));
        modelVersion.setConfusionMatrixJson(jsonString(metrics.path("confusion_matrix")));
        modelVersion.setModelPath(trainResult.getModelFilePath().toString());
        modelVersion.setEncoderPath(null);
        modelVersion.setTrainedAt(LocalDateTime.now());
        modelVersion.setCreatedBy(SysUserContext.getUserId());
        return modelVersion;
    }

    private ModelTrainResultVO toTrainResultVO(ModelVersion modelVersion, JsonNode metrics) {
        ModelTrainResultVO vo = new ModelTrainResultVO();
        vo.setModelVersionId(modelVersion.getId());
        vo.setVersionNo(modelVersion.getVersionNo());
        vo.setModelName(modelVersion.getModelName());
        vo.setAlgorithm(modelVersion.getAlgorithm());
        vo.setTargetColumn(modelVersion.getTargetColumn());
        vo.setFeatureColumns(metrics.path("feature_columns"));
        vo.setSearchMode(text(metrics, "search_mode", null));
        vo.setSearchCandidates(integer(metrics, "search_candidates"));
        vo.setTrainRows(integer(metrics, "train_rows"));
        vo.setTestRows(integer(metrics, "test_rows"));
        vo.setBestParameters(metrics.path("best_parameters"));
        vo.setAccuracy(modelVersion.getAccuracy());
        vo.setPrecisionMacro(modelVersion.getPrecisionMacro());
        vo.setRecallMacro(modelVersion.getRecallMacro());
        vo.setF1Macro(modelVersion.getF1Macro());
        vo.setAucOvrMacro(decimal(metrics, "auc_ovr_macro"));
        vo.setConfusionMatrix(metrics.path("confusion_matrix"));
        vo.setModelPath(modelVersion.getModelPath());
        vo.setActive(modelVersion.getIsActive() != null && modelVersion.getIsActive() == 1);
        vo.setTrainedAt(modelVersion.getTrainedAt());
        return vo;
    }

    private ModelVersionVO toModelVersionVO(ModelVersion modelVersion) {
        ModelVersionVO vo = new ModelVersionVO();
        vo.setId(modelVersion.getId());
        vo.setModelName(modelVersion.getModelName());
        vo.setVersionNo(modelVersion.getVersionNo());
        vo.setAlgorithm(modelVersion.getAlgorithm());
        vo.setAccuracy(modelVersion.getAccuracy());
        vo.setPrecisionMacro(modelVersion.getPrecisionMacro());
        vo.setRecallMacro(modelVersion.getRecallMacro());
        vo.setF1Macro(modelVersion.getF1Macro());
        vo.setActive(modelVersion.getIsActive() != null && modelVersion.getIsActive() == 1);
        vo.setTrainedAt(modelVersion.getTrainedAt());
        vo.setCreatedAt(modelVersion.getCreatedAt());
        return vo;
    }

    private ModelVersionDetailVO toModelVersionDetailVO(ModelVersion modelVersion) {
        ModelVersionDetailVO vo = new ModelVersionDetailVO();
        vo.setId(modelVersion.getId());
        vo.setModelName(modelVersion.getModelName());
        vo.setVersionNo(modelVersion.getVersionNo());
        vo.setAlgorithm(modelVersion.getAlgorithm());
        vo.setFeatureColumns(parseJson(modelVersion.getFeatureColumns()));
        vo.setTargetColumn(modelVersion.getTargetColumn());
        vo.setCriterion(modelVersion.getCriterion());
        vo.setMaxDepth(modelVersion.getMaxDepth());
        vo.setMinSamplesLeaf(modelVersion.getMinSamplesLeaf());
        vo.setAccuracy(modelVersion.getAccuracy());
        vo.setPrecisionMacro(modelVersion.getPrecisionMacro());
        vo.setRecallMacro(modelVersion.getRecallMacro());
        vo.setF1Macro(modelVersion.getF1Macro());
        vo.setConfusionMatrix(parseJson(modelVersion.getConfusionMatrixJson()));
        vo.setMetrics(readMetricsJson(modelVersion.getModelPath()));
        vo.setModelPath(modelVersion.getModelPath());
        vo.setActive(modelVersion.getIsActive() != null && modelVersion.getIsActive() == 1);
        vo.setTrainedAt(modelVersion.getTrainedAt());
        vo.setCreatedAt(modelVersion.getCreatedAt());
        return vo;
    }

    private JsonNode readMetricsJson(String modelPath) {
        if (modelPath == null || modelPath.isBlank()) {
            return objectMapper.createObjectNode();
        }
        Path metricsPath = Paths.get(modelPath).getParent().resolve("metrics.json");
        if (!Files.exists(metricsPath)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(metricsPath.toFile());
        } catch (IOException e) {
            return objectMapper.createObjectNode();
        }
    }

    private ModelTrainRequest normalizeTrainRequest(ModelTrainRequest request) {
        ModelTrainRequest safeRequest = request == null ? new ModelTrainRequest() : request;
        if (safeRequest.getMode() == null || safeRequest.getMode().isBlank()) {
            safeRequest.setMode("default");
        }
        String mode = safeRequest.getMode().trim().toLowerCase();
        if (!List.of("quick", "default", "exhaustive").contains(mode)) {
            throw new BusinessException("训练模式只能是 quick、default 或 exhaustive");
        }
        safeRequest.setMode(mode);
        if (safeRequest.getActivate() == null) {
            safeRequest.setActivate(true);
        }
        return safeRequest;
    }

    private ModelVersionQueryRequest normalizeVersionQuery(ModelVersionQueryRequest query) {
        ModelVersionQueryRequest safeQuery = query == null ? new ModelVersionQueryRequest() : query;
        if (safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1) {
            safeQuery.setPageNum(DEFAULT_PAGE_NUM);
        }
        if (safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1) {
            safeQuery.setPageSize(DEFAULT_PAGE_SIZE);
        }
        if (safeQuery.getPageSize() > MAX_PAGE_SIZE) {
            safeQuery.setPageSize(MAX_PAGE_SIZE);
        }
        return safeQuery;
    }

    private String generateVersionNo() {
        return "dt_cls_" + LocalDateTime.now().format(VERSION_TIME_FORMATTER);
    }

    private String text(JsonNode node, String fieldName, String defaultValue) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return defaultValue;
        }
        return value.asText();
    }

    private Integer integer(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asInt();
    }

    private Integer nullableInt(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asInt();
    }

    private BigDecimal decimal(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return BigDecimal.valueOf(value.asDouble());
    }

    private String jsonString(JsonNode node) {
        try {
            if (node == null || node.isMissingNode() || node.isNull()) {
                return "{}";
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{}";
        }
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }
}
