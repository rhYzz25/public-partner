package com.xz.match.common;

import lombok.Data;

import java.io.Serializable;

// 自定义返回格式肯定要序列化的
// 重名了
@Data
public class ResponseEntity<T> implements Serializable {
	private String message;
	private T data;
	private int code;

	public ResponseEntity() {
	}

	public ResponseEntity(int code, T data, String message) {
		this.message = message;
		this.data = data;
		this.code = code;
	}

}
