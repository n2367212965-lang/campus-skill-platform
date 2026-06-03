package com.example.campusskillplatform.handler;

import com.example.campusskillplatform.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器
 * 统一处理系统异常，返回标准格式 {code, message, data}
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常（运行时异常）
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理参数校验异常（配合 @Valid 使用）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.error("参数校验失败: {}", message);
        return Result.error(400, message);
    }

    /**
     * 处理请求参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result handleMissingParams(MissingServletRequestParameterException e) {
        String message = "缺少必填参数：" + e.getParameterName();
        log.error("参数缺失: {}", message);
        return Result.error(400, message);
    }

    /**
     * 处理参数类型转换异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "参数类型错误：" + e.getName() + " 应为 " + e.getRequiredType().getSimpleName();
        log.error("类型转换失败: {}", message);
        return Result.error(400, message);
    }

    /**
     * 处理请求体解析异常（JSON格式错误等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.error("请求体解析失败: {}", e.getMessage());
        return Result.error(400, "请求数据格式错误，请检查JSON格式是否正确");
    }

    /**
     * 处理其他所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.error(500, "系统异常，请稍后重试");
    }
}
