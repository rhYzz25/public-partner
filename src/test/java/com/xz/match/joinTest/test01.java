package com.xz.match.joinTest;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xz.match.mapper.JoinReqMapper;
import com.xz.match.model.entity.JoinReq;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class test01 {
	@Resource
	JoinReqMapper joinReqMapper;
	@Test
	public void test1() {
		JoinReq joinReq = new JoinReq();
		joinReq.setStatus(1);
		joinReq.setTeamId(1L);
		joinReq.setUserId(1L);
		joinReqMapper.insert(joinReq);
	}
}
