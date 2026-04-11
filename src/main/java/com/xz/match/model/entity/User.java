package com.xz.match.model.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

// note mybatisPlus的映射规则
// note 1. User类名 -> user UserInfo -> user_info
// note 2. userName -> user_name
// note 可以使用注解直接指定,不用默认映射

@TableName(value = "user") // think 这个好像可有可无
@Data
public class User implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private Long id;

	private String nickname;

	private String avatar;

	private String account;

	private String password;

	private String email;

	private String introduction;

	private Integer gender; // 0-未知, 1-男, 2-女

	private String role;

	@TableLogic
	private Integer isDelete; // 逻辑删除标识

	private Date createTime;

	private Date updateTime;

	// 无参构造
	public User() {
	}
}