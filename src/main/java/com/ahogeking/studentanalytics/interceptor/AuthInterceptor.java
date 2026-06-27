package com.ahogeking.studentanalytics.interceptor;

import com.ahogeking.studentanalytics.common.JwtUtil;
import com.ahogeking.studentanalytics.context.SysUserContext;
import com.ahogeking.studentanalytics.entity.SysUser;
import com.ahogeking.studentanalytics.exception.JwtAuthenticationException;
import com.ahogeking.studentanalytics.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;

    public AuthInterceptor(JwtUtil jwtUtil, SysUserMapper sysUserMapper) {
        this.jwtUtil = jwtUtil;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. 读取请求头
        String token = request.getHeader("Authorization");

        // 2. 没有 token
        if (token == null || token.isBlank()) {
            throw new JwtAuthenticationException("缺少 Authorization 请求头");
        }

        // 3. 解析 token
        Claims claims = jwtUtil.parseToken(token);
        Integer userId = Integer.valueOf(claims.getSubject());
        SysUser currentUser = sysUserMapper.selectById(userId);
        if (currentUser == null || currentUser.getStatus() == null || currentUser.getStatus() != 1) {
            throw new JwtAuthenticationException("用户不存在或已被禁用");
        }

        // 4. 放入用户上下文。JJWT 0.12 返回的 Claims 不可变，不能直接写回 claims。
        SysUserContext.set(currentUser);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        SysUserContext.remove();
    }
}
