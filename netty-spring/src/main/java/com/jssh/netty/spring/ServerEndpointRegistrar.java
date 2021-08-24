package com.jssh.netty.spring;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ServerEndpointRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {

    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ServerEndpointConfigurer.class);

        AnnotationAttributes attributes = AnnotationAttributes
                .fromMap(importingClassMetadata.getAnnotationAttributes(EnableServerEndpoint.class.getName()));

        List<String> basePackages = new ArrayList<String>();
        for (String pkg : attributes.getStringArray("value")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (String pkg : attributes.getStringArray("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }

        String beanName = attributes.getString("serverBeanName");
        builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(basePackages));
        builder.addPropertyValue("serverBeanName", beanName);
        registry.registerBeanDefinition(ServerEndpointConfigurer.class.getName(), builder.getBeanDefinition());
    }
}
