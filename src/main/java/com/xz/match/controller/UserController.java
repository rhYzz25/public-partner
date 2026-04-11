package com.xz.match.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xz.match.common.ErrorCode;
import com.xz.match.common.ResponseEntity;
import com.xz.match.common.ResultUtils;
import com.xz.match.job.cosService.CosManager;
import com.xz.match.model.entity.User;
import com.xz.match.model.request.user.LoginRequest;
import com.xz.match.model.request.user.RegisterRequest;
import com.xz.match.model.request.user.UpdateUserRequest;
import com.xz.match.model.vo.UserVO;
import com.xz.match.exception.MyCustomException;
import com.xz.match.job.convert.UserConvert;
import com.xz.match.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/user") // 傻鸟ai,全部都改掉了,就是没发现这个参数带错了
@CrossOrigin(originPatterns = "*")
public class UserController {
	// 先写一套增删改查查查
	@Resource
	private UserService userService;
	@Resource
	private UserConvert userConvert;
	@Resource
	private RedisTemplate<String, Object> redisTemplate;
	@Resource
	private CosManager cosManager;


	@PostMapping("/register")
	public ResponseEntity<Long> register(@RequestBody RegisterRequest registerRequest) {
		// 1. 判空,直接点击注册按钮
		if (registerRequest == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		// 2. 判空,未填完整
		String account = registerRequest.getAccount();
		String password = registerRequest.getPassword();
		String checkPassword = registerRequest.getCheckPassword();
		if (StringUtils.isAnyBlank(account, password, checkPassword)) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		long num = userService.register(account, password, checkPassword);
		return ResultUtils.success(num);
	}

	@PostMapping("/login")
	public ResponseEntity<UserVO> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
		if (loginRequest == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		String account = loginRequest.getAccount();
		String password = loginRequest.getPassword();
		if (StringUtils.isAnyBlank(account, password)) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		return ResultUtils.success(userService.login(account, password, request));
	}

	@PostMapping("/delete")
	public ResponseEntity<Boolean> delete(@RequestParam long id, HttpServletRequest request) {
		if (id <= 0) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}
		User loginUser = userService.getLoginUser(request);
		if (id != loginUser.getId()) {
			throw new MyCustomException(ErrorCode.NO_AUTH);
		}
		boolean result = userService.removeById(id);
		if (!result) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR);
		}
		return ResultUtils.success(true);
	}

	@PostMapping("/logout")
	public ResponseEntity<Boolean> logout(HttpServletRequest request) {
		if (request == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		Boolean b = userService.logout(request);
		if (!b) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR);
		}
		return ResultUtils.success(true);
	}

	@PostMapping("/update")
	public ResponseEntity<Boolean> update(@RequestBody UpdateUserRequest updateUserRequest, HttpServletRequest request) {
		if (updateUserRequest == null) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}
		// think 我应该在这完成copy
		User user = userConvert.updateUserRequestToUser(updateUserRequest);
		user.setId(userService.getLoginUser(request).getId());
		Boolean result = userService.update(user, request);
		return ResultUtils.success(result);
	}

	@PostMapping("/uploadAvatar")
	public ResponseEntity<Boolean> uploadAvatar(@RequestParam("file") MultipartFile multipartFile, HttpServletRequest request) {
		// 1. 判空 && 对文件进行校验
		User loginUser = userService.getLoginUser(request);
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.NO_AUTH, "未登录");
		}
		cosManager.validPicture(multipartFile);
		// 2. 获取原文件名,然后进行拼接出上传路径
		String uuid = RandomUtil.randomString(16);
		String originalFilename = multipartFile.getOriginalFilename();
		// think 图片名称 : 日期_随机数.原图名字后缀 上传路径 : public/用户id/日期_随机数.后缀
		String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFilename));
		String uploadPath = String.format("/%s/%s/%s", "public", loginUser.getId(), uploadFilename);
		File file = null;
		// 3. 进行上传操作
		try {
			file = File.createTempFile("cos-upload-", "_" + originalFilename);
			multipartFile.transferTo(file);
			cosManager.uploadPicture(uploadPath, file);

			// 4. 上传完成 将url填入用户信息中
			User user = new User();
			user.setId(loginUser.getId());
			user.setAvatar(uploadPath);
			userService.updateById(user);
			return ResultUtils.success(true);
		} catch (Exception e) {
			log.error("file upload error, filepath = {}", uploadPath, e);
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR, "上传失败");
		} finally {
			// 5. 删除临时文件 即使没上传成功本地也有有,所以还是删掉
			if (file != null) {
				boolean delete = file.delete();
				if (!delete) {
					log.error("file delete error, filepath = {}", uploadPath);
				}
			}
		}
	}

	// note 你怎么在get方法中使用requestBody,e,应该用param,而且前端传入的不是json格式
	@GetMapping("/search")
	public ResponseEntity<List<UserVO>> search(@RequestParam String keyword) {
		if (StringUtils.isEmpty(keyword)) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		List<UserVO> userVOList = userService.search(keyword);
		return ResultUtils.success(Objects.requireNonNullElse(userVOList, Collections.emptyList()));
	}

	// think 如何查询,以及如何优化?
	@GetMapping("/searchByTags")
	public ResponseEntity<List<UserVO>> searchByTag(@RequestParam(required = false) List<String> tagList) {
		// think 用户id -> 标签id
		// think 如何传参数? 用户先选择大类,然后选择小类,大类只是前端为了逻辑显示的,实际只是一个list
		// note 多对多使用自定义mapper
		if (CollectionUtils.isEmpty(tagList)) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		log.info("tagList:{}", tagList);
		List<UserVO> userVOList = userService.searchByTags(tagList);
		return ResultUtils.success(Objects.requireNonNullElse(userVOList, Collections.emptyList()));
	}

	// todo 后面是什么,根据标签去查询,推荐系统,还有前面登录的redis修改
	@GetMapping("/my")
	public ResponseEntity<UserVO> my(HttpServletRequest request) {
		if (request == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		User user = userService.getLoginUser(request);
		if (user == null) {
			throw new MyCustomException(ErrorCode.NO_AUTH);
		}
		// think 现在还是基于没有就不给进的逻辑
		UserVO userVO = userConvert.userToUserVO(user);
		return ResultUtils.success(userVO);
	}

	// think 这里引入redis?
	// note 哦哦,共享登录态才引入的
	// think 我需要提供什么必要的信息吗
	// think 这个不就是userList吗,查询队伍就不叫推荐了
	@GetMapping("/recommend")
	public ResponseEntity<Page<UserVO>> recommend(
			@RequestParam(required = false, defaultValue = "1") Integer pageNum,
			@RequestParam(required = false, defaultValue = "10") Integer pageSize,
			HttpServletRequest request) {
		if (request == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		// 配合缓存
		// 1. 获取key
		User loginUser = userService.getLoginUser(request);
		String key = String.format("user:recommend:%s", loginUser.getId());
		// 2. 先去redis中查询
		ValueOperations<String, Object> operations = redisTemplate.opsForValue();
		Page<UserVO> userVOPage = (Page<UserVO>) operations.get(key);

		// 3. 如果redis中没有就去数据库中查询并且放到redis中
		if (userVOPage != null) {
			return ResultUtils.success(userVOPage);
		}
		try {
			userVOPage = userService.recommend(pageNum, pageSize, request);
			int expireTime = 600 + RandomUtil.randomInt(0, 60);
			operations.set(key, userVOPage, expireTime, TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error("set key error !!!", e);
		}
		return ResultUtils.success(userVOPage);
	}

}
