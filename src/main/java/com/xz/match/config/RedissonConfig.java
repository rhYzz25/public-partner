package com.xz.match.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis") // note 会根据profiles来找
public class RedissonConfig {
	// 创建对象是通过反射,你这里确实可以自己调用
	// 但是ioc不会这样
	private String host;
	private int port;
	private int database;
	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();
		config.useSingleServer()
				// use "redis://" for Redis connection
				// use "val key://" for Val key connection
				// use "val keys://" for Val key SSL connection
				// use "rediss://" for Redis SSL connection
				.setAddress(String.format("redis://%s:%s", host, port)).setDatabase(database);
		return Redisson.create(config);
	}
}