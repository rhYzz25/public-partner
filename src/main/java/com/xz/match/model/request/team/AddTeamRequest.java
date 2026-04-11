package com.xz.match.model.request.team;

import com.xz.match.model.constant.TeamStatusEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class AddTeamRequest implements Serializable {
	@Serial
	private static final long serialVersionUID = 100L;

	/**
	 * 队伍名称
	 */
	private String name;

	/**
	 * 描述
	 */
	private String description;

	/**
	 * 最大人数
	 */
	private Integer maxNum;

	/**
	 * 过期时间
	 */
	private Date expireTime;

	/**
	 * 0 - 公开，1 - 私有，2 - 加密
	 */
	private TeamStatusEnum status;

	/**
	 * 密码
	 */
	private String password;

}
