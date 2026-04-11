package com.xz.match.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// note 生效类型以及生效时期,通用的,而且建议都这么用
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

	/**
	 * 角色权限
	 */
	String mustRole() default "";
}
