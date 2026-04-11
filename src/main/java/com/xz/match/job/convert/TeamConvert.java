package com.xz.match.job.convert;

import com.xz.match.model.entity.Team;
import com.xz.match.model.request.team.AddTeamRequest;
import com.xz.match.model.request.team.UpdateTeamRequest;
import com.xz.match.model.vo.TeamVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TeamConvert {

	// note 可以在方法注解上备注什么字段转什么字段
	/**
	 * 将AddTeamRequest转化为Team实体
	 */
	Team addTeamRequestToTeam(AddTeamRequest addTeamRequest);

	/**
	 *
	 */
	Team updateTeamRequestToTeam(UpdateTeamRequest updateTeamRequest);

	/**
	 * 将Team实体转化为TeamVO
	 */
	@Mapping(target = "userVO", ignore = true)
	TeamVO teamToTeamVO(Team team);

	@Mapping(target = "userVO", ignore = true)
	List<TeamVO> teamListToTeamVOList(List<Team> teamList);


}

