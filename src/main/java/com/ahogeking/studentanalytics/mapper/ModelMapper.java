package com.ahogeking.studentanalytics.mapper;

import com.ahogeking.studentanalytics.dto.ModelVersionQueryRequest;
import com.ahogeking.studentanalytics.dto.row.ModelTrainingRow;
import com.ahogeking.studentanalytics.entity.ModelVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ModelMapper {
    List<ModelTrainingRow> selectModelTrainingRows();

    Integer deactivateAllModels();

    Integer insertModelVersion(ModelVersion modelVersion);

    ModelVersion selectActiveModelVersion();

    ModelVersion selectModelVersionById(@Param("id") Integer id);

    Long countModelVersions(@Param("query") ModelVersionQueryRequest query);

    List<ModelVersion> selectModelVersionPage(
            @Param("query") ModelVersionQueryRequest query,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );
}
