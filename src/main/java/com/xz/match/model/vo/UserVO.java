package com.xz.match.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class UserVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Long id;
	private String nickname;
	private String avatar;
	private String account;
	private String email;
	private String introduction;
	private Integer gender; // 0-未知, 1-男, 2-女
	private String role;
	private List<String> tags;
}
