package com.ahogeking.studentanalytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度在6到100个字符之间")
    private String password;

    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String realName;

    private String role;
}
