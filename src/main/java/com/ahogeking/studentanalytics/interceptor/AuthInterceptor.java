package com.ahogeking.studentanalytics.interceptor;

import com.ahogeking.studentanalytics.common.JwtUtil;
import com.ahogeking.studentanalytics.context.SysUserContext;
import com.ahogeking.studentanalytics.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
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

        // 4. 放入用户上下文
        SysUserContext.set(claims);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        SysUserContext.remove();
    }
}
