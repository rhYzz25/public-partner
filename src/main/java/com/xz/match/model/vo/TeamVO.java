package com.xz.match.model.vo;

import com.xz.match.model.constant.TeamStatusEnum;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class TeamVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 2L;

	/**
	 * 后面才加的id
	 * 一开始我是不想加的,要用的时候才加上
	 * userVO也加了
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

	/**
	 * 过期时间
	 */
	private Date expireTime;

	/**
	 * 用户id（队长 id）
	 * 这里是不是应该再查询一下?
	 */
	private Long userId;

	/**
	 * 0 - 公开，1 - 私有，2 - 加密
	 */
	private TeamStatusEnum status;

	/**
	 * 0(默认) 不要审批 1 需要审批
	 */
	private Integer needApproval;

	/**
	 * 展示队长的相关信息
	 */
	private UserVO userVO;

	/**
	 * 已加入人数
	 */
	private Integer hasJoinNum;

	/**
	 * 是否已经加入队伍
	 */
	private boolean hasJoin = false;

	/**
	 * 是否是自己创建的队伍
	 */
	private boolean isCreator = false;
}
