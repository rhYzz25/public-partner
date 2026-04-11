package com.xz.match.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xz.match.annotation.AuthCheck;
import com.xz.match.common.*;
import com.xz.match.job.convert.TagConvert;
import com.xz.match.model.entity.Tag;
import com.xz.match.model.entity.User;
import com.xz.match.exception.MyCustomException;
import com.xz.match.model.entity.UserTag;
import com.xz.match.model.request.tag.AddTagRequest;
import com.xz.match.service.TagService;
import com.xz.match.service.UserService;
import com.xz.match.service.UserTagService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tags")
public class TagsController {
	@Resource
	private UserService userService;
	@Resource
	private TagService tagService;
	@Resource
	private UserTagService userTagService;
	@Resource
	private TagConvert tagConvert;
	@Resource
	private com.xz.match.mapper.TagMapper tagMapper;

	// think 这两个方法是不是可以合并成update方法,按理来说add应该是管理员用的
	@PostMapping("/addTag")
	@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
	public ResponseEntity<Boolean> addTag(@RequestBody AddTagRequest addTagRequest) {
		if (StringUtils.isAnyBlank(addTagRequest.getName(),  addTagRequest.getKind())) {
			throw new MyCustomException(ErrorCode.NULL_ERROR, "有参数为空");
		}
		Tag tag = tagConvert.addTagRequestToTag(addTagRequest);
		boolean result = tagService.save(tag);
		if (!result) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR, "添加失败");
		}
		return ResultUtils.success(true);
	}

	// todo 让ai帮我打标签
	@PostMapping("/deleteTag")
	@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
	public ResponseEntity<Boolean> deleteTag(@RequestParam(required = false) Long tagId) {
		if (tagId == null || tagId <= 0) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}
		QueryWrapper<UserTag> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("tag_id", tagId);
		userTagService.remove(queryWrapper);
		boolean result = tagService.removeById(tagId);
		if (!result) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR, "删除失败");
		}
		return ResultUtils.success(true);
	}

	// think 如何和已经存在进行对比?
	// sup 更新的策略 全量更新 增量更新 传空就代表清空标签
	@PostMapping("/updateTag")
	public ResponseEntity<Boolean> updateTag(@RequestParam(required = false) List<String> tagList, HttpServletRequest request) {
		User loginUser = userService.getLoginUser(request);
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.NO_AUTH);
		}
		Boolean result = tagService.updateTag(loginUser, tagList);
		if (!result) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR);
		}
		return ResultUtils.success(true);
	}

	@GetMapping("/list")
	public ResponseEntity<List<Tag>> getTagList() {
		return ResultUtils.success(tagService.list());
	}

	@GetMapping("/my-tags")
	public ResponseEntity<List<Tag>> getMyTags(HttpServletRequest request) {
		User loginUser = userService.getLoginUser(request);
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.N0T_L0GIN);
		}
		Long userId = loginUser.getId();
		List<Tag> userTags = tagMapper.selectTagsByUserId(userId);
		return ResultUtils.success(userTags);
	}

}
