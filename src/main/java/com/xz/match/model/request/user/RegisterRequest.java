package com.xz.match.model.request.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 注册请求体
 */
@Data
public class RegisterRequest implements Serializable {
	@Serial
	private static final long serialVersionUID = 20068932L;

	private String account;
	private String password;
	private String checkPassword;

	//  todo 也许会做的邮箱验证
	//	private String email;
	//	private int securityCode;

}
