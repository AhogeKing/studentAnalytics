package com.ahogeking.studentanalytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MeResponse {
    private Integer userId;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("real_name")
    private String realName;

    private String role;
}
