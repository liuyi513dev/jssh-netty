package jssh.netty.rpc.autoconfigure;

import jssh.netty.rpc.core.client.ClientInfoProvider;
import jssh.netty.rpc.core.client.ClientNettyListener;
import jssh.netty.rpc.core.client.DefaultClientNettyManager;
import jssh.netty.rpc.core.client.ServerSocketInfo;
import jssh.netty.rpc.core.serial.MessageSerialFactory;
import jssh.netty.rpc.spring.ActionScanner;
import jssh.netty.rpc.spring.ClientEndpointConfigurer;
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
@EnableConfigurationProperties(JsshNettyClientProperties.class)
@ConditionalOnProperty(prefix = JsshNettyClientProperties.JSSH_NETTY_PREFIX, name = "host")
public class JsshNettyClientAutoConfiguration {

    private final JsshNettyClientProperties properties;

    public JsshNettyClientAutoConfiguration(JsshNettyClientProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "client", destroyMethod = "close", initMethod = "init")
    @ConditionalOnMissingBean
    public DefaultClientNettyManager clientNettyManager(ClientInfoProvider clientInfoProvider, MessageSerialFactory messageSerialFactory, Optional<List<ClientNettyListener>> listeners) {
        DefaultClientNettyManager manager = new DefaultClientNettyManager();
        manager.setClientInfoProvider(clientInfoProvider);
        manager.setServerSocketInfo(new ServerSocketInfo(new InetSocketAddress(properties.getHost(), properties.getPort())));
        manager.setConfiguration(properties.getConfiguration());
        manager.setMessageSerialFactory(messageSerialFactory);
        listeners.ifPresent(manager::addListeners);
        return manager;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(DefaultClientNettyManager.class)
    public ActionScanner actionScanner(DefaultClientNettyManager nettyManager) {
        return new ActionScanner(nettyManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientInfoProvider clientInfoProvider() {
        return ctx -> null;
    }

    @Bean
    @ConditionalOnSingleCandidate(DefaultClientNettyManager.class)
    public ApplicationListener<ApplicationStartedEvent> startNetty(DefaultClientNettyManager nettyManager) {
        return e -> nettyManager.start();
    }

    @Configuration
    @Import(AutoConfiguredClientEndpointRegistrar.class)
    @ConditionalOnMissingBean(ClientEndpointConfigurer.class)
    static class EnableClientEndpointConfiguration {
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
            builder.addPropertyValue("clientBeanName", "client");
            registry.registerBeanDefinition(ClientEndpointConfigurer.class.getName(), builder.getBeanDefinition());
        }

        @Override
        public void setBeanFactory(BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

    }
}
