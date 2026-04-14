package com.xz.match.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisTemplateConfig {
	// 这样就可以返回属于自己的redisTemplate对象?
	// springboot不就是这样吗,很多默认的规则再加上一些自定义
	// 在这里修改RedisTemplate,直接resource的对象没有kv设定
	// 这个应该是Redis本身需要的依赖
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(connectionFactory);
		// note 分开设置key value 的值
		redisTemplate.setKeySerializer(RedisSerializer.string());
		redisTemplate.setValueSerializer(RedisSerializer.json()); // 默认使用jdk的序列化器
		return redisTemplate;
	}

}