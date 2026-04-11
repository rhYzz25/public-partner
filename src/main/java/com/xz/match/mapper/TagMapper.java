package com.xz.match.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xz.match.model.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
	/**
	 * 查询用户的所有标签完整信息
	 */
	List<Tag> selectTagsByUserId(@Param("userId") Long userId);

	// 报错不必理会
	List<Map<String,Object>> selectTagsByUserIdList(@Param("userIds") List<Long> userIdList);
}
