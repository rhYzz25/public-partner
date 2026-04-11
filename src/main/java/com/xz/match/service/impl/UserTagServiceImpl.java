package com.xz.match.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xz.match.model.entity.UserTag;
import com.xz.match.mapper.UserTagMapper;
import com.xz.match.service.UserTagService;
import org.springframework.stereotype.Service;

@Service
public class UserTagServiceImpl extends ServiceImpl<UserTagMapper, UserTag> implements UserTagService {

}
