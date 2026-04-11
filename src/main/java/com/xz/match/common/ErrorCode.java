package com.xz.match.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

	SUCCESS(0, "成功", ""),
	PARAMS_ERROR(40000, "请求参数错误", ""),
	NULL_ERROR(40001, "请求参数为空", ""),
	REPEAT_DATA(40002, "请求参数重复", ""),
	N0T_L0GIN(40100, "未登录", ""),
	NO_AUTH(40101, "无权限", ""),
	SYSTEM_ERROR(50000, "系统异常", "");

	private final int code;

	/**
	 * 错误信息
	 */
	private final String message;

	/**
	 * 状态码描述,更详细一点而已
	 */
	private final String description;

	ErrorCode(int code, String message, String description) {
		this.code = code;
		this.message = message;
		this.description = description;
	}

}
