package com.ahogeking.studentanalytics.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserPasswordResetRequest {
    @JsonProperty("new_password")
    @JsonAlias("newPassword")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度在6到100个字符之间")
    private String newPassword;
}
