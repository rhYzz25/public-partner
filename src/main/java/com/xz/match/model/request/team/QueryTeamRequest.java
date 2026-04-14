package com.xz.match.model.request.team;

import lombok.Data;

import java.util.Date;

@Data
public class QueryTeamRequest {
	/**
	 * 队伍名称（模糊搜索）
	 */
	private String name;

	/**
	 * 队伍描述（模糊搜索）
	 */
	private String description;

	/**
	 * 创建者用户ID
	 */
	private Long userId;

	/**
	 * 队伍状态：0-公开，1-私有，2-加密
	 */
	private Integer status;

	/**
	 * 0(默认) 不要审批 1 需要审批
	 */
	private Integer needApproval;

	/**
	 * 最大人数最小值
	 */
	private Integer minMaxNum;

	/**
	 * 最大人数最大值
	 */
	private Integer maxMaxNum;

	/**
	 * 过期时间
	 */
	private Date expireTime;
}
