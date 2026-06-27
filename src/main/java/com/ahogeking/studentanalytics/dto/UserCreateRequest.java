package com.ahogeking.studentanalytics.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度在6到100个字符之间")
    private String password;

    @JsonProperty("real_name")
    @JsonAlias("realName")
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String realName;

    @NotBlank(message = "角色不能为空")
    private String role;
}
