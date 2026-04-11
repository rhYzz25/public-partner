package com.xz.match.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xz.match.model.entity.UserTeam;
import com.xz.match.service.UserTeamService;
import com.xz.match.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{
}




