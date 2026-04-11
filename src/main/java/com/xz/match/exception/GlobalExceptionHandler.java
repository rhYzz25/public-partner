package com.xz.match.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.xz.match.common.ErrorCode;
import com.xz.match.common.ResponseEntity;
import com.xz.match.common.ResultUtils;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Hidden
@Slf4j
public class GlobalExceptionHandler {

    // 专门捕获我们自定义的 BusinessException
    @ExceptionHandler(MyCustomException.class)
    public ResponseEntity<?> handleBusinessException(MyCustomException e) {
		log.error("what happened", e); // 打印错误对象
        // 返回你统一定义的响应格式
        return ResultUtils.error(e.getErrorCode(), e.getMessage());
    }

    // 捕获系统未预料到的其他异常（如空指针、数据库连接失败）
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
        // 记录日志，并给前端一个友好的提示，而不是报错详情
        log.error("runtimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统开小差了，请稍后再试");
    }

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
		log.error("json error", e);

		if (e.getCause() instanceof InvalidFormatException) {
			InvalidFormatException ife = (InvalidFormatException) e.getCause();
			String fieldName = ife.getPath().get(0).getFieldName();
			// 这里fieldName是status
			return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请求参数错误 : " + fieldName);
		}

		return ResultUtils.error(ErrorCode.PARAMS_ERROR,"请求数据格式不正确");
	}
}