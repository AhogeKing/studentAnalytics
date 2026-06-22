package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.common.Result;
import com.ahogeking.studentanalytics.dto.LoginRequest;
import com.ahogeking.studentanalytics.dto.LoginResponse;
import com.ahogeking.studentanalytics.dto.RegisterRequest;
import com.ahogeking.studentanalytics.context.SysUserContext;
import com.ahogeking.studentanalytics.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class SysUserController {
    private final SysUserService sysUserService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid RegisterRequest registerRequest) {
        sysUserService.register(registerRequest);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        LoginResponse response = sysUserService.login(loginRequest);
        return Result.success(response);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        SysUserContext.remove();
        return Result.success();
    }
}
