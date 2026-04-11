package com.xz.match.model.constant;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TeamStatusEnum {

	PUBLIC(0, "公开"),
	PRIVATE(1, "私有"),
	SECRET(2, "加密");

	@EnumValue
	@JsonValue // 返回给前端
	private final int value;
	private final String status;

	TeamStatusEnum(int value, String status) {
		this.value = value;
		this.status = status;
	}

	// 通过值获取状态
	public static TeamStatusEnum getEnumByValue(Integer value) {
		if (value == null) {
			return null;
		}
		// note 自带的values遍历所有枚举项(自定义常量)
		for (TeamStatusEnum teamStatusEnum : TeamStatusEnum.values()) {
			if (teamStatusEnum.value == value) {
				return teamStatusEnum;
			}
		}
		return null;
	}
	public int getValue() {
		return value;
	}
	public String getStatus() {
		return status;
	}
}
