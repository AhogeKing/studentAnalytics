package com.ahogeking.studentanalytics.service;

import com.ahogeking.studentanalytics.dto.ModelTrainRequest;
import com.ahogeking.studentanalytics.dto.ml.MlPredictionInput;
import com.ahogeking.studentanalytics.dto.ml.MlPredictionResult;
import com.ahogeking.studentanalytics.dto.ml.MlTrainResult;

import java.nio.file.Path;

public interface MlModelClient {
    MlTrainResult trainDecisionTree(
            Path datasetCsvPath,
            Path versionOutputDir,
            ModelTrainRequest request
    );

    MlPredictionResult predictGradeClass(
            Path modelPath,
            MlPredictionInput input
    );
}
