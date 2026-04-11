package com.xz.match.model.request.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class LoginRequest implements Serializable {
	@Serial
	private static final long serialVersionUID = 20068933L;

	private String account;
	private String password;
}
