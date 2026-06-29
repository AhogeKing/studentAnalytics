package com.ahogeking.studentanalytics.mapper;

import com.ahogeking.studentanalytics.dto.row.PredictionInputRow;
import com.ahogeking.studentanalytics.dto.row.PredictionResultRow;
import com.ahogeking.studentanalytics.entity.PredictionResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PredictionMapper {
    PredictionInputRow selectPredictionInputByStudentNo(@Param("studentNo") Integer studentNo);

    Integer insertPredictionResult(PredictionResult predictionResult);

    PredictionResultRow selectPredictionResultById(@Param("id") Integer id);

    PredictionResultRow selectLatestPredictionByStudentNo(@Param("studentNo") Integer studentNo);
}
