package com.xz.match.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xz.match.model.entity.Team;
import com.xz.match.model.entity.User;
import com.xz.match.model.request.team.JoinTeamRequest;
import com.xz.match.model.request.team.QueryTeamRequest;
import com.xz.match.model.vo.TeamVO;
import com.xz.match.model.vo.UserVO;

import java.util.List;
import java.util.Set;

public interface TeamService extends IService<Team> {
	Long addTeam(Team team, User loginUser);

	Boolean deleteTeam(Long teamId, User loginUser);

	Boolean updateTeam(Team newTeam, User loginUser);

	List<TeamVO> searchTeam(String name);

	List<TeamVO> listTeamByCondition(QueryTeamRequest queryTeamRequest, User loginUser);

	Set<Long> hasJoinTeamId(List<Long> queryTeamId, User loginUser);

	Boolean joinTeam(JoinTeamRequest joinTeamRequest, Long userId);

	Boolean quitTeam(Long teamId,User loginUser);

	List<TeamVO> myTeam(User loginUser);

	List<UserVO> TeamInfo(Long teamId);
}
