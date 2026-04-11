package com.xz.match.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xz.match.model.entity.UserTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserTagMapper extends BaseMapper<UserTag> {
	// 慢就是快,应该是返回影响行数?比如插入成功就1?同时插入几条就n?
	Long saveTags(@Param("tagIdList") List<Long> idList, @Param("userId") Long userId);
}
