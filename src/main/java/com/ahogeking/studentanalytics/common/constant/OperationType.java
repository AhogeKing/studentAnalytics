package com.ahogeking.studentanalytics.common.constant;

public final class OperationType {
    private OperationType() {
    }

    public static final String CREATE = "CREATE";
    public static final String UPDATE = "UPDATE";
    public static final String DELETE = "DELETE";
    public static final String UPSERT = "UPSERT";
    public static final String ENABLE = "ENABLE";
    public static final String DISABLE = "DISABLE";
    public static final String RESET_PASSWORD = "RESET_PASSWORD";
    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";
    public static final String IMPORT = "IMPORT";
    public static final String TRAIN = "TRAIN";
    public static final String ACTIVATE = "ACTIVATE";
    public static final String PREDICT = "PREDICT";
    public static final String GENERATE_WARNING = "GENERATE_WARNING";
    public static final String HANDLE_WARNING = "HANDLE_WARNING";
    public static final String UPDATE_STATUS = "UPDATE_STATUS";
}
