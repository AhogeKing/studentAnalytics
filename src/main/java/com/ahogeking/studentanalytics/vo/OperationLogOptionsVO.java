package com.ahogeking.studentanalytics.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OperationLogOptionsVO {
    private List<OptionVO<String>> modules;

    @JsonProperty("operation_types")
    private List<OptionVO<String>> operationTypes;

    private List<OptionVO<String>> results;
    private List<OptionVO<String>> roles;

    @JsonProperty("target_types")
    private List<OptionVO<String>> targetTypes;
}
