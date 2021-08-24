package com.jssh.netty.autoconfigure;

import com.jssh.netty.serial.DefaultMessageSerialFactory;
import com.jssh.netty.serial.MessageSerialFactory;
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
