package com.jssh.netty.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Import(ServerEndpointRegistrar.class)
public @interface EnableServerEndpoint {

	String[] value() default {};

	String[] basePackages() default {};
	
	String serverBeanName() default "server";
}
