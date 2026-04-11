package com.xz.match.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xz.match.common.ErrorCode;
import com.xz.match.model.entity.Tag;
import com.xz.match.model.entity.User;
import com.xz.match.model.entity.UserTag;
import com.xz.match.exception.MyCustomException;
import com.xz.match.mapper.TagMapper;
import com.xz.match.mapper.UserTagMapper;
import com.xz.match.service.TagService;
import com.xz.match.service.UserTagService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {
	@Resource
	private UserTagService userTagService;
	@Resource
	private UserTagMapper userTagMapper;

	@Override
	public Boolean updateTag(User user, List<String> tagsList) {
		// 1. 参数校验
		if (user == null) {
			throw new MyCustomException(ErrorCode.N0T_L0GIN);
		}
		if (CollectionUtils.isEmpty(tagsList)) {
			Long userId = user.getId();
			QueryWrapper<UserTag> deleteWrapper = new QueryWrapper<>();
			deleteWrapper.eq("user_id", userId);
			userTagService.remove(deleteWrapper);
			return true;
		}
		if (tagsList.size() > 5) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "标签数过多");
		}

		// 2. 查询list中是否含有无效标签
		QueryWrapper<Tag> wrapper = new QueryWrapper<>();
		// note 能尽量用in就in,不然就是for加eq查询多次
		wrapper.in("name", tagsList);
		List<Tag> getTagList = this.list(wrapper);
		if (getTagList.size() != tagsList.size()) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "存在无效标签");
		}
		List<Long> idList = getTagList.stream().map(Tag::getId).toList();

		// 3. 先删除所有标签
		// note 全量替换,毕竟才五个标签,要不了多少时间
		Long userId = user.getId();
		QueryWrapper<UserTag> deleteWrapper = new QueryWrapper<>();
		deleteWrapper.eq("user_id", userId);
		userTagService.remove(deleteWrapper);

		// 4. 添加到user_tag表中,还有什么注意事项吗?
		Long result = userTagMapper.saveTags(idList, userId);
		if (result < 0){
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR, "保存失败QAQ");
		}
		return true;
	}

}
