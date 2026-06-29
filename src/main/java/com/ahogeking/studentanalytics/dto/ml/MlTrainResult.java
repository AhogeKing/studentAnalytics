package com.ahogeking.studentanalytics.dto.ml;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.nio.file.Path;

@Data
public class MlTrainResult {
    private Path modelFilePath;
    private Path metricsFilePath;
    private JsonNode metrics;
}
