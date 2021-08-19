package com.jssh.netty.autoconfigure;

import com.jssh.netty.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(JsshNettyClientProperties.JSSH_NETTY_PREFIX)
public class JsshNettyClientProperties {

    public static final String JSSH_NETTY_PREFIX = "jssh.netty.client";

    private String host;
    private Integer port;

    @NestedConfigurationProperty
    private Configuration configuration = new Configuration();

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

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
