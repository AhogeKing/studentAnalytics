package com.ahogeking.studentanalytics.interceptor;

import com.ahogeking.studentanalytics.annotation.RequireRole;
import com.ahogeking.studentanalytics.context.SysUserContext;
import com.ahogeking.studentanalytics.exception.ForbiddenException;
import com.ahogeking.studentanalytics.exception.JwtAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;

@Component
public class RoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) {
            return true;
        }

        String currentRole = SysUserContext.getRole();
        if (currentRole == null || currentRole.isBlank()) {
            throw new JwtAuthenticationException("请先登录");
        }

        boolean matched = Arrays.asList(requireRole.value()).contains(currentRole);
        if (!matched) {
            throw new ForbiddenException("无权限访问该接口");
        }
        return true;
    }
}
