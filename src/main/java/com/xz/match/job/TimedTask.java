package com.xz.match.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xz.match.model.entity.User;
import com.xz.match.model.vo.UserVO;
import com.xz.match.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TimedTask {

	@Resource
	private UserService userService;

	@Resource
	private RedisTemplate<String, Object> redisTemplate;

	@Resource
	private RedissonClient redissonClient;

	// 定时推送用户,并且设置内存锁,只让一台服务器执行
	// 推送个蛋,刚发现,推送的用户和接受的用户是同一批
	// note 由此引入redisson
	// think 但是貌似根本就没有分布式化
	@Scheduled(cron = "0 0 2 * * *")
	public void job() {
		RLock lock = redissonClient.getLock("user:recommend:lock");
		// lock.lock(); 无限等待锁
		try {
			if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) { // 尝试非阻塞获取锁
				log.info("开始预热用户推荐缓存");
				int pageNum = 1;
				int pageSize = 20;
				while (true) {
					QueryWrapper<User> queryWrapper = new QueryWrapper<>();
					// 查询所有用户并分页
					Page<User> page = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
					if (page.getRecords().isEmpty()) {
						break;
					}
					for (User user : page.getRecords()) {
						try {
							Long userId = user.getId();
							Page<UserVO> userVOPage = userService.recommendByUserId(1, 10, userId);
							String key = String.format("user:recommend:%s", userId);
							redisTemplate.opsForValue().set(key, userVOPage, 28800, TimeUnit.SECONDS);
							log.info("缓存成功");
						} catch (Exception e) {
							log.error("预热用户失败, 用户id = {}", user.getId(), e);
						}
					}
					if (page.getCurrent() >= page.getPages()) {
						break;
					}
				}
			} else {
				log.info("获取锁失败,已被其他节点执行,跳过本次预热");
			}
		} catch (InterruptedException e) {
			log.info("缓存失败");
		} finally {
			if (lock.isLocked() && lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}
}
