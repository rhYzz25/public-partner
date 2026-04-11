package com.xz.match.model.request.team;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class JoinTeamRequest implements Serializable {

	@Serial
	private static final long serialVersionUID = 20000L;

	private Long teamId;
	private String password;
}
