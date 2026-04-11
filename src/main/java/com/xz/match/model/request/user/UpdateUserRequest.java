package com.xz.match.model.request.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UpdateUserRequest implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	private Long id;
	private String nickname;
	private String account;
	private String email;
	private String introduction;
	private Integer gender;
}
