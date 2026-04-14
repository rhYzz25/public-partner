package com.xz.match.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xz.match.common.ErrorCode;
import com.xz.match.exception.MyCustomException;
import com.xz.match.job.convert.UserConvert;
import com.xz.match.mapper.TagMapper;
import com.xz.match.model.entity.JoinReq;
import com.xz.match.model.entity.Tag;
import com.xz.match.model.entity.User;
import com.xz.match.model.vo.JoinReqVO;
import com.xz.match.model.vo.UserVO;
import com.xz.match.service.JoinReqService;
import com.xz.match.mapper.JoinReqMapper;
import com.xz.match.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author zy
 * @description 针对表【join_req】的数据库操作Service实现
 * @createDate 2026-04-14 21:33:09
 */
@Service
public class JoinReqServiceImpl extends ServiceImpl<JoinReqMapper, JoinReq>
		implements JoinReqService {


	private final UserService userService;
	private final UserConvert userConvert;
	private final TagMapper tagMapper;

	public JoinReqServiceImpl(UserService userService, UserConvert userConvert, TagMapper tagMapper) {
		this.userService = userService;
		this.userConvert = userConvert;
		this.tagMapper = tagMapper;
	}

	// think 这题我会,查询了两次肯定会有N+1
	// think 完了,又变成N+2了,算了+2就+2吧,反正不是 N + N + 1就行了
	// think 不能直接用ById的方法,这个map也是循环
	@Override
	public List<JoinReqVO> reqList(Long teamId) {
		// 1. 判空
		if (teamId == null) throw new MyCustomException(ErrorCode.NULL_ERROR, "没有这样的队伍");

		// 2. 查询所有用户
		QueryWrapper<JoinReq> wrapper = new QueryWrapper<>();
		wrapper.eq("team_id", teamId);
		List<JoinReq> joinReqs = this.list(wrapper);

		// 4. 填充joinReq信息
		List<JoinReqVO> joinReqVOList = joinReqs.stream().map(joinReq -> {
			JoinReqVO vo = new JoinReqVO();
			vo.setRequestId(joinReq.getId());
			vo.setTeamId(joinReq.getTeamId());
			vo.setUserId(joinReq.getUserId());
			vo.setStatus(joinReq.getStatus());
			vo.setPassword(joinReq.getPassword());
			vo.setCreateTime(joinReq.getCreateTime());
			User user = userService.getById(joinReq.getUserId());
			UserVO userVO = userConvert.userToUserVO(user);
			userVO.setTags(tagMapper.selectTagsByUserId(userVO.getId()).stream().map(Tag::getName).toList());
			return vo;
		}).toList();

		// 5. 返回
		if (CollectionUtils.isEmpty(joinReqVOList)) {
			return Collections.emptyList();
		}

		return joinReqVOList;
	}
}




