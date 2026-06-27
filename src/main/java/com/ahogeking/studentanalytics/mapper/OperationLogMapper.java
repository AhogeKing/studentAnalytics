package com.ahogeking.studentanalytics.mapper;

import com.ahogeking.studentanalytics.dto.OperationLogQueryRequest;
import com.ahogeking.studentanalytics.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OperationLogMapper {
    Integer insert(OperationLog log);

    Long countOperationLogs(@Param("query") OperationLogQueryRequest query);

    List<OperationLog> selectOperationLogPage(
            @Param("query") OperationLogQueryRequest query,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit);

    OperationLog selectOperationLogById(@Param("id") Integer id);
}
