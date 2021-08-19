package com.jssh.netty.autoconfigure;

import com.jssh.netty.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(JsshNettyServerProperties.JSSH_NETTY_PREFIX)
public class JsshNettyServerProperties {

    public static final String JSSH_NETTY_PREFIX = "jssh.netty.server";

    private Integer port;

    @NestedConfigurationProperty
    private Configuration configuration = new Configuration();

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
