package com.ahogeking.studentanalytics.service;

import com.ahogeking.studentanalytics.dto.LoginRequest;
import com.ahogeking.studentanalytics.dto.LoginResponse;
import com.ahogeking.studentanalytics.dto.RegisterRequest;

public interface SysUserService {
    void register(RegisterRequest registerRequest);

    LoginResponse login(LoginRequest loginRequest);
}
