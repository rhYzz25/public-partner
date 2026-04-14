package com.xz.match.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("join_req")
public class JoinReq {
	@TableId(type = IdType.AUTO)
	private Long id;
	private Long teamId;
	private Long userId;
	/**
	 * 0 待审批 1 同意 2 拒绝
	 */
	private int status;
	private String password;
	private Date createTime;
}
