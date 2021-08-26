package jssh.netty.rpc.autoconfigure;

import jssh.netty.rpc.core.serial.MessageSerialFactory;
import jssh.netty.rpc.core.server.*;
import jssh.netty.rpc.spring.ActionScanner;
import jssh.netty.rpc.spring.ServerEndpointConfigurer;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties(JsshNettyServerProperties.class)
@ConditionalOnProperty(prefix = JsshNettyServerProperties.JSSH_NETTY_PREFIX, name = "port")
public class JsshNettyServerAutoConfiguration {

    private final JsshNettyServerProperties properties;

    public JsshNettyServerAutoConfiguration(JsshNettyServerProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "server", destroyMethod = "close", initMethod = "init")
    @ConditionalOnMissingBean
    public DefaultServerNettyManager serverNettyManager(ClientValidator clientValidator, MessageSerialFactory messageSerialFactory, Optional<List<ServerNettyListener>> listeners) {
        DefaultServerNettyManager manager = new DefaultServerNettyManager();
        manager.setClientValidator(clientValidator);
        manager.setServerBindInfo(new ServerBindInfo(new InetSocketAddress(properties.getPort())));
        manager.setConfiguration(properties.getConfiguration());
        manager.setMessageSerialFactory(messageSerialFactory);
        listeners.ifPresent(manager::addListeners);
        return manager;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(DefaultServerNettyManager.class)
    public ActionScanner actionScanner(DefaultServerNettyManager nettyManager) {
        return new ActionScanner(nettyManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientValidator clientValidator() {
        return (ctx, param) -> new SimpleClientInfo<>(ctx.channel().id().asShortText());
    }

    @Bean
    @ConditionalOnSingleCandidate(DefaultServerNettyManager.class)
    public ApplicationListener<ApplicationStartedEvent> startNetty(DefaultServerNettyManager nettyManager) {
        return e -> nettyManager.start();
    }

    @Configuration
    @Import(AutoConfiguredServerEndpointRegistrar.class)
    @ConditionalOnMissingBean(ServerEndpointConfigurer.class)
    static class EnableServerEndpointConfiguration {
    }

    public static class AutoConfiguredServerEndpointRegistrar implements BeanFactoryAware, ImportBeanDefinitionRegistrar {

        private BeanFactory beanFactory;

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

            if (!AutoConfigurationPackages.has(this.beanFactory)) {
                return;
            }

            List<String> packages = AutoConfigurationPackages.get(this.beanFactory);
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ServerEndpointConfigurer.class);
            builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(packages));
            builder.addPropertyValue("serverBeanName", "server");
            registry.registerBeanDefinition(ServerEndpointConfigurer.class.getName(), builder.getBeanDefinition());
        }

        @Override
        public void setBeanFactory(BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

    }
}
