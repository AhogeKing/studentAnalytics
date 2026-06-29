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

    Integer activateModelById(@Param("id") Integer id);

    Integer insertModelVersion(ModelVersion modelVersion);

    Integer updateModelVersionNo(
            @Param("id") Integer id,
            @Param("versionNo") String versionNo
    );

    Integer deleteModelVersionById(@Param("id") Integer id);

    ModelVersion selectActiveModelVersion();

    ModelVersion selectModelVersionById(@Param("id") Integer id);

    Long countModelVersionByVersionNo(
            @Param("id") Integer id,
            @Param("versionNo") String versionNo
    );

    Long countPredictionResultsByModelVersionId(@Param("id") Integer id);

    Long countModelVersions(@Param("query") ModelVersionQueryRequest query);

    List<ModelVersion> selectModelVersionPage(
            @Param("query") ModelVersionQueryRequest query,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );
}
