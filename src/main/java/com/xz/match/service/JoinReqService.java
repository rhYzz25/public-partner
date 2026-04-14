package com.xz.match.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xz.match.model.entity.JoinReq;
import com.xz.match.model.vo.JoinReqVO;
import com.xz.match.model.vo.UserVO;

import java.util.List;

/**
* @author zy
* @description 针对表【join_req】的数据库操作Service
* @createDate 2026-04-14 21:33:09
*/
public interface JoinReqService extends IService<JoinReq> {
	List<JoinReqVO> reqList(Long teamId);
}
