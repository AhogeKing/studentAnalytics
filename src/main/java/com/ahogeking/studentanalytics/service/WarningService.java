package com.ahogeking.studentanalytics.service;

import com.ahogeking.studentanalytics.dto.WarningQueryRequest;
import com.ahogeking.studentanalytics.dto.WarningStatusUpdateRequest;
import com.ahogeking.studentanalytics.vo.PageResultVO;
import com.ahogeking.studentanalytics.vo.WarningDetailVO;
import com.ahogeking.studentanalytics.vo.WarningRecordVO;

public interface WarningService {
    WarningRecordVO generateWarningFromPrediction(Integer predictionResultId);

    WarningRecordVO selectWarningByPredictionResultId(Integer predictionResultId);

    WarningRecordVO selectLatestWarningByStudentNo(Integer studentNo);

    PageResultVO<WarningRecordVO> selectWarningPage(WarningQueryRequest query);

    WarningDetailVO selectWarningDetail(Integer id);

    WarningDetailVO updateWarningStatus(Integer id, WarningStatusUpdateRequest request);
}
