package com.xz.match.controller;

import com.xz.match.common.ErrorCode;
import com.xz.match.common.ResponseEntity;
import com.xz.match.common.ResultUtils;
import com.xz.match.model.entity.Team;
import com.xz.match.model.entity.User;
import com.xz.match.model.request.team.AddTeamRequest;
import com.xz.match.model.request.team.JoinTeamRequest;
import com.xz.match.model.request.team.QueryTeamRequest;
import com.xz.match.model.request.team.UpdateTeamRequest;
import com.xz.match.model.vo.TeamVO;
import com.xz.match.exception.MyCustomException;
import com.xz.match.job.convert.TeamConvert;
import com.xz.match.model.vo.UserVO;
import com.xz.match.service.TeamService;
import com.xz.match.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/team")
@CrossOrigin(originPatterns = "*")
public class TeamController {

	@Resource
	private UserService userService;

	@Resource
	private TeamService teamService;

	@Resource
	private TeamConvert teamConvert;
	// 增删改查查查

	@PostMapping("/add")
	public ResponseEntity<Long> addTeam(@RequestBody AddTeamRequest addTeamRequest, HttpServletRequest request) {
		if (addTeamRequest == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		User loginUser = userService.getLoginUser(request);
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		Team team = teamConvert.addTeamRequestToTeam(addTeamRequest); // mapStruct进行转化,set方法且编译,效率高
		Long teamId = teamService.addTeam(team, loginUser);
		if (teamId == null) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR);
		}
		return ResultUtils.success(teamId);
	}

	// note 建议带上Param
	@PostMapping("/delete")
	public ResponseEntity<Boolean> deleteTeam(@RequestParam Long teamId, HttpServletRequest request) {
		if (teamId == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		User loginUser = userService.getLoginUser(request);
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		Boolean result = teamService.deleteTeam(teamId, loginUser);
		if (!result){
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR);
		}
		return ResultUtils.success(true);
	}

	@PostMapping("/update")
	public ResponseEntity<Boolean> updateTeam(@RequestBody UpdateTeamRequest updateTeamRequest, HttpServletRequest request) {
		if (updateTeamRequest == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		User loginUser = userService.getLoginUser(request);
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		// think 如果没有更改status,传过来为null,会抛异常,转化不了
		// note  允许null转为枚举类 nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNOR
		Team newTeam = teamConvert.updateTeamRequestToTeam(updateTeamRequest);
		// 操作 + 实体类名
		Boolean result = teamService.updateTeam(newTeam, loginUser);
		if (!result){
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR);
		}
		return ResultUtils.success(true);
	}

	// think 在这里引入通用分页查询类?
	// think 这个的意义是,通过名称搜索
	// think 貌似没用,就这样吧,也没多少内容
	@GetMapping("/search")
	public ResponseEntity<List<TeamVO>> searchTeam(@RequestParam String name) {
		if (name == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		List<TeamVO> teamVOList = teamService.searchTeam(name);
		return ResultUtils.success(Objects.requireNonNullElse(teamVOList, Collections.emptyList()));
	}

	// think 这个的意义是,复杂组合搜索,默认为null进行全部搜索展示
	// sup 你之前在老项目中思考了传入全是null,现在又想了一遍
	// think 什么场景下会这么复杂的搜索
	// think 这个用来筛选搜索,那种,购物平台知道吧,就这样
	// think 不只有搜索,搜索出来还要考虑 1. 队伍状态 2. 我是否已经加入
	// note 不用param 和 body就能过了,为啥
	@GetMapping("/list")
	public ResponseEntity<List<TeamVO>> listTeamByCondition(@ModelAttribute QueryTeamRequest queryTeamRequest, HttpServletRequest request) {
		// 1. 过滤,判空 + 基础的查询
		if (queryTeamRequest == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		User loginUser = userService.getLoginUser(request);
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.N0T_L0GIN);
		}
		List<TeamVO> teamVOList = teamService.listTeamByCondition(queryTeamRequest, loginUser);
		return ResultUtils.success(Objects.requireNonNullElse(teamVOList, Collections.emptyList()));
	}

	@PostMapping("/join")
	public ResponseEntity<Boolean> joinTeam(@RequestBody JoinTeamRequest joinTeamRequest, HttpServletRequest request) {
		if (joinTeamRequest == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		User loginUser = userService.getLoginUser(request);
		System.out.println(joinTeamRequest.getTeamId());
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.NO_AUTH);
		}
		Boolean result = teamService.joinTeam(joinTeamRequest, loginUser);
		if (!result) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR);
		}
		return ResultUtils.success(true);
	}

	// todo 退出队伍,解散队伍(删除不就是解散吗),查看我加入的队伍/我创建的队伍
	@PostMapping("/quit")
	public ResponseEntity<Boolean> quitTeam(@RequestParam Long teamId, HttpServletRequest request) {
		if (teamId == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		User loginUser = userService.getLoginUser(request);
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.NO_AUTH);
		}
		Boolean result = teamService.quitTeam(teamId, loginUser);
		if (!result) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR);
		}
		return ResultUtils.success(true);
	}

	// 合并成一个方法,只在视图中加以区分
	@GetMapping("/my/team")
	public ResponseEntity<List<TeamVO>> myTeam(HttpServletRequest request) {
		if (request == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		User loginUser = userService.getLoginUser(request);
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.NO_AUTH);
		}
		List<TeamVO> teamVOS = teamService.myTeam(loginUser);
		// 因为自己可以没加入队伍,然后又不能报错
		// think 还有什么类似的方法吗,这个方法有写if来的实在吗
		return ResultUtils.success(Objects.requireNonNullElse(teamVOS, Collections.emptyList()));
	}

	// 获取队伍成员
	@GetMapping("/members")
	public ResponseEntity<List<UserVO>> getTeamMembers(@RequestParam Long teamId, HttpServletRequest request) {
		if (teamId == null || teamId <= 0) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}
		User loginUser = userService.getLoginUser(request);
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.N0T_L0GIN);
		}
		// 需要已登录才能查看成员
		List<UserVO> memberList = teamService.TeamInfo(teamId);
		return ResultUtils.success(Objects.requireNonNullElse(memberList, Collections.emptyList()));
	}
}
