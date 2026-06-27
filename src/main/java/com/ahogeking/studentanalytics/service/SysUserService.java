package com.ahogeking.studentanalytics.service;

import com.ahogeking.studentanalytics.dto.LoginRequest;
import com.ahogeking.studentanalytics.dto.LoginResponse;
import com.ahogeking.studentanalytics.dto.RegisterRequest;
import com.ahogeking.studentanalytics.dto.UserCreateRequest;
import com.ahogeking.studentanalytics.dto.UserPasswordResetRequest;
import com.ahogeking.studentanalytics.dto.UserStatusUpdateRequest;
import com.ahogeking.studentanalytics.dto.UserUpdateRequest;
import com.ahogeking.studentanalytics.vo.AdminUserVO;
import com.ahogeking.studentanalytics.vo.PageResultVO;

public interface SysUserService {
    void register(RegisterRequest registerRequest);

    LoginResponse login(LoginRequest loginRequest);

    PageResultVO<AdminUserVO> selectAdminUsers(Integer pageNum, Integer pageSize);

    AdminUserVO selectAdminUserById(Integer id);

    AdminUserVO createAdminUser(UserCreateRequest request);

    AdminUserVO updateAdminUser(Integer id, UserUpdateRequest request);

    AdminUserVO updateAdminUserStatus(Integer id, UserStatusUpdateRequest request);

    void resetAdminUserPassword(Integer id, UserPasswordResetRequest request);

    void disableAdminUser(Integer id);
}
