package com.xz.match.aop;

import com.xz.match.annotation.AuthCheck;
import com.xz.match.common.ErrorCode;
import com.xz.match.common.UserRoleEnum;
import com.xz.match.model.entity.User;
import com.xz.match.exception.MyCustomException;
import com.xz.match.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {
	@Resource
	private UserService userService;

	@Pointcut("@annotation(com.xz.match.annotation.AuthCheck)")
	public void authPointcut() {}

	@Around("authPointcut() && @annotation(authCheck)")
	public Object doIntercept(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
		String mustRole = authCheck.mustRole();

		// think 因为我要校验用户,所以我需要获取request,但我不是控制层,所以只能用全局处理器获取
		RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
		// feat 将普通的request转为servlet的request
		HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

		// feat 获取当前登录用户 并且获取用户权限
		User loginUser = userService.getLoginUser(request);
		UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

		// feat 如果不需要权限,放行
		// think 这里的不需要权限,是因为mustRole是注解的值,注解会放在方法上
		// think 相当于方法有了属性,如果方法设置的role为null,则谁都可以使用
		if (mustRoleEnum == null) {
			return joinPoint.proceed();
		}
		UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getRole());
		if (userRoleEnum == null) {
			throw new MyCustomException(ErrorCode.N0T_L0GIN);
		}

		// feat 这个方法设置了管理员权限 && 但是用户不是管理员 (排除错的,就是对的
		if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && ! UserRoleEnum.ADMIN.equals(userRoleEnum)) {
			throw new MyCustomException(ErrorCode.NO_AUTH);
		}

		return joinPoint.proceed();
	}

}
