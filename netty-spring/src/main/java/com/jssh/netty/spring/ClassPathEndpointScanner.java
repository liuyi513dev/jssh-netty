package com.jssh.netty.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

public class ClassPathEndpointScanner extends ClassPathBeanDefinitionScanner {

	private Map<String, Object> properties = new HashMap<>();
	
	private Class<?> factoryBean;

	public ClassPathEndpointScanner(BeanDefinitionRegistry registry) {
		super(registry, false);
	}

	@Override
	public Set<BeanDefinitionHolder> doScan(String... basePackages) {
		Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

		if (!beanDefinitions.isEmpty()) {
			processBeanDefinitions(beanDefinitions);
		}

		return beanDefinitions;
	}

	private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
		GenericBeanDefinition definition;
		for (BeanDefinitionHolder holder : beanDefinitions) {
			definition = (GenericBeanDefinition) holder.getBeanDefinition();

			definition.getConstructorArgumentValues().addGenericArgumentValue(definition.getBeanClassName());
			definition.setBeanClass(this.factoryBean);

			for (Entry<String, Object> entry : properties.entrySet()) {
				definition.getPropertyValues().add(entry.getKey(), entry.getValue());
			}

			definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
		}
	}

	public void addProperty(String key, Object value) {
		properties.put(key, value);
	}

	public Class<?> getFactoryBean() {
		return factoryBean;
	}

	public void setFactoryBean(Class<?> factoryBean) {
		this.factoryBean = factoryBean;
	}

	@Override
	protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
		return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
	}
}
