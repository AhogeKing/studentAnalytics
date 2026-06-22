package com.ahogeking.studentanalytics.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;   // 0 success, others failed
    private String message; // response message
    private T data; // response data
    private LocalDateTime timestamp;    // response time

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "操作成功", data, LocalDateTime.now());
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null, LocalDateTime.now());
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(1, message, null, LocalDateTime.now());
    }
}
