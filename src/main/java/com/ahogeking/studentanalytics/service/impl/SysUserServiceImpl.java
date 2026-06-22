package com.ahogeking.studentanalytics.service.impl;

import com.ahogeking.studentanalytics.common.JwtUtil;
import com.ahogeking.studentanalytics.common.PasswordUtil;
import com.ahogeking.studentanalytics.dto.LoginRequest;
import com.ahogeking.studentanalytics.dto.LoginResponse;
import com.ahogeking.studentanalytics.dto.RegisterRequest;
import com.ahogeking.studentanalytics.entity.SysUser;
import com.ahogeking.studentanalytics.exception.BusinessException;
import com.ahogeking.studentanalytics.mapper.SysUserMapper;
import com.ahogeking.studentanalytics.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;

    public SysUserServiceImpl(SysUserMapper sysUserMapper, JwtUtil jwtUtil) {
        this.sysUserMapper = sysUserMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void register(RegisterRequest registerRequest) {
        // 1. 检查用户名是否已存在
        SysUser existUser = sysUserMapper.selectByUsername(registerRequest.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 2. 创建用户对象
        SysUser user = new SysUser();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(PasswordUtil.encode(registerRequest.getPassword()));
        user.setRealName(registerRequest.getRealName());
        user.setRole(registerRequest.getRole() == null || registerRequest.getRole().isBlank()
                ? "TEACHER"
                : registerRequest.getRole());
        user.setStatus(1);

        sysUserMapper.insert(user);
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // 根据用户名查询用户
        SysUser dbUser = sysUserMapper.selectByUsername(loginRequest.getUsername());
        if (dbUser == null) {   // 用户不存在
            throw new BusinessException("用户名或密码错误");
        }

        // 校验密码
        if (!PasswordUtil.matches(loginRequest.getPassword(), dbUser.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = jwtUtil.createLoginToken(dbUser.getId(), dbUser.getUsername(), dbUser.getRole());

        // 组装返回结果
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(dbUser.getId());
        response.setUserName(dbUser.getUsername());
        response.setRealName(dbUser.getRealName());
        response.setRole(dbUser.getRole());

        return response;
    }
}
