package com.xz.match.teamTest;

import com.xz.match.model.constant.TeamStatusEnum;
import com.xz.match.model.entity.Team;
import com.xz.match.model.entity.User;
import com.xz.match.exception.MyCustomException;
import com.xz.match.service.TeamService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
public class teamTest {

	@Resource
	private TeamService teamService;

	@Test
	void testSaveTeam() {
		Team team = new Team();
		team.setName("原神玩家666");
		team.setMaxNum(5);
		team.setStatus(TeamStatusEnum.PUBLIC);
		boolean save = teamService.save(team);
		Assertions.assertTrue(save);
	}

	@Test
	void testAddTeam() {
		// 创建测试用户
		User user = new User();
		user.setId(1L);
		user.setAccount("test");
		user.setPassword("123456");

		// 创建测试队伍
		Team team = new Team();
		team.setName("测试队伍");
		team.setDescription("这是一个测试队伍");
		team.setMaxNum(10);
		// 设置过期时间为1天后
		Date expireTime = new Date();
		expireTime.setTime(expireTime.getTime() + 24 * 60 * 60 * 1000);
		team.setExpireTime(expireTime);
		team.setStatus(TeamStatusEnum.PUBLIC);

		// 测试添加队伍
		Long teamId = teamService.addTeam(team, user);
		Assertions.assertNotNull(teamId);
		Assertions.assertTrue(teamId > 0);
	}

	@Test
	void testDeleteTeam() {
		// 创建测试用户
		User user = new User();
		user.setId(1L);
		user.setAccount("test");
		user.setPassword("123456");

		// 创建测试队伍
		Team team = new Team();
		team.setName("测试队伍");
		team.setDescription("这是一个测试队伍");
		team.setMaxNum(10);
		// 设置过期时间为1天后
		Date expireTime = new Date();
		expireTime.setTime(expireTime.getTime() + 24 * 60 * 60 * 1000);
		team.setExpireTime(expireTime);
		team.setStatus(TeamStatusEnum.PUBLIC);

		// 先添加队伍
		Long teamId = teamService.addTeam(team, user);
		Assertions.assertNotNull(teamId);

		// 测试删除队伍
		Boolean result = teamService.deleteTeam(teamId, user);
		Assertions.assertTrue(result);
	}

	@Test
	void testUpdateTeam() {
		// 创建测试用户
		User user = new User();
		user.setId(1L);
		user.setAccount("test");
		user.setPassword("123456");

		// 创建测试队伍
		Team team = new Team();
		team.setName("测试队伍");
		team.setDescription("这是一个测试队伍");
		team.setMaxNum(10);
		// 设置过期时间为1天后
		Date expireTime = new Date();
		expireTime.setTime(expireTime.getTime() + 24 * 60 * 60 * 1000);
		team.setExpireTime(expireTime);
		team.setStatus(TeamStatusEnum.PUBLIC);

		// 先添加队伍
		Long teamId = teamService.addTeam(team, user);
		Assertions.assertNotNull(teamId);

		// 创建更新后的队伍信息
		Team updateTeam = new Team();
		updateTeam.setId(teamId);
		updateTeam.setName("更新后的测试队伍");
		updateTeam.setDescription("这是更新后的测试队伍描述");

		// 测试更新队伍
		Boolean result = teamService.updateTeam(updateTeam, user);
		Assertions.assertTrue(result);
	}

	@Test
	void testAddTeamWithSecret() {
		// 创建测试用户
		User user = new User();
		user.setId(1L);
		user.setAccount("test");
		user.setPassword("123456");

		// 创建测试加密队伍
		Team team = new Team();
		team.setName("加密测试队伍");
		team.setDescription("这是一个加密测试队伍");
		team.setMaxNum(10);
		// 设置过期时间为1天后
		Date expireTime = new Date();
		expireTime.setTime(expireTime.getTime() + 24 * 60 * 60 * 1000);
		team.setExpireTime(expireTime);
		team.setStatus(TeamStatusEnum.SECRET);
		team.setPassword("123456");

		// 测试添加加密队伍
		Long teamId = teamService.addTeam(team, user);
		Assertions.assertNotNull(teamId);
		Assertions.assertTrue(teamId > 0);
	}

	@Test
	void testAddTeamWithoutPassword() {
		// 创建测试用户
		User user = new User();
		user.setId(1L);
		user.setAccount("test");
		user.setPassword("123456");

		// 创建测试加密队伍但不设置密码
		Team team = new Team();
		team.setName("加密测试队伍");
		team.setDescription("这是一个加密测试队伍");
		team.setMaxNum(10);
		// 设置过期时间为1天后
		Date expireTime = new Date();
		expireTime.setTime(expireTime.getTime() + 24 * 60 * 60 * 1000);
		team.setExpireTime(expireTime);
		team.setStatus(TeamStatusEnum.SECRET);
		// 不设置密码

		// 测试添加加密队伍（应该抛出异常）
		Assertions.assertThrows(MyCustomException.class, () -> {
			teamService.addTeam(team, user);
		});
	}
}

