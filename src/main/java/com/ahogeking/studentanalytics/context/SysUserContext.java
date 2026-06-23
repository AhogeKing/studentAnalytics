package com.ahogeking.studentanalytics.context;

import io.jsonwebtoken.Claims;

public final class SysUserContext {
    private SysUserContext() {
    }

    private static final ThreadLocal<Claims> HOLDER = new ThreadLocal<>();

    public static void set(Claims claims) {
        HOLDER.set(claims);
    }

    public static Claims get() {
        return HOLDER.get();
    }

    public static Integer getUserId() {
        Claims claims = get();
        if (claims == null) {
            return null;
        }
        return Integer.valueOf(claims.getSubject());
    }

    public static String getUsername() {
        Claims claims = get();
        return claims == null
                ? null
                : claims.get("username", String.class);
    }

    public static String getRealName() {
        Claims claims = get();
        return claims == null
                ? null
                : claims.get("realName", String.class);
    }

    public static String getRole() {
        Claims claims = get();
        return claims == null
                ? null
                : claims.get("role", String.class);
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(getRole());
    }

    public static boolean isTeacher() {
        return "TEACHER".equals(getRole());
    }

    public static boolean isStudent() {
        return "STUDENT".equals(getRole());
    }

    public static void remove() {
        HOLDER.remove();
    }
}
