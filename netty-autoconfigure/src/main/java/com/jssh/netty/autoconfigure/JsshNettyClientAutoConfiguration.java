package com.jssh.netty.autoconfigure;

import com.jssh.netty.client.ClientInfoProvider;
import com.jssh.netty.client.DefaultClientNettyManager;
import com.jssh.netty.serial.FileMessageSerialFactory;
import com.jssh.netty.spring.ActionScanner;
import com.jssh.netty.spring.ClientEndpointConfigurer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.util.List;

@Configuration
@EnableConfigurationProperties(JsshNettyClientProperties.class)
@ConditionalOnProperty(prefix = JsshNettyClientProperties.JSSH_NETTY_PREFIX, name = "host")
public class JsshNettyClientAutoConfiguration {

    private final JsshNettyClientProperties properties;

    public JsshNettyClientAutoConfiguration(JsshNettyClientProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "client", destroyMethod = "close")
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(ClientInfoProvider.class)
    public DefaultClientNettyManager clientNettyManager(ClientInfoProvider clientInfoProvider, FileMessageSerialFactory fileMessageSerialFactory) {
        DefaultClientNettyManager manager = new DefaultClientNettyManager();
        manager.setClientInfoProvider(clientInfoProvider);
        manager.setTcpPort(new InetSocketAddress(properties.getHost(), properties.getPort()));
        manager.setConfiguration(properties.getConfiguration());
        manager.setFileMessageSerialFactory(fileMessageSerialFactory);
        return manager;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(DefaultClientNettyManager.class)
    public ActionScanner actionScanner(DefaultClientNettyManager nettyManager) {
        return new ActionScanner(nettyManager);
    }

    public static class AutoConfiguredClientEndpointRegistrar implements BeanFactoryAware, ImportBeanDefinitionRegistrar {

        private BeanFactory beanFactory;

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

            if (!AutoConfigurationPackages.has(this.beanFactory)) {
                return;
            }

            List<String> packages = AutoConfigurationPackages.get(this.beanFactory);
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ClientEndpointConfigurer.class);
            builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(packages));
            builder.addPropertyValue("baseBeanName", "client");
            registry.registerBeanDefinition(ClientEndpointConfigurer.class.getName(), builder.getBeanDefinition());
        }

        @Override
        public void setBeanFactory(BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

    }

    @Configuration
    @Import(AutoConfiguredClientEndpointRegistrar.class)
    @ConditionalOnMissingBean(ClientEndpointConfigurer.class)
    static class EnableClientEndpointConfiguration {


    }
}
