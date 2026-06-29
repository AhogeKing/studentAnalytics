package com.ahogeking.studentanalytics.service;

import com.ahogeking.studentanalytics.dto.ModelTrainRequest;
import com.ahogeking.studentanalytics.dto.ModelVersionQueryRequest;
import com.ahogeking.studentanalytics.vo.ModelTrainResultVO;
import com.ahogeking.studentanalytics.vo.ModelVersionDetailVO;
import com.ahogeking.studentanalytics.vo.ModelVersionVO;
import com.ahogeking.studentanalytics.vo.PageResultVO;

public interface ModelService {
    ModelTrainResultVO trainDecisionTree(ModelTrainRequest request);

    ModelVersionVO selectActiveModel();

    PageResultVO<ModelVersionVO> selectModelVersionPage(ModelVersionQueryRequest query);

    ModelVersionDetailVO selectModelVersionDetail(Integer id);
}
