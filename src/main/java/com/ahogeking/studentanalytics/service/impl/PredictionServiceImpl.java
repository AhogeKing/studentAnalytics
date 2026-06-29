package com.ahogeking.studentanalytics.service.impl;

import com.ahogeking.studentanalytics.dto.PredictionRequest;
import com.ahogeking.studentanalytics.dto.ml.MlImportantFeatureItem;
import com.ahogeking.studentanalytics.dto.ml.MlPredictionInput;
import com.ahogeking.studentanalytics.dto.ml.MlPredictionResult;
import com.ahogeking.studentanalytics.dto.row.PredictionInputRow;
import com.ahogeking.studentanalytics.dto.row.PredictionResultRow;
import com.ahogeking.studentanalytics.entity.ModelVersion;
import com.ahogeking.studentanalytics.entity.PredictionResult;
import com.ahogeking.studentanalytics.exception.BusinessException;
import com.ahogeking.studentanalytics.mapper.ModelMapper;
import com.ahogeking.studentanalytics.mapper.PredictionMapper;
import com.ahogeking.studentanalytics.service.MlModelClient;
import com.ahogeking.studentanalytics.service.PredictionService;
import com.ahogeking.studentanalytics.service.WarningService;
import com.ahogeking.studentanalytics.vo.ClassInfoVO;
import com.ahogeking.studentanalytics.vo.GradeClassEnum;
import com.ahogeking.studentanalytics.vo.ImportantFactorVO;
import com.ahogeking.studentanalytics.vo.PredictionEligibilityVO;
import com.ahogeking.studentanalytics.vo.PredictionProbabilityVO;
import com.ahogeking.studentanalytics.vo.PredictionResultVO;
import com.ahogeking.studentanalytics.vo.StudentPredictionVO;
import com.ahogeking.studentanalytics.vo.WarningRecordVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PredictionServiceImpl implements PredictionService {
    private static final List<String> GRADE_LABELS = List.of("A", "B", "C", "D", "F");
    private static final String SPLIT_TRAIN = "TRAIN";
    private static final String SPLIT_TEST = "TEST";
    private static final String SPLIT_NEW = "NEW";
    private static final String SPLIT_UNKNOWN = "UNKNOWN";
    private static final Map<String, Integer> LABEL_TO_CLASS = Map.of(
            "A", 0,
            "B", 1,
            "C", 2,
            "D", 3,
            "F", 4
    );
    private static final Map<Integer, String> CLASS_TO_LABEL = Map.of(
            0, "A",
            1, "B",
            2, "C",
            3, "D",
            4, "F"
    );
    private static final Map<String, String> FEATURE_LABELS = Map.ofEntries(
            Map.entry("Age", "年龄"),
            Map.entry("Gender", "性别"),
            Map.entry("Ethnicity", "族群"),
            Map.entry("ParentalEducation", "家长受教育程度"),
            Map.entry("StudyTimeWeekly", "每周学习时长"),
            Map.entry("Absences", "缺勤次数"),
            Map.entry("Tutoring", "是否参加辅导"),
            Map.entry("ParentalSupport", "家长支持程度"),
            Map.entry("Extracurricular", "是否参加课外活动"),
            Map.entry("Sports", "是否参加体育活动"),
            Map.entry("Music", "是否参加音乐活动"),
            Map.entry("Volunteering", "是否参加志愿活动"),
            Map.entry("ActivityCount", "活动参与数量"),
            Map.entry("StudyAbsenceRatio", "学习时长与缺勤比")
    );

    private final PredictionMapper predictionMapper;
    private final ModelMapper modelMapper;
    private final MlModelClient mlModelClient;
    private final WarningService warningService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public StudentPredictionVO predictStudent(Integer studentNo, PredictionRequest request) {
        if (studentNo == null || studentNo <= 0) {
            throw new BusinessException("学生编号不合法");
        }

        PredictionInputRow inputRow = predictionMapper.selectPredictionInputByStudentNo(studentNo);
        if (inputRow == null) {
            throw new BusinessException("学生不存在或已删除");
        }
        if (inputRow.getPerformanceId() == null) {
            throw new BusinessException("该学生暂无学业表现记录，无法进行预测");
        }

        ModelVersion modelVersion = selectModelVersion(request);
        if (modelVersion.getModelPath() == null || modelVersion.getModelPath().isBlank()) {
            throw new BusinessException("模型文件路径为空");
        }
        PredictionEligibilityVO eligibility = buildPredictionEligibility(inputRow, modelVersion);
        if (!Boolean.TRUE.equals(eligibility.getCanPredict())) {
            throw new BusinessException(eligibility.getReason());
        }

        MlPredictionInput mlInput = toMlPredictionInput(inputRow);
        MlPredictionResult mlResult = mlModelClient.predictGradeClass(Paths.get(modelVersion.getModelPath()), mlInput);
        List<PredictionProbabilityVO> probabilities = normalizeProbabilities(mlResult);
        List<ImportantFactorVO> importantFactors = enrichImportantFactors(inputRow, mlResult.getImportantFeatures());

        PredictionResult predictionResult = buildPredictionResult(
                inputRow,
                modelVersion,
                mlInput,
                mlResult,
                probabilities,
                importantFactors
        );
        predictionMapper.insertPredictionResult(predictionResult);

        PredictionResultRow savedRow = predictionMapper.selectPredictionResultById(predictionResult.getId());
        StudentPredictionVO vo = toStudentPredictionVO(savedRow);
        if (shouldGenerateWarning(request)) {
            WarningRecordVO warning = warningService.generateWarningFromPrediction(savedRow.getId());
            vo.setWarning(warning);
        }
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public PredictionEligibilityVO selectPredictionEligibility(Integer studentNo, Integer modelVersionId) {
        if (studentNo == null || studentNo <= 0) {
            throw new BusinessException("学生编号不合法");
        }
        PredictionInputRow inputRow = predictionMapper.selectPredictionInputByStudentNo(studentNo);
        if (inputRow == null) {
            throw new BusinessException("学生不存在或已删除");
        }
        return buildPredictionEligibility(inputRow, selectModelVersion(modelVersionId));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentPredictionVO selectPredictionDetail(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException("预测结果ID不合法");
        }
        PredictionResultRow row = predictionMapper.selectPredictionResultById(id);
        if (row == null) {
            throw new BusinessException("预测结果不存在");
        }
        StudentPredictionVO vo = toStudentPredictionVO(row);
        vo.setWarning(warningService.selectWarningByPredictionResultId(row.getId()));
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public StudentPredictionVO selectLatestPredictionByStudentNo(Integer studentNo) {
        if (studentNo == null || studentNo <= 0) {
            throw new BusinessException("学生编号不合法");
        }
        PredictionResultRow row = predictionMapper.selectLatestPredictionByStudentNo(studentNo);
        if (row == null) {
            throw new BusinessException("该学生暂无预测记录");
        }
        StudentPredictionVO vo = toStudentPredictionVO(row);
        vo.setWarning(warningService.selectWarningByPredictionResultId(row.getId()));
        return vo;
    }

    private boolean shouldGenerateWarning(PredictionRequest request) {
        return request == null
                || request.getGenerateWarning() == null
                || Boolean.TRUE.equals(request.getGenerateWarning());
    }

    private ModelVersion selectModelVersion(PredictionRequest request) {
        Integer modelVersionId = request == null ? null : request.getModelVersionId();
        return selectModelVersion(modelVersionId);
    }

    private ModelVersion selectModelVersion(Integer modelVersionId) {
        if (modelVersionId != null) {
            ModelVersion modelVersion = modelMapper.selectModelVersionById(modelVersionId);
            if (modelVersion == null) {
                throw new BusinessException("指定模型版本不存在");
            }
            return modelVersion;
        }
        ModelVersion active = modelMapper.selectActiveModelVersion();
        if (active == null) {
            throw new BusinessException("当前没有启用的模型，请管理员先训练模型");
        }
        return active;
    }

    private PredictionEligibilityVO buildPredictionEligibility(PredictionInputRow inputRow, ModelVersion modelVersion) {
        String datasetSplit = resolveDatasetSplit(modelVersion, inputRow.getStudentNo());
        PredictionEligibilityVO vo = new PredictionEligibilityVO();
        vo.setStudentNo(inputRow.getStudentNo());
        vo.setModelVersionId(modelVersion.getId());
        vo.setModelVersionNo(modelVersion.getVersionNo());
        vo.setDatasetSplit(datasetSplit);
        vo.setDatasetSplitLabel(datasetSplitLabel(datasetSplit));

        if (inputRow.getPerformanceId() == null) {
            vo.setCanPredict(false);
            vo.setReason("该学生暂无学业表现记录，无法进行预测");
            return vo;
        }
        if (SPLIT_TRAIN.equals(datasetSplit)) {
            vo.setCanPredict(false);
            vo.setReason("该学生属于当前模型训练集，预测结果只会反映模型已学习过的数据，不适合作为演示或评估样本");
            return vo;
        }
        vo.setCanPredict(true);
        vo.setReason(datasetSplitReason(datasetSplit));
        return vo;
    }

    private String resolveDatasetSplit(ModelVersion modelVersion, Integer studentNo) {
        if (studentNo == null) {
            return SPLIT_UNKNOWN;
        }
        JsonNode metrics = readMetricsJson(modelVersion.getModelPath());
        JsonNode trainStudentIds = metrics.path("train_student_ids");
        JsonNode testStudentIds = metrics.path("test_student_ids");
        if (!trainStudentIds.isArray() || !testStudentIds.isArray()) {
            return SPLIT_UNKNOWN;
        }
        if (containsStudentId(trainStudentIds, studentNo)) {
            return SPLIT_TRAIN;
        }
        if (containsStudentId(testStudentIds, studentNo)) {
            return SPLIT_TEST;
        }
        return SPLIT_NEW;
    }

    private boolean containsStudentId(JsonNode ids, Integer studentNo) {
        for (JsonNode id : ids) {
            if (id.asInt() == studentNo) {
                return true;
            }
        }
        return false;
    }

    private String datasetSplitLabel(String datasetSplit) {
        return switch (datasetSplit) {
            case SPLIT_TRAIN -> "训练集";
            case SPLIT_TEST -> "测试集";
            case SPLIT_NEW -> "新增样本";
            default -> "未知";
        };
    }

    private String datasetSplitReason(String datasetSplit) {
        return switch (datasetSplit) {
            case SPLIT_TEST -> "该学生属于当前模型测试集，适合用于演示模型预测效果";
            case SPLIT_NEW -> "该学生未参与当前模型训练或测试，可作为真实新增样本进行预测";
            default -> "该模型版本未记录训练/测试集学生编号，无法判断样本来源";
        };
    }

    private MlPredictionInput toMlPredictionInput(PredictionInputRow row) {
        MlPredictionInput input = new MlPredictionInput();
        input.setAge(row.getAge());
        input.setGender(row.getGender());
        input.setEthnicity(row.getEthnicity());
        input.setParentalEducation(row.getParentalEducation());
        input.setStudyTimeWeekly(row.getStudyTimeWeekly());
        input.setAbsences(row.getAbsences());
        input.setTutoring(row.getTutoring());
        input.setParentalSupport(row.getParentalSupport());
        input.setExtracurricular(row.getExtracurricular());
        input.setSports(row.getSports());
        input.setMusic(row.getMusic());
        input.setVolunteering(row.getVolunteering());
        return input;
    }

    private PredictionResult buildPredictionResult(
            PredictionInputRow inputRow,
            ModelVersion modelVersion,
            MlPredictionInput mlInput,
            MlPredictionResult mlResult,
            List<PredictionProbabilityVO> probabilities,
            List<ImportantFactorVO> importantFactors) {
        Integer predictedClass = normalizePredictedClass(mlResult);
        String predictedLabel = mlResult.getPredictedGradeLabel();
        if (predictedLabel == null || predictedLabel.isBlank()) {
            predictedLabel = CLASS_TO_LABEL.get(predictedClass);
        }

        PredictionResult predictionResult = new PredictionResult();
        predictionResult.setStudentId(inputRow.getStudentId());
        predictionResult.setPerformanceId(inputRow.getPerformanceId());
        predictionResult.setModelVersionId(modelVersion.getId());
        predictionResult.setPredictedGradeClass(predictedClass);
        predictionResult.setPredictedGradeLabel(predictedLabel);
        predictionResult.setProbabilityJson(toJson(probabilities));
        predictionResult.setImportantFactorsJson(toJson(importantFactors));
        predictionResult.setPredictInputJson(buildPredictInputSnapshot(inputRow, modelVersion, mlInput));
        return predictionResult;
    }

    private Integer normalizePredictedClass(MlPredictionResult result) {
        if (result.getPredictedGradeClass() != null) {
            Integer value = result.getPredictedGradeClass();
            if (value < 0 || value > 4) {
                throw new BusinessException("模型返回的成绩等级不合法：" + value);
            }
            return value;
        }

        String label = result.getPredictedGradeLabel();
        if (label != null) {
            Integer mapped = LABEL_TO_CLASS.get(label.trim().toUpperCase());
            if (mapped != null) {
                return mapped;
            }
        }
        throw new BusinessException("模型未返回有效预测等级");
    }

    private List<PredictionProbabilityVO> normalizeProbabilities(MlPredictionResult result) {
        Map<String, BigDecimal> probabilityMap = result.getProbabilities() == null
                ? Map.of()
                : result.getProbabilities();
        List<PredictionProbabilityVO> list = new ArrayList<>();
        for (String label : GRADE_LABELS) {
            Integer gradeClass = LABEL_TO_CLASS.get(label);
            BigDecimal probability = probabilityMap
                    .getOrDefault(label, BigDecimal.ZERO)
                    .setScale(4, RoundingMode.HALF_UP);
            list.add(new PredictionProbabilityVO(
                    GradeClassEnum.toOption(gradeClass),
                    label,
                    probability
            ));
        }
        return list;
    }

    private List<ImportantFactorVO> enrichImportantFactors(
            PredictionInputRow row,
            List<MlImportantFeatureItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        Map<String, Object> featureValues = buildFeatureValueMap(row);
        return items.stream()
                .filter(Objects::nonNull)
                .map(item -> {
                    String feature = normalizeFeatureName(item.getFeature());
                    BigDecimal importance = item.getImportance() == null
                            ? BigDecimal.ZERO
                            : item.getImportance().setScale(4, RoundingMode.HALF_UP);
                    return new ImportantFactorVO(
                            feature,
                            FEATURE_LABELS.getOrDefault(feature, feature),
                            featureValues.get(feature),
                            importance
                    );
                })
                .toList();
    }

    private Map<String, Object> buildFeatureValueMap(PredictionInputRow row) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("Age", row.getAge());
        values.put("Gender", row.getGender());
        values.put("Ethnicity", row.getEthnicity());
        values.put("ParentalEducation", row.getParentalEducation());
        values.put("StudyTimeWeekly", row.getStudyTimeWeekly());
        values.put("Absences", row.getAbsences());
        values.put("Tutoring", row.getTutoring());
        values.put("ParentalSupport", row.getParentalSupport());
        values.put("Extracurricular", row.getExtracurricular());
        values.put("Sports", row.getSports());
        values.put("Music", row.getMusic());
        values.put("Volunteering", row.getVolunteering());
        values.put("ActivityCount", calculateActivityCount(row));
        values.put("StudyAbsenceRatio", calculateStudyAbsenceRatio(row));
        return values;
    }

    private Integer calculateActivityCount(PredictionInputRow row) {
        return safeInt(row.getExtracurricular())
                + safeInt(row.getSports())
                + safeInt(row.getMusic())
                + safeInt(row.getVolunteering());
    }

    private BigDecimal calculateStudyAbsenceRatio(PredictionInputRow row) {
        BigDecimal studyTime = row.getStudyTimeWeekly() == null ? BigDecimal.ZERO : row.getStudyTimeWeekly();
        BigDecimal denominator = BigDecimal.valueOf(safeInt(row.getAbsences()) + 1L);
        return studyTime.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeFeatureName(String feature) {
        if (feature == null || feature.isBlank()) {
            return "";
        }
        String normalized = feature.trim();
        int prefixIndex = normalized.indexOf("__");
        if (prefixIndex >= 0 && prefixIndex + 2 < normalized.length()) {
            normalized = normalized.substring(prefixIndex + 2);
        }
        if (normalized.startsWith("Ethnicity_")) {
            return "Ethnicity";
        }
        if (normalized.startsWith("Gender_")) {
            return "Gender";
        }
        if (normalized.startsWith("Tutoring_")) {
            return "Tutoring";
        }
        if (normalized.startsWith("Extracurricular_")) {
            return "Extracurricular";
        }
        if (normalized.startsWith("Sports_")) {
            return "Sports";
        }
        if (normalized.startsWith("Music_")) {
            return "Music";
        }
        if (normalized.startsWith("Volunteering_")) {
            return "Volunteering";
        }
        return normalized;
    }

    private String buildPredictInputSnapshot(
            PredictionInputRow row,
            ModelVersion modelVersion,
            MlPredictionInput mlInput) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("student_no", row.getStudentNo());
        root.put("model_version_id", modelVersion.getId());
        root.put("model_version_no", modelVersion.getVersionNo());
        String datasetSplit = resolveDatasetSplit(modelVersion, row.getStudentNo());
        root.put("dataset_split", datasetSplit);
        root.put("dataset_split_label", datasetSplitLabel(datasetSplit));

        JsonNode featuresNode = objectMapper.valueToTree(mlInput);
        ObjectNode features = featuresNode instanceof ObjectNode
                ? (ObjectNode) featuresNode
                : objectMapper.createObjectNode();
        features.put("ActivityCount", calculateActivityCount(row));
        features.put("StudyAbsenceRatio", calculateStudyAbsenceRatio(row));
        root.set("features", features);

        if (row.getGpa() != null) {
            root.put("current_gpa", row.getGpa());
        }
        if (row.getGradeClass() != null) {
            root.put("current_grade_class", row.getGradeClass());
            root.put("current_grade_label", CLASS_TO_LABEL.get(row.getGradeClass()));
        }
        return toJson(root);
    }

    private StudentPredictionVO toStudentPredictionVO(PredictionResultRow row) {
        StudentPredictionVO vo = new StudentPredictionVO();
        vo.setStudentNo(row.getStudentNo());
        vo.setName(row.getStudentName());
        vo.setClassInfo(ClassInfoVO.fromRaw(row.getGradeLevel(), row.getClassName()));
        vo.setPrediction(toPredictionResultVO(row));
        return vo;
    }

    private PredictionResultVO toPredictionResultVO(PredictionResultRow row) {
        PredictionResultVO vo = new PredictionResultVO();
        vo.setPredictionResultId(row.getId());
        vo.setModelVersionId(row.getModelVersionId());
        vo.setModelVersionNo(row.getModelVersionNo());
        vo.setPredictedGradeClass(GradeClassEnum.toOption(row.getPredictedGradeClass()));
        vo.setPredictedGradeLabel(row.getPredictedGradeLabel());
        vo.setProbabilities(readJsonList(
                row.getProbabilityJson(),
                new TypeReference<List<PredictionProbabilityVO>>() {
                }
        ));
        vo.setImportantFactors(readJsonList(
                row.getImportantFactorsJson(),
                new TypeReference<List<ImportantFactorVO>>() {
                }
        ));
        JsonNode predictInput = readJsonNode(row.getPredictInputJson());
        vo.setPredictInput(predictInput);
        vo.setDatasetSplit(text(predictInput, "dataset_split", null));
        vo.setDatasetSplitLabel(text(predictInput, "dataset_split_label", null));
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    private JsonNode readJsonNode(String json) {
        if (json == null || json.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private JsonNode readMetricsJson(String modelPath) {
        if (modelPath == null || modelPath.isBlank()) {
            return objectMapper.createObjectNode();
        }
        Path parent = Paths.get(modelPath).getParent();
        if (parent == null) {
            return objectMapper.createObjectNode();
        }
        Path metricsPath = parent.resolve("metrics.json");
        if (!Files.exists(metricsPath)) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(metricsPath.toFile());
        } catch (IOException e) {
            return objectMapper.createObjectNode();
        }
    }

    private String text(JsonNode node, String fieldName, String defaultValue) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return defaultValue;
        }
        return value.asText();
    }

    private <T> List<T> readJsonList(String json, TypeReference<List<T>> typeReference) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            return List.of();
        }
    }
}
