package com.jssh.netty.autoconfigure;

import com.jssh.netty.serial.DefaultFileMessageSerialFactory;
import com.jssh.netty.serial.FileMessageSerialFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileMessageSerialFactoryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FileMessageSerialFactory defaultFileMessageSerialFactory() {
        return new DefaultFileMessageSerialFactory();
    }
}
