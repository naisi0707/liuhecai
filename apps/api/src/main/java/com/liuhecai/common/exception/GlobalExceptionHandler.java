package com.liuhecai.common.exception;

import com.liuhecai.common.enums.ErrorCode;
import com.liuhecai.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValid(Exception e) {
        return Result.fail(ErrorCode.VALIDATION_FAILED.getCode(), extractFirstErrorMessage(e));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("Unhandled exception", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }

    private String extractFirstErrorMessage(Exception e) {
        if (e instanceof MethodArgumentNotValidException manv) {
            FieldError fieldError = manv.getBindingResult().getFieldError();
            if (fieldError != null) {
                return fieldError.getDefaultMessage();
            }
        }
        if (e instanceof BindException be) {
            FieldError fieldError = be.getBindingResult().getFieldError();
            if (fieldError != null) {
                return fieldError.getDefaultMessage();
            }
        }
        return ErrorCode.VALIDATION_FAILED.getMessage();
    }
}
