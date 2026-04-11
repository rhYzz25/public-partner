package com.xz.match.exception;

import com.xz.match.common.ErrorCode;
import lombok.Getter;

@Getter
public class MyCustomException extends RuntimeException{

	// 异常信息
	private String message;

	// 异常码
	private int errorCode;

	public MyCustomException(String message, int errorCode) {
		this.message = message;
		this.errorCode = errorCode;
	}

	// todo 不懂
	public MyCustomException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.message = errorCode.getMessage();
		this.errorCode = errorCode.getCode();
	}

	public MyCustomException(ErrorCode errorCode, String message) {
		super(message); // 给控制台看的
		this.message = message;
		this.errorCode = errorCode.getCode();
	}
}
