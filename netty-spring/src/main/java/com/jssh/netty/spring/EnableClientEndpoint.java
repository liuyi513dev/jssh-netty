package com.jssh.netty.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Import(ClientEndpointRegistrar.class)
public @interface EnableClientEndpoint {

	String[] value() default {};

	String[] basePackages() default {};
	
	String clientBeanName() default "client";
}
