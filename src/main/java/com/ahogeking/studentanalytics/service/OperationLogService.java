package com.ahogeking.studentanalytics.service;

import com.ahogeking.studentanalytics.dto.OperationLogQueryRequest;
import com.ahogeking.studentanalytics.entity.OperationLog;
import com.ahogeking.studentanalytics.vo.OperationLogDetailVO;
import com.ahogeking.studentanalytics.vo.OperationLogOptionsVO;
import com.ahogeking.studentanalytics.vo.OperationLogVO;
import com.ahogeking.studentanalytics.vo.PageResultVO;

public interface OperationLogService {
    void saveOperationLog(OperationLog log);

    PageResultVO<OperationLogVO> selectOperationLogPage(OperationLogQueryRequest query);

    OperationLogDetailVO selectOperationLogDetail(Integer id);

    OperationLogOptionsVO selectOperationLogOptions();
}
