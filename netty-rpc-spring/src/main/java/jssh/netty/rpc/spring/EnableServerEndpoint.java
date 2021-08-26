package jssh.netty.rpc.spring;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(ServerEndpointRegistrar.class)
public @interface EnableServerEndpoint {

    String[] value() default {};

    String[] basePackages() default {};

    String serverBeanName() default "server";
}
