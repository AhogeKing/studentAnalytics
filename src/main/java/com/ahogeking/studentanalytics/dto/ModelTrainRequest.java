package com.ahogeking.studentanalytics.dto;

import lombok.Data;

@Data
public class ModelTrainRequest {
    private String mode = "default";
    private Boolean activate = true;
}
