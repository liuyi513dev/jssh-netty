package jssh.netty.rpc.autoconfigure;

import jssh.netty.rpc.core.serial.DefaultMessageSerialFactory;
import jssh.netty.rpc.core.serial.MessageSerialFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageSerialFactoryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MessageSerialFactory defaultMessageSerialFactory() {
        return new DefaultMessageSerialFactory();
    }
}
