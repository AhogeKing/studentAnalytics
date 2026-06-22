package com.ahogeking.studentanalytics.exception;

import com.ahogeking.studentanalytics.common.Result;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public Result<Void> handleJwtAuthenticationException(JwtAuthenticationException ex) {
        return Result.error(401, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null ? "请求参数错误" : fieldError.getDefaultMessage();
        return Result.error(message);
    }
}
