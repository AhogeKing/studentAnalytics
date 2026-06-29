package com.ahogeking.studentanalytics.service.impl;

import com.ahogeking.studentanalytics.config.MlProperties;
import com.ahogeking.studentanalytics.dto.ModelTrainRequest;
import com.ahogeking.studentanalytics.dto.ml.MlPredictionInput;
import com.ahogeking.studentanalytics.dto.ml.MlPredictionResult;
import com.ahogeking.studentanalytics.dto.ml.MlTrainResult;
import com.ahogeking.studentanalytics.exception.BusinessException;
import com.ahogeking.studentanalytics.service.MlModelClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProcessMlModelClient implements MlModelClient {
    private static final String MODEL_FILE_NAME = "grade_class_decision_tree.joblib";
    private static final String METRICS_FILE_NAME = "metrics.json";

    private final MlProperties mlProperties;
    private final ObjectMapper objectMapper;

    @Override
    public MlTrainResult trainDecisionTree(
            Path datasetCsvPath,
            Path versionOutputDir,
            ModelTrainRequest request) {
        Path projectRoot = Paths.get(mlProperties.getProjectRoot())
                .toAbsolutePath()
                .normalize();
        Path trainScript = projectRoot.resolve(mlProperties.getScriptDir())
                .resolve(mlProperties.getTrainScriptName())
                .normalize();
        if (!Files.exists(trainScript)) {
            throw new BusinessException("训练脚本不存在：" + trainScript);
        }

        try {
            Files.createDirectories(versionOutputDir);
        } catch (IOException e) {
            throw new BusinessException("创建模型版本目录失败");
        }

        List<String> command = buildTrainCommand(trainScript, datasetCsvPath, versionOutputDir, request);
        Path trainLogPath = versionOutputDir.resolve("train.log");
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(projectRoot.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(trainLogPath.toFile());

        int exitCode;
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(mlProperties.getTrainTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("模型训练超时，超过 " + mlProperties.getTrainTimeoutSeconds() + " 秒");
            }
            exitCode = process.exitValue();
        } catch (IOException e) {
            throw new BusinessException("启动 Python 训练进程失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("模型训练进程被中断");
        }

        String output = readTrainLog(trainLogPath);
        if (exitCode != 0) {
            throw new BusinessException("模型训练失败，Python exitCode=" + exitCode + "，输出：" + truncate(output, 2000));
        }

        Path modelPath = versionOutputDir.resolve(MODEL_FILE_NAME);
        Path metricsPath = versionOutputDir.resolve(METRICS_FILE_NAME);
        if (!Files.exists(modelPath)) {
            throw new BusinessException("模型训练完成，但未找到模型文件：" + modelPath);
        }
        if (!Files.exists(metricsPath)) {
            throw new BusinessException("模型训练完成，但未找到指标文件：" + metricsPath);
        }

        try {
            JsonNode metrics = objectMapper.readTree(metricsPath.toFile());
            MlTrainResult result = new MlTrainResult();
            result.setModelFilePath(modelPath);
            result.setMetricsFilePath(metricsPath);
            result.setMetrics(metrics);
            return result;
        } catch (IOException e) {
            throw new BusinessException("读取模型训练指标失败");
        }
    }

    @Override
    public MlPredictionResult predictGradeClass(Path modelPath, MlPredictionInput input) {
        if (modelPath == null || !Files.exists(modelPath)) {
            throw new BusinessException("模型文件不存在：" + modelPath);
        }
        Path projectRoot = Paths.get(mlProperties.getProjectRoot())
                .toAbsolutePath()
                .normalize();
        Path predictScript = projectRoot.resolve(mlProperties.getScriptDir())
                .resolve(mlProperties.getPredictScriptName())
                .normalize();
        if (!Files.exists(predictScript)) {
            throw new BusinessException("预测脚本不存在：" + predictScript);
        }

        Path tempDir = projectRoot.resolve(mlProperties.getPredictionTempDir()).normalize();
        String fileKey = UUID.randomUUID().toString();
        Path inputPath = tempDir.resolve("predict-input-" + fileKey + ".json");
        Path outputPath = tempDir.resolve("predict-output-" + fileKey + ".json");
        try {
            Files.createDirectories(tempDir);
            objectMapper.writeValue(inputPath.toFile(), input);
        } catch (IOException e) {
            throw new BusinessException("写入预测输入文件失败");
        }

        List<String> command = new ArrayList<>();
        command.add(mlProperties.getPythonPath());
        command.add(predictScript.toString());
        command.add("--model-path");
        command.add(modelPath.toAbsolutePath().normalize().toString());
        command.add("--input");
        command.add(inputPath.toAbsolutePath().normalize().toString());
        command.add("--output");
        command.add(outputPath.toAbsolutePath().normalize().toString());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(projectRoot.toFile());
        builder.redirectErrorStream(true);

        String output;
        int exitCode;
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(mlProperties.getPredictTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new BusinessException("模型预测超时，超过 " + mlProperties.getPredictTimeoutSeconds() + " 秒");
            }
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            exitCode = process.exitValue();
        } catch (IOException e) {
            throw new BusinessException("启动 Python 预测进程失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("模型预测进程被中断");
        }

        if (exitCode != 0) {
            throw new BusinessException("模型预测失败，Python exitCode=" + exitCode + "，输出：" + truncate(output, 2000));
        }

        try {
            if (Files.exists(outputPath)) {
                return objectMapper.readValue(outputPath.toFile(), MlPredictionResult.class);
            }
            if (output != null && !output.isBlank()) {
                return objectMapper.readValue(output, MlPredictionResult.class);
            }
            throw new BusinessException("预测脚本未生成预测结果");
        } catch (IOException e) {
            throw new BusinessException("读取预测结果 JSON 失败");
        }
    }

    private List<String> buildTrainCommand(
            Path trainScript,
            Path datasetCsvPath,
            Path versionOutputDir,
            ModelTrainRequest request) {
        List<String> command = new ArrayList<>();
        command.add(mlProperties.getPythonPath());
        command.add(trainScript.toString());
        command.add("--data-path");
        command.add(datasetCsvPath.toString());
        command.add("--artifact-dir");
        command.add(versionOutputDir.toString());

        String mode = normalizeMode(request == null ? null : request.getMode());
        if ("quick".equals(mode)) {
            command.add("--quick");
        } else if ("exhaustive".equals(mode)) {
            command.add("--exhaustive");
        }
        return command;
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "default";
        }
        return switch (mode.trim().toLowerCase()) {
            case "quick" -> "quick";
            case "default" -> "default";
            case "exhaustive" -> "exhaustive";
            default -> throw new BusinessException("训练模式只能是 quick、default 或 exhaustive");
        };
    }

    private String readTrainLog(Path trainLogPath) {
        try {
            if (!Files.exists(trainLogPath)) {
                return "";
            }
            return Files.readString(trainLogPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
