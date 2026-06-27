package com.ahogeking.studentanalytics.controller;

import com.ahogeking.studentanalytics.annotation.LogOperation;
import com.ahogeking.studentanalytics.common.Result;
import com.ahogeking.studentanalytics.common.constant.OperationModule;
import com.ahogeking.studentanalytics.common.constant.OperationTargetType;
import com.ahogeking.studentanalytics.common.constant.OperationType;
import com.ahogeking.studentanalytics.dto.LoginRequest;
import com.ahogeking.studentanalytics.dto.LoginResponse;
import com.ahogeking.studentanalytics.dto.MeResponse;
import com.ahogeking.studentanalytics.dto.RegisterRequest;
import com.ahogeking.studentanalytics.context.SysUserContext;
import com.ahogeking.studentanalytics.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    @LogOperation(
            module = OperationModule.AUTH,
            type = OperationType.LOGOUT,
            targetType = OperationTargetType.USER,
            recordRequest = false
    )
    public Result<Void> logout(@RequestHeader("Authorization") String token) {
        return Result.success();
    }

    @GetMapping("/me")
    public Result<MeResponse> me(@RequestHeader("Authorization") String token) {
        MeResponse meResponse = new MeResponse();
        meResponse.setUserId(SysUserContext.getUserId());
        meResponse.setUserName(SysUserContext.getUsername());
        meResponse.setRealName(SysUserContext.getRealName());
        meResponse.setRole(SysUserContext.getRole());
        return Result.success(meResponse);
    }
}
