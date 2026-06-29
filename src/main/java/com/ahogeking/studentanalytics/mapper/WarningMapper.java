package com.ahogeking.studentanalytics.mapper;

import com.ahogeking.studentanalytics.dto.WarningQueryRequest;
import com.ahogeking.studentanalytics.dto.row.WarningGenerationContextRow;
import com.ahogeking.studentanalytics.dto.row.WarningRecordRow;
import com.ahogeking.studentanalytics.entity.WarningRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WarningMapper {
    WarningGenerationContextRow selectWarningGenerationContext(@Param("predictionResultId") Integer predictionResultId);

    Integer insertWarningRecord(WarningRecord warningRecord);

    WarningRecordRow selectWarningRecordById(@Param("id") Integer id);

    WarningRecordRow selectWarningByPredictionResultId(@Param("predictionResultId") Integer predictionResultId);

    WarningRecordRow selectLatestWarningByStudentNo(@Param("studentNo") Integer studentNo);

    Long countWarningRecords(@Param("query") WarningQueryRequest query);

    List<WarningRecordRow> selectWarningRecordPage(
            @Param("query") WarningQueryRequest query,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    Integer updateWarningStatus(
            @Param("id") Integer id,
            @Param("status") String status,
            @Param("handlerUserId") Integer handlerUserId
    );

    Integer deleteWarningRecordById(@Param("id") Integer id);
}
