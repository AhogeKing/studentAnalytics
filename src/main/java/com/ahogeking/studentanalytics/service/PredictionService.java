package com.ahogeking.studentanalytics.service;

import com.ahogeking.studentanalytics.dto.PredictionRequest;
import com.ahogeking.studentanalytics.vo.PredictionEligibilityVO;
import com.ahogeking.studentanalytics.vo.StudentPredictionVO;

public interface PredictionService {
    StudentPredictionVO predictStudent(Integer studentNo, PredictionRequest request);

    PredictionEligibilityVO selectPredictionEligibility(Integer studentNo, Integer modelVersionId);

    StudentPredictionVO selectPredictionDetail(Integer id);

    StudentPredictionVO selectLatestPredictionByStudentNo(Integer studentNo);
}
