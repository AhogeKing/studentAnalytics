package com.ahogeking.studentanalytics.context;

import com.ahogeking.studentanalytics.entity.SysUser;

public final class SysUserContext {
    private SysUserContext() {
    }

    private static final ThreadLocal<SysUser> HOLDER = new ThreadLocal<>();

    public static void set(SysUser user) {
        HOLDER.set(user);
    }

    public static SysUser get() {
        return HOLDER.get();
    }

    public static Integer getUserId() {
        SysUser user = get();
        if (user == null) {
            return null;
        }
        return user.getId();
    }

    public static String getUsername() {
        SysUser user = get();
        return user == null
                ? null
                : user.getUsername();
    }

    public static String getRealName() {
        SysUser user = get();
        return user == null
                ? null
                : user.getRealName();
    }

    public static String getRole() {
        SysUser user = get();
        return user == null
                ? null
                : user.getRole();
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
