package com.ahogeking.studentanalytics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ml")
public class MlProperties {
    private String pythonPath = "python/venv/bin/python";
    private String projectRoot = ".";
    private String scriptDir = "python/ml";
    private String trainScriptName = "train_decision.py";
    private String predictScriptName = "predict_descision.py";
    private String datasetPath = "dataset/Student_performance_data_.csv";
    private String artifactDir = "python/ml/artifacts";
    private String modelRootDir = "runtime/ml/models";
    private String predictionTempDir = "runtime/ml/predictions";
    private Long trainTimeoutSeconds = 600L;
    private Long predictTimeoutSeconds = 60L;
}
