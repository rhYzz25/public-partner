package com.xz.match.common;

// 不用再在返回格式中手写code and message
public class ResultUtils {
	public static <T> ResponseEntity<T> success(T data) {
		return new ResponseEntity<>(200, data, "ok");
	}

	// 这个真的可以触发吗
	public static <T> ResponseEntity<T> error(int errorCode, String message) {
		return new ResponseEntity<>(errorCode, null, message);
	}

	public static <T> ResponseEntity<T> error(ErrorCode errorCode, String message) {
		return new ResponseEntity<>(errorCode.getCode(), null, message);
	}

}
