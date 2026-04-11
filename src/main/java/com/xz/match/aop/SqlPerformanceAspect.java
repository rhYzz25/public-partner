package com.xz.match.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class SqlPerformanceAspect {
	// 切所有 mapper 方法
	@Around("execution(* com.xz.match.mapper.*.*(..))")
	public Object around(ProceedingJoinPoint point) throws
			Throwable {
		long start = System.currentTimeMillis();
		Object result = point.proceed();
		long time = System.currentTimeMillis() - start;

		// 超过 500ms 记警告，方便你优化
		if (time > 500) {
			log.warn("🚀 慢查询 detected: {} 耗时: {}ms",
					point.getSignature().getName(), time);
		} else {
			log.debug("SQL执行: {} 耗时: {}ms",
					point.getSignature().getName(), time);
		}
		return result;
	}
}