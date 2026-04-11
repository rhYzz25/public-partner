package com.xz.match.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xz.match.common.ErrorCode;
import com.xz.match.model.entity.Tag;
import com.xz.match.model.entity.User;
import com.xz.match.model.vo.UserVO;
import com.xz.match.exception.MyCustomException;
import com.xz.match.job.convert.UserConvert;
import com.xz.match.mapper.TagMapper;
import com.xz.match.mapper.UserMapper;
import com.xz.match.service.UserService;
import com.xz.match.job.EditDistanceUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.xz.match.model.constant.UserConstant.USER_LOGIN_STATE;

// note 最后一道防线,一定要再次判空
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
	private final static String ENCRYPT_SALT = "xz";
	private final static String TOKEN = "login:token";
	private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{5,13}$");

	@Resource
	private UserMapper userMapper;
	@Resource
	private TagMapper tagMapper;
	@Resource
	private UserConvert userConvert;
	@Resource
	private RedisTemplate<String, Object> redisTemplate;

	@Deprecated // 因为这是我抛弃plus用batis写的,意义重大,不能删
	public long register_mybatis(String account, String password, String checkPassword) {
		// 注册判断流程,目的是将数据插到数据库当中去
		// 判断是否符合规则
		// think 通用思路:去空查重?
		// 1. 账号密码是否为空
		boolean b = validAccount(account, password, checkPassword);
		if (!b) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}
		// 3. 账号是否重复
		int i = userMapper.selectByAccount(account);
		if (i > 0) {
			throw new MyCustomException(ErrorCode.REPEAT_DATA);
		}
		// 4. 密码加盐
		// todo 可以使用secure random进行随机生成
		String saltPassword = DigestUtils.md5DigestAsHex((ENCRYPT_SALT + password).getBytes());

		// 5. 可以存进去了?
		// note 还要考虑保存失败的情况
		// think 这里是返回行数,如果是返回用户id我该怎么返回,又要写一个mapper?
		long register = userMapper.register_mybatis(account, saltPassword);
		if (register < 0) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR);
		}
		return register;
	}

	@Override
	public long register(String account, String password, String checkPassword) {
		// 1. 通用账户校验规则
		boolean validAccount = validAccount(account, password, checkPassword);
		if (!validAccount) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}

		// 3. 账号是否重复
		QueryWrapper<User> wrapper = new QueryWrapper<>();
		wrapper.eq("account", account);

		// 4. 密码加盐
		// todo 可以使用secure random进行随机生成盐,好像可以抗什么彩虹表攻击
		String saltPassword = DigestUtils.md5DigestAsHex((ENCRYPT_SALT + password).getBytes());

		// 5. 可以存进去了?
		User user = new User();
		user.setAccount(account);
		user.setPassword(saltPassword);
		user.setIntroduction("这位用户什么都没有写");
		int result = userMapper.insert(user);
		if (result <= 0) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR);
		}
		// think 牛皮,刚保存,这里就可以获取id了
		return user.getId();
	}

	@Override
	public UserVO login(String account, String password, HttpServletRequest request) {
		// 1. 判空
		boolean validAccount = validAccount(account, password, password);
		if (!validAccount) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}

		// 2. 查询有没有
		String safePw = DigestUtils.md5DigestAsHex((ENCRYPT_SALT + password).getBytes());
		QueryWrapper<User> wrapper = new QueryWrapper<>();
		wrapper.eq("account", account);
		wrapper.eq("password", safePw);
		User queryUser = userMapper.selectOne(wrapper);
		if (queryUser == null) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}

		// 3. 考虑脱敏,并且将对象存到session中去
		UserVO safedUser = safeUser(queryUser);

		// feat 获取token,然后,同之前的token进行比较,token的值唯一,key一个账号唯一
		// 4. 生成登录令牌
		String newToken = UUID.randomUUID().toString();
		Long userid = safedUser.getId();

		// 更新redis中的token
		String redisTokenKey = TOKEN + userid;
		// redisTemplate.opsForValue().set(redisTokenKey, newToken, 30, TimeUnit.MINUTES);
		// feat 改成存放map
		HashMap<String, Object> userMap = new HashMap<>();
		userMap.put("id", safedUser.getId());
		userMap.put("account", safedUser.getAccount());
		userMap.put("nickname", safedUser.getNickname());
		userMap.put("avatar", safedUser.getAvatar());
		userMap.put("email", safedUser.getEmail());
		userMap.put("introduction", safedUser.getIntroduction());
		userMap.put("gender", safedUser.getGender());
		userMap.put("token", newToken);
		// think 默认半小时?我在哪里设置过?
		redisTemplate.opsForHash().putAll(redisTokenKey, userMap);

		// 写入session
		HttpSession session = request.getSession();
		session.setAttribute(USER_LOGIN_STATE, safedUser);
		session.setAttribute(TOKEN, newToken);

		return safedUser;
	}

	@Override
	public Boolean logout(HttpServletRequest request) {
		// todo 这里要执行删除,将随机的token删掉
		if (request == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		HttpSession session = request.getSession();
		UserVO userVO = (UserVO) session.getAttribute(USER_LOGIN_STATE);

		if (userVO != null) {
			redisTemplate.delete(TOKEN + userVO.getId()); // 删掉token
		}

		session.invalidate(); // 销毁session
		return true;
	}

	// note 一开始我还在传需要修改的id,后来发现根本就没用,哈哈
	// note 然后就是相应的request请求,还是不行?why 我已经忘了
	// note 好像是因为复制的问题,我把它挪到外面去复制了
	@Override
	public Boolean update(User user, HttpServletRequest request) {
		if (user == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		User loginUser = getLoginUser(request);
		if (!loginUser.getId().equals(user.getId())) {
			throw new MyCustomException(ErrorCode.NO_AUTH);
		}
		int result = userMapper.updateById(user);
		return result > 0;
	}

	@Override
	public List<UserVO> search(String keyword) {
		// note 1. == null 只是null
		// note 2. isEmpty 还能判读是否长度为0
		// note 3. isBlank 还能过滤空格
		if (StringUtils.isEmpty(keyword)) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		QueryWrapper<User> wrapper = new QueryWrapper<>();
		wrapper.like("nickname", keyword)
				.or()
				.like("introduction", keyword);
		wrapper.last("limit 1000"); // feat 限定100条
		List<User> userList = userMapper.selectList(wrapper);
		if (userList.isEmpty()) {
			return Collections.emptyList();
		}

		// 给每个用户设置标签列表(N+1写法)
//				List<UserVO> userVOList = userList.stream()
//						.map(user -> {
//							UserVO userVO = userConvert.userToUserVO(user);
//							List<Tag> tags = tagMapper.selectTagsByUserId(user.getId());
//							List<String> tagsName = tags.stream().map(Tag::getName).toList();
//							userVO.setTags(tagsName);
//							return userVO;
//						})
//						.toList();
//				if (userVOList.isEmpty()) {
//					return Collections.emptyList(); // 好的
//				}

		// 1. 先收集所有id到内存当中
		List<Long> userIdList = userList.stream().map(User::getId).toList();
		// 2. 查询出所有id对应的tag
		List<Map<String, Object>> tagList = tagMapper.selectTagsByUserIdList(userIdList);
		// 3. 将查出来的tagName进行分组
		Map<Long, List<String>> tagsByUserId =
				tagList.stream()
						.collect(Collectors.groupingBy(
								row -> ((Number) row.get("user_id")).longValue(),
								Collectors.mapping(row -> (String) row.get("name"), Collectors.toList())
						));
		// 4. 组装结果,将tags填入userList
		List<UserVO> userVOList = userList.stream().map(user -> {
			UserVO userVO = userConvert.userToUserVO(user);
			List<String> tags = tagsByUserId.getOrDefault(userVO.getId(), Collections.emptyList());
			userVO.setTags(tags);
			return userVO;
		}).toList();
		return userVOList;
	}

	@Override
	public List<UserVO> searchByTags(List<String> tagList) {
		// think 什么都不都就不行吗
		// todo 改一下这两个搜索的逻辑
		if (CollectionUtils.isEmpty(tagList)) {
			return Collections.emptyList();
		}
		List<User> users = userMapper.searchByTags(tagList, tagList.size());
		if (CollectionUtils.isEmpty(users)) {
			return Collections.emptyList();
		}
		// 给每个用户设置标签列表
		List<UserVO> userVOList = users.stream()
				.map(user -> {
					UserVO userVO = userConvert.userToUserVO(user);
					List<Tag> userTagList = tagMapper.selectTagsByUserId(user.getId());
					userVO.setTags(userTagList.stream().map(Tag::getName).toList());
					return userVO;
				})
				.collect(Collectors.toList());
		// 其实只是我不想看到报黄罢了
		if (CollectionUtils.isEmpty(userVOList)) {
			return Collections.emptyList();
		}
		return userVOList;
	}

	@Override
	public Page<UserVO> recommend(int pageNum, int pageSize, HttpServletRequest request) {
		User loginUser = getLoginUser(request);
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}
		Long userId = loginUser.getId();
		return recommendByUserId(pageNum, pageSize, userId);
	}

	@Override
	public Page<UserVO> recommendByUserId(int pageNum, int pageSize, Long loginUserId) {
		// 1. 获取当前登录用户的所有标签
		List<Tag> myTagsList = tagMapper.selectTagsByUserId(loginUserId);
		List<String> myTags = myTagsList.stream().map(Tag::getName).toList();

		// 2. 查询所有其他用户（最多5000条）
		QueryWrapper<User> queryWrapper = new QueryWrapper<>();
		queryWrapper.ne("id", loginUserId); // 排除自己
		queryWrapper.last("limit 5000");
		List<User> allUser = this.list(queryWrapper);

		if (CollectionUtils.isEmpty(allUser)) {
			Page<UserVO> emptyPage = new Page<>(pageNum, pageSize, 0);
			emptyPage.setRecords(Collections.emptyList());
			return emptyPage;
		}

		// 3.1 收集所有用户ID
		List<Long> allUserIds = allUser.stream().map(User::getId).toList();
		// 3.2 一次SQL查询出所有用户的所有标签
		List<Map<String, Object>> allTags = tagMapper.selectTagsByUserIdList(allUserIds);
		// 3.3 按 userId 分组，得到 用户id | 标签列表
		Map<Long, List<String>> tagsByUserId = allTags.stream()
				.collect(Collectors.groupingBy(
						row -> ((Number) row.get("user_id")).longValue(),
						Collectors.mapping(row -> (String) row.get("name"), Collectors.toList())
				));


		// 4. 给每个用户计算相似度，排序（距离越小越相似放前面）
		record UserWithSimilarity(User user, int distance) implements Comparable<UserWithSimilarity> {
			@Override
			public int compareTo(UserWithSimilarity o) {
				// 升序：距离小的排前面（更相似）
				return Integer.compare(this.distance, o.distance);
			}
		}

		List<UserWithSimilarity> userWithSimilarityList = allUser.stream()
				.map(user -> {
					// 从内存分组拿标签，不用再查DB
					List<String> userTagNameList = tagsByUserId.getOrDefault(user.getId(), Collections.emptyList());
					if (myTags.isEmpty() || userTagNameList.isEmpty()) {
						// 如果都没标签，给个最大距离，排最后
						return new UserWithSimilarity(user, Integer.MAX_VALUE);
					}
					int distance = EditDistanceUtils.calculateTotalDistance(myTags, userTagNameList);
					return new UserWithSimilarity(user, distance);
				})
				.sorted()
				.toList();

		// 5. 转换为 UserVO 返回
		int fromIndex = (pageNum - 1) * pageSize;
		int toIndex = Math.min(fromIndex + pageSize, userWithSimilarityList.size());
		List<UserVO> userVOList = userWithSimilarityList
				.subList(fromIndex, toIndex)
				.stream()
				.map(UserWithSimilarity::user)
				.map(user -> {
					UserVO userVO = safeUser(user);
					// 还是从内存拿标签，不用再查DB
					List<String> tagList = tagsByUserId.getOrDefault(user.getId(), Collections.emptyList());
					userVO.setTags(tagList);
					return userVO;
				})
				.toList();
		Page<UserVO> resultPage = new Page<>(pageNum, pageSize, userWithSimilarityList.size());
		resultPage.setRecords(userVOList);

		return resultPage;
	}

	/**
	 * 检查输入是否合规
	 */
	public boolean validAccount(String account, String password, String checkPassword) {
		// 1. 校验基本参数信息
		if (StringUtils.isAnyBlank(account, password, checkPassword)) {
			throw new MyCustomException(ErrorCode.NULL_ERROR, "有字段为空");
		}
		if (account.length() < 6 || account.length() > 14) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "账户长度不符合要求");
		}
		if (password.length() < 6) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "密码长度过短");
		}
		if (!checkPassword.equals(password)) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "两次输入密码不相同");
		}

		// 2. 账户是否符合规则 字母开头,6到14位
		Matcher matcher = ACCOUNT_PATTERN.matcher(account);
		if (!matcher.matches()) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "账户名称无效");
		}
		return true;
	}

	@Override
	public UserVO safeUser(User originalUser) {
		return userConvert.userToUserVO(originalUser);
	}

	public User getLoginUser(HttpServletRequest request) {
		// think request可不会为null,哈哈,还真是
		if (request == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		// note 说实话只改了这里就成功了,说明什么,就是这个问题,不能用int
		//Long userId = (Long)request.getSession().getAttribute(USER_LOGIN_STATE);

		// 1. 从 Session 获取基础信息
		HttpSession session = request.getSession();
		UserVO userVO = (UserVO) session.getAttribute(USER_LOGIN_STATE);
		String sessionToken = (String) session.getAttribute(TOKEN);

		if (userVO == null || sessionToken == null) {
			throw new MyCustomException(ErrorCode.N0T_L0GIN, "未登录");
		}

		// 2. 【核心】从 Redis 获取该用户当前全国唯一的合法 Token
		String redisTokenKey = TOKEN + userVO.getId();
		// String latestToken = (String) redisTemplate.opsForValue().get(redisTokenKey);
		// feat 改成从map中取 think 前一个是map名字,后一个是字段名字 以前是map.get() 现在map也在里面了
		String latestToken = (String) redisTemplate.opsForHash().get(redisTokenKey, "token");

		// 3. 比对：如果 Session 里的 Token 跟 Redis 里的不一致，说明后面有人登录了
		if (!sessionToken.equals(latestToken)) {
			session.invalidate(); // 顺手清理掉这个过时的 Session
			throw new MyCustomException(ErrorCode.N0T_L0GIN, "账号已在别处登录，请重新登录");
		}

		// 4. 自动续期：如果校验通过，给 Redis 的 Token 续命（防止用户操作着突然掉线）
		redisTemplate.expire(redisTokenKey, 30, TimeUnit.MINUTES);

		// 5. 返回数据库最新信息（防止用户改了头像/昵称，Session 里还是旧的）
		return userMapper.selectById(userVO.getId());
	}

}
