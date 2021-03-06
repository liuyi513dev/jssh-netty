package jssh.netty.rpc.core.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MarshallingProperties {

    String[] value() default {};

    String[] ignoreProperties() default {};

    String className() default "";
}
