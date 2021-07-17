package com.jssh.netty.spring;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;

public class ClientEndpointRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {

	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		AnnotationAttributes annoAttrs = AnnotationAttributes
				.fromMap(importingClassMetadata.getAnnotationAttributes(EnableClientEndpoint.class.getName()));
		ClassPathEndpointScanner scanner = new ClassPathEndpointScanner(registry);

		List<String> basePackages = new ArrayList<String>();
		for (String pkg : annoAttrs.getStringArray("value")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (String pkg : annoAttrs.getStringArray("basePackages")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		
		scanner.setBeanClass(ClientEndpointFactoryBean.class);
		scanner.addProperty("client", new RuntimeBeanReference(annoAttrs.getString("clientBeanName")));
		scanner.addIncludeFilter(new AnnotationTypeFilter(ClientEndpoint.class));
		scanner.scan(StringUtils.toStringArray(basePackages));
	}
}
