package com.ahogeking.studentanalytics.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ModelVersionUpdateRequest {
    @JsonAlias("version_no")
    @NotBlank(message = "模型版本号不能为空")
    @Size(max = 50, message = "模型版本号长度不能超过50个字符")
    private String versionNo;
}
