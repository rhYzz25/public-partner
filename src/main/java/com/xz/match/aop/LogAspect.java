package com.xz.match.aop;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class LogAspect {
	// 切点：所有 controller 包下的所有 public 方法
	@Pointcut("execution(public * com.xz.match.controller..*.*(..))")
	public void webLog() {
	}

	@Around("webLog()")
	public Object doAround(ProceedingJoinPoint point) throws Throwable {
		long startTime = System.currentTimeMillis();

		// 获取请求信息
		ServletRequestAttributes attributes =
				(ServletRequestAttributes)
						RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = null;
		if (attributes != null) {
			request = attributes.getRequest();
		}

		// 打印日志
		log.info("=== 请求开始 ===");
		if (request != null) {
			log.info("URL: {}", request.getRequestURL());
		}
		if (request != null) {
			log.info("Method: {}", request.getMethod());
		}
		if (request != null) {
			log.info("IP: {}", request.getRemoteAddr());
		}
		log.info("Class.Method: {}",
				point.getSignature().getDeclaringTypeName() + "." +
						point.getSignature().getName());
		// log.info("参数: {}", point.getArgs()); //

		// 执行方法
		Object result = point.proceed();

		// 打印执行耗时
		log.info("=== 请求结束，耗时: {}ms ===",
				System.currentTimeMillis() - startTime);

		return result;
	}

}
