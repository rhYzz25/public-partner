package com.xz.match.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

// JoinReqVO.java
@Data
public class JoinReqVO implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	private Long requestId;    //
	// 申请记录id（用于审批操作）
	private Long teamId;
	private Long userId;
	private Integer status;    // 0-待审批 1-已同意 2-已拒绝
	private String password;
	private Date createTime;
	private UserVO user;       //
}