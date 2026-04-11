package com.xz.match.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xz.match.common.ErrorCode;
import com.xz.match.mapper.TagMapper;
import com.xz.match.model.constant.TeamStatusEnum;
import com.xz.match.model.entity.Tag;
import com.xz.match.model.entity.Team;
import com.xz.match.model.entity.User;
import com.xz.match.model.entity.UserTeam;
import com.xz.match.model.request.team.JoinTeamRequest;
import com.xz.match.model.request.team.QueryTeamRequest;
import com.xz.match.model.vo.TeamVO;
import com.xz.match.model.vo.UserVO;
import com.xz.match.exception.MyCustomException;
import com.xz.match.job.convert.TeamConvert;
import com.xz.match.job.convert.UserConvert;
import com.xz.match.service.TeamService;
import com.xz.match.mapper.TeamMapper;
import com.xz.match.service.UserService;
import com.xz.match.service.UserTeamService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@SuppressWarnings("all")
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
		implements TeamService {

	@Resource
	private UserService userService;

	@Resource
	private UserTeamService userTeamService;

	@Resource
	private UserConvert userConvert;

	@Resource
	private TeamConvert teamConvert;

	@Resource
	private RedissonClient redissonClient;
	@Autowired
	private TagMapper tagMapper;

	@Override
	// note spring事务只对error和runTimeException回滚,对Checked Exception(编译异常不回滚)
	// note 为什么不直接Throwable.class 因为出现error的时候已经不是你能控制回滚不回滚了,jvm大概率崩了
	@Transactional(rollbackFor = Exception.class)
	public Long addTeam(Team team, User loginUser) {
		// 1. 权限校验
		if (team == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.N0T_L0GIN);
		}
		//name; description; maxNum; expireTime; status; password;
		String name = team.getName();
		if (StringUtils.isBlank(name) || name.length() > 10) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "未命名或者长度大于10");
		}
		String description = team.getDescription();
		if (StringUtils.isNotBlank(description) && description.length() > 100) { // 如果不为空才进入
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "队伍描述错误为空或者描述长度过长");
		}
		int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
		if (maxNum < 5 || maxNum > 20) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "队伍人数设置错误,应在5-20人");
		}
		Date expireTime = team.getExpireTime();
		if (expireTime == null || (new Date().compareTo(expireTime)) > 0) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "过期时间未设置或过期时间小于当前时间");
		}
		// feat 限制最长天数
		final int MAX_EXPIRE_DAYS = 90;
		long maxExpireTimeMillis = System.currentTimeMillis()
				+ (long) MAX_EXPIRE_DAYS * 24 * 60 * 60 * 1000;
		Date maxExpireDate = new Date(maxExpireTimeMillis);
		if (expireTime.after(maxExpireDate)) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "过期时间最长不能超过" + MAX_EXPIRE_DAYS + "天");
		}
		TeamStatusEnum status = team.getStatus();
		String password = team.getPassword();
		// note 常量放在前面,枚举类建议用 == 如果两个枚举类本身就不是同一个,== 会直接报错,而equals是接受object
		if (TeamStatusEnum.SECRET.equals(status)) {
			if (StringUtils.isBlank(password) || password.length() > 32) {
				throw new MyCustomException(ErrorCode.PARAMS_ERROR, "未设置密码或者密码长度过长");
			}
		}

		// 2. 判断是否创建队伍到达上限
		QueryWrapper<Team> wrapper = new QueryWrapper<>();
		Long userId = loginUser.getId();
		wrapper.eq("user_id", userId);
		// userTeam表只是,一个队伍id对应多个用户id,查不到用户创建多少个队伍,只能看用户加了多少队伍
		long hasTeamCount = this.count(wrapper);
		if (hasTeamCount >= 5) {
			throw new MyCustomException(ErrorCode.NO_AUTH, "创建队伍数量已经达到上限");
		}

		// 3 . 添加队伍信息在队伍表中
		team.setId(null); // 确保自增
		team.setUserId(userId);
		boolean save = this.save(team);
		Long teamId = team.getId();
		if (!save || teamId == null) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR, "队伍创建失败");
		}

		// 4. 队伍关系表中添加信息
		UserTeam userTeam = new UserTeam();
		userTeam.setUserId(userId);
		userTeam.setTeamId(teamId);
		boolean saved = userTeamService.save(userTeam);
		if (!saved) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR, "队伍创建失败,关系表创建失败");
		}
		return teamId;
	}

	@Override
	@Transactional(rollbackFor = Exception.class) // note 涉及两张表,加上事务
	public Boolean deleteTeam(Long teamId, User user) {
		// 1. 判空
		if (teamId == null || teamId <= 0) {
			throw new MyCustomException(ErrorCode.NULL_ERROR);
		}

		// 2. 判断队伍是否存在并且校验权限
		Team team = this.getById(teamId);
		if (!(team.getUserId().equals(user.getId()))) {
			throw new MyCustomException(ErrorCode.NO_AUTH, "这不是你的队伍,你怎么找到这的");
		}

		// 3. 先删除队伍关系表,再删除队伍表
		QueryWrapper<UserTeam> wrapper = new QueryWrapper<>();
		wrapper.eq("team_id", teamId);
		boolean result = userTeamService.remove(wrapper);
		if (!result) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
		}
		return removeById(teamId);
	}

	//自己写的update
	//	@Override
	//	public Boolean updateTeam(Team newTeam, User loginUser) {
	//		// 1. 判空
	//		if (newTeam == null){
	//			throw new MyCustomException(ErrorCode.NULL_ERROR);
	//		}
	//
	//		// 2. 检验队伍的更新是否合规
	//		//name; description;
	//		//expireTime; status; password;
	//		String name = newTeam.getName();
	//		if (StringUtils.isBlank(name) || name.length() > 10){
	//			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "未命名或者长度大于10");
	//		}
	//		String description = newTeam.getDescription();
	//		if (StringUtils.isNotBlank(description) && description.length() > 100){
	//			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "队伍描述错误为空或者描述长度过长");
	//		}
	//		Date expireTime = newTeam.getExpireTime();
	//		if (expireTime == null || (new Date().compareTo(expireTime)) > 0){
	//			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "过期时间未设置或过期时间小于当前时间");
	//		}
	//		TeamStatusEnum status = newTeam.getStatus();
	//		String password = newTeam.getPassword();
	//		if (TeamStatusEnum.SECRET.equals(status)){
	//			if (StringUtils.isBlank(password) ||  password.length() > 32){
	//				throw new MyCustomException(ErrorCode.PARAMS_ERROR, "未设置密码或者密码长度过长");
	//			}
	//		}
	//
	//		// 3. 校验权限
	//		Team team = this.getById(newTeam.getId());
	//		if (!(team.getUserId().equals(loginUser.getId()))){
	//			throw new MyCustomException(ErrorCode.NO_AUTH, "不准改!!!");
	//		}
	//
	//		// 4. 状态的改动? 已知只有加密需要密码,其他
	//		// 其他改加密 : 上密码
	//		// 加密改其他 : 密码还是会保留,所以这里得进行删除?
	//		TeamStatusEnum oldTeamStatus = team.getStatus();
	//		if (TeamStatusEnum.SECRET.equals(oldTeamStatus) && !(TeamStatusEnum.SECRET.equals(status))){
	//			newTeam.setPassword(null);
	//		}
	//
	//		return updateById(newTeam); // 为什么叫ById却传入实体类,真奇怪
	//	}

	@Override
	public Boolean updateTeam(Team newTeam, User loginUser) {
		// 1. 基础判空
		if (newTeam == null || newTeam.getId() == null || newTeam.getId() <= 0) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}

		// 2. 核心：先查出旧数据（鉴权 + 拿旧状态）
		Team oldTeam = this.getById(newTeam.getId());
		if (oldTeam == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR, "队伍不存在");
		}
		// 创建者可以修改
		if (!oldTeam.getUserId().equals(loginUser.getId())) {
			throw new MyCustomException(ErrorCode.NO_AUTH);
		}

		// 3. 参数校验
		// think 这里的校验规则和add不完全一样,这里基本都可以为空,但不能全部为空
		String name = newTeam.getName();
		if (name != null && name.length() > 10) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "未命名或者长度大于10");
		}
		String description = newTeam.getDescription();
		if (description != null && description.length() > 100) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "队伍描述错误为空或者描述长度过长");
		}
		Date expireTime = newTeam.getExpireTime();
		if (expireTime != null && new Date().compareTo(expireTime) > 0) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "过期时间小于当前时间");
		}
		TeamStatusEnum status = newTeam.getStatus();
		String password = newTeam.getPassword();
		if (TeamStatusEnum.SECRET.equals(status)) {
			if (StringUtils.isBlank(password) || password.length() > 32) {
				throw new MyCustomException(ErrorCode.PARAMS_ERROR, "未设置密码或者密码长度过长");
			}
		}

		// 4. 状态与密码的联动处理 (重点)
		// todo 如果加密改成非加密,但是又传入了密码,因为没有校验,密码可以无敌长
		// todo 就让你无限长
		TeamStatusEnum newStatus = newTeam.getStatus();
		// 如果状态改为了加密，必须有密码
		if (TeamStatusEnum.SECRET.equals(newStatus)) {
			if (StringUtils.isBlank(newTeam.getPassword()) && StringUtils.isBlank(oldTeam.getPassword())) {
				throw new MyCustomException(ErrorCode.PARAMS_ERROR, "加密房间必须设置密码");
			}
		}

		UpdateWrapper<Team> wrapper = new UpdateWrapper<>();
		wrapper.eq("id", newTeam.getId());
		// 5. 处理密码清空：如果从加密转为非加密，显式清空密码
		// != 处理并没有意愿改状态的请求,因为默认就这样
		// note 如果进入这里,就已经更新过了一次数据库,所以走不到后面update方法
		if (TeamStatusEnum.SECRET.equals(oldTeam.getStatus()) && newStatus != null && !TeamStatusEnum.SECRET.equals(newStatus)) {
			wrapper.set("password", "");
		}

		// think 遇见问题,打败问题
		return this.update(newTeam, wrapper);
	}

	@Override
	public List<TeamVO> searchTeam(String name) {
		// 1. 判空
		if (StringUtils.isBlank(name)) {
			throw new MyCustomException(ErrorCode.NULL_ERROR, "队伍名称为空");
		}

		// 2. like模糊匹配,并且限制返回数量
		QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
		queryWrapper.like("name", name).last("limit 20").orderByAsc("id");
		List<Team> teamList = this.list(queryWrapper);
		if (CollectionUtils.isEmpty(teamList)) {
			return Collections.emptyList();
		}

		// 3. 转化
		// think 总是你出问题 [怒][怒][怒]
		// think 好像是我忘记加data注解了
		// think 抱歉(还得clean一下才能用
		return teamConvert.teamListToTeamVOList(teamList);
	}

	// todo 人数还有点问题
	@Override
	public List<TeamVO> listTeamByCondition(QueryTeamRequest queryTeamRequest, User loginUser) {
		// 1. 判空
		if (queryTeamRequest == null) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}

		// todo 2. 权限校验(检验用户)

		// 3. 进行qw动态拼接
		QueryWrapper<Team> wrapper = new QueryWrapper<>();
		// String name;
		String name = queryTeamRequest.getName();
		if (StringUtils.isNotBlank(name)) {
			wrapper.like("name", name);
		}
		// String description;
		String description = queryTeamRequest.getDescription();
		if (StringUtils.isNotBlank(description)) {
			wrapper.like("description", description);
		}
		// Long userId;
		Long userId = queryTeamRequest.getUserId();
		if (userId != null && userId > 0) {
			wrapper.eq("user_id", userId);
		}
		// Integer status;
		// think 这里只是查询,对应数据库,不需要进行转化
		Integer status = queryTeamRequest.getStatus();
		if (status != null) {
			TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
			if (statusEnum == null) {
				throw new MyCustomException(ErrorCode.PARAMS_ERROR);
			}
			wrapper.eq("status", status);
		}
		// Integer minMaxNum;Integer maxMaxNum;
		Integer minMaxNum = queryTeamRequest.getMinMaxNum();
		Integer maxMaxNum = queryTeamRequest.getMaxMaxNum();
		// think 传一个,两个,两个都不合规
		if (minMaxNum != null && maxMaxNum != null && minMaxNum > maxMaxNum) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}
		if (minMaxNum != null && minMaxNum > 0) {
			wrapper.ge("max_num", minMaxNum);
		}
		if (maxMaxNum != null && maxMaxNum > 0) {
			wrapper.le("max_num", maxMaxNum);
		}

		// private Date expireTime;
		// feat 不展示已经过期的队伍
		wrapper.and(qw -> qw.gt("expire_time", new Date()).or().isNull("expire_time"));

		// 4. 权限过滤
		if (loginUser == null) {
			wrapper.eq("status", TeamStatusEnum.PUBLIC);
		} else {
			//	这段代码的业务逻辑是：用户只能看到以下团队：
			//	- 所有公开的团队 ✓
			//	- 所有秘密/加密的团队 ✓
			//	- 自己创建的私有团队 ✓
			//	- 别人创建的私有团队对你不可见 ✗
			// todo 看不懂
			// 这里根据业务需求调整，本示例：私有的只有自己能看，加密的都能搜到但需要密码才能加入
			wrapper.and(queryWrapper -> queryWrapper
					.eq("status", TeamStatusEnum.PUBLIC)
					.or()
					.eq("status", TeamStatusEnum.SECRET)
					.or(w -> w.eq("status", TeamStatusEnum.PRIVATE).eq("user_id", loginUser.getId()))
			);
		}

		// 5. 查询数据库
		List<Team> teamList = this.list(wrapper);
		if (CollectionUtils.isEmpty(teamList)) {
			return Collections.emptyList();
		}

		// 6. 插入创建者用户信息
		ArrayList<TeamVO> teamVOList = new ArrayList<>();
		for (Team team : teamList) {
			TeamVO teamVO = teamConvert.teamToTeamVO(team);
			Long createId = teamVO.getUserId();
			if (createId != null) {
				User createUser = userService.getById(createId);
				UserVO userVO = userConvert.userToUserVO(createUser);
				teamVO.setUserVO(userVO);
			}
			teamVOList.add(teamVO);
		}

		// sup 这是将controller中的逻辑下沉到service层
		// 7. 校验当前用户是否已经加入队伍(当前用户加了哪些队伍)
		// think 通过teamId拿到队伍关系表,获取userId集合,判断loginUser.getId是否在其中
		// think select team_id from user_team where user_id = ? and team_id in ( team_id list)
		ArrayList<Long> queryTeamIdList = new ArrayList<>();
		for (TeamVO teamVO : teamVOList) {
			Long id = teamVO.getId();
			// think teamId集合
			queryTeamIdList.add(id);
		}
		// think 通过user_team表获取单个用户所对应的teamId集合
		Set<Long> set = hasJoinTeamId(queryTeamIdList, loginUser);
		teamVOList.forEach(teamVO -> {
			// think 判断所查询到的队伍中哪些队伍包含用户所加入队伍
			boolean hasJoin = set.contains(teamVO.getId());
			teamVO.setHasJoin(hasJoin);
		});

		// 8. 查询当前队伍的人数
		QueryWrapper<UserTeam> hasJoinNumWrapper = new QueryWrapper<>();
		hasJoinNumWrapper.in("team_id", queryTeamIdList);
		// think 依据所查询到的teamId集合获取所有加入这些队伍的用户(用户关系表)
		List<UserTeam> userTeamList = userTeamService.list(hasJoinNumWrapper);
		// todo 这是静态方法?
		// think 按照teamId进行分组,每个队伍有多少人userTeam.getTeamId,分完组后teamId就唯一了
		Map<Long, List<UserTeam>> teamIdUserTeamMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
		teamVOList.forEach(teamVO -> {
			// think 这个map是Long , List.通过get key, 得到value(List), 再获取size
			// note get or default
			int size = teamIdUserTeamMap.getOrDefault(teamVO.getId(), Collections.emptyList()).size();
			teamVO.setHasJoinNum(size);
		});

		// 9. 返回限制人数
		if (teamVOList.size() > 20) {
			teamVOList.subList(0, 20);
		}

		return teamVOList;
	}

	// note 传入list开销更小,一次性批量查询. eq每次都要网络往返交互
	@Override
	public Set<Long> hasJoinTeamId(List<Long> queryTeamIdList, User loginUser) {
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.NO_AUTH);
		}
		QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("user_id", loginUser.getId());
		queryWrapper.in("team_id", queryTeamIdList);
		List<UserTeam> hasJoinTeamIdList = userTeamService.list(queryWrapper);
		Set<Long> hasJoinTeamIdSet = hasJoinTeamIdList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
		if (CollectionUtils.isEmpty(hasJoinTeamIdSet)) {
			return Collections.emptySet();
		}
		return hasJoinTeamIdSet;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Boolean joinTeam(JoinTeamRequest joinTeamRequest, User loginUser) {
		// 判空
		// 该队伍是什么状态
		// 队伍是否过期 think 都不显示过期队伍这里还要校验吗
		// 通过teamId,将userId插入user_team表中
		if (joinTeamRequest == null || loginUser == null) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}
		// 1. 获取队伍
		Team team = this.getById(joinTeamRequest.getTeamId());
		if (team == null) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "该队伍不存在");
		}

		// 2. 获取当前队伍人数
		QueryWrapper<UserTeam> hasJoinNumWrapper = new QueryWrapper<>();
		hasJoinNumWrapper.eq("team_id", joinTeamRequest.getTeamId());
		long joinNum = userTeamService.count(hasJoinNumWrapper);
		if (joinNum < 0 || joinNum >= team.getMaxNum()) {
			throw new MyCustomException(ErrorCode.REPEAT_DATA, "队伍人数已满或者队伍不存在");
		}

		// 3. Team状态
		TeamStatusEnum teamStatus = team.getStatus();
		String password = joinTeamRequest.getPassword();
		if (TeamStatusEnum.PRIVATE.equals(teamStatus)) {
			throw new MyCustomException(ErrorCode.NO_AUTH, "不能加入私有队伍");
		}
		if (TeamStatusEnum.SECRET.equals(teamStatus)) {
			String oldPassword = team.getPassword();
			if (StringUtils.isBlank(oldPassword) || !oldPassword.equals(password)) {
				throw new MyCustomException(ErrorCode.PARAMS_ERROR, "密码错误");
			}
		}

		// 4. 查询Team过期时间
		Date expireTime = team.getExpireTime();
		if (expireTime != null && new Date().compareTo(expireTime) > 0) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "队伍已过期");
		}

		// 5. 分布式锁 允许不同队伍可以并行
		RLock lock = redissonClient.getLock("partner:join:" + joinTeamRequest.getTeamId());
		// note 尝试获取锁，最多等待3秒，锁10秒自动过期
		boolean isLocked;
		try {
			isLocked = lock.tryLock(3, 10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
		}
		if (!isLocked) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
		}
		try {
			// 6. 查询用户加入队伍的数量
			QueryWrapper<UserTeam> countWrapper = new QueryWrapper<>();
			Long userId = loginUser.getId();
			countWrapper.eq("user_id", userId);
			long joinedCount = userTeamService.count(countWrapper);
			if (joinedCount >= 5) {
				throw new MyCustomException(ErrorCode.PARAMS_ERROR, "最多只能加入 5 个队伍");
			}
			// 7. 查询用户是否已经加入该队伍
			//UserTeam userTeam = userTeamService.getById(joinTeamRequest.getTeamId()); // think 这个需要原生id,不是teamId
			QueryWrapper<UserTeam> hasJoinWrapper = new QueryWrapper<>();
			hasJoinWrapper.eq("user_id", userId);
			// note 加上eq teamId 判断返回行数是否大于0, 不用在内存中遍历
			hasJoinWrapper.eq("team_id", joinTeamRequest.getTeamId());
			long count = userTeamService.count(hasJoinWrapper);
			if (count > 0) {
				throw new MyCustomException(ErrorCode.REPEAT_DATA, "已加入当前队伍");
			}
			// 8. 执行插入操作,已经确保了user_team表中没有这样的数据
			UserTeam userTeam = new UserTeam();
			userTeam.setUserId(userId);
			userTeam.setTeamId(joinTeamRequest.getTeamId());
			userTeam.setJoinTime(new Date());
			return userTeamService.save(userTeam);
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class) // note 设计多张表
	public Boolean quitTeam(Long teamId, User loginUser) {
		// 1. 判空
		if (teamId == null) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR);
		}
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.NO_AUTH);
		}
		// think 在执行删除之前还得进行很多校验,让我想想,从数据库字段下手
		// think 要考虑是队长吗,但是队长不应该提供删除接口吗,你能点击删除
		// think 前提是这个队伍中有你, and ,你不是队长, and,
		// think 其实就是想偷懒少写一点, 还是得完全考虑
		Long userId = loginUser.getId();

		// 2. 队伍是否存在
		Team team = this.getById(teamId);
		if (team == null) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "当前队伍不存在");
		}

		// 3. 判断当前用户是否在队伍中
		QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("user_id", userId);
		queryWrapper.eq("team_id", teamId);
		UserTeam inTeam = userTeamService.getOne(queryWrapper);
		if (inTeam == null) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "未加入当前队伍");
		}

		// note 下面的逻辑重构了,之前是if else 然后if else
		// 4. 当前用户是否为队长
		QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
		teamQueryWrapper.eq("id", teamId);
		teamQueryWrapper.eq("user_id", userId);
		Team one = this.getOne(teamQueryWrapper);
		if (one == null) {
			// 不是队长,可以直接退出 note qw可以复用
			// 如果是队长也可以直接退出,哈哈
			return userTeamService.remove(queryWrapper);
		}
		// 5. 是队长, 查询人数
		QueryWrapper<UserTeam> countWrapper = new QueryWrapper<>();
		countWrapper.eq("team_id", teamId);
		long count = userTeamService.count(countWrapper);
		if (count == 1) {
			// 只有一个人,删除也就是解散了,外加删除队伍
			userTeamService.remove(queryWrapper);
			return this.removeById(teamId);
		}

		// 6. 不是只有一个人, 进行转让, 并且删除关联
		// not equals 排除 last 最近一人 order 按照时间排序(的最近一人)
		countWrapper.ne("user_id", userId);
		countWrapper.last("limit 1");
		countWrapper.orderByAsc("join_time");
		List<UserTeam> userTeamList = userTeamService.list(countWrapper);
		if (CollectionUtils.isEmpty(userTeamList)) {
			throw new MyCustomException(ErrorCode.SYSTEM_ERROR, "转移队长失败");
		}
		Long newCaptainId = userTeamList.get(0).getUserId();
		team.setUserId(newCaptainId);
		this.updateById(team);
		return userTeamService.remove(queryWrapper);
	}

	@Override
	public List<TeamVO> myTeam(User loginUser) {
		// 1. 判空
		if (loginUser == null) {
			throw new MyCustomException(ErrorCode.NO_AUTH);
		}

		// 2. 查询所有和自己有关的队伍关系,现在还只是user_team表
		QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("user_id", loginUser.getId());
		List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
		if (CollectionUtils.isEmpty(userTeamList)) {
			return Collections.emptyList();
		}

		// 3. 所有队伍 think 不用强行使用已有方法,复用逻辑就行
		List<Long> teamIdList = userTeamList.stream().map(UserTeam::getTeamId).toList();
		QueryWrapper<Team> teamQuery = new QueryWrapper<>();
		teamQuery.in("id", teamIdList);
		List<Team> teamList = this.list(teamQuery);
		if (CollectionUtils.isEmpty(teamList)) {
			return Collections.emptyList();
		}

		// 4.1 转化为VO,填充创建者信息
		ArrayList<TeamVO> teamVOList = new ArrayList<>();
		for (Team team : teamList) {
			TeamVO teamVO = teamConvert.teamToTeamVO(team);
			Long createId = teamVO.getUserId();
			if (createId != null) {
				User createUser = userService.getById(createId);
				UserVO userVO = userConvert.userToUserVO(createUser);
				teamVO.setUserVO(userVO);
			}
			// think is去哪了?
			// teamVO.setCreator(loginUser.getId().equals(createId));
			// 注意：Long是对象，要用equals比较值，==比较引用会错
			teamVO.setCreator(Objects.equals(loginUser.getId(), createId));
			teamVOList.add(teamVO);
		}

		// 4.2 填充hasJoin标记（我肯定都加入了）think 没用也填充,填充也没用
		Set<Long> myTeamIdSet = new HashSet<>(teamIdList);
		teamVOList.forEach(teamVO ->
				teamVO.setHasJoin(myTeamIdSet.contains(teamVO.getId()))
		);

		// 5. 判断队伍人数
		QueryWrapper<UserTeam> hasJoinNumWrapper = new QueryWrapper<>();
		hasJoinNumWrapper.in("team_id", teamIdList);
		List<UserTeam> hasJoinNumList = userTeamService.list(hasJoinNumWrapper);
		// map就是将lambda表达式中的get当做key,本身当做value,分组也有一样的效果
		Map<Long, List<UserTeam>> teamIdUserTeamMap = hasJoinNumList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
		// 需要填充的队伍遍历
		teamVOList.forEach(teamVO -> {
			int size = teamIdUserTeamMap.getOrDefault(teamVO.getId(), Collections.emptyList()).size();
			teamVO.setHasJoinNum(size);
		});

		return teamVOList;
	}

	@Override
	public List<UserVO> TeamInfo(Long teamId) {
		// 1. 判空
		if (teamId == null) {
			throw new MyCustomException(ErrorCode.NULL_ERROR, "请求参数为空");
		}

		// 2. 查询所有用户
		QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("team_id", teamId);
		List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
		if (CollectionUtils.isEmpty(userTeamList)) {
			throw new MyCustomException(ErrorCode.PARAMS_ERROR, "队伍不存在");
		}
		// 给用户上标签
		List<UserVO> userVOList = userTeamList.stream().map(userTeam -> {
			Long userId = userTeam.getUserId();
			User user = userService.getById(userId);
			UserVO userVO = userConvert.userToUserVO(user);
			userVO.setTags(tagMapper.selectTagsByUserId(userId).stream().map(Tag::getName).toList());
			return userVO;
		}).toList();

		// 3. 有队伍肯定就有人,直接返回
		return userVOList;
	}
}




