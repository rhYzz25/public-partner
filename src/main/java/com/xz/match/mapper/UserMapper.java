package com.xz.match.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xz.match.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

	long register_mybatis(@Param("account") String account, @Param("saltPassword") String saltPassword);

	int selectByAccount(@Param("account") String account);

	List<User> searchByTags(@Param("tagList") List<String> tagList, @Param("listSize") int listSize);
}

