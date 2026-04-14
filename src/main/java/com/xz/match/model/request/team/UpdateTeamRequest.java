package com.xz.match.model.request.team;

import com.xz.match.model.constant.TeamStatusEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UpdateTeamRequest implements Serializable {
	@Serial
	private static final long serialVersionUID = 100L;

	/**
	 * id
	 */
	private Long id;

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

	// 会员功能
	// private Date expireTime;

	/**
	 * 0 - 公开，1 - 私有，2 - 加密
	 */
	private TeamStatusEnum status;

	/**
	 * 密码
	 */
	private String password;

	/**
	 * 0(默认) 不要审批 1 需要审批
	 */
	private Integer needApproval;

}
